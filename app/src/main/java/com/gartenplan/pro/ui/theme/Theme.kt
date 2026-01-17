package com.gartenplan.pro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Garten-optimiertes Dark Color Scheme mit gutem Kontrast
private val DarkColorScheme = darkColorScheme(
    primary = GardenGreenDark,
    onPrimary = GardenOnPrimaryDark,
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = GardenGreenDark,
    secondary = GardenBrownDark,
    tertiary = GardenGreenDarkVariant
)

// Garten-optimiertes Light Color Scheme mit gutem Kontrast
private val LightColorScheme = lightColorScheme(
    primary = GardenGreen,            // Dunkles Grün
    onPrimary = GardenOnPrimary,      // Weiß - hoher Kontrast!
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = GardenBrown,
    onSecondary = Color.White,
    tertiary = GardenGreenLight,
    background = Color(0xFFFDFDF5),
    surface = Color(0xFFFDFDF5),
    onBackground = Color(0xFF1A1C19),
    onSurface = Color(0xFF1A1C19)
)

@Composable
fun GartenplanProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color deaktiviert für konsistenten, kontrastreichen Look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}