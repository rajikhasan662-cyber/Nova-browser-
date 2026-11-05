package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class BrowserThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

private val DarkColorScheme = darkColorScheme(
    primary = NovaCyan,
    onPrimary = Color(0xFF003258),
    primaryContainer = NovaPrimaryDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = NovaOrange,
    onSecondary = Color(0xFF452200),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkOutline
)

private val AmoledColorScheme = darkColorScheme(
    primary = NovaCyan,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003B73),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = NovaOrange,
    onSecondary = Color(0xFF000000),
    background = AmoledBackground,
    onBackground = Color(0xFFFFFFFF),
    surface = AmoledSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = AmoledOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NovaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = NovaOrange,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LightOutline
)

private val IncognitoColorScheme = darkColorScheme(
    primary = IncognitoPrimary,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03DAC6),
    background = IncognitoBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = IncognitoSurface,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = IncognitoSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F)
)

@Composable
fun NovaBrowserTheme(
    themeMode: BrowserThemeMode = BrowserThemeMode.SYSTEM,
    isIncognito: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when {
        isIncognito -> IncognitoColorScheme
        themeMode == BrowserThemeMode.AMOLED -> AmoledColorScheme
        themeMode == BrowserThemeMode.DARK -> DarkColorScheme
        themeMode == BrowserThemeMode.LIGHT -> LightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        systemDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
