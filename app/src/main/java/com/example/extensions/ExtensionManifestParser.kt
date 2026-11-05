package com.example.extensions

import org.json.JSONArray
import org.json.JSONObject

object ExtensionManifestParser {

    fun parse(jsonString: String): Result<ExtensionManifest> {
        return runCatching {
            val json = JSONObject(jsonString)

            val rawVersion = json.optInt("manifest_version", 2)
            val manifestVersion = when (rawVersion) {
                2 -> ManifestVersion.V2
                3 -> ManifestVersion.V3
                else -> ManifestVersion.UNKNOWN
            }

            val name = json.optString("name", "Unnamed Extension")
            val version = json.optString("version", "1.0.0")
            val description = json.optString("description", "")
            val author = json.optString("author", "Web Developer")

            // Permissions
            val permissions = json.optJSONArray("permissions")?.toStringList() ?: emptyList()
            val hostPermissions = json.optJSONArray("host_permissions")?.toStringList() ?: emptyList()

            // Background config (MV2 scripts vs MV3 service_worker)
            val backgroundObj = json.optJSONObject("background")
            val backgroundConfig = if (backgroundObj != null) {
                val serviceWorker = if (backgroundObj.has("service_worker")) backgroundObj.getString("service_worker") else null
                val scripts = backgroundObj.optJSONArray("scripts")?.toStringList() ?: emptyList()
                val persistent = backgroundObj.optBoolean("persistent", false)
                BackgroundScriptConfig(serviceWorker = serviceWorker, scripts = scripts, isPersistent = persistent)
            } else null

            // Content Scripts
            val contentScriptsArray = json.optJSONArray("content_scripts")
            val contentScripts = mutableListOf<ContentScriptConfig>()
            if (contentScriptsArray != null) {
                for (i in 0 until contentScriptsArray.length()) {
                    val csObj = contentScriptsArray.getJSONObject(i)
                    val matches = csObj.optJSONArray("matches")?.toStringList() ?: listOf("<all_urls>")
                    val js = csObj.optJSONArray("js")?.toStringList() ?: emptyList()
                    val css = csObj.optJSONArray("css")?.toStringList() ?: emptyList()
                    val runAt = csObj.optString("run_at", "document_idle")
                    contentScripts.add(
                        ContentScriptConfig(
                            matches = matches,
                            jsFiles = js,
                            cssFiles = css,
                            runAt = runAt
                        )
                    )
                }
            }

            // Action / Browser Action
            val actionObj = json.optJSONObject("action") ?: json.optJSONObject("browser_action")
            val popup = if (actionObj?.has("default_popup") == true) actionObj.getString("default_popup") else null
            val defaultIcon = if (actionObj?.has("default_icon") == true) actionObj.getString("default_icon") else null
            val optionsPage = if (json.has("options_page")) json.getString("options_page") else null

            ExtensionManifest(
                manifestVersion = manifestVersion,
                name = name,
                version = version,
                description = description,
                author = author,
                permissions = permissions,
                hostPermissions = hostPermissions,
                background = backgroundConfig,
                contentScripts = contentScripts,
                popupHtml = popup,
                optionsHtml = optionsPage,
                defaultIcon = defaultIcon
            )
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            list.add(getString(i))
        }
        return list
    }
}
