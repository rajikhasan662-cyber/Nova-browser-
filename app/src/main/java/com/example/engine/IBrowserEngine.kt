package com.example.engine

import android.view.View
import kotlinx.coroutines.flow.StateFlow

data class SslInfo(
    val isSecure: Boolean,
    val issuedTo: String,
    val issuedBy: String,
    val validUntil: String,
    val protocol: String = "TLS 1.3 / HTTPS"
)

data class WebNavigationState(
    val currentUrl: String = "about:blank",
    val title: String = "New Tab",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val favicon: android.graphics.Bitmap? = null,
    val isDesktopMode: Boolean = false,
    val sslInfo: SslInfo? = null,
    val errorMessage: String? = null
)

interface IBrowserEngine {
    val navigationState: StateFlow<WebNavigationState>
    fun getView(): View
    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun reload()
    fun stopLoading()
    fun setDesktopMode(enabled: Boolean)
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null)
    fun findInPage(query: String, forward: Boolean)
    fun clearMatches()
    fun clearCache()
    fun clearCookies()
    fun onPause()
    fun onResume()
    fun onDestroy()
}
