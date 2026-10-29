package io.github.christiandonatti.huginn.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.christiandonatti.huginn.R

/** The Huginn palette (Noite, Papel, Ouro and Fiorde from the identity page). */
object HColors {
    val Ink = Color(0xFF14110D)
    val Panel = Color(0xFF1E1A14)
    val Surface = Color(0x12FFF8E6)
    val SurfaceHigh = Color(0x24FFF8E6)
    val Stroke = Color(0x22FFF8E6)
    val Text = Color(0xFFF4EEE3)
    val TextSoft = Color(0xFFD3CBBB)
    val TextMuted = Color(0xFFA89E8A)
    val Accent = Color(0xFFE1B552)
    val AccentInk = Color(0xFF14110D)
    /** Default glow (Fiorde) before a game or app gives its own colour. */
    val Glow = Color(0xFF2A7FB0)
    val Ok = Color(0xFF6FD08C)
}

val Unbounded = FontFamily(
    Font(R.font.unbounded_500, FontWeight.Medium),
    Font(R.font.unbounded_600, FontWeight.SemiBold),
    Font(R.font.unbounded_700, FontWeight.Bold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
)

private val base = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium)

private fun display(weight: FontWeight, size: Float, line: Float) = TextStyle(
    fontFamily = Unbounded,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (size * -0.02f).sp,
)

private val HuginnTypography = Typography(
    displaySmall = display(FontWeight.Bold, 30f, 34f),
    headlineSmall = display(FontWeight.Bold, 22f, 26f),
    titleLarge = display(FontWeight.SemiBold, 18f, 22f),
    titleMedium = display(FontWeight.SemiBold, 15f, 20f),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = base.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp),
)

@Composable
fun HuginnTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = HColors.Text, onPrimary = HColors.Ink,
        secondary = HColors.Accent, onSecondary = HColors.AccentInk,
        background = HColors.Ink, onBackground = HColors.Text,
        surface = HColors.Panel, onSurface = HColors.Text,
        surfaceVariant = HColors.SurfaceHigh, onSurfaceVariant = HColors.TextSoft,
    )
    MaterialTheme(colorScheme = scheme, typography = HuginnTypography) {
        CompositionLocalProvider(LocalContentColor provides HColors.Text, content = content)
    }
}
