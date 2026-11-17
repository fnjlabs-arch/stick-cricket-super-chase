package com.sadsha.stickcricket.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CricketDarkColorScheme = darkColorScheme(
    primary = CricketOrange,
    onPrimary = Color.White,
    primaryContainer = CricketOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = CricketGold,
    onSecondary = Color.Black,
    tertiary = BoundaryCyan,
    background = StadiumDark,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    error = WicketRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CricketDarkColorScheme,
        typography = Typography,
        content = content
    )
}

