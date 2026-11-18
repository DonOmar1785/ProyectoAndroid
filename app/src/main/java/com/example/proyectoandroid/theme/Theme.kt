package com.example.proyectoandroid.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DarkGrayBackground,
    primaryContainer = ElectricBlueVariant,
    onPrimaryContainer = TextPrimary,
    secondary = WoodOak,
    onSecondary = TextPrimary,
    secondaryContainer = WoodOakDark,
    onSecondaryContainer = TextPrimary,
    background = DarkGrayBackground,
    onBackground = TextPrimary,
    surface = DarkGraySurface,
    onSurface = TextPrimary,
    outline = WoodOak
)

@Composable
fun ARAssemblyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
