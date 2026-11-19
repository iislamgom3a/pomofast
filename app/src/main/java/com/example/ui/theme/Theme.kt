package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CadenceDarkColorScheme = darkColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = Color(0xFF2E0B03),
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = OnCoralContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = Color(0xFF04261B),
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = SlateTertiaryDark,
    onTertiary = Color(0xFF0D1F3B),
    tertiaryContainer = SlateContainerDark,
    onTertiaryContainer = OnSlateContainerDark,
    background = ObsidianCanvas,
    onBackground = SandInk,
    surface = ObsidianSurface,
    onSurface = SandInk,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = SandMutedInk,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = ObsidianHighest,
    surfaceContainerHighest = Color(0xFF3E3832),
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF2F2A25),
    error = CrimsonErrorDark,
)

private val CadenceLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = SageSecondary,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = SlateTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = OnSlateContainer,
    background = StudioParchment,
    onBackground = StudioInk,
    surface = StudioPaper,
    onSurface = StudioInk,
    surfaceVariant = StudioSand,
    onSurfaceVariant = StudioMutedInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = StudioPaper,
    surfaceContainer = StudioSand,
    surfaceContainerHigh = StudioStone,
    surfaceContainerHighest = Color(0xFFDDD4C3),
    outline = StudioOutline,
    outlineVariant = Color(0xFFE7DFD2),
    error = CrimsonError,
)

val CadenceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun CadenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CadenceDarkColorScheme else CadenceLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CadenceTypography,
        shapes = CadenceShapes,
        content = content,
    )
}
