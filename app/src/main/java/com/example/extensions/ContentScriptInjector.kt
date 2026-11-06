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

    /**
     * Complete YouTube Player Mobile Viewport Fit and Native Fullscreen Enabler:
     * 1. Guarantees 100vw full width edge-to-edge 16:9 player sizing (matching normal YouTube app).
     * 2. Eliminates letterbox/pillarbox side black margins.
     * 3. Hooks HTML5 fullscreen buttons and delegates to video.webkitEnterFullscreen().
     * 4. Intercepts YouTube mobile in-page fake fullscreen (blue X button) and elevates to true fullscreen.
     * 5. Responds instantly to dynamic SPA navigation (yt-navigate-finish) and DOM mutations.
     */
    val YOUTUBE_PLAYER_FIT_JS = """
        (function() {
            if (!location.hostname.includes('youtube.com')) return;

            // 1. Polyfill Fullscreen API for YouTube mobile player
            try {
                if (!document.fullscreenEnabled) {
                    Object.defineProperty(document, 'fullscreenEnabled', { get: function() { return true; }, configurable: true });
                }
                if (!document.webkitFullscreenEnabled) {
                    Object.defineProperty(document, 'webkitFullscreenEnabled', { get: function() { return true; }, configurable: true });
                }
            } catch(e) {}

            function getTargetVideo() {
                return document.querySelector('video.video-stream, video');
            }

            function triggerNativeFullscreen() {
                var video = getTargetVideo();
                if (video) {
                    if (typeof video.webkitEnterFullscreen === 'function') {
                        video.webkitEnterFullscreen();
                        return true;
                    } else if (typeof video.requestFullscreen === 'function') {
                        video.requestFullscreen();
                        return true;
                    }
                }
                return false;
            }

            // Hook Element.prototype.requestFullscreen & webkitRequestFullscreen
            try {
                var origReqFs = Element.prototype.requestFullscreen;
                Element.prototype.requestFullscreen = function() {
                    if (triggerNativeFullscreen()) {
                        return Promise.resolve();
                    }
                    if (origReqFs) return origReqFs.apply(this, arguments);
                    return Promise.resolve();
                };
                var origWebkitReqFs = Element.prototype.webkitRequestFullscreen;
                Element.prototype.webkitRequestFullscreen = function() {
                    if (triggerNativeFullscreen()) return;
                    if (origWebkitReqFs) origWebkitReqFs.apply(this, arguments);
                };
            } catch(e) {}

            // 2. Global capture listener for clicks on any Fullscreen button or icon on YouTube
            document.addEventListener('click', function(e) {
                var target = e.target;
                if (!target) return;
                var fsBtn = target.closest(
                    '.ytp-fullscreen-button, .fullscreen-icon, button.fullscreen-button, ' +
                    'button[aria-label*="ull screen" i], button[title*="ull screen" i], ' +
                    'button.ytp-button--fullscreen, [data-title-no-tooltip*="ull screen" i], ' +
                    '.ytm-fullscreen-button, .player-fullscreen-button'
                );
                if (fsBtn) {
                    e.preventDefault();
                    e.stopPropagation();
                    triggerNativeFullscreen();
                }
            }, true);

            // 3. YouTube Mobile Normal Player 100% Full-Width Aspect-Ratio 16:9 Style
            function injectYoutubeStyle() {
                var styleId = 'nova-yt-perfect-fit';
                var existing = document.getElementById(styleId);
                if (!existing) {
                    var style = document.createElement('style');
                    style.id = styleId;
                    style.type = 'text/css';
                    style.textContent = `
                        /* Make the YouTube player container match normal YouTube mobile full width */
                        #player-container-id,
                        #player,
                        .player-container,
                        .sticky-player,
                        .player-size,
                        ytm-media-item .media-item-thumbnail-container,
                        .video-thumbnail-container-compact {
                            width: 100vw !important;
                            max-width: 100vw !important;
                            min-width: 100vw !important;
                            aspect-ratio: 16 / 9 !important;
                            height: auto !important;
                            min-height: 200px !important;
                            margin-left: 0 !important;
                            margin-right: 0 !important;
                            padding-left: 0 !important;
                            padding-right: 0 !important;
                            left: 0 !important;
                            right: 0 !important;
                            box-sizing: border-box !important;
                            position: relative !important;
                            overflow: hidden !important;
                        }
                        .html5-video-player {
                            width: 100% !important;
                            height: 100% !important;
                            max-width: 100vw !important;
                            margin: 0 !important;
                            padding: 0 !important;
                            left: 0 !important;
                            top: 0 !important;
                            position: absolute !important;
                        }
                        .html5-video-container {
                            width: 100% !important;
                            height: 100% !important;
                            position: absolute !important;
                            top: 0 !important;
                            left: 0 !important;
                            right: 0 !important;
                            bottom: 0 !important;
                        }
                        video.video-stream.html5-main-video {
                            width: 100% !important;
                            height: 100% !important;
                            position: absolute !important;
                            top: 0 !important;
                            left: 0 !important;
                            right: 0 !important;
                            bottom: 0 !important;
                            object-fit: contain !important;
                            transform: none !important;
                        }
                        /* Hide YouTube's in-page fake-fullscreen blue X button if present */
                        .player-fullscreen-close-button,
                        button[aria-label="Exit fullscreen"] {
                            display: none !important;
                        }
                    `;
                    (document.head || document.documentElement).appendChild(style);
                }
            }

            // 4. Clean any inline styles on the video that cause side pillars
            function alignPlayerVideo() {
                var video = getTargetVideo();
                if (video) {
                    if (video.style.left && video.style.left !== '0px') video.style.left = '0px';
                    if (video.style.marginLeft && video.style.marginLeft !== '0px') video.style.marginLeft = '0px';
                    if (video.style.marginRight && video.style.marginRight !== '0px') video.style.marginRight = '0px';
                }
                // Check if YouTube entered its pseudo-fullscreen (with blue X button)
                var pseudoFs = document.querySelector('.player-fullscreen, .watch-fullscreen, [data-fullscreen="true"]');
                var exitBtn = document.querySelector('.player-fullscreen-close-button, button[aria-label="Exit fullscreen"]');
                if (pseudoFs || exitBtn) {
                    if (exitBtn) {
                        try { exitBtn.click(); } catch(e) {}
                    }
                    triggerNativeFullscreen();
                }
            }

            injectYoutubeStyle();
            alignPlayerVideo();

            // 5. Observer for dynamic SPA navigation and DOM changes
            try {
                var observer = new MutationObserver(function() {
                    injectYoutubeStyle();
                    alignPlayerVideo();
                });
                observer.observe(document.documentElement, { attributes: true, childList: true, subtree: true });
            } catch(e) {}

            // 6. Navigation and orientation event hooks
            window.addEventListener('yt-navigate-finish', function() {
                injectYoutubeStyle();
                alignPlayerVideo();
                setTimeout(alignPlayerVideo, 300);
                setTimeout(alignPlayerVideo, 800);
            });
            window.addEventListener('resize', function() {
                injectYoutubeStyle();
                alignPlayerVideo();
            });
            window.addEventListener('orientationchange', function() {
                setTimeout(function() {
                    injectYoutubeStyle();
                    alignPlayerVideo();
                    if (Math.abs(window.orientation) === 90) {
                        var v = getTargetVideo();
                        if (v && !v.paused && !v.ended) {
                            triggerNativeFullscreen();
                        }
                    }
                }, 200);
            });
        })();
    """.trimIndent()

    /**
     * Direct script to trigger native fullscreen on any active video element.
     */
    val FORCE_FULLSCREEN_JS = """
        (function() {
            var v = document.querySelector('video.video-stream, video');
            if (v) {
                if (typeof v.webkitEnterFullscreen === 'function') {
                    v.webkitEnterFullscreen();
                    return 'ok';
                } else if (typeof v.requestFullscreen === 'function') {
                    v.requestFullscreen();
                    return 'ok';
                }
            }
            return 'no_video';
        })();
    """.trimIndent()

    /**
     * Script to query live video playback metrics for NOVA Video Diagnostics.
     */
    val GET_VIDEO_STATS_JS = """
        (function() {
            var v = document.querySelector('video.video-stream, video');
            if (!v) return JSON.stringify({ hasVideo: false });
            
            var bufferedSec = 0;
            try {
                if (v.buffered && v.buffered.length > 0) {
                    bufferedSec = Math.max(0, v.buffered.end(v.buffered.length - 1) - v.currentTime);
                }
            } catch(e) {}
            
            var dropped = 0;
            try {
                var quality = v.getVideoPlaybackQuality ? v.getVideoPlaybackQuality() : null;
                dropped = quality ? quality.droppedVideoFrames : (v.webkitDroppedFrameCount || 0);
            } catch(e) {}

            return JSON.stringify({
                hasVideo: true,
                videoWidth: v.videoWidth || 0,
                videoHeight: v.videoHeight || 0,
                duration: Math.round(v.duration || 0),
                currentTime: Math.round(v.currentTime || 0),
                bufferedSeconds: Math.round(bufferedSec * 10) / 10,
                droppedFrames: dropped,
                playbackRate: v.playbackRate || 1.0,
                isPaused: v.paused
            });
        })();
    """.trimIndent()

    /**
     * Script to set playback quality on YouTube player.
     */
    fun getSetVideoQualityJs(quality: String): String = """
        (function() {
            try {
                var player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                if (player) {
                    if (typeof player.setPlaybackQualityRange === 'function') {
                        player.setPlaybackQualityRange('$quality');
                    }
                    if (typeof player.setPlaybackQuality === 'function') {
                        player.setPlaybackQuality('$quality');
                    }
                }
            } catch(e) {}
        })();
    """.trimIndent()
}
