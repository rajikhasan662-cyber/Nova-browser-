package com.example.extensions

object ExtensionCompatibilityAuditor {

    // APIs implemented or bridged in the NOVA Browser shell runtime
    private val RUNTIME_SUPPORTED_APIS = setOf(
        "activeTab",
        "storage",
        "tabs",
        "scripting",
        "declarativeNetRequest",
        "declarativeNetRequestWithHostAccess",
        "alarms",
        "contextMenus",
        "cookies",
        "webNavigation"
    )

    // APIs that strictly require desktop Chromium C++ native code (//chrome/browser/extensions)
    private val NATIVE_CHROMIUM_REQUIRED_APIS = setOf(
        "nativeMessaging",
        "debugger",
        "vpnProvider",
        "management",
        "enterprise.deviceAttributes",
        "fileSystemProvider",
        "system.cpu",
        "system.memory",
        "system.storage",
        "printerProvider"
    )

    fun audit(manifest: ExtensionManifest): ExtensionCompatibilityReport {
        val supported = mutableListOf<String>()
        val unsupported = mutableListOf<String>()

        val allRequested = (manifest.permissions + manifest.hostPermissions).distinct()

        for (perm in allRequested) {
            when {
                RUNTIME_SUPPORTED_APIS.contains(perm) -> supported.add(perm)
                NATIVE_CHROMIUM_REQUIRED_APIS.contains(perm) -> unsupported.add(perm)
                perm == "<all_urls>" || perm.startsWith("http://") || perm.startsWith("https://") -> {
                    supported.add("host:$perm")
                }
                else -> unsupported.add(perm)
            }
        }

        // Check content scripts capability
        val hasContentScripts = manifest.contentScripts.isNotEmpty()
        if (hasContentScripts) {
            supported.add("content_scripts (DOM & CSS Injection)")
        }

        // Check background capability
        val hasBackgroundWorker = manifest.background?.serviceWorker != null || manifest.background?.scripts?.isNotEmpty() == true
        if (hasBackgroundWorker) {
            if (manifest.manifestVersion == ManifestVersion.V3) {
                supported.add("mv3_service_worker (Simulated / In-Process)")
            } else {
                supported.add("mv2_background_scripts (Page Context)")
            }
        }

        val rating = when {
            unsupported.any { NATIVE_CHROMIUM_REQUIRED_APIS.contains(it) } -> {
                CompatibilityRating.NATIVE_CHROMIUM_REQUIRED
            }
            hasContentScripts && unsupported.isEmpty() -> {
                CompatibilityRating.FULL_RUNTIME_SUPPORTED
            }
            hasContentScripts && unsupported.isNotEmpty() -> {
                CompatibilityRating.PARTIAL_CONTENT_SCRIPTS_ACTIVE
            }
            hasBackgroundWorker && unsupported.isEmpty() -> {
                CompatibilityRating.PARTIAL_CONTENT_SCRIPTS_ACTIVE
            }
            else -> CompatibilityRating.NATIVE_CHROMIUM_REQUIRED
        }

        val summary = when (rating) {
            CompatibilityRating.FULL_RUNTIME_SUPPORTED ->
                "100% Compatible with NOVA Browser Runtime. Content scripts, storage, and styling execute directly."
            CompatibilityRating.PARTIAL_CONTENT_SCRIPTS_ACTIVE ->
                "Partially Compatible. Content scripts, DOM modifications, and ad filtering are fully active. Native background socket APIs are restricted."
            CompatibilityRating.NATIVE_CHROMIUM_REQUIRED ->
                "Requires Custom Chromium Native C++ Build. Depends on desktop APIs (e.g. ${unsupported.take(2).joinToString()}) that are disabled in Android Chromium source."
            CompatibilityRating.INCOMPATIBLE ->
                "Incompatible with Android environment."
        }

        val nativeNotes = if (rating == CompatibilityRating.NATIVE_CHROMIUM_REQUIRED) {
            "To execute this extension with full desktop fidelity, compile a custom Chromium Android APK with ENABLE_EXTENSIONS=true and JNI bridge to //chrome/browser/extensions."
        } else null

        return ExtensionCompatibilityReport(
            extensionName = manifest.name,
            version = manifest.version,
            manifestVersion = manifest.manifestVersion,
            rating = rating,
            supportedApis = supported,
            unsupportedApis = unsupported,
            requestedPermissions = allRequested,
            compatibilitySummary = summary,
            nativeDependencyNotes = nativeNotes
        )
    }
}
