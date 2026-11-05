package com.example.extensions

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionManager(private val context: Context) {

    private val _installedExtensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installedExtensions: StateFlow<List<InstalledExtension>> = _installedExtensions.asStateFlow()

    init {
        loadPreinstalledExtensions()
    }

    private fun loadPreinstalledExtensions() {
        // Pre-configure initial popular extensions like Dark Reader, uBlock Origin Lite, and ClearURLs
        val darkReader = ExtensionDiscoveryRepository.CATALOG.first { it.id == "dark_reader" }
        val ublock = ExtensionDiscoveryRepository.CATALOG.first { it.id == "ublock_origin" }
        val clearurls = ExtensionDiscoveryRepository.CATALOG.first { it.id == "clear_urls" }

        val list = mutableListOf<InstalledExtension>()
        listOf(darkReader, ublock, clearurls).forEach { listing ->
            val manifest = ExtensionManifestParser.parse(listing.manifestJson).getOrNull()
            if (manifest != null) {
                val report = ExtensionCompatibilityAuditor.audit(manifest)
                list.add(
                    InstalledExtension(
                        id = listing.id,
                        name = listing.name,
                        version = listing.version,
                        description = listing.description,
                        category = listing.category,
                        manifest = manifest,
                        compatibilityReport = report,
                        isEnabled = listing.id == "clear_urls", // default ClearURLs active
                        isBuiltIn = true
                    )
                )
            }
        }
        _installedExtensions.value = list
    }

    fun installFromCatalog(listing: ExtensionDiscoveryRepository.ExtensionListing): Boolean {
        if (_installedExtensions.value.any { it.id == listing.id }) {
            return false // already installed
        }
        val manifest = ExtensionManifestParser.parse(listing.manifestJson).getOrNull() ?: return false
        val report = ExtensionCompatibilityAuditor.audit(manifest)
        val ext = InstalledExtension(
            id = listing.id,
            name = listing.name,
            version = listing.version,
            description = listing.description,
            category = listing.category,
            manifest = manifest,
            compatibilityReport = report,
            isEnabled = true,
            isBuiltIn = false
        )
        _installedExtensions.value = _installedExtensions.value + ext
        return true
    }

    fun installFromCustomJson(manifestJson: String): Result<InstalledExtension> {
        return runCatching {
            val manifest = ExtensionManifestParser.parse(manifestJson).getOrThrow()
            val report = ExtensionCompatibilityAuditor.audit(manifest)
            val id = "custom_" + System.currentTimeMillis()
            val ext = InstalledExtension(
                id = id,
                name = manifest.name,
                version = manifest.version,
                description = manifest.description,
                category = "Custom",
                manifest = manifest,
                compatibilityReport = report,
                isEnabled = true,
                isBuiltIn = false
            )
            _installedExtensions.value = _installedExtensions.value + ext
            ext
        }
    }

    fun setExtensionEnabled(id: String, enabled: Boolean) {
        _installedExtensions.value = _installedExtensions.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
    }

    fun uninstallExtension(id: String) {
        _installedExtensions.value = _installedExtensions.value.filter { it.id != id }
    }

    /**
     * Executes all enabled content scripts and CSS on the active WebView for the current page.
     */
    fun applyActiveExtensions(webView: WebView, pageUrl: String) {
        val enabledExts = _installedExtensions.value.filter { it.isEnabled }
        for (ext in enabledExts) {
            ContentScriptInjector.injectChromePolyfill(webView, ext.id)

            // Dark Reader built-in injection
            if (ext.id == "dark_reader") {
                ContentScriptInjector.injectCss(webView, ContentScriptInjector.DARK_READER_CSS)
            }

            // ClearURLs built-in injection
            if (ext.id == "clear_urls") {
                ContentScriptInjector.injectJavaScript(webView, ContentScriptInjector.CLEAR_URLS_JS)
            }

            // SponsorBlock built-in injection on YouTube
            if (ext.id == "sponsorblock" && pageUrl.contains("youtube.com")) {
                ContentScriptInjector.injectJavaScript(webView, ContentScriptInjector.SPONSOR_BLOCK_JS)
            }

            // Custom inline JS/CSS from manifest content_scripts
            for (cs in ext.manifest.contentScripts) {
                cs.inlineCss?.let { ContentScriptInjector.injectCss(webView, it) }
                cs.inlineJs?.let { ContentScriptInjector.injectJavaScript(webView, it) }
            }
        }
    }
}
