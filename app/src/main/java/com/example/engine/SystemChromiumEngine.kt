package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.adblock.AdBlockFilterEngine
import com.example.extensions.ExtensionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemChromiumEngine(
    context: Context,
    private val adBlockEngine: AdBlockFilterEngine,
    private val extensionManager: ExtensionManager,
    val isIncognito: Boolean = false,
    private val onFullscreenChanged: ((View?, WebChromeClient.CustomViewCallback?) -> Unit)? = null,
    private val onDownloadRequested: ((String, String, String, String, Long) -> Unit)? = null
) : IBrowserEngine {

    val webView: WebView = try {
        WebView(context)
    } catch (e: Throwable) {
        Log.w("SystemChromiumEngine", "Direct context WebView init failed, attempting applicationContext fallback: ${e.message}")
        WebView(context.applicationContext)
    }

    private val _navigationState = MutableStateFlow(WebNavigationState())
    override val navigationState: StateFlow<WebNavigationState> = _navigationState.asStateFlow()

    private val mobileUserAgent: String = runCatching { webView.settings.userAgentString }
        .getOrDefault("Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36")
    private val desktopUserAgent: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    init {
        configureWebSettings()
        setupClients()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebSettings() {
        try {
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = false
                allowContentAccess = false
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportMultipleWindows(false)
                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (isIncognito) {
                cookieManager.setAcceptThirdPartyCookies(webView, false)
            } else {
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
        } catch (e: Throwable) {
            Log.e("SystemChromiumEngine", "Error configuring WebSettings: ${e.message}", e)
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadRequested?.invoke(url, userAgent, contentDisposition, mimetype, contentLength)
        }
    }

    private fun setupClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val url = uri.toString()
                val scheme = uri.scheme?.lowercase() ?: ""

                // Standard web schemes handled by this Chromium WebView
                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "chrome") {
                    return false
                }

                // Handle external application intents safely (e.g. mailto:, tel:, market:, intent:)
                return try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    view?.context?.startActivity(intent)
                    true
                } catch (e: Throwable) {
                    Log.w("SystemChromiumEngine", "External scheme not handled for $url: ${e.message}")
                    true
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                // Main frame requests (the webpage itself or search results) must never be blocked!
                if (request == null || request.isForMainFrame) return null

                val url = request.url?.toString() ?: return null
                // CRITICAL THREAD SAFETY: Do NOT invoke view.url! shouldInterceptRequest runs on Chrome_IOThread.
                // Calling view.url asserts UI thread and throws RuntimeException.
                val pageUrl = _navigationState.value.currentUrl

                return try {
                    val blockedResponse = adBlockEngine.shouldIntercept(url, pageUrl)
                    blockedResponse ?: super.shouldInterceptRequest(view, request)
                } catch (e: Throwable) {
                    null
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val current = _navigationState.value
                _navigationState.value = current.copy(
                    currentUrl = url ?: "about:blank",
                    isLoading = true,
                    progress = 15,
                    errorMessage = null
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val current = _navigationState.value
                val sslInfo = extractSslInfo(view, url)

                _navigationState.value = current.copy(
                    currentUrl = url ?: current.currentUrl,
                    title = view?.title ?: current.title,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false,
                    isLoading = false,
                    progress = 100,
                    sslInfo = sslInfo
                )

                // Inject enabled Chrome Extensions & Content Scripts
                if (view != null && url != null && !url.startsWith("chrome://") && !url.startsWith("about:")) {
                    runCatching {
                        extensionManager.applyActiveExtensions(view, url)
                    }.onFailure {
                        Log.w("SystemChromiumEngine", "Extension injection notice: ${it.message}")
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val current = _navigationState.value
                    _navigationState.value = current.copy(
                        isLoading = false,
                        errorMessage = error?.description?.toString() ?: "Failed to load webpage"
                    )
                }
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                val current = _navigationState.value
                if (failingUrl == null || failingUrl == current.currentUrl) {
                    _navigationState.value = current.copy(
                        isLoading = false,
                        errorMessage = description ?: "Failed to load webpage ($errorCode)"
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Strict Security: Cancel on unverified SSL certificates
                handler?.cancel()
                val current = _navigationState.value
                _navigationState.value = current.copy(
                    isLoading = false,
                    errorMessage = "SSL Certificate Security Error: The connection is not secure."
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                val current = _navigationState.value
                _navigationState.value = current.copy(
                    progress = newProgress,
                    isLoading = newProgress < 100
                )
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                val current = _navigationState.value
                _navigationState.value = current.copy(
                    title = title ?: current.title,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                val current = _navigationState.value
                _navigationState.value = current.copy(favicon = icon)
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                onFullscreenChanged?.invoke(view, callback)
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                onFullscreenChanged?.invoke(null, null)
            }
        }
    }

    private fun extractSslInfo(view: WebView?, currentUrl: String?): SslInfo? {
        val cert = view?.certificate ?: return null
        val pageUrl = currentUrl ?: _navigationState.value.currentUrl
        val isHttps = pageUrl.startsWith("https://")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val validUntil = runCatching { dateFormat.format(cert.validNotAfterDate) }.getOrDefault("Unknown")

        return SslInfo(
            isSecure = isHttps,
            issuedTo = cert.issuedTo?.cName ?: cert.issuedTo?.dName ?: "Verified Host",
            issuedBy = cert.issuedBy?.oName ?: cert.issuedBy?.cName ?: "Authorized CA",
            validUntil = validUntil
        )
    }

    override fun getView(): View = webView

    override fun loadUrl(url: String) {
        val formatted = formatUrl(url)
        webView.loadUrl(formatted)
    }

    private fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:") || trimmed.startsWith("chrome://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.startsWith("?")) {
            return "https://$trimmed"
        }
        return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }

    override fun goBack() {
        if (webView.canGoBack()) webView.goBack()
    }

    override fun goForward() {
        if (webView.canGoForward()) webView.goForward()
    }

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun setDesktopMode(enabled: Boolean) {
        webView.settings.userAgentString = if (enabled) desktopUserAgent else mobileUserAgent
        webView.settings.useWideViewPort = enabled
        _navigationState.value = _navigationState.value.copy(isDesktopMode = enabled)
        webView.reload()
    }

    override fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }

    override fun findInPage(query: String, forward: Boolean) {
        if (query.isEmpty()) {
            webView.clearMatches()
        } else {
            webView.findNext(forward)
        }
    }

    fun startFindInPage(query: String, onMatchesFound: (Int, Int) -> Unit) {
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            onMatchesFound(activeMatchOrdinal + 1, numberOfMatches)
        }
        webView.findAllAsync(query)
    }

    override fun clearMatches() {
        webView.clearMatches()
    }

    override fun clearCache() {
        webView.clearCache(true)
    }

    override fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    override fun onPause() {
        webView.onPause()
    }

    override fun onResume() {
        webView.onResume()
    }

    override fun onDestroy() {
        try {
            webView.stopLoading()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        } catch (e: Throwable) {
            Log.e("SystemChromiumEngine", "Error destroying WebView: ${e.message}", e)
        }
    }
}
