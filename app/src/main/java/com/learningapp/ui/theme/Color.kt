package com.learningapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** All app colours. Light values are the wireframe tokens (wireframes/README.md); dark mirrors them. */
@Immutable
data class AppColors(
    val screenBg: Color,
    val cardBg: Color,
    val line: Color,
    val ink: Color,
    val bodyText: Color,
    val muted: Color,
    val violet: Color,
    val violetLight: Color,
    val pink: Color,
    val teal: Color,
    val track: Color,
    val tagBg: Color,
    /** Background of a selected option card. */
    val selectedBg: Color,
    val todayBorder: Color,
    val dateMuted: Color,
    val lockedBorder: Color,
    val lockedText: Color,
    val navIdle: Color,
    val rightBg: Color,
    val rightInk: Color,
    val wrongBg: Color,
    val wrongInk: Color,
    val closeInk: Color,
    val recording: Color,
    val isDark: Boolean,
)

val LightColors = AppColors(
    screenBg = Color(0xFFEFEEF3), cardBg = Color(0xFFFAFAFC), line = Color(0xFFE1DFE8),
    ink = Color(0xFF2E1760), bodyText = Color(0xFF3D3557), muted = Color(0xFF9A95AB),
    violet = Color(0xFFA24CF0), violetLight = Color(0xFFC98BFF), pink = Color(0xFFF0A0D8), teal = Color(0xFF4CC3D9),
    track = Color(0xFFE2DCEF), tagBg = Color(0xFFEFE4FD), selectedBg = Color(0xFFF6EEFE),
    todayBorder = Color(0xFFCFC9DC), dateMuted = Color(0xFFA9A4B8), lockedBorder = Color(0xFFD4CFE0), lockedText = Color(0xFFBBB5C9),
    navIdle = Color(0xFFD6D2E0),
    rightBg = Color(0xFFEAFAF1), rightInk = Color(0xFF1F7A4D), wrongBg = Color(0xFFFDECEF), wrongInk = Color(0xFFB4234A),
    closeInk = Color(0xFFB7791F), recording = Color(0xFFF05A8C),
    isDark = false,
)

val DarkColors = AppColors(
    screenBg = Color(0xFF15121F), cardBg = Color(0xFF221D31), line = Color(0xFF332C47),
    ink = Color(0xFFEDE7FB), bodyText = Color(0xFFCFC8E3), muted = Color(0xFF8E87A6),
    violet = Color(0xFFB576F7), violetLight = Color(0xFFD3A6FF), pink = Color(0xFFF0A0D8), teal = Color(0xFF5ED0E4),
    track = Color(0xFF332C47), tagBg = Color(0xFF2E2347), selectedBg = Color(0xFF34265A),
    todayBorder = Color(0xFF4A4163), dateMuted = Color(0xFF6E6788), lockedBorder = Color(0xFF3A3350), lockedText = Color(0xFF5F5878),
    navIdle = Color(0xFF3A3350),
    rightBg = Color(0xFF173A2A), rightInk = Color(0xFF7FE0B0), wrongBg = Color(0xFF3F1C27), wrongInk = Color(0xFFFF9BB5),
    closeInk = Color(0xFFF2C36B), recording = Color(0xFFF46B98),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

/** Shortcut: the current theme's colours. */
val appColors: AppColors @Composable @ReadOnlyComposable get() = LocalAppColors.current

// Token names used across the screens; they follow the current light/dark theme.
val ScreenBg: Color @Composable @ReadOnlyComposable get() = appColors.screenBg
val CardBg: Color @Composable @ReadOnlyComposable get() = appColors.cardBg
val Line: Color @Composable @ReadOnlyComposable get() = appColors.line
val Ink: Color @Composable @ReadOnlyComposable get() = appColors.ink
val BodyText: Color @Composable @ReadOnlyComposable get() = appColors.bodyText
val Muted: Color @Composable @ReadOnlyComposable get() = appColors.muted
val Violet: Color @Composable @ReadOnlyComposable get() = appColors.violet
val VioletLight: Color @Composable @ReadOnlyComposable get() = appColors.violetLight
val Pink: Color @Composable @ReadOnlyComposable get() = appColors.pink
val Teal: Color @Composable @ReadOnlyComposable get() = appColors.teal
val Track: Color @Composable @ReadOnlyComposable get() = appColors.track
val TagBg: Color @Composable @ReadOnlyComposable get() = appColors.tagBg

val VioletGradient: Brush @Composable @ReadOnlyComposable get() = Brush.linearGradient(listOf(appColors.violet, appColors.violetLight))
