package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FireZoneColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = ObsidianBg,
    primaryContainer = BlazingOrange,
    onPrimaryContainer = TextPrimary,
    secondary = GlooCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = GlooCyanDark,
    onSecondaryContainer = TextPrimary,
    tertiary = CrimsonStrike,
    onTertiary = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = GunmetalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalCard,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = CrimsonStrike,
    onError = TextPrimary
)

val FireZoneShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun FireZoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FireZoneColorScheme,
        typography = FireZoneTypography,
        shapes = FireZoneShapes,
        content = content
    )
}
