package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = CopperAmber,
    onSecondary = Color(0xFF281A00),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = SignalEmerald,
    onTertiary = Color(0xFF00281C),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    error = DiagnosticCoral,
    onError = Color(0xFF420006),
    errorContainer = CoralContainer,
    onErrorContainer = OnCoralContainer,
    background = MidnightBg,
    onBackground = InkPrimary,
    surface = SurfaceNavy,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = SurfaceNavy,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceHighest,
    outline = OutlineSubtle,
    outlineVariant = Color(0xFF1F2E47)
)

private val LightColorScheme = lightColorScheme(
    primary = BlueprintPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF002026),
    secondary = BlueprintSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = BlueprintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = BlueprintBg,
    onBackground = BlueprintOnSurface,
    surface = BlueprintSurface,
    onSurface = BlueprintOnSurface,
    surfaceVariant = BlueprintSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = BlueprintOutline
)

val McuStudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun McuStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = McuStudioShapes,
        content = content
    )
}
