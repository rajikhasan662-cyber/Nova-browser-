package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.WebNavigationState
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaOrange
import com.example.ui.theme.NovaShieldGreen

@Composable
fun BrowserTopBar(
    navState: WebNavigationState,
    omniboxText: String,
    onOmniboxTextChanged: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onTabCounterClick: () -> Unit,
    tabCount: Int,
    blockedCount: Long,
    isIncognito: Boolean,
    onShieldClick: () -> Unit,
    onExtensionsClick: () -> Unit,
    onSslLockClick: () -> Unit,
    onMenuClick: () -> Unit,
    onVideoAssistantClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Omnibox Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // SSL Lock or Warning indicator
                        val isHttps = navState.currentUrl.startsWith("https://")
                        val isSpecial = navState.currentUrl.startsWith("about:") || navState.currentUrl.startsWith("chrome://")

                        IconButton(
                            onClick = onSslLockClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("ssl_lock_button")
                        ) {
                            if (isSpecial) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Internal",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isHttps) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure HTTPS",
                                    tint = NovaShieldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Not Secure",
                                    tint = NovaOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // URL input field
                        TextField(
                            value = omniboxText,
                            onValueChange = onOmniboxTextChanged,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = if (isIncognito) "Search or enter URL (Incognito)" else "Search or enter address",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                focusManager.clearFocus()
                                onSubmitUrl(omniboxText)
                            }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isFocused = it.isFocused }
                                .testTag("omnibox_input")
                        )

                        // Clear text button when focused, or Reload/Stop button
                        if (isFocused && omniboxText.isNotEmpty()) {
                            IconButton(
                                onClick = { onOmniboxTextChanged("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            if (navState.isLoading) {
                                IconButton(
                                    onClick = onStop,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onReload,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // AdBlock Shield Badge
                IconButton(
                    onClick = onShieldClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("shield_badge_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (blockedCount > 0) {
                                Badge(
                                    containerColor = NovaShieldGreen,
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = if (blockedCount > 99) "99+" else "$blockedCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Shield",
                            tint = if (blockedCount > 0) NovaShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Extension Store Button
                IconButton(
                    onClick = onExtensionsClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("extensions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Extensions",
                        tint = NovaCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Video Assistant Button (visible when on video site)
                val isVideoSite = navState.currentUrl.contains("youtube.com") || navState.currentUrl.contains("watch") || navState.currentUrl.contains("video")
                if (isVideoSite && onVideoAssistantClick != null) {
                    IconButton(
                        onClick = onVideoAssistantClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("video_assistant_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video Assistant",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Tab Counter Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onTabCounterClick() }
                        .testTag("tab_counter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$tabCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Three-dot overflow menu
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Animated Loading Progress Bar
            AnimatedVisibility(
                visible = navState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { navState.progress / 100f },
                    color = NovaCyan,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                )
            }
        }
    }
}
