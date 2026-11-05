package com.example.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.adblock.ProtectionMode
import com.example.ui.theme.BrowserThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "nova_browser_prefs")

enum class SearchEngine(val displayName: String, val searchUrl: String, val iconResName: String) {
    GOOGLE("Google", "https://www.google.com/search?q=", "google"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "duckduckgo"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=", "brave"),
    BING("Microsoft Bing", "https://www.bing.com/search?q=", "bing"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=", "ecosia"),
    STARTPAGE("Startpage", "https://www.startpage.com/sp/search?query=", "startpage")
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val themeMode: BrowserThemeMode = BrowserThemeMode.SYSTEM,
    val protectionMode: ProtectionMode = ProtectionMode.BALANCED,
    val defaultDesktopMode: Boolean = false,
    val httpsOnlyMode: Boolean = true,
    val homepageUrl: String = "about:home",
    val doNotTrack: Boolean = true
)

class BrowserPreferences(private val context: Context) {

    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PROTECTION_MODE = stringPreferencesKey("protection_mode")
        val DEFAULT_DESKTOP_MODE = booleanPreferencesKey("default_desktop_mode")
        val HTTPS_ONLY_MODE = booleanPreferencesKey("https_only_mode")
        val HOMEPAGE_URL = stringPreferencesKey("homepage_url")
        val DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
    }

    val settingsFlow: Flow<BrowserSettings> = context.dataStore.data.map { prefs ->
        val searchEngineStr = prefs[Keys.SEARCH_ENGINE] ?: SearchEngine.GOOGLE.name
        val themeModeStr = prefs[Keys.THEME_MODE] ?: BrowserThemeMode.SYSTEM.name
        val protectionModeStr = prefs[Keys.PROTECTION_MODE] ?: ProtectionMode.BALANCED.name

        BrowserSettings(
            searchEngine = runCatching { SearchEngine.valueOf(searchEngineStr) }.getOrDefault(SearchEngine.GOOGLE),
            themeMode = runCatching { BrowserThemeMode.valueOf(themeModeStr) }.getOrDefault(BrowserThemeMode.SYSTEM),
            protectionMode = runCatching { ProtectionMode.valueOf(protectionModeStr) }.getOrDefault(ProtectionMode.BALANCED),
            defaultDesktopMode = prefs[Keys.DEFAULT_DESKTOP_MODE] ?: false,
            httpsOnlyMode = prefs[Keys.HTTPS_ONLY_MODE] ?: true,
            homepageUrl = prefs[Keys.HOMEPAGE_URL] ?: "about:home",
            doNotTrack = prefs[Keys.DO_NOT_TRACK] ?: true
        )
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.dataStore.edit { it[Keys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun setThemeMode(mode: BrowserThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setProtectionMode(mode: ProtectionMode) {
        context.dataStore.edit { it[Keys.PROTECTION_MODE] = mode.name }
    }

    suspend fun setDefaultDesktopMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_DESKTOP_MODE] = enabled }
    }

    suspend fun setHttpsOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HTTPS_ONLY_MODE] = enabled }
    }

    suspend fun setHomepageUrl(url: String) {
        context.dataStore.edit { it[Keys.HOMEPAGE_URL] = url }
    }
}
