package com.miqu.thinktwice.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miqu.thinktwice.R

val SunghyunSans = FontFamily(
    Font(R.font.sunghyun_sans_regular, FontWeight.Normal),
    Font(R.font.sunghyun_sans_medium, FontWeight.Medium),
    Font(R.font.sunghyun_sans_semibold, FontWeight.SemiBold),
    Font(R.font.sunghyun_sans_bold, FontWeight.Bold),
    Font(R.font.sunghyun_sans_extrabold, FontWeight.ExtraBold),
    Font(R.font.sunghyun_sans_black, FontWeight.Black),
)

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0, heading: Boolean = false) = TextStyle(
    fontFamily = SunghyunSans,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeightStyle = tight,
    lineBreak = if (heading) LineBreak.Heading else LineBreak.Paragraph,
)

/**
 * Type scale (size/line · weight · tracking):
 * display 34/38 Black −2% · headline 28/32 ExtraBold −1.5% · title large 22/28 ExtraBold −1%
 * title 17/22 Bold · title small 15/20 Bold · body 15/22 Medium · body small 14/20 Regular
 * label 13/16 Bold · overline 12/16 ExtraBold +8% · caption 12/16 Medium.
 */
val AppTypography = Typography(
    displayLarge = style(40, 44, FontWeight.Black, -0.02, heading = true),
    displayMedium = style(36, 40, FontWeight.Black, -0.02, heading = true),
    displaySmall = style(34, 38, FontWeight.Black, -0.02, heading = true),
    headlineLarge = style(30, 34, FontWeight.ExtraBold, -0.015, heading = true),
    headlineMedium = style(28, 32, FontWeight.ExtraBold, -0.015, heading = true),
    headlineSmall = style(24, 30, FontWeight.ExtraBold, -0.01, heading = true),
    titleLarge = style(22, 28, FontWeight.ExtraBold, -0.01, heading = true),
    titleMedium = style(17, 22, FontWeight.Bold),
    titleSmall = style(15, 20, FontWeight.Bold),
    bodyLarge = style(15, 22, FontWeight.Medium),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Medium),
    labelLarge = style(13, 16, FontWeight.Bold, 0.01),
    labelMedium = style(12, 16, FontWeight.ExtraBold, 0.08),
    labelSmall = style(12, 16, FontWeight.Bold),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
