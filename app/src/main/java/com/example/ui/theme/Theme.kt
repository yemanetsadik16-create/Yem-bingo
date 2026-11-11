package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = BgDark,
    primaryContainer = BgCardElevated,
    onPrimaryContainer = GoldLight,
    secondary = MintPrimary,
    onSecondary = BgDark,
    secondaryContainer = BgCard,
    onSecondaryContainer = MintLight,
    tertiary = OrangeDynamic,
    onTertiary = TextPrimary,
    background = BgDark,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
