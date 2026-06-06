package com.wisdomtree.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Colors (exact HTML vars) ──────────────────────────────────────────────────
object WTColors {
    val Bg        = Color(0xFF0A0A0A)
    val Surf      = Color(0xFF131313)
    val Surf2     = Color(0xFF1A1A1A)
    val Surf3     = Color(0xFF202020)
    val Border    = Color(0xFF252525)
    val Border2   = Color(0xFF2E2E2E)
    val Border3   = Color(0xFF444444)
    val Text      = Color(0xFFE2E0D9)
    val Muted     = Color(0xFF555555)
    val Muted2    = Color(0xFF888888)
    val Muted3    = Color(0xFFAAAAAA)
    val Accent    = Color(0xFFB8F050)   // green
    val Accent2   = Color(0xFFF05858)   // red
    val Accent3   = Color(0xFF58A8F0)   // blue
    val Accent4   = Color(0xFFF0B858)   // yellow/orange
    val Accent5   = Color(0xFFC084FC)   // purple

    // Derived
    val AccentDim    = Color(0xFF0D1A02)
    val Accent3Dim   = Color(0xFF081220)
    val Accent5Dim   = Color(0xFF120820)
}

// ── Typography ────────────────────────────────────────────────────────────────
// Using system fonts as fallback (Syne & DM Mono would be bundled as assets in production)
val DMMono = FontFamily.Monospace
val Syne   = FontFamily.Default

val WTTypography = Typography(
    bodySmall   = TextStyle(fontFamily = DMMono, fontSize = 11.sp, color = WTColors.Text),
    bodyMedium  = TextStyle(fontFamily = DMMono, fontSize = 12.sp, color = WTColors.Text),
    bodyLarge   = TextStyle(fontFamily = DMMono, fontSize = 13.sp, color = WTColors.Text),
    labelSmall  = TextStyle(fontFamily = Syne, fontSize = 10.sp, letterSpacing = 0.12.sp),
    labelMedium = TextStyle(fontFamily = Syne, fontSize = 11.sp, fontWeight = FontWeight.Bold),
    labelLarge  = TextStyle(fontFamily = Syne, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
    titleSmall  = TextStyle(fontFamily = Syne, fontSize = 14.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Syne, fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleLarge  = TextStyle(fontFamily = Syne, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
)

private val WTColorScheme = darkColorScheme(
    background        = WTColors.Bg,
    surface           = WTColors.Surf,
    surfaceVariant    = WTColors.Surf2,
    onBackground      = WTColors.Text,
    onSurface         = WTColors.Text,
    primary           = WTColors.Accent,
    onPrimary         = Color.Black,
    secondary         = WTColors.Accent3,
    onSecondary       = Color.Black,
    tertiary          = WTColors.Accent5,
    error             = WTColors.Accent2,
    outline           = WTColors.Border2,
    outlineVariant    = WTColors.Border,
)

@Composable
fun WisdomTreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WTColorScheme,
        typography  = WTTypography,
        content     = content
    )
}
