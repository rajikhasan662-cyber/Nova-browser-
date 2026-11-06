package com.example.ui.screens

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tabs.BrowserTab

@Composable
fun BrowserWebViewContainer(
    activeTab: BrowserTab?,
    fullscreenView: View? = null,
    onExitFullscreen: () -> Unit = {},
    tabManager: com.example.tabs.TabManager? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (activeTab != null) {
            val engine = activeTab.engine ?: tabManager?.ensureEngineForTab(activeTab, context)
            if (engine != null) {
                val webView = engine.webView
                androidx.compose.runtime.key(activeTab.id) {
                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView
                        },
                        update = { view ->
                            view.requestFocus()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
