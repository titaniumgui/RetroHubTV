package com.retrohub.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/** Black, monochrome console palette. Focus is white; colour is reserved for profiles and status. */
object RH {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF111111)
    val SurfaceHi = Color(0xFF1C1C1C)
    val Line = Color(0xFF262626)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFA3A3A3)
    val TextFaint = Color(0xFF6E6E6E)
    val Danger = Color(0xFFFF5A5F)
    val Ok = Color(0xFF34D399)
    val Gold = Color(0xFFFFD60A)
    val TrackOff = Color(0xFF3A3A3A)

    /** Avatar colours for profiles. */
    val ProfileColors = listOf(
        Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFF64748B),
    )

    fun profileColor(index: Int): Color = ProfileColors[Math.floorMod(index, ProfileColors.size)]
}

/** True when Lite mode is active: no scale animations, no crossfades, no backdrops. */
val LocalLite = staticCompositionLocalOf { false }

// No colours in the type scale: text follows LocalContentColor, so it turns black on a focused
// (white) button automatically.
private val typography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
)

@Composable
fun RetroHubTheme(lite: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = RH.Bg,
            onBackground = RH.Text,
            surface = RH.Surface,
            onSurface = RH.Text,
            surfaceVariant = RH.SurfaceHi,
            onSurfaceVariant = RH.TextDim,
            border = Color.White,
        ),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalLite provides lite, LocalContentColor provides RH.Text, content = content)
    }
}
