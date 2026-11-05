package com.example.tabs

import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import com.example.adblock.AdBlockFilterEngine
import com.example.data.db.NovaDatabase
import com.example.data.db.TabEntity
import com.example.engine.SystemChromiumEngine
import com.example.extensions.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TabManager(
    private val context: Context,
    private val adBlockEngine: AdBlockFilterEngine,
    private val extensionManager: ExtensionManager,
    private val scope: CoroutineScope,
    private val onFullscreenChanged: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    private val onDownloadRequested: (String, String, String, String, Long) -> Unit
) {

    private val tabDao = NovaDatabase.getInstance(context).tabDao()

    private val _regularTabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val regularTabs: StateFlow<List<BrowserTab>> = _regularTabs.asStateFlow()

    private val _incognitoTabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val incognitoTabs: StateFlow<List<BrowserTab>> = _incognitoTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    private val _recentlyClosedTabs = MutableStateFlow<List<ClosedTabRecord>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<ClosedTabRecord>> = _recentlyClosedTabs.asStateFlow()

    init {
        // Initialize default regular tab safely on Main dispatcher to prevent thread check crashes
        scope.launch(Dispatchers.Main) {
            val initialTab = createTabInternal(
                url = "about:blank",
                isIncognito = false,
                isDesktopMode = false
            )
            _regularTabs.value = listOf(initialTab)
            _activeTabId.value = initialTab.id
        }
    }

    private var activityContextRef: java.lang.ref.WeakReference<Context>? = null

    fun attachActivityContext(ctx: Context) {
        activityContextRef = java.lang.ref.WeakReference(ctx)
    }

    fun setIncognitoMode(active: Boolean) {
        _isIncognitoMode.value = active
        if (active) {
            if (_incognitoTabs.value.isEmpty()) {
                openNewTab("about:blank", isIncognito = true)
            } else {
                _activeTabId.value = _incognitoTabs.value.first().id
            }
        } else {
            if (_regularTabs.value.isEmpty()) {
                openNewTab("about:blank", isIncognito = false)
            } else {
                _activeTabId.value = _regularTabs.value.first().id
            }
        }
    }

    fun getActiveTab(): BrowserTab? {
        val currentList = if (_isIncognitoMode.value) _incognitoTabs.value else _regularTabs.value
        return currentList.firstOrNull { it.id == _activeTabId.value } ?: currentList.firstOrNull()
    }

    fun ensureEngineForTab(tab: BrowserTab, contextForView: Context? = null): SystemChromiumEngine {
        val existing = tab.engine
        if (existing != null) return existing

        val newEngine = createEngineSafely(tab.isIncognito, tab.isDesktopMode, contextForView)
        tab.engine = newEngine
        return newEngine
    }

    private fun createEngineSafely(
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        contextForView: Context? = null
    ): SystemChromiumEngine {
        val useCtx = contextForView ?: activityContextRef?.get() ?: context
        val engine = SystemChromiumEngine(
            context = useCtx,
            adBlockEngine = adBlockEngine,
            extensionManager = extensionManager,
            isIncognito = isIncognito,
            onFullscreenChanged = onFullscreenChanged,
            onDownloadRequested = onDownloadRequested
        )
        if (isDesktopMode) {
            engine.setDesktopMode(true)
        }
        return engine
    }

    fun openNewTab(
        url: String = "about:blank",
        isIncognito: Boolean = _isIncognitoMode.value,
        inBackground: Boolean = false,
        groupName: String? = null,
        groupColor: Long? = null,
        contextForView: Context? = null
    ): BrowserTab {
        val tab = createTabInternal(url, isIncognito, false, groupName, groupColor)

        if (url != "about:blank" && url != "about:home") {
            val engine = ensureEngineForTab(tab, contextForView)
            engine.loadUrl(url)
        }

        if (isIncognito) {
            _incognitoTabs.value = _incognitoTabs.value + tab
            if (!inBackground) {
                _isIncognitoMode.value = true
                _activeTabId.value = tab.id
            }
        } else {
            _regularTabs.value = _regularTabs.value + tab
            if (!inBackground) {
                _isIncognitoMode.value = false
                _activeTabId.value = tab.id
            }
            persistRegularTabs()
        }

        return tab
    }

    private fun createTabInternal(
        url: String,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        groupName: String? = null,
        groupColor: Long? = null
    ): BrowserTab {
        val id = UUID.randomUUID().toString()
        // Engine is initialized lazily when loading a web URL or attaching to view
        val engine = if (url != "about:blank" && url != "about:home") {
            try {
                createEngineSafely(isIncognito, isDesktopMode)
            } catch (e: Throwable) {
                Log.e("TabManager", "Failed to create engine on init: ${e.message}", e)
                null
            }
        } else {
            null
        }

        return BrowserTab(
            id = id,
            title = if (url == "about:blank" || url == "about:home") "New Tab" else url,
            url = url,
            engine = engine,
            isDesktopMode = isDesktopMode,
            isIncognito = isIncognito,
            groupName = groupName,
            groupColor = groupColor,
            isSuspended = false,
            lastAccessed = System.currentTimeMillis()
        )
    }

    fun selectTab(tabId: String) {
        val isIncog = _incognitoTabs.value.any { it.id == tabId }
        _isIncognitoMode.value = isIncog
        _activeTabId.value = tabId

        val tab = getActiveTab()
        if (tab?.isSuspended == true) {
            unsuspendTab(tab.id)
        }
    }

    fun closeTab(tabId: String) {
        val isIncog = _incognitoTabs.value.any { it.id == tabId }
        if (isIncog) {
            val target = _incognitoTabs.value.firstOrNull { it.id == tabId }
            target?.engine?.onDestroy()
            val remaining = _incognitoTabs.value.filter { it.id != tabId }
            _incognitoTabs.value = remaining
            if (remaining.isNotEmpty()) {
                _activeTabId.value = remaining.last().id
            } else {
                _isIncognitoMode.value = false
                _activeTabId.value = _regularTabs.value.firstOrNull()?.id ?: ""
            }
        } else {
            val target = _regularTabs.value.firstOrNull { it.id == tabId }
            if (target != null && target.url != "about:blank" && target.url != "about:home") {
                val record = ClosedTabRecord(title = target.title, url = target.url)
                _recentlyClosedTabs.value = (listOf(record) + _recentlyClosedTabs.value).take(20)
            }
            target?.engine?.onDestroy()
            val remaining = _regularTabs.value.filter { it.id != tabId }
            if (remaining.isEmpty()) {
                val fresh = openNewTab("about:blank", isIncognito = false)
                _regularTabs.value = listOf(fresh)
                _activeTabId.value = fresh.id
            } else {
                _regularTabs.value = remaining
                if (_activeTabId.value == tabId) {
                    _activeTabId.value = remaining.last().id
                }
            }
            persistRegularTabs()
        }
    }

    fun restoreLastClosedTab() {
        val last = _recentlyClosedTabs.value.firstOrNull() ?: return
        _recentlyClosedTabs.value = _recentlyClosedTabs.value.drop(1)
        openNewTab(url = last.url, isIncognito = false)
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        if (incognitoOnly) {
            _incognitoTabs.value.forEach { it.engine?.onDestroy() }
            _incognitoTabs.value = emptyList()
            _isIncognitoMode.value = false
            _activeTabId.value = _regularTabs.value.firstOrNull()?.id ?: ""
        } else {
            _regularTabs.value.forEach { it.engine?.onDestroy() }
            _incognitoTabs.value.forEach { it.engine?.onDestroy() }
            _incognitoTabs.value = emptyList()
            _isIncognitoMode.value = false
            val fresh = openNewTab("about:blank", isIncognito = false)
            _regularTabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            persistRegularTabs()
        }
    }

    fun duplicateTab(tabId: String) {
        val tab = (_regularTabs.value + _incognitoTabs.value).firstOrNull { it.id == tabId } ?: return
        val currentUrl = tab.engine?.navigationState?.value?.currentUrl ?: tab.url
        openNewTab(
            url = currentUrl,
            isIncognito = tab.isIncognito,
            groupName = tab.groupName,
            groupColor = tab.groupColor
        )
    }

    fun setTabGroup(tabId: String, groupName: String?, groupColor: Long?) {
        _regularTabs.value = _regularTabs.value.map {
            if (it.id == tabId) it.copy(groupName = groupName, groupColor = groupColor) else it
        }
        persistRegularTabs()
    }

    fun toggleDesktopModeForTab(tabId: String) {
        val tab = (_regularTabs.value + _incognitoTabs.value).firstOrNull { it.id == tabId } ?: return
        val newMode = !tab.isDesktopMode
        tab.engine?.setDesktopMode(newMode)

        if (tab.isIncognito) {
            _incognitoTabs.value = _incognitoTabs.value.map {
                if (it.id == tabId) it.copy(isDesktopMode = newMode) else it
            }
        } else {
            _regularTabs.value = _regularTabs.value.map {
                if (it.id == tabId) it.copy(isDesktopMode = newMode) else it
            }
            persistRegularTabs()
        }
    }

    /**
     * Memory-aware tab suspension: frees WebView heap for background tabs while retaining tab metadata.
     */
    fun suspendInactiveTabs() {
        val activeId = _activeTabId.value
        _regularTabs.value = _regularTabs.value.map { tab ->
            if (tab.id != activeId && !tab.isSuspended && tab.engine != null) {
                tab.engine?.onPause()
                tab.copy(isSuspended = true)
            } else tab
        }
    }

    private fun unsuspendTab(tabId: String) {
        _regularTabs.value = _regularTabs.value.map { tab ->
            if (tab.id == tabId && tab.isSuspended) {
                tab.engine?.onResume()
                tab.copy(isSuspended = false)
            } else tab
        }
    }

    private fun persistRegularTabs() {
        scope.launch(Dispatchers.IO) {
            try {
                val entities = _regularTabs.value.mapIndexed { index, tab ->
                    val currentUrl = tab.engine?.navigationState?.value?.currentUrl ?: tab.url
                    val currentTitle = tab.engine?.navigationState?.value?.title ?: tab.title
                    TabEntity(
                        tabId = tab.id,
                        title = currentTitle,
                        url = currentUrl,
                        isDesktopMode = tab.isDesktopMode,
                        groupName = tab.groupName,
                        groupColor = tab.groupColor?.toInt(),
                        isSuspended = tab.isSuspended,
                        position = index
                    )
                }
                tabDao.clearAll()
                tabDao.insertAll(entities)
            } catch (e: Throwable) {
                Log.e("TabManager", "Error saving tabs to DB: ${e.message}")
            }
        }
    }
}
