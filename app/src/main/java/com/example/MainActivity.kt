package com.example

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.BrowserTopBar
import com.example.ui.components.FindInPageBar
import com.example.ui.components.QrCodeDialog
import com.example.ui.components.SslCertificateDialog
import com.example.ui.screens.BookmarksHistoryScreen
import com.example.ui.screens.BrowserWebViewContainer
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.EngineAuditScreen
import com.example.ui.screens.ExtensionDetailDialog
import com.example.ui.screens.ExtensionDiscoveryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PasswordsScreen
import com.example.ui.screens.PrivacyDashboardScreen
import com.example.ui.screens.ReaderModeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.theme.NovaBrowserTheme
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaOrange
import com.example.ui.theme.NovaShieldGreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel.tabManager.attachActivityContext(this)

        // Handle incoming VIEW intent with URL if opened externally
        intent?.dataString?.let { incomingUrl ->
            if (incomingUrl.isNotBlank()) {
                viewModel.loadUrl(incomingUrl)
            }
        }

        setContent {
            val settings by viewModel.browserSettings.collectAsState()
            val isIncognito by viewModel.tabManager.isIncognitoMode.collectAsState()

            NovaBrowserTheme(
                themeMode = settings.themeMode,
                isIncognito = isIncognito,
                dynamicColor = false
            ) {
                MainBrowserApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainBrowserApp(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(context) {
        viewModel.tabManager.attachActivityContext(context)
    }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeNavState by viewModel.activeNavState.collectAsState()
    val omniboxText by viewModel.omniboxText.collectAsState()
    val settings by viewModel.browserSettings.collectAsState()

    val regularTabs by viewModel.tabManager.regularTabs.collectAsState()
    val incognitoTabs by viewModel.tabManager.incognitoTabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val isIncognito by viewModel.tabManager.isIncognitoMode.collectAsState()
    val recentlyClosed by viewModel.tabManager.recentlyClosedTabs.collectAsState()

    val adBlockStats by viewModel.adBlockEngine.stats.collectAsState()
    val protectionMode by viewModel.adBlockEngine.protectionMode.collectAsState()
    val recentBlockLogs by viewModel.adBlockEngine.logsFlow.collectAsState()

    val installedExtensions by viewModel.extensionManager.installedExtensions.collectAsState()
    val bookmarks by viewModel.bookmarksFlow.collectAsState()
    val history by viewModel.historyFlow.collectAsState()
    val downloads by viewModel.downloadsFlow.collectAsState()
    val vaultCredentials by viewModel.vaultFlow.collectAsState()

    val fullscreenView by viewModel.fullscreenCustomView.collectAsState()
    val isFindVisible by viewModel.isFindInPageVisible.collectAsState()
    val findQuery by viewModel.findQuery.collectAsState()
    val findMatches by viewModel.findMatchCount.collectAsState()
    val findOrdinal by viewModel.findActiveOrdinal.collectAsState()

    val showSslDialog by viewModel.showSslDialog.collectAsState()
    val showQrDialog by viewModel.showQrDialog.collectAsState()
    val selectedExtDetail by viewModel.selectedExtensionForDetail.collectAsState()
    val readerArticle by viewModel.readerArticle.collectAsState()

    var showMenuDropdown by remember { mutableStateOf(false) }

    val activeTab = remember(activeTabId, regularTabs, incognitoTabs, isIncognito) {
        viewModel.tabManager.getActiveTab()
    }

    val activity = context as? Activity

    // Immersive System Bars and Landscape orientation handling for Fullscreen Video Playback
    androidx.compose.runtime.DisposableEffect(fullscreenView != null) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (fullscreenView != null) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                val controller = WindowCompat.getInsetsController(w, w.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Android Hardware / Gesture BackHandler:
    // When in fullscreen video, exit fullscreen directly. Otherwise navigate browser history.
    BackHandler(enabled = fullscreenView != null) {
        viewModel.exitFullscreen()
    }

    BackHandler(enabled = fullscreenView == null) {
        if (!viewModel.goBack()) {
            (context as? ComponentActivity)?.finish()
        }
    }

    if (fullscreenView != null) {
        // True Fullscreen Immersive Mode: Completely hides all browser chrome and expands edge-to-edge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = {
                    (fullscreenView?.parent as? ViewGroup)?.removeView(fullscreenView)
                    fullscreenView?.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    } ?: View(it)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            when (currentScreen) {
                BrowserScreen.BROWSER -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        BrowserTopBar(
                            navState = activeNavState,
                            omniboxText = omniboxText,
                            onOmniboxTextChanged = { viewModel.setOmniboxText(it) },
                            onSubmitUrl = { viewModel.loadUrl(it) },
                            onReload = { viewModel.reload() },
                            onStop = { viewModel.stopLoading() },
                            onTabCounterClick = { viewModel.navigateTo(BrowserScreen.TAB_SWITCHER) },
                            tabCount = if (isIncognito) incognitoTabs.size else regularTabs.size,
                            blockedCount = adBlockStats.totalBlockedCount,
                            isIncognito = isIncognito,
                            onShieldClick = { viewModel.navigateTo(BrowserScreen.PRIVACY_DASHBOARD) },
                            onExtensionsClick = { viewModel.navigateTo(BrowserScreen.EXTENSION_STORE) },
                            onSslLockClick = { viewModel.setShowSslDialog(true) },
                            onMenuClick = { showMenuDropdown = true }
                        )

                        // Three-Dot Overflow Menu
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Tab") },
                                leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.tabManager.openNewTab(isIncognito = false)
                                    viewModel.navigateTo(BrowserScreen.BROWSER)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Incognito Tab") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.tabManager.openNewTab(isIncognito = true)
                                    viewModel.navigateTo(BrowserScreen.BROWSER)
                                }
                            )
                            HorizontalDivider()

                            val isBookmarked = viewModel.isCurrentPageBookmarked()
                            DropdownMenuItem(
                                text = { Text(if (isBookmarked) "Bookmarked" else "Bookmark Page") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isBookmarked) NovaOrange else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.toggleBookmarkCurrentPage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks & History") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(BrowserScreen.BOOKMARKS_HISTORY)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Downloads") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(BrowserScreen.DOWNLOADS)
                                }
                            )
                            HorizontalDivider()

                            val isDesktop = activeTab?.isDesktopMode == true
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text("Desktop Site", modifier = Modifier.weight(1f))
                                        Checkbox(checked = isDesktop, onCheckedChange = null)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.toggleDesktopMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Find in Page") },
                                leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.openFindInPage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reader Mode") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.enterReaderMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share / QR Code") },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.setShowQrDialog(true)
                                }
                            )
                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Password Vault") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(BrowserScreen.PASSWORDS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chrome Extensions") },
                                leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = NovaCyan) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(BrowserScreen.EXTENSION_STORE)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Privacy Shield") },
                                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = NovaShieldGreen) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(BrowserScreen.PRIVACY_DASHBOARD)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.navigateTo(BrowserScreen.SETTINGS)
                                }
                            )
                        }

                        // Find in Page Bar
                        if (isFindVisible) {
                            FindInPageBar(
                                query = findQuery,
                                onQueryChanged = { viewModel.onFindQueryChanged(it) },
                                matchCount = findMatches,
                                activeOrdinal = findOrdinal,
                                onNext = { viewModel.findNext(true) },
                                onPrevious = { viewModel.findNext(false) },
                                onClose = { viewModel.closeFindInPage() }
                            )
                        }

                        // Main Content: Home Screen or Web View
                        val currentUrl = activeNavState.currentUrl
                        Box(modifier = Modifier.weight(1f)) {
                            if (currentUrl == "about:blank" || currentUrl == "about:home" || currentUrl.isEmpty()) {
                                HomeScreen(
                                    searchEngine = settings.searchEngine,
                                    adBlockStats = adBlockStats,
                                    recentHistory = history,
                                    bookmarks = bookmarks,
                                    onNavigateUrl = { viewModel.loadUrl(it) },
                                    onOpenPrivacyDashboard = { viewModel.navigateTo(BrowserScreen.PRIVACY_DASHBOARD) },
                                    onOpenBookmarks = { viewModel.navigateTo(BrowserScreen.BOOKMARKS_HISTORY) },
                                    onOpenHistory = { viewModel.navigateTo(BrowserScreen.BOOKMARKS_HISTORY) },
                                    isIncognito = isIncognito
                                )
                            } else {
                                BrowserWebViewContainer(
                                    activeTab = activeTab,
                                    tabManager = viewModel.tabManager
                                )
                            }
                        }

                        // Bottom Navigation Bar
                        BrowserBottomBar(
                            canGoBack = activeNavState.canGoBack,
                            canGoForward = activeNavState.canGoForward,
                            onBack = { viewModel.goBack() },
                            onForward = { viewModel.goForward() },
                            onHome = { viewModel.loadUrl("about:blank") },
                            onTabs = { viewModel.navigateTo(BrowserScreen.TAB_SWITCHER) },
                            onMenu = { showMenuDropdown = true }
                        )
                    }
                }

                BrowserScreen.TAB_SWITCHER -> {
                    TabSwitcherScreen(
                        regularTabs = regularTabs,
                        incognitoTabs = incognitoTabs,
                        activeTabId = activeTabId,
                        isIncognito = isIncognito,
                        recentlyClosed = recentlyClosed,
                        onSelectTab = { viewModel.tabManager.selectTab(it) },
                        onCloseTab = { viewModel.tabManager.closeTab(it) },
                        onNewTab = { incog -> viewModel.tabManager.openNewTab(isIncognito = incog) },
                        onDuplicateTab = { viewModel.tabManager.duplicateTab(it) },
                        onRestoreTab = { viewModel.tabManager.restoreLastClosedTab() },
                        onCloseAll = { incogOnly -> viewModel.tabManager.closeAllTabs(incogOnly) },
                        onToggleIncognitoMode = { viewModel.tabManager.setIncognitoMode(it) },
                        onBackToBrowser = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.EXTENSION_STORE -> {
                    ExtensionDiscoveryScreen(
                        installedExtensions = installedExtensions,
                        onInstallListing = { viewModel.extensionManager.installFromCatalog(it) },
                        onUninstallExtension = { viewModel.extensionManager.uninstallExtension(it) },
                        onToggleExtension = { id, enabled -> viewModel.extensionManager.setExtensionEnabled(id, enabled) },
                        onInstallCustomManifest = { viewModel.extensionManager.installFromCustomJson(it) },
                        onInspectInstalled = { viewModel.setSelectedExtensionForDetail(it) },
                        onBack = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.PRIVACY_DASHBOARD -> {
                    val currentDomain = viewModel.adBlockEngine.extractDomain(activeNavState.currentUrl)
                    val isProtected = viewModel.adBlockEngine.isSiteProtectionEnabled(currentDomain)

                    PrivacyDashboardScreen(
                        stats = adBlockStats,
                        protectionMode = protectionMode,
                        onProtectionModeChanged = { viewModel.adBlockEngine.setProtectionMode(it) },
                        currentSiteDomain = currentDomain,
                        isSiteProtected = isProtected,
                        onToggleSiteProtection = { viewModel.adBlockEngine.toggleSiteProtection(currentDomain) },
                        logs = recentBlockLogs,
                        onClearStats = { viewModel.adBlockEngine.clearStats() },
                        onBack = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.BOOKMARKS_HISTORY -> {
                    BookmarksHistoryScreen(
                        bookmarks = bookmarks,
                        history = history,
                        initialTab = 0,
                        onNavigateUrl = {
                            viewModel.loadUrl(it)
                            viewModel.navigateTo(BrowserScreen.BROWSER)
                        },
                        onDeleteBookmark = { bm ->
                            scope.launch { viewModel.db.bookmarkDao().deleteBookmark(bm) }
                        },
                        onAddBookmark = { title, url, folder ->
                            scope.launch { viewModel.db.bookmarkDao().insertBookmark(com.example.data.db.BookmarkEntity(title = title, url = url, folder = folder)) }
                        },
                        onDeleteHistoryItem = { item ->
                            scope.launch { viewModel.db.historyDao().deleteHistory(item) }
                        },
                        onClearAllHistory = {
                            scope.launch { viewModel.db.historyDao().clearAll() }
                        },
                        onBack = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.DOWNLOADS -> {
                    DownloadsScreen(
                        downloads = downloads,
                        onOpenFile = { viewModel.downloadManagerService.openDownloadedFile(it) },
                        onShareFile = { viewModel.downloadManagerService.shareDownloadedFile(it) },
                        onDeleteFile = { viewModel.downloadManagerService.deleteDownload(it) },
                        onBack = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.PASSWORDS -> {
                    PasswordsScreen(
                        credentials = vaultCredentials,
                        vaultManager = viewModel.secureVaultManager,
                        onSaveCredential = { domain, user, pass ->
                            val encrypted = viewModel.secureVaultManager.encryptPassword(pass)
                            scope.launch {
                                viewModel.db.vaultDao().insertCredential(
                                    com.example.data.db.VaultEntity(
                                        siteDomain = domain,
                                        username = user,
                                        encryptedPasswordBase64 = encrypted.ciphertextBase64,
                                        initializationVectorBase64 = encrypted.ivBase64
                                    )
                                )
                            }
                        },
                        onDeleteCredential = { item ->
                            scope.launch { viewModel.db.vaultDao().deleteCredential(item) }
                        },
                        onBack = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.READER_MODE -> {
                    val article = readerArticle
                    if (article != null) {
                        ReaderModeScreen(
                            article = article,
                            onExit = { viewModel.exitReaderMode() }
                        )
                    } else {
                        viewModel.exitReaderMode()
                    }
                }

                BrowserScreen.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onSetSearchEngine = { engine -> scope.launch { viewModel.preferences.setSearchEngine(engine) } },
                        onSetThemeMode = { mode -> scope.launch { viewModel.preferences.setThemeMode(mode) } },
                        onSetDefaultDesktopMode = { def -> scope.launch { viewModel.preferences.setDefaultDesktopMode(def) } },
                        onSetHttpsOnlyMode = { https -> scope.launch { viewModel.preferences.setHttpsOnlyMode(https) } },
                        onOpenPrivacyDashboard = { viewModel.navigateTo(BrowserScreen.PRIVACY_DASHBOARD) },
                        onOpenPasswords = { viewModel.navigateTo(BrowserScreen.PASSWORDS) },
                        onOpenExtensions = { viewModel.navigateTo(BrowserScreen.EXTENSION_STORE) },
                        onOpenDiagnostics = { viewModel.navigateTo(BrowserScreen.ENGINE_DIAGNOSTICS) },
                        onClearBrowsingData = { hist, cook, cache, tabs ->
                            viewModel.clearBrowsingData(hist, cook, cache, tabs)
                        },
                        onBack = { viewModel.navigateTo(BrowserScreen.BROWSER) }
                    )
                }

                BrowserScreen.ENGINE_DIAGNOSTICS -> {
                    EngineAuditScreen(
                        onBack = { viewModel.navigateTo(BrowserScreen.SETTINGS) }
                    )
                }
            }

            // Global Dialogs
            if (showSslDialog) {
                SslCertificateDialog(
                    sslInfo = activeNavState.sslInfo,
                    url = activeNavState.currentUrl,
                    onDismiss = { viewModel.setShowSslDialog(false) }
                )
            }

            if (showQrDialog) {
                QrCodeDialog(
                    url = activeNavState.currentUrl,
                    onDismiss = { viewModel.setShowQrDialog(false) }
                )
            }

            selectedExtDetail?.let { ext ->
                ExtensionDetailDialog(
                    extension = ext,
                    onToggleEnabled = { enabled ->
                        viewModel.extensionManager.setExtensionEnabled(ext.id, enabled)
                        viewModel.setSelectedExtensionForDetail(ext.copy(isEnabled = enabled))
                    },
                    onUninstall = {
                        viewModel.extensionManager.uninstallExtension(ext.id)
                        viewModel.setSelectedExtensionForDetail(null)
                    },
                    onDismiss = { viewModel.setSelectedExtensionForDetail(null) }
                )
            }
        }
    }
}
}
