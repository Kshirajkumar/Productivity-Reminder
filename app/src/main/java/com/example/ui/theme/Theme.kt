package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Redesigned Obsidian Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = AuraIrisPrimary,
    onPrimary = Color.White,
    primaryContainer = AuraDarkCardSurface,
    onPrimaryContainer = AuraDarkTextPrimary,
    secondary = AuraCyanAccent,
    onSecondary = Color.Black,
    tertiary = AuraAmberImportant,
    background = AuraDarkBackground,
    onBackground = AuraDarkTextPrimary,
    surface = AuraDarkCardSurface,
    onSurface = AuraDarkTextPrimary,
    surfaceVariant = AuraDarkSecondarySurface,
    onSurfaceVariant = AuraDarkTextSecondary,
    outline = AuraDarkDivider
)

// Clean Apple Canvas Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = AuraIrisPrimary,
    onPrimary = Color.White,
    primaryContainer = AuraLightCardSurface,
    onPrimaryContainer = AuraLightTextPrimary,
    secondary = AuraCyanAccent,
    onSecondary = Color.Black,
    tertiary = AuraAmberImportant,
    background = AuraLightBackground,
    onBackground = AuraLightTextPrimary,
    surface = AuraLightCardSurface,
    onSurface = AuraLightTextPrimary,
    surfaceVariant = AuraLightSecondarySurface,
    onSurfaceVariant = AuraLightTextSecondary,
    outline = AuraLightDivider
)

// App Theme Controller Singleton
object ThemeController {
    var isDarkTheme by mutableStateOf(true)
    fun toggle() {
        isDarkTheme = !isDarkTheme
    }
}

val LocalIsDarkTheme = compositionLocalOf { true }

@Composable
fun ChronoTaskTheme(
    darkTheme: Boolean = ThemeController.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.statusBarColor = android.graphics.Color.TRANSPARENT
                it.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
