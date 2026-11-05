# NOVA Browser: Chromium Engine & Chrome Extension Feasibility Audit

## Executive Summary

NOVA Browser is a high-performance Android browser engineered with architectural integrity, delivering genuine Chromium Blink & V8 rendering, a production-grade extension compatibility auditor & content-script runtime, declarative network request ad & tracker filtering, tab group management, and Android KeyStore-backed credential security.

---

## Phase 0: Feasibility Audit & Architectural Reality

### 1. Android Chromium Engine Reality
- In upstream Google Chromium source (`src/chrome/browser`), the desktop extension subsystem (`//chrome/browser/extensions`) is unconditionally disabled for Android targets (`enable_extensions = false` when `target_os = "android"`).
- Stock Android system WebView embeds Blink, V8, and the Chromium networking stack, providing full HTML5, CSS3, ES2024, WebSockets, WebRTC, Service Workers, and hardware-accelerated WebGL. However, it does not expose the desktop `//extensions` C++ subsystem.
- Out-of-tree projects that historically supported Chrome extensions on Android (e.g. Kiwi Browser) maintain extensive C++ patchsets across `chrome/browser/ui/android`, `extensions/browser`, and `extensions/renderer`. Maintaining these patchsets requires a ~100GB Chromium checkout and dedicated native Clang/Ninja build infrastructure.

### 2. Dual-Tier Architectural Implementation
To adhere to the user's integrity rule ("Never lie about Chrome extension compatibility. I would rather have 10 fully working features than 100 fake features"), NOVA Browser implements a dual-tier architecture:

1. **Active Production Shell (`SystemChromiumEngine`)**:
   - Employs the Android Chromium engine with hardware acceleration.
   - Enforces strict HTTPS policies, rejects invalid SSL certificates (never automatically proceeds).
   - Desktop Mode: switches user-agent to desktop Chrome (Windows 11 / Mac OS X 64-bit), modifies viewport scaling, and remembers per-site settings.
   - Fullscreen video playback with `WebChromeClient.CustomViewCallback`.
   - Reader mode distillation (`ReaderModeExtractor`).

2. **Chrome Extension Runtime & Compatibility Auditor**:
   - **Manifest Parser (`ExtensionManifestParser`)**: Fully parses Manifest V2 and Manifest V3 JSON schemas, extracting permissions, background workers, content scripts, actions, and declarative net request rules.
   - **Compatibility Auditor (`ExtensionCompatibilityAuditor`)**: Evaluates requested APIs and grades extensions:
     * `FULL_RUNTIME_SUPPORTED`: Extensions operating via Content Scripts, DOM CSS/JS injections, and Web APIs (e.g., Dark Reader, ClearURLs, Stylus).
     * `PARTIAL_CONTENT_SCRIPTS_ACTIVE`: Extensions with content scripts active in-page, but background workers simulated or restricted.
     * `NATIVE_CHROMIUM_REQUIRED`: Extensions depending on desktop native C++ APIs (`chrome.nativeMessaging`, `chrome.debugger`, `chrome.vpnProvider`).
     * `INCOMPATIBLE`: Unsupported OS-level features.
   - **Content Script Injector (`ContentScriptInjector`)**: Genuinely executes CSS filters, link cleaners, and userscripts on visited pages via `evaluateJavascript`, providing a functioning `window.chrome.runtime` and `window.chrome.storage` polyfill.

3. **Native Chromium Contract (`ChromiumEngineSpec`)**:
   - Documents the exact GN build flags (`enable_extensions = true`, `target_os = "android"`, `target_cpu = "arm64"`) and JNI bindings for engineers compiling a standalone custom Chromium APK from source.

---

## Core Feature Overview

| Feature | Implementation | Status |
| :--- | :--- | :--- |
| **Chromium Blink/V8 Rendering** | `SystemChromiumEngine` with hardware acceleration & strict HTTPS | Operational |
| **Chrome Extension Store & Importer** | `ExtensionDiscoveryRepository` & `ExtensionDiscoveryScreen` | Operational |
| **Extension Manifest Parser & Auditor** | `ExtensionManifestParser` & `ExtensionCompatibilityAuditor` | Operational |
| **Real Content Script Execution** | `ContentScriptInjector` (Dark Reader CSS, ClearURLs, SponsorBlock) | Operational |
| **Ad & Tracker Blocking** | `AdBlockFilterEngine` (EasyList/EasyPrivacy patterns, request interception) | Operational |
| **Privacy Dashboard** | Real-time blocked request counters, data saved metrics, live request log | Operational |
| **Advanced Tab Manager** | `TabManager` (multi-tabs, tab groups, memory suspension, tab restore) | Operational |
| **Isolated Incognito Mode** | Zero persistent cookies/cache, separate session, isolated history | Operational |
| **Bookmarks & History** | Room DB (`NovaDatabase`) with folder organization and date filters | Operational |
| **Download Manager** | Android `DownloadManager` with FileProvider share/open intents | Operational |
| **Password Vault** | Android KeyStore AES-256 GCM encryption & built-in password generator | Operational |
| **Desktop Site Mode** | Dynamic Chrome Windows 11 desktop User-Agent & viewport controls | Operational |
| **Reader Mode** | DOM text/headings distillation with typography and theme controls | Operational |
| **SSL Security Dialog** | Real-time TLS certificate inspection & host verification | Operational |
| **QR Code Sharing** | Canvas-rendered QR matrix pattern and standard share intent | Operational |
| **Find in Page** | Real WebView match indexing with active ordinal navigation | Operational |
