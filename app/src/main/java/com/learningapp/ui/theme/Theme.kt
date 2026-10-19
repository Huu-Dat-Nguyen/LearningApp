package com.learningapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Light theme only for now; dark theme comes later.
private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = VioletLight,
    tertiary = Teal,
    background = ScreenBg,
    onBackground = Ink,
    surface = CardBg,
    onSurface = BodyText,
    surfaceVariant = Track,
    onSurfaceVariant = Muted,
    outline = Line,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun LearningAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
