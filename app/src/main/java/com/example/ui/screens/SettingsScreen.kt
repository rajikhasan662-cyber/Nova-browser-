package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.BrowserSettings
import com.example.settings.SearchEngine
import com.example.ui.theme.BrowserThemeMode
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaOrange
import com.example.ui.theme.NovaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    onSetSearchEngine: (SearchEngine) -> Unit,
    onSetThemeMode: (BrowserThemeMode) -> Unit,
    onSetDefaultDesktopMode: (Boolean) -> Unit,
    onSetHttpsOnlyMode: (Boolean) -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onOpenPasswords: () -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onClearBrowsingData: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    var clearHistoryChecked by remember { mutableStateOf(true) }
    var clearCookiesChecked by remember { mutableStateOf(true) }
    var clearCacheChecked by remember { mutableStateOf(true) }
    var clearTabsChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // General Group
            item {
                Text(text = "General", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingItemRow(
                            icon = Icons.Default.Search,
                            title = "Search Engine",
                            subtitle = settings.searchEngine.displayName,
                            onClick = { showSearchEngineDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingItemRow(
                            icon = Icons.Default.ColorLens,
                            title = "Appearance & Theme",
                            subtitle = settings.themeMode.name,
                            onClick = { showThemeDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DesktopWindows, contentDescription = null, tint = NovaOrange, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Always Request Desktop Site", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(text = "Send Desktop Chrome Windows User-Agent by default", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.defaultDesktopMode,
                                onCheckedChange = onSetDefaultDesktopMode
                            )
                        }
                    }
                }
            }

            // Privacy & Security Group
            item {
                Text(text = "Privacy & Security", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingItemRow(
                            icon = Icons.Default.Shield,
                            title = "NOVA Shield & Adblocking",
                            subtitle = "Mode: ${settings.protectionMode.name}",
                            onClick = onOpenPrivacyDashboard
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingItemRow(
                            icon = Icons.Default.Key,
                            title = "Password Vault",
                            subtitle = "Hardware-encrypted credentials",
                            onClick = onOpenPasswords
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "HTTPS-Only Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(text = "Upgrade navigations to HTTPS and reject invalid SSL", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.httpsOnlyMode,
                                onCheckedChange = onSetHttpsOnlyMode
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingItemRow(
                            icon = Icons.Default.Delete,
                            title = "Clear Browsing Data",
                            subtitle = "History, cache, cookies, active tabs",
                            onClick = { showClearDataDialog = true }
                        )
                    }
                }
            }

            // Extensions & Engine Group
            item {
                Text(text = "Engine & Extensions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingItemRow(
                            icon = Icons.Default.Extension,
                            title = "Chrome Extensions Manager",
                            subtitle = "Web Store discovery, content scripts, audit reports",
                            onClick = onOpenExtensions
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingItemRow(
                            icon = Icons.Default.Terminal,
                            title = "Chromium Engine & Feasibility Audit",
                            subtitle = "Phase 0 feasibility report & C++ GN build args",
                            onClick = onOpenDiagnostics
                        )
                    }
                }
            }

            // About NOVA Browser
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = "NOVA Browser for Android", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Version 1.0.0 (Production Architecture)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Blink 537.36 • V8 12.x • Extension Compatibility Auditor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    // Search Engine Selection Dialog
    if (showSearchEngineDialog) {
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Select Search Engine", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SearchEngine.entries.forEach { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetSearchEngine(engine)
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = settings.searchEngine == engine,
                                onClick = {
                                    onSetSearchEngine(engine)
                                    showSearchEngineDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = engine.displayName, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showSearchEngineDialog = false }) { Text("Close") }
            }
        )
    }

    // Theme Mode Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Appearance", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    BrowserThemeMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (mode) {
                                    BrowserThemeMode.SYSTEM -> "System Default"
                                    BrowserThemeMode.LIGHT -> "Light Theme"
                                    BrowserThemeMode.DARK -> "Midnight Slate Dark"
                                    BrowserThemeMode.AMOLED -> "AMOLED Pure Black"
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    // Clear Browsing Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Browsing Data", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearHistoryChecked = !clearHistoryChecked }) {
                        Checkbox(checked = clearHistoryChecked, onCheckedChange = { clearHistoryChecked = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browsing History", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearCookiesChecked = !clearCookiesChecked }) {
                        Checkbox(checked = clearCookiesChecked, onCheckedChange = { clearCookiesChecked = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cookies & Site Data", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearCacheChecked = !clearCacheChecked }) {
                        Checkbox(checked = clearCacheChecked, onCheckedChange = { clearCacheChecked = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cached Images & Files", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearTabsChecked = !clearTabsChecked }) {
                        Checkbox(checked = clearTabsChecked, onCheckedChange = { clearTabsChecked = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Close All Open Tabs", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBrowsingData(clearHistoryChecked, clearCookiesChecked, clearCacheChecked, clearTabsChecked)
                        showClearDataDialog = false
                    }
                ) {
                    Text("Clear Selected")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}
