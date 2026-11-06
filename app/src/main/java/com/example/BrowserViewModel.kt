package com.example

import android.app.Application
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adblock.ProtectionMode
import com.example.data.db.BookmarkEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.VaultEntity
import com.example.downloads.DownloadManagerService
import com.example.engine.ReaderArticle
import com.example.engine.ReaderModeExtractor
import com.example.engine.WebNavigationState
import com.example.extensions.ExtensionDiscoveryRepository
import com.example.extensions.InstalledExtension
import com.example.settings.BrowserSettings
import com.example.settings.SearchEngine
import com.example.tabs.BrowserTab
import com.example.tabs.TabManager
import com.example.ui.theme.BrowserThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BrowserScreen {
    BROWSER,
    TAB_SWITCHER,
    EXTENSION_STORE,
    PRIVACY_DASHBOARD,
    BOOKMARKS_HISTORY,
    DOWNLOADS,
    PASSWORDS,
    READER_MODE,
    SETTINGS,
    ENGINE_DIAGNOSTICS
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NovaApplication
    val db = app.database
    val adBlockEngine = app.adBlockEngine
    val extensionManager = app.extensionManager
    val secureVaultManager = app.secureVaultManager
    val preferences = app.browserPreferences

    val downloadManagerService = DownloadManagerService(app, db.downloadDao(), viewModelScope)

    // Current Screen
    private val _currentScreen = MutableStateFlow(BrowserScreen.BROWSER)
    val currentScreen: StateFlow<BrowserScreen> = _currentScreen.asStateFlow()

    // Fullscreen View for Video Playback
    private val _fullscreenCustomView = MutableStateFlow<View?>(null)
    val fullscreenCustomView: StateFlow<View?> = _fullscreenCustomView.asStateFlow()
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    // Find In Page
    private val _isFindInPageVisible = MutableStateFlow(false)
    val isFindInPageVisible: StateFlow<Boolean> = _isFindInPageVisible.asStateFlow()
    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()
    private val _findMatchCount = MutableStateFlow(0)
    val findMatchCount: StateFlow<Int> = _findMatchCount.asStateFlow()
    private val _findActiveOrdinal = MutableStateFlow(0)
    val findActiveOrdinal: StateFlow<Int> = _findActiveOrdinal.asStateFlow()

    // SSL Certificate Dialog
    private val _showSslDialog = MutableStateFlow(false)
    val showSslDialog: StateFlow<Boolean> = _showSslDialog.asStateFlow()

    // QR Code Dialog
    private val _showQrDialog = MutableStateFlow(false)
    val showQrDialog: StateFlow<Boolean> = _showQrDialog.asStateFlow()

    // Video Diagnostics & Assistant Dialog
    private val _showVideoDiagnosticsDialog = MutableStateFlow(false)
    val showVideoDiagnosticsDialog: StateFlow<Boolean> = _showVideoDiagnosticsDialog.asStateFlow()

    fun setShowVideoDiagnosticsDialog(show: Boolean) {
        _showVideoDiagnosticsDialog.value = show
    }

    fun forceVideoFullscreen() {
        val active = tabManager.getActiveTab()
        (active?.engine as? com.example.engine.SystemChromiumEngine)?.triggerVideoFullscreen()
    }

    fun setVideoPlaybackQuality(quality: String) {
        val active = tabManager.getActiveTab()
        (active?.engine as? com.example.engine.SystemChromiumEngine)?.setVideoQuality(quality)
    }

    fun queryVideoDiagnostics(callback: (String) -> Unit) {
        val active = tabManager.getActiveTab()
        (active?.engine as? com.example.engine.SystemChromiumEngine)?.getVideoDiagnostics(callback)
    }

    // Reader Mode State
    private val _readerArticle = MutableStateFlow<ReaderArticle?>(null)
    val readerArticle: StateFlow<ReaderArticle?> = _readerArticle.asStateFlow()

    // Extension Detail Dialog
    private val _selectedExtensionForDetail = MutableStateFlow<InstalledExtension?>(null)
    val selectedExtensionForDetail: StateFlow<InstalledExtension?> = _selectedExtensionForDetail.asStateFlow()

    // Selected Extension Listing For Preview
    private val _selectedCatalogListing = MutableStateFlow<ExtensionDiscoveryRepository.ExtensionListing?>(null)
    val selectedCatalogListing: StateFlow<ExtensionDiscoveryRepository.ExtensionListing?> = _selectedCatalogListing.asStateFlow()

    // Search / Omnibox text
    private val _omniboxText = MutableStateFlow("")
    val omniboxText: StateFlow<String> = _omniboxText.asStateFlow()
    private val _isOmniboxFocused = MutableStateFlow(false)
    val isOmniboxFocused: StateFlow<Boolean> = _isOmniboxFocused.asStateFlow()

    // Settings
    val browserSettings: StateFlow<BrowserSettings> = preferences.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BrowserSettings()
    )

    // Tab Manager
    val tabManager = TabManager(
        context = app,
        adBlockEngine = adBlockEngine,
        extensionManager = extensionManager,
        scope = viewModelScope,
        onFullscreenChanged = { view, callback ->
            _fullscreenCustomView.value = view
            fullscreenCallback = callback
        },
        onDownloadRequested = { url, userAgent, contentDisposition, mimetype, contentLength ->
            downloadManagerService.enqueueDownload(url, userAgent, contentDisposition, mimetype, contentLength)
        }
    )

    // Active Navigation State (updated from active tab engine)
    private val _activeNavState = MutableStateFlow(WebNavigationState())
    val activeNavState: StateFlow<WebNavigationState> = _activeNavState.asStateFlow()

    // History & Bookmarks flows
    val bookmarksFlow = db.bookmarkDao().getAllBookmarks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val historyFlow = db.historyDao().getAllHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val downloadsFlow = db.downloadDao().getAllDownloads().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val vaultFlow = db.vaultDao().getAllCredentials().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Observe active tab navigation state
        viewModelScope.launch {
            tabManager.activeTabId.collect {
                updateActiveTabState()
            }
        }
    }

    private var activeTabJob: kotlinx.coroutines.Job? = null

    fun updateActiveTabState() {
        activeTabJob?.cancel()
        val active = tabManager.getActiveTab()
        val engine = active?.engine
        if (engine != null) {
            activeTabJob = viewModelScope.launch {
                engine.navigationState.collect { navState ->
                    _activeNavState.value = navState
                    if (!_isOmniboxFocused.value) {
                        _omniboxText.value = if (navState.currentUrl == "about:blank" || navState.currentUrl == "about:home") "" else navState.currentUrl
                    }

                    // Save to history if valid URL and not incognito
                    if (!navState.isLoading && navState.currentUrl.startsWith("http") && active.isIncognito.not()) {
                        saveHistoryEntry(navState.title, navState.currentUrl)
                    }
                }
            }
        } else {
            _activeNavState.value = WebNavigationState()
        }
    }

    private fun saveHistoryEntry(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.historyDao().getByUrl(url)
            if (existing != null) {
                db.historyDao().insertHistory(
                    existing.copy(
                        title = if (title.isNotEmpty()) title else existing.title,
                        visitTime = System.currentTimeMillis(),
                        visitCount = existing.visitCount + 1
                    )
                )
            } else {
                db.historyDao().insertHistory(
                    HistoryEntity(title = if (title.isNotEmpty()) title else url, url = url)
                )
            }
        }
    }

    fun navigateTo(screen: BrowserScreen) {
        _currentScreen.value = screen
    }

    fun loadUrl(urlInput: String) {
        val active = tabManager.getActiveTab() ?: tabManager.openNewTab()
        val formatted = formatSearchOrUrl(urlInput)
        active.url = formatted
        if (formatted == "about:blank" || formatted == "about:home") {
            active.engine?.loadUrl(formatted)
            _activeNavState.value = WebNavigationState(currentUrl = formatted, title = "New Tab")
        } else {
            val engine = tabManager.ensureEngineForTab(active)
            _activeNavState.value = _activeNavState.value.copy(
                currentUrl = formatted,
                isLoading = true,
                progress = 15,
                errorMessage = null
            )
            _omniboxText.value = formatted
            updateActiveTabState()
            engine.loadUrl(formatted)
        }
        _isOmniboxFocused.value = false
        _currentScreen.value = BrowserScreen.BROWSER
    }

    private fun formatSearchOrUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:") || trimmed.startsWith("chrome://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.startsWith("?")) {
            return "https://$trimmed"
        }
        val currentEngine = browserSettings.value.searchEngine
        return currentEngine.searchUrl + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }

    fun goBack(): Boolean {
        if (_currentScreen.value != BrowserScreen.BROWSER) {
            _currentScreen.value = BrowserScreen.BROWSER
            return true
        }
        if (_fullscreenCustomView.value != null) {
            exitFullscreen()
            return true
        }
        if (_isFindInPageVisible.value) {
            closeFindInPage()
            return true
        }
        val active = tabManager.getActiveTab()
        if (active?.engine?.navigationState?.value?.canGoBack == true) {
            active.engine?.goBack()
            return true
        }
        return false
    }

    fun goForward() {
        tabManager.getActiveTab()?.engine?.goForward()
    }

    fun reload() {
        tabManager.getActiveTab()?.engine?.reload()
    }

    fun stopLoading() {
        tabManager.getActiveTab()?.engine?.stopLoading()
    }

    fun toggleDesktopMode() {
        val active = tabManager.getActiveTab() ?: return
        tabManager.toggleDesktopModeForTab(active.id)
    }

    fun openFindInPage() {
        _isFindInPageVisible.value = true
    }

    fun closeFindInPage() {
        _isFindInPageVisible.value = false
        _findQuery.value = ""
        tabManager.getActiveTab()?.engine?.clearMatches()
    }

    fun onFindQueryChanged(query: String) {
        _findQuery.value = query
        val active = tabManager.getActiveTab()
        if (query.isEmpty()) {
            active?.engine?.clearMatches()
            _findMatchCount.value = 0
            _findActiveOrdinal.value = 0
        } else {
            active?.engine?.startFindInPage(query) { activeOrdinal, totalMatches ->
                _findActiveOrdinal.value = activeOrdinal
                _findMatchCount.value = totalMatches
            }
        }
    }

    fun findNext(forward: Boolean) {
        tabManager.getActiveTab()?.engine?.findInPage(_findQuery.value, forward)
    }

    fun enterReaderMode() {
        val active = tabManager.getActiveTab()?.engine?.webView ?: return
        ReaderModeExtractor.extractArticle(active) { article ->
            if (article != null && article.contentHtml.isNotEmpty()) {
                _readerArticle.value = article
                _currentScreen.value = BrowserScreen.READER_MODE
            }
        }
    }

    fun exitReaderMode() {
        _readerArticle.value = null
        _currentScreen.value = BrowserScreen.BROWSER
    }

    fun toggleBookmarkCurrentPage() {
        val nav = _activeNavState.value
        val url = nav.currentUrl
        if (url == "about:blank" || url.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.bookmarkDao().getBookmarkByUrl(url)
            if (existing != null) {
                db.bookmarkDao().deleteBookmark(existing)
            } else {
                db.bookmarkDao().insertBookmark(
                    BookmarkEntity(title = nav.title.ifEmpty { url }, url = url)
                )
            }
        }
    }

    fun isCurrentPageBookmarked(): Boolean {
        val url = _activeNavState.value.currentUrl
        return bookmarksFlow.value.any { it.url == url }
    }

    fun setOmniboxText(text: String) {
        _omniboxText.value = text
    }

    fun setOmniboxFocused(focused: Boolean) {
        _isOmniboxFocused.value = focused
    }

    fun setShowSslDialog(show: Boolean) {
        _showSslDialog.value = show
    }

    fun setShowQrDialog(show: Boolean) {
        _showQrDialog.value = show
    }

    fun setSelectedExtensionForDetail(ext: InstalledExtension?) {
        _selectedExtensionForDetail.value = ext
    }

    fun setSelectedCatalogListing(listing: ExtensionDiscoveryRepository.ExtensionListing?) {
        _selectedCatalogListing.value = listing
    }

    fun exitFullscreen() {
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        _fullscreenCustomView.value = null
    }

    fun clearBrowsingData(
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearTabs: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (clearHistory) db.historyDao().clearAll()
            if (clearTabs) tabManager.closeAllTabs(incognitoOnly = false)
            if (clearCookies) tabManager.getActiveTab()?.engine?.clearCookies()
            if (clearCache) tabManager.getActiveTab()?.engine?.clearCache()
        }
    }
}
