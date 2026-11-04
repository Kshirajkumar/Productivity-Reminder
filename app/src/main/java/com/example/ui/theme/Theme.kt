package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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

// Apple iOS Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = IosColorScheduled,
    onPrimary = Color.White,
    primaryContainer = AppleDarkCardSurface,
    onPrimaryContainer = AppleDarkTextPrimary,
    secondary = IosColorToday,
    onSecondary = Color.White,
    tertiary = IosColorImportant,
    background = AppleDarkBackground,
    onBackground = AppleDarkTextPrimary,
    surface = AppleDarkCardSurface,
    onSurface = AppleDarkTextPrimary,
    surfaceVariant = AppleDarkSecondarySurface,
    onSurfaceVariant = AppleDarkTextSecondary,
    outline = AppleDarkDivider
)

// Apple iOS Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = IosColorScheduled,
    onPrimary = Color.White,
    primaryContainer = AppleLightCardSurface,
    onPrimaryContainer = AppleLightTextPrimary,
    secondary = IosColorToday,
    onSecondary = Color.White,
    tertiary = IosColorImportant,
    background = AppleLightBackground,
    onBackground = AppleLightTextPrimary,
    surface = AppleLightCardSurface,
    onSurface = AppleLightTextPrimary,
    surfaceVariant = AppleLightSecondarySurface,
    onSurfaceVariant = AppleLightTextSecondary,
    outline = AppleLightDivider
)

// App Theme Controller Singleton for Light/Dark toggling
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
