package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MarketColors(
    val ceiling: Color,
    val floor: Color,
    val up: Color,
    val down: Color,
    val reference: Color,
    val border: Color,
    val isDark: Boolean,
)

val LocalMarketColors = staticCompositionLocalOf {
    MarketColors(
        ceiling = MarketCeilingDark,
        floor = MarketFloorDark,
        up = MarketUpDark,
        down = MarketDownDark,
        reference = MarketRefDark,
        border = ObsidianBorder,
        isDark = true,
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = PulseEmerald,
    onPrimary = Color(0xFF041C13),
    primaryContainer = Color(0xFF0C3B2A),
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = PulseBlue,
    onSecondary = Color(0xFF062030),
    secondaryContainer = Color(0xFF0F2D42),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = PulseGold,
    onTertiary = Color(0xFF281A02),
    background = ObsidianBg,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = ObsidianTextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1A263D),
    error = MarketDownDark,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = PulseEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = PaperBg,
    onBackground = PaperTextPrimary,
    surface = PaperSurface,
    onSurface = PaperTextPrimary,
    surfaceVariant = PaperSurfaceElevated,
    onSurfaceVariant = PaperTextSecondary,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurfaceElevated,
    surfaceContainerHigh = PaperSurfaceHigh,
    outline = PaperBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = MarketDownLight,
    onError = Color.White,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VnPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val marketColors = if (darkTheme) {
        MarketColors(
            ceiling = MarketCeilingDark,
            floor = MarketFloorDark,
            up = MarketUpDark,
            down = MarketDownDark,
            reference = MarketRefDark,
            border = ObsidianBorder,
            isDark = true,
        )
    } else {
        MarketColors(
            ceiling = MarketCeilingLight,
            floor = MarketFloorLight,
            up = MarketUpLight,
            down = MarketDownLight,
            reference = MarketRefLight,
            border = PaperBorder,
            isDark = false,
        )
    }

    CompositionLocalProvider(LocalMarketColors provides marketColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
