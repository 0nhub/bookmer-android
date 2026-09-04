package com.bookmer.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkColorScheme = darkColorScheme(
    primary = BookmerPaper,
    onPrimary = BookmerInk,
    secondary = Color(0xFF9E9EA4),
    onSecondary = BookmerInk,
    tertiary = Color(0xFF8E8E93),
    onTertiary = Color.White,
    background = BookmerDark,
    onBackground = Color.White,
    surface = BookmerSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceContainerLowest = Color(0xFF111112),
    surfaceContainerLow = Color(0xFF1A1A1C),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF3A3A3C),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    error = Color(0xFFFF453A),
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = BookmerInk,
    onPrimary = BookmerPaper,
    secondary = Color(0xFF66666B),
    onSecondary = Color.White,
    tertiary = Color(0xFF8E8E93),
    onTertiary = Color.White,
    background = BookmerLight,
    onBackground = Color(0xFF111111),
    surface = BookmerSurfaceLight,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8E8ED),
    onSurfaceVariant = Color(0xFF66666B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F9),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEFEFF2),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFE5E5EA),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),
    error = Color(0xFFFF3B30),
    onError = Color.White,
)

/** True when the *app* color scheme is dark (ignores system setting when user forced Light/Dark). */
@Composable
fun bookmerIsDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun BookmerBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
