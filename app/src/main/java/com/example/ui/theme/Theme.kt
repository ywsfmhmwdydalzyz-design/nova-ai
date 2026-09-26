package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getAccentColor(accentName: String): Color {
    return when (accentName.lowercase()) {
        "cyan" -> SecondaryCyan
        "violet" -> PrimaryNeon
        "emerald" -> AccentEmerald
        "pink" -> AccentPink
        "amber" -> AccentAmber
        else -> SecondaryCyan
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark", // "dark", "light", "system"
    accentColorName: String = "cyan",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light" -> false
        "system" -> isSystemDark
        else -> true
    }

    val primaryColor = getAccentColor(accentColorName)
    val secondaryColor = if (accentColorName == "cyan") PrimaryNeon else SecondaryCyan

    // Theme Locking: Always Dark Futuristic UI (#0d0f17 / #121520)
    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.Black,
        primaryContainer = primaryColor.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = secondaryColor,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF181C2B),
        onSecondaryContainer = secondaryColor,
        tertiary = AccentPink,
        onTertiary = Color.White,
        background = BackgroundDark,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = TextSecondary,
        outline = CardBorderDark
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
