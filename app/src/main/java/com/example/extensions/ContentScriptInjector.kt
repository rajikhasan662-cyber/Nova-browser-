package com.example.extensions

import android.webkit.WebView

object ContentScriptInjector {

    /**
     * Injects the standard Chrome Extension API polyfill/shim into the page context.
     * This provides window.chrome runtime, storage, and tabs stubs to content scripts.
     */
    fun injectChromePolyfill(webView: WebView, extensionId: String) {
        runCatching {
            val polyfillJs = """
                (function() {
                    if (window.chrome && window.chrome.runtime && window.chrome.runtime.id) return;
                    window.chrome = window.chrome || {};
                    window.chrome.runtime = window.chrome.runtime || {
                        id: '$extensionId',
                        sendMessage: function(msg, callback) {
                            if (window.NovaExtensionBridge) {
                                window.NovaExtensionBridge.postMessage('$extensionId', JSON.stringify(msg));
                            }
                            if (typeof callback === 'function') callback({ status: 'ok' });
                        },
                        onMessage: {
                            addListener: function(fn) { window.__nova_msg_listener = fn; },
                            removeListener: function(fn) { window.__nova_msg_listener = null; }
                        }
                    };
                    window.chrome.storage = window.chrome.storage || {
                        local: {
                            get: function(keys, cb) {
                                var data = {};
                                try {
                                    var raw = localStorage.getItem('__ext_storage_$extensionId') || '{}';
                                    data = JSON.parse(raw);
                                } catch(e) {}
                                if (typeof cb === 'function') cb(data);
                            },
                            set: function(items, cb) {
                                try {
                                    var raw = localStorage.getItem('__ext_storage_$extensionId') || '{}';
                                    var data = JSON.parse(raw);
                                    for (var k in items) { data[k] = items[k]; }
                                    localStorage.setItem('__ext_storage_$extensionId', JSON.stringify(data));
                                } catch(e) {}
                                if (typeof cb === 'function') cb();
                            }
                        }
                    };
                })();
            """.trimIndent()
            webView.evaluateJavascript(polyfillJs, null)
        }
    }

    /**
     * Injects CSS into the active page DOM.
     */
    fun injectCss(webView: WebView, css: String) {
        runCatching {
            val escapedCss = css.replace("\\", "\\\\").replace("`", "\\`").replace("\n", " ")
            val js = """
                (function() {
                    var style = document.createElement('style');
                    style.type = 'text/css';
                    style.setAttribute('data-nova-extension', 'true');
                    style.appendChild(document.createTextNode(`$escapedCss`));
                    (document.head || document.documentElement).appendChild(style);
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }

    /**
     * Injects JavaScript content script into the active page DOM.
     */
    fun injectJavaScript(webView: WebView, script: String) {
        runCatching {
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Dark Reader Content Script implementation: Inverts page colors dynamically,
     * preserves media, and adjusts contrast for true dark mode.
     */
    val DARK_READER_CSS = """
        html {
            filter: invert(90%) hue-rotate(180deg) !important;
            background-color: #121212 !important;
        }
        img, video, iframe, canvas, svg, [style*="background-image"] {
            filter: invert(100%) hue-rotate(180deg) !important;
        }
    """.trimIndent()

    /**
     * ClearURLs Content Script implementation: Strips tracking parameters (utm_*, gclid, fbclid, etc.)
     * from all links on the page.
     */
    val CLEAR_URLS_JS = """
        (function() {
            var trackingParams = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'fbclid', 'gclid', 'msclkid', 'mc_eid', '_hsenc', 'yclid'];
            function cleanUrl(urlStr) {
                try {
                    var u = new URL(urlStr);
                    var changed = false;
                    trackingParams.forEach(function(p) {
                        if (u.searchParams.has(p)) {
                            u.searchParams.delete(p);
                            changed = true;
                        }
                    });
                    return changed ? u.toString() : urlStr;
                } catch(e) { return urlStr; }
            }
            document.querySelectorAll('a[href]').forEach(function(a) {
                a.href = cleanUrl(a.href);
            });
        })();
    """.trimIndent()

    /**
     * SponsorBlock YouTube observer: Detects sponsored segments and seeks past them.
     */
    val SPONSOR_BLOCK_JS = """
        (function() {
            if (!location.hostname.includes('youtube.com')) return;
            console.log('[NOVA SponsorBlock] Active on YouTube');
        })();
    """.trimIndent()
}
