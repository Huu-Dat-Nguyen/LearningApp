package com.learningapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.learningapp.R

/** Plus Jakarta Sans (SIL OFL 1.1, licences/PlusJakartaSans-OFL.txt): one variable font, one entry per weight we use. */
@OptIn(ExperimentalTextApi::class)
private val AppFont = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.plus_jakarta_sans, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

/** Text styles with the theme's colours baked in, so Text() looks right in light and dark. */
fun appTypography(c: AppColors) = Typography(
    headlineLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.5).sp, color = c.ink),
    headlineSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = c.ink),
    titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.ink),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = c.ink),
    bodyLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = c.bodyText),
    bodyMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = c.bodyText),
    labelLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp, color = c.ink),
    labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = c.muted),
    labelSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp, color = c.muted),
)
