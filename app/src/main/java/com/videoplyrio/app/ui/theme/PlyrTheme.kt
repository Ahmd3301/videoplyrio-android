package com.videoplyrio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = PlyrColors.GlassBg,
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White
)

@Composable
fun PlyrTheme(
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
