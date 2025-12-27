package com.hctt.clubmembers.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Sand,
    onSecondary = Navy,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Navy,
    onSurface = Navy
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    secondary = Sand,
    onSecondary = Navy,
    background = DarkGray,
    surface = Navy,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ClubMembersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
