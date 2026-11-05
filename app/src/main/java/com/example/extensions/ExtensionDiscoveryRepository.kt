package com.example.extensions

object ExtensionDiscoveryRepository {

    data class ExtensionListing(
        val id: String,
        val name: String,
        val version: String,
        val developer: String,
        val category: String,
        val description: String,
        val rating: Float,
        val usersCount: String,
        val manifestJson: String,
        val iconEmoji: String
    )

    val CATALOG: List<ExtensionListing> = listOf(
        ExtensionListing(
            id = "dark_reader",
            name = "Dark Reader",
            version = "4.9.86",
            developer = "Alexander Shutov",
            category = "Customization",
            description = "Dark mode for every website. Care for your eyes, use dark theme for night and daily browsing.",
            rating = 4.8f,
            usersCount = "5,000,000+",
            iconEmoji = "🌙",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Dark Reader",
                    "version": "4.9.86",
                    "description": "Dark mode for every website.",
                    "author": "Alexander Shutov",
                    "permissions": ["storage", "activeTab", "scripting"],
                    "content_scripts": [
                        {
                            "matches": ["<all_urls>"],
                            "run_at": "document_start",
                            "css": ["darkreader.css"]
                        }
                    ],
                    "action": {
                        "default_popup": "popup.html"
                    }
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "ublock_origin",
            name = "uBlock Origin Lite",
            version = "2024.10.1",
            developer = "Raymond Hill (gorhill)",
            category = "Ad blockers",
            description = "Efficient content blocker using Manifest V3 Declarative Net Request. Easy on CPU and memory.",
            rating = 4.9f,
            usersCount = "10,000,000+",
            iconEmoji = "🛡️",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "uBlock Origin Lite",
                    "version": "2024.10.1",
                    "description": "Efficient content blocker for MV3.",
                    "author": "Raymond Hill",
                    "permissions": ["declarativeNetRequest", "storage"],
                    "declarative_net_request": {
                        "rule_resources": [{"id": "ruleset_1", "enabled": true, "path": "rules.json"}]
                    },
                    "action": {
                        "default_popup": "popup.html"
                    }
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "clear_urls",
            name = "ClearURLs",
            version = "1.26.1",
            developer = "Kevin Roebert",
            category = "Privacy",
            description = "Automatically removes tracking elements and query parameters (utm_*, gclid, fbclid) from URLs.",
            rating = 4.7f,
            usersCount = "400,000+",
            iconEmoji = "🧹",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "ClearURLs",
                    "version": "1.26.1",
                    "description": "Removes tracking fields from URLs.",
                    "author": "Kevin Roebert",
                    "permissions": ["storage", "tabs", "scripting"],
                    "content_scripts": [
                        {
                            "matches": ["<all_urls>"],
                            "run_at": "document_idle",
                            "js": ["clearurls.js"]
                        }
                    ]
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "sponsorblock",
            name = "SponsorBlock for YouTube",
            version = "5.5.9",
            developer = "Ajay Ramachandran",
            category = "Utilities",
            description = "Skip sponsor segments, intros, outros, and subscribe reminders on YouTube videos automatically.",
            rating = 4.9f,
            usersCount = "1,500,000+",
            iconEmoji = "⏩",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "SponsorBlock for YouTube",
                    "version": "5.5.9",
                    "description": "Skip sponsorships on YouTube.",
                    "author": "Ajay Ramachandran",
                    "permissions": ["storage"],
                    "host_permissions": ["*://*.youtube.com/*"],
                    "content_scripts": [
                        {
                            "matches": ["*://*.youtube.com/*"],
                            "run_at": "document_idle",
                            "js": ["sponsorblock.js"]
                        }
                    ]
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "privacy_badger",
            name = "Privacy Badger",
            version = "2024.5.17",
            developer = "Electronic Frontier Foundation (EFF)",
            category = "Privacy",
            description = "Automatically learns to block invisible trackers as you browse. From the EFF.",
            rating = 4.6f,
            usersCount = "2,000,000+",
            iconEmoji = "🦡",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Privacy Badger",
                    "version": "2024.5.17",
                    "description": "Automatically learns to block third-party trackers.",
                    "author": "EFF",
                    "permissions": ["declarativeNetRequest", "storage", "cookies"],
                    "action": {
                        "default_popup": "popup.html"
                    }
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "tampermonkey",
            name = "Tampermonkey",
            version = "5.3.3",
            developer = "Jan Biniok",
            category = "Developer tools",
            description = "The world's most popular userscript manager. Run custom JavaScript routines on websites.",
            rating = 4.7f,
            usersCount = "10,000,000+",
            iconEmoji = "🐵",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Tampermonkey",
                    "version": "5.3.3",
                    "description": "Userscript manager for Chromium.",
                    "author": "Jan Biniok",
                    "permissions": ["storage", "tabs", "scripting"],
                    "host_permissions": ["<all_urls>"]
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "bitwarden",
            name = "Bitwarden Password Manager",
            version = "2024.9.0",
            developer = "Bitwarden Inc.",
            category = "Security",
            description = "Secure and free password manager for all of your devices. Autofill passwords easily.",
            rating = 4.8f,
            usersCount = "4,000,000+",
            iconEmoji = "🔐",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Bitwarden Password Manager",
                    "version": "2024.9.0",
                    "description": "Password Manager.",
                    "author": "Bitwarden Inc.",
                    "permissions": ["storage", "activeTab", "scripting", "nativeMessaging"],
                    "action": {
                        "default_popup": "popup.html"
                    }
                }
            """.trimIndent()
        ),
        ExtensionListing(
            id = "google_translate",
            name = "Google Translate Extension",
            version = "2.0.13",
            developer = "Google LLC",
            category = "Translation",
            description = "View translations easily as you browse the web by highlighting text or translating full pages.",
            rating = 4.4f,
            usersCount = "10,000,000+",
            iconEmoji = "🌐",
            manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Google Translate",
                    "version": "2.0.13",
                    "description": "Highlight and translate text.",
                    "author": "Google LLC",
                    "permissions": ["storage", "activeTab", "contextMenus"],
                    "action": {
                        "default_popup": "popup.html"
                    }
                }
            """.trimIndent()
        )
    )

    val CATEGORIES = listOf(
        "All",
        "Ad blockers",
        "Privacy",
        "Security",
        "Customization",
        "Utilities",
        "Developer tools",
        "Translation",
        "Productivity"
    )
}
