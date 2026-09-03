package com.squatchsports.training.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SquatchBlueDark,
    secondary = SquatchPink,
    tertiary = SquatchGreen,
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
)

private val LightColorScheme = lightColorScheme(
    primary = SquatchBlue,
    secondary = SquatchPink,
    tertiary = SquatchGreen,
    background = Color(0xFFF7F7F7),
    surface = Color.White,
)

@Composable
fun SquatchSportsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
