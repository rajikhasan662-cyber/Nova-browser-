package com.example

import com.example.adblock.AdBlockFilterEngine
import com.example.extensions.CompatibilityRating
import com.example.extensions.ContentScriptConfig
import com.example.extensions.ExtensionCompatibilityAuditor
import com.example.extensions.ExtensionManifest
import com.example.extensions.ManifestVersion
import com.example.security.SecureVaultManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAuditorFlagsNativeMessagingAsRequiringNativeBuild() {
        val manifest = ExtensionManifest(
            manifestVersion = ManifestVersion.V3,
            name = "Desktop Companion",
            version = "1.0",
            description = "Requires native messaging",
            permissions = listOf("storage", "nativeMessaging")
        )

        val report = ExtensionCompatibilityAuditor.audit(manifest)
        assertEquals(CompatibilityRating.NATIVE_CHROMIUM_REQUIRED, report.rating)
        assertTrue(report.unsupportedApis.contains("nativeMessaging"))
    }

    @Test
    fun testDomainExtraction() {
        val engine = AdBlockFilterEngine()
        assertEquals("example.com", engine.extractDomain("https://www.example.com/path?arg=1"))
        assertEquals("sub.domain.org", engine.extractDomain("http://sub.domain.org/test"))
    }

    @Test
    fun testExtensionManifestAndAuditor() {
        val manifest = ExtensionManifest(
            manifestVersion = ManifestVersion.V3,
            name = "Dark Reader",
            version = "4.9.86",
            description = "Dark mode for every website.",
            permissions = listOf("storage", "activeTab", "scripting"),
            contentScripts = listOf(ContentScriptConfig(matches = listOf("<all_urls>"), cssFiles = listOf("darkreader.css")))
        )
        val audit = ExtensionCompatibilityAuditor.audit(manifest)
        assertEquals(CompatibilityRating.FULL_RUNTIME_SUPPORTED, audit.rating)
        assertEquals("Dark Reader", audit.extensionName)
    }

    @Test
    fun testPasswordGenerator() {
        val vault = SecureVaultManager()
        val pass = vault.generateStrongPassword(16)
        assertEquals(16, pass.length)
    }
}
