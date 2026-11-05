package com.example.extensions

enum class ManifestVersion {
    V2,
    V3,
    UNKNOWN
}

enum class CompatibilityRating {
    FULL_RUNTIME_SUPPORTED,           // Fully supported in NOVA Browser shell
    PARTIAL_CONTENT_SCRIPTS_ACTIVE,   // Content scripts, CSS, and DOM APIs active; background service worker simulated
    NATIVE_CHROMIUM_REQUIRED,          // Requires native C++ //extensions patchset to compile into Chromium APK
    INCOMPATIBLE                      // Unsupported desktop OS-specific APIs (nativeMessaging, raw USB/Bluetooth)
}

data class ContentScriptConfig(
    val matches: List<String>,
    val jsFiles: List<String> = emptyList(),
    val cssFiles: List<String> = emptyList(),
    val runAt: String = "document_idle",
    val inlineJs: String? = null,
    val inlineCss: String? = null
)

data class BackgroundScriptConfig(
    val serviceWorker: String? = null, // MV3
    val scripts: List<String> = emptyList(), // MV2
    val isPersistent: Boolean = false
)

data class ExtensionManifest(
    val manifestVersion: ManifestVersion,
    val name: String,
    val version: String,
    val description: String,
    val author: String = "Chrome Web Store Developer",
    val permissions: List<String> = emptyList(),
    val hostPermissions: List<String> = emptyList(),
    val background: BackgroundScriptConfig? = null,
    val contentScripts: List<ContentScriptConfig> = emptyList(),
    val popupHtml: String? = null,
    val optionsHtml: String? = null,
    val defaultIcon: String? = null
)

data class ExtensionCompatibilityReport(
    val extensionName: String,
    val version: String,
    val manifestVersion: ManifestVersion,
    val rating: CompatibilityRating,
    val supportedApis: List<String>,
    val unsupportedApis: List<String>,
    val requestedPermissions: List<String>,
    val compatibilitySummary: String,
    val nativeDependencyNotes: String? = null
)

data class InstalledExtension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: String,
    val manifest: ExtensionManifest,
    val compatibilityReport: ExtensionCompatibilityReport,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val installedTimestamp: Long = System.currentTimeMillis(),
    val runtimeStorage: MutableMap<String, String> = mutableMapOf()
)
