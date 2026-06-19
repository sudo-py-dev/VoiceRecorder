package com.voicerecorder.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.voicerecorder.domain.model.ThemeMode

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        secondary = DarkSecondary,
        tertiary = DarkTertiary,
        background = DarkBackgroundReal,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = DarkOnBackground,
        onSurface = DarkOnSurface,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        secondary = LightSecondary,
        tertiary = LightTertiary,
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = LightOnBackground,
        onSurface = LightOnSurface,
    )

// Custom gradients provider
data class FinalTalkGradients(
    val primaryGradient: Brush,
    val backgroundGradient: Brush,
    val glassBackground: Brush,
    val glassBorder: Brush,
    val neonGlow: Color,
)

val LocalFinalTalkGradients =
    staticCompositionLocalOf {
        FinalTalkGradients(
            primaryGradient = Brush.linearGradient(listOf(DarkPrimary, DarkSecondary)),
            backgroundGradient = Brush.verticalGradient(listOf(DarkBackgroundReal, DarkSurface)),
            glassBackground = Brush.linearGradient(listOf(Color(0xFF161622).copy(alpha = 0.4f), Color(0xFF0A0A12).copy(alpha = 0.25f))),
            glassBorder = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent, Color(0xFF8B5CF6).copy(alpha = 0.2f))),
            neonGlow = Color(0xFF8B5CF6),
        )
    }

object FinalTalkTheme {
    val gradients: FinalTalkGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalFinalTalkGradients.current
}

@Composable
fun FinalTalkTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val primaryColors = if (darkTheme) DarkPrimaryGradient else LightPrimaryGradient
    val backgroundColors =
        if (darkTheme) {
            listOf(CosmicSpaceVoid, CosmicNebulaPurple, CosmicNebulaBlue)
        } else {
            listOf(LightBackground, LightBackground, LightSurface)
        }

    val customGradients =
        FinalTalkGradients(
            primaryGradient = Brush.linearGradient(primaryColors),
            backgroundGradient = Brush.verticalGradient(backgroundColors),
            glassBackground = if (darkTheme) {
                Brush.linearGradient(listOf(Color(0xFF161622).copy(alpha = 0.45f), Color(0xFF0A0A12).copy(alpha = 0.3f)))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.5f)))
            },
            glassBorder = if (darkTheme) {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color(0xFF8B5CF6).copy(alpha = 0.25f)))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent, Color(0xFF6D28D9).copy(alpha = 0.2f)))
            },
            neonGlow = if (darkTheme) Color(0xFF8B5CF6) else Color(0xFF6D28D9)
        )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalFinalTalkGradients provides customGradients,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
