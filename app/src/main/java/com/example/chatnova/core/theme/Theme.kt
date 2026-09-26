package com.example.chatnova.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NovaPrimaryLight,
    onPrimary = NovaOnPrimary,
    primaryContainer = NovaPrimaryDark,
    onPrimaryContainer = NovaPrimaryLight,
    secondary = NovaCyanLight,
    onSecondary = NovaOnSecondary,
    secondaryContainer = NovaCyanDark,
    onSecondaryContainer = NovaCyanLight,
    tertiary = NovaCoral,
    background = NovaDarkBackground,
    onBackground = NovaDarkTextPrimary,
    surface = NovaDarkSurface,
    onSurface = NovaDarkTextPrimary,
    surfaceVariant = NovaDarkSurfaceVariant,
    onSurfaceVariant = NovaDarkTextSecondary,
    outline = NovaDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NovaPrimary,
    onPrimary = NovaOnPrimary,
    primaryContainer = NovaPrimaryLight,
    onPrimaryContainer = NovaOnPrimary,
    secondary = NovaCyanDark,
    onSecondary = NovaOnPrimary,
    secondaryContainer = NovaCyanLight,
    onSecondaryContainer = NovaOnSecondary,
    tertiary = NovaCoral,
    background = NovaLightBackground,
    onBackground = NovaLightTextPrimary,
    surface = NovaLightSurface,
    onSurface = NovaLightTextPrimary,
    surfaceVariant = NovaLightSurfaceVariant,
    onSurfaceVariant = NovaLightTextSecondary,
    outline = NovaLightBorder
)

@Composable
fun ChatNovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChatNovaTypography,
        content = content
    )
}
