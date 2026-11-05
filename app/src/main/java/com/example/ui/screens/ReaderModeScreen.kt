package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.engine.ReaderArticle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeScreen(
    article: ReaderArticle,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fontSizeMultiplier by remember { mutableFloatStateOf(1.0f) }
    var selectedTheme by remember { mutableStateOf("Sepia") } // Light, Sepia, Dark

    val (bgColor, textColor) = when (selectedTheme) {
        "Light" -> Pair(Color(0xFFFAFAFA), Color(0xFF1A1A1A))
        "Sepia" -> Pair(Color(0xFFFBF0D9), Color(0xFF3E3123))
        else -> Pair(Color(0xFF181A1B), Color(0xFFE8E6E3))
    }

    val plainContent = remember(article.contentHtml) {
        HtmlCompat.fromHtml(article.contentHtml, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }
    val readingTimeMin = (article.textLength / 800).coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Reader")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        fontSizeMultiplier = (fontSizeMultiplier - 0.15f).coerceAtLeast(0.7f)
                    }) {
                        Text("A-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(onClick = {
                        fontSizeMultiplier = (fontSizeMultiplier + 0.15f).coerceAtMost(1.8f)
                    }) {
                        Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor)
        ) {
            // Theme selection chips (Light / Sepia / Dark)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                listOf("Sepia", "Light", "Dark").forEach { themeName ->
                    FilterChip(
                        selected = selectedTheme == themeName,
                        onClick = { selectedTheme = themeName },
                        label = { Text(themeName, fontSize = 11.sp) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = textColor.copy(alpha = 0.15f))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = article.title,
                        fontSize = (22 * fontSizeMultiplier).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = (28 * fontSizeMultiplier).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${article.siteName} • ~$readingTimeMin min read",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = plainContent,
                        fontSize = (16 * fontSizeMultiplier).sp,
                        color = textColor,
                        lineHeight = (26 * fontSizeMultiplier).sp,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
