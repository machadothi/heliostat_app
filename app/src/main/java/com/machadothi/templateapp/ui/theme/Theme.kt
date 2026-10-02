package com.machadothi.templateapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Amber80,
    onPrimary = Amber20,
    primaryContainer = Color(0xFF5E3A00),
    onPrimaryContainer = Amber90,
    secondary = Sky80,
    onSecondary = Color(0xFF003258),
    secondaryContainer = Sky30,
    onSecondaryContainer = Sky90,
    tertiary = Coral80,
    onTertiary = Color(0xFF5F1500),
    background = Night10,
    onBackground = Mist90,
    surface = Night10,
    onSurface = Mist90,
    surfaceVariant = Night20,
    onSurfaceVariant = Mist80,
    surfaceContainerLowest = Night05,
    surfaceContainerLow = Night15,
    surfaceContainer = Night15,
    surfaceContainerHigh = Night20,
    surfaceContainerHighest = Night30,
    outline = Color(0xFF6F7A91),
    outlineVariant = Night30,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = Amber40,
    onPrimary = Color.White,
    primaryContainer = Amber90,
    onPrimaryContainer = Color(0xFF2C1700),
    secondary = Sky40,
    onSecondary = Color.White,
    secondaryContainer = Sky90,
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Coral40,
    onTertiary = Color.White,
    background = Paper98,
    onBackground = Ink10,
    surface = Paper98,
    onSurface = Ink10,
    surfaceVariant = Paper90,
    onSurfaceVariant = Ink30,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCF4EA),
    surfaceContainer = Paper95,
    surfaceContainerHigh = Paper90,
    surfaceContainerHighest = Color(0xFFE3D8C8),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD2C5B3),
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val HeliostatShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * The heliostat's own look. Dynamic (wallpaper) colour is deliberately off: the
 * amber and sky colours carry meaning here -- the sun, the target, the beam --
 * and should not change with the phone's wallpaper.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = HeliostatShapes,
        content = content,
    )
}
