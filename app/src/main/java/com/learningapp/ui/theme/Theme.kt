package com.learningapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/** Material colour scheme from our tokens, so Material parts (dialogs, switches, text fields) match. */
private fun scheme(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.violet, onPrimary = Color.White, secondary = c.violetLight, tertiary = c.teal,
        background = c.screenBg, onBackground = c.ink, surface = c.cardBg, onSurface = c.bodyText,
        surfaceVariant = c.track, onSurfaceVariant = c.muted, outline = c.line,
        surfaceContainerHigh = c.cardBg, surfaceContainerHighest = c.track,
    )
} else {
    lightColorScheme(
        primary = c.violet, onPrimary = Color.White, secondary = c.violetLight, tertiary = c.teal,
        background = c.screenBg, onBackground = c.ink, surface = c.cardBg, onSurface = c.bodyText,
        surfaceVariant = c.track, onSurfaceVariant = c.muted, outline = c.line,
    )
}

/** Follows the phone's light/dark setting. */
@Composable
fun LearningAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = scheme(colors),
            typography = appTypography(colors),
            shapes = AppShapes,
            content = content,
        )
    }
}
