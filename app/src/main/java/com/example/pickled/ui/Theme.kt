package com.example.pickled.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B30),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1A8),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF6A7A00),
    secondaryContainer = Color(0xFFE4F57A),
    onSecondaryContainer = Color(0xFF1E2400),
    tertiaryContainer = Color(0xFFCFE6F2),
    onTertiaryContainer = Color(0xFF0E3445),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98D58E),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF14521A),
    onPrimaryContainer = Color(0xFFB4F1A8),
    secondary = Color(0xFFC7D860),
    secondaryContainer = Color(0xFF4F5B00),
    onSecondaryContainer = Color(0xFFE4F57A),
    tertiaryContainer = Color(0xFF26495B),
    onTertiaryContainer = Color(0xFFCFE6F2),
)

@Composable
fun PickledTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
