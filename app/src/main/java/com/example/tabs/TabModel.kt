package com.example.tabs

import android.graphics.Bitmap
import com.example.engine.SystemChromiumEngine

data class BrowserTab(
    val id: String,
    var title: String = "New Tab",
    var url: String = "about:blank",
    var engine: SystemChromiumEngine? = null,
    val isDesktopMode: Boolean = false,
    val isIncognito: Boolean = false,
    val groupName: String? = null,
    val groupColor: Long? = null,
    val isSuspended: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis(),
    val favicon: Bitmap? = null
)

data class ClosedTabRecord(
    val title: String,
    val url: String,
    val closedAt: Long = System.currentTimeMillis()
)

data class TabGroup(
    val name: String,
    val color: Long,
    val tabs: List<BrowserTab>
)
