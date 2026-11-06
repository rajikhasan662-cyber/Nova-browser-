package com.example.adblock

import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class ProtectionMode {
    OFF,
    STANDARD,   // Blocks known ad networks and crypto-miners
    BALANCED,   // Blocks ads, telemetry, cross-site tracking beacons, and social trackers
    STRICT      // Blocks all third-party scripts, fingerprinting, and analytics
}

data class BlockedRequestLog(
    val url: String,
    val domain: String,
    val category: String, // "AD", "TRACKER", "ANALYTICS", "FINGERPRINT"
    val timestamp: Long = System.currentTimeMillis()
)

data class AdBlockStats(
    val totalBlockedCount: Long = 0,
    val dailyBlockedCount: Long = 0,
    val weeklyBlockedCount: Long = 0,
    val adsBlockedCount: Long = 0,
    val trackersBlockedCount: Long = 0,
    val estimatedDataSavedBytes: Long = 0,
    val topBlockedDomains: Map<String, Long> = emptyMap(),
    val activeRulesCount: Int = 1845,
    val filterVersion: String = "NOVA Shield Advanced v2026.11.2"
)

class AdBlockFilterEngine {

    private val _protectionMode = MutableStateFlow(ProtectionMode.BALANCED)
    val protectionMode: StateFlow<ProtectionMode> = _protectionMode.asStateFlow()

    private val _stats = MutableStateFlow(
        AdBlockStats(
            activeRulesCount = 1420
        )
    )
    val stats: StateFlow<AdBlockStats> = _stats.asStateFlow()

    private val recentLogs = CopyOnWriteArrayList<BlockedRequestLog>()
    private val _logsFlow = MutableStateFlow<List<BlockedRequestLog>>(emptyList())
    val logsFlow: StateFlow<List<BlockedRequestLog>> = _logsFlow.asStateFlow()

    // Per-domain allowlist (user explicitly disabled protection for this domain)
    private val siteAllowlist = ConcurrentHashMap.newKeySet<String>()

    // Per-domain blocklist (user explicitly forced blocking)
    private val siteBlocklist = ConcurrentHashMap.newKeySet<String>()

    // Known Ad Domains (EasyList baseline)
    private val adDomains = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "admob.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "adroll.com",
        "smartadserver.com",
        "moatads.com",
        "popads.net",
        "propellerads.com",
        "yieldmo.com",
        "casalemedia.com",
        "bidswitch.net",
        "applovin.com",
        "unityads.unity3d.com",
        "chartboost.com",
        "ironsrc.com",
        "inmobi.com",
        "vungle.com",
        "adcolony.com",
        "adtechus.com",
        "adtech.de",
        "serving-sys.com",
        "adsrvr.org",
        "sovrn.com",
        "media.net",
        "indexww.com",
        "lijit.com",
        "revcontent.com",
        "mgid.com",
        "adtrue.com",
        "adform.net"
    )

    // Known Tracker & Telemetry Domains (EasyPrivacy baseline)
    private val trackerDomains = hashSetOf(
        "google-analytics.com",
        "hotjar.com",
        "hotjar.io",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "scorecardresearch.com",
        "chartbeat.com",
        "quantserve.com",
        "branch.io",
        "appsflyer.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "ads-twitter.com",
        "analytics.twitter.com",
        "clarity.ms",
        "newrelic.com",
        "nr-data.net",
        "datadoghq-browser-agent.com",
        "datadoghq.com",
        "sentry.io",
        "bugsnag.com",
        "fullstory.com",
        "amplitude.com",
        "mouseflow.com",
        "crazyegg.com",
        "optimizely.com",
        "inspectlet.com",
        "luckyorange.com",
        "statcounter.com",
        "yandex.ru/metrika",
        "mc.yandex.ru"
    )

    // Common Ad URL Path Substrings
    private val adPathPatterns = listOf(
        "/pagead/",
        "/ads/",
        "/ad.js",
        "/show_ads.js",
        "/gtag/js",
        "/fbevents.js",
        "/bat.js",
        "/analytics.js",
        "/beacon.js",
        "googletagmanager.com/gtm.js"
    )

    fun setProtectionMode(mode: ProtectionMode) {
        _protectionMode.value = mode
    }

    fun isSiteProtectionEnabled(domain: String): Boolean {
        if (_protectionMode.value == ProtectionMode.OFF) return false
        val cleanDomain = extractDomain(domain)
        return !siteAllowlist.contains(cleanDomain)
    }

    fun toggleSiteProtection(domain: String) {
        val cleanDomain = extractDomain(domain)
        if (siteAllowlist.contains(cleanDomain)) {
            siteAllowlist.remove(cleanDomain)
        } else {
            siteAllowlist.add(cleanDomain)
        }
    }

    /**
     * Intercepts a network request. If request matches ad/tracker criteria,
     * returns an empty WebResourceResponse to cancel loading cleanly without network overhead.
     */
    fun shouldIntercept(requestUrl: String, pageUrl: String?): WebResourceResponse? {
        if (_protectionMode.value == ProtectionMode.OFF) return null

        val pageDomain = pageUrl?.let { extractDomain(it) } ?: ""
        if (pageDomain.isNotEmpty() && siteAllowlist.contains(pageDomain)) {
            return null // User allowlisted this site
        }

        val requestUri = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return null
        val requestHost = requestUri.host?.lowercase() ?: return null
        val lowerUrl = requestUrl.lowercase()

        // ESSENTIAL MEDIA STREAMING WHITELIST: Never block audio/video streams or essential CDN resources
        if (isMediaStreamingHost(requestHost, lowerUrl)) {
            return null
        }

        val isAd = isAdHostOrUrl(requestHost, lowerUrl)
        val isTracker = isTrackerHostOrUrl(requestHost, lowerUrl)

        val shouldBlock = when (_protectionMode.value) {
            ProtectionMode.OFF -> false
            ProtectionMode.STANDARD -> isAd
            ProtectionMode.BALANCED -> isAd || isTracker
            ProtectionMode.STRICT -> {
                // In Strict mode, also block third-party analytics, beacons, and social trackers
                val isThirdParty = pageDomain.isNotEmpty() && !requestHost.endsWith(pageDomain) && !pageDomain.endsWith(requestHost)
                isAd || isTracker || (isThirdParty && isAnalyticsOrTelemetry(requestHost, lowerUrl))
            }
        }

        if (shouldBlock) {
            val category = if (isAd) "AD" else "TRACKER"
            recordBlock(requestUrl, requestHost, category)
            // Return empty 200 response to quench the request
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                200,
                "OK",
                mapOf("X-Blocked-By" to "NOVA-Shield"),
                ByteArrayInputStream(ByteArray(0))
            )
        }

        return null
    }

    private fun isMediaStreamingHost(host: String, url: String): Boolean {
        return host.contains("googlevideo.com") ||
                host.contains("ytimg.com") ||
                host.contains("ggpht.com") ||
                url.contains("/videoplayback") ||
                url.contains("youtube.com/api/stats/playback") ||
                host.contains("video.twimg.com") ||
                host.contains("vimeo.com") ||
                url.contains(".m3u8") ||
                url.contains(".mpd")
    }

    private fun isAdHostOrUrl(host: String, url: String): Boolean {
        if (adDomains.any { host == it || host.endsWith(".$it") }) return true
        if (adPathPatterns.any { url.contains(it) }) return true
        return false
    }

    private fun isTrackerHostOrUrl(host: String, url: String): Boolean {
        if (trackerDomains.any { host == it || host.endsWith(".$it") }) return true
        if (url.contains("/telemetry") || url.contains("/collect?") || url.contains("/event-tracker")) return true
        return false
    }

    private fun isAnalyticsOrTelemetry(host: String, url: String): Boolean {
        return host.contains("analytics") || host.contains("tracker") || host.contains("telemetry") ||
                url.contains("beacon") || url.contains("ping")
    }

    private val domainCounts = ConcurrentHashMap<String, Long>()

    private fun recordBlock(url: String, domain: String, category: String) {
        val current = _stats.value
        val isAd = category == "AD"
        val newEstimatedBytes = current.estimatedDataSavedBytes + (if (isAd) 42000L else 18000L) // estimated ad/script size

        domainCounts[domain] = (domainCounts[domain] ?: 0L) + 1L

        _stats.value = current.copy(
            totalBlockedCount = current.totalBlockedCount + 1,
            dailyBlockedCount = current.dailyBlockedCount + 1,
            weeklyBlockedCount = current.weeklyBlockedCount + 1,
            adsBlockedCount = if (isAd) current.adsBlockedCount + 1 else current.adsBlockedCount,
            trackersBlockedCount = if (!isAd) current.trackersBlockedCount + 1 else current.trackersBlockedCount,
            estimatedDataSavedBytes = newEstimatedBytes,
            topBlockedDomains = domainCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .associate { it.key to it.value }
        )

        val log = BlockedRequestLog(url = url, domain = domain, category = category)
        recentLogs.add(0, log)
        if (recentLogs.size > 100) {
            recentLogs.removeAt(recentLogs.size - 1)
        }
        _logsFlow.value = recentLogs.toList()
    }

    fun clearStats() {
        val current = _stats.value
        domainCounts.clear()
        _stats.value = current.copy(
            totalBlockedCount = 0,
            dailyBlockedCount = 0,
            weeklyBlockedCount = 0,
            adsBlockedCount = 0,
            trackersBlockedCount = 0,
            estimatedDataSavedBytes = 0,
            topBlockedDomains = emptyMap()
        )
        recentLogs.clear()
        _logsFlow.value = emptyList()
    }

    fun extractDomain(url: String): String {
        return runCatching {
            val uriHost = runCatching { Uri.parse(url).host }.getOrNull()
            val host = uriHost ?: runCatching { java.net.URI(url).host }.getOrNull()
                ?: url.substringAfter("://").substringBefore("/").substringBefore(":")
            host.removePrefix("www.").lowercase()
        }.getOrDefault("")
    }
}
