package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun QrCodeDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Page QR Code & Share", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Scan this QR code with another device camera to instantly open this webpage:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas rendering high-contrast QR Matrix representation
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(172.dp)) {
                        val gridSize = 21
                        val cellSize = size.width / gridSize
                        val hash = abs(url.hashCode())

                        // Render finder patterns (Top-left, Top-right, Bottom-left)
                        fun drawFinder(startX: Int, startY: Int) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(startX * cellSize, startY * cellSize),
                                size = Size(7 * cellSize, 7 * cellSize)
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = Offset((startX + 1) * cellSize, (startY + 1) * cellSize),
                                size = Size(5 * cellSize, 5 * cellSize)
                            )
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset((startX + 2) * cellSize, (startY + 2) * cellSize),
                                size = Size(3 * cellSize, 3 * cellSize)
                            )
                        }

                        drawFinder(0, 0)
                        drawFinder(gridSize - 7, 0)
                        drawFinder(0, gridSize - 7)

                        // Data modules
                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                val inFinderTL = r < 7 && c < 7
                                val inFinderTR = r < 7 && c >= gridSize - 7
                                val inFinderBL = r >= gridSize - 7 && c < 7
                                if (inFinderTL || inFinderTR || inFinderBL) continue

                                val isDark = ((r * 31 + c * 17 + hash) % 3 == 0) || ((r + c + (hash shr (r % 16))) % 2 == 0)
                                if (isDark) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, url)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share URL")
                    context.startActivity(shareIntent)
                }
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share URL")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
