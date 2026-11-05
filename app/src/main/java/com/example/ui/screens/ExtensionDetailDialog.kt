package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extensions.CompatibilityRating
import com.example.extensions.InstalledExtension
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaOrange
import com.example.ui.theme.NovaShieldGreen

@Composable
fun ExtensionDetailDialog(
    extension: InstalledExtension,
    onToggleEnabled: (Boolean) -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val report = extension.compatibilityReport

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = NovaCyan,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = extension.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "v${extension.version} • by ${extension.manifest.author}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = extension.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Enabled Toggle Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (extension.isEnabled) "Extension Enabled" else "Extension Disabled",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = extension.isEnabled,
                        onCheckedChange = onToggleEnabled
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Compatibility Audit Report
                Text(
                    text = "Compatibility Audit Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val (badgeColor, statusText) = when (report.rating) {
                    CompatibilityRating.FULL_RUNTIME_SUPPORTED -> Pair(NovaShieldGreen, "Full Runtime Compatibility")
                    CompatibilityRating.PARTIAL_CONTENT_SCRIPTS_ACTIVE -> Pair(NovaCyan, "Content Script & Adblock Active")
                    CompatibilityRating.NATIVE_CHROMIUM_REQUIRED -> Pair(NovaOrange, "Requires Custom Native C++ Build")
                    CompatibilityRating.INCOMPATIBLE -> Pair(Color.Red, "Incompatible")
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (report.rating == CompatibilityRating.FULL_RUNTIME_SUPPORTED) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.compatibilitySummary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )

                if (report.nativeDependencyNotes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.nativeDependencyNotes,
                        fontSize = 11.sp,
                        color = NovaOrange,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Supported APIs
                if (report.supportedApis.isNotEmpty()) {
                    Text(
                        text = "Supported APIs in Runtime:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = NovaShieldGreen
                    )
                    report.supportedApis.forEach { api ->
                        Text(text = "✓ $api", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Unsupported / Native APIs
                if (report.unsupportedApis.isNotEmpty()) {
                    Text(
                        text = "Native-Only Desktop APIs:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = NovaOrange
                    )
                    report.unsupportedApis.forEach { api ->
                        Text(text = "⚠ $api", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (!extension.isBuiltIn) {
                OutlinedButton(
                    onClick = {
                        onUninstall()
                        onDismiss()
                    }
                ) {
                    Text("Uninstall", color = Color.Red)
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
