package ai.openclaw.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ai.openclaw.app.R

internal val mobileBackground = Color(0xFFF4F8F2)
internal val mobileBackgroundGradient =
  Brush.verticalGradient(
    listOf(
      mobileBackground,
      mobileBackground,
    ),
  )

internal val mobileSurface = Color(0xFFFFFFFF)
internal val mobileSurfaceStrong = Color(0xFFEAF3E7)
internal val mobileBorder = Color(0xFFD4E1D0)
internal val mobileBorderStrong = Color(0xFFBFD0BC)
internal val mobileText = Color(0xFF182119)
internal val mobileTextSecondary = Color(0xFF556358)
internal val mobileTextTertiary = Color(0xFF8B978D)
internal val mobileAccent = Color(0xFF49A25A)
internal val mobileAccentSoft = Color(0xFFE6F4E8)
internal val mobileSuccess = Color(0xFF2E8A51)
internal val mobileSuccessSoft = Color(0xFFE8F6ED)
internal val mobileWarning = Color(0xFFB67B28)
internal val mobileWarningSoft = Color(0xFFFFF6E7)
internal val mobileDanger = Color(0xFFBF5A5A)
internal val mobileDangerSoft = Color(0xFFFBECEC)
internal val mobileCodeBg = Color(0xFF182119)
internal val mobileCodeText = Color(0xFFEAF4EB)

internal val mobileFontFamily =
  FontFamily(
    Font(resId = R.font.manrope_400_regular, weight = FontWeight.Normal),
    Font(resId = R.font.manrope_500_medium, weight = FontWeight.Medium),
    Font(resId = R.font.manrope_600_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.manrope_700_bold, weight = FontWeight.Bold),
  )

internal val mobileTitle1 =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.5).sp,
  )

internal val mobileTitle2 =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.3).sp,
  )

internal val mobileHeadline =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.1).sp,
  )

internal val mobileBody =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  )

internal val mobileCallout =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  )

internal val mobileCaption1 =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
  )

internal val mobileCaption2 =
  TextStyle(
    fontFamily = mobileFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.4.sp,
  )

internal val mobileTypography =
  Typography(
    displayLarge = mobileTitle1,
    titleLarge = mobileTitle1,
    titleMedium = mobileTitle2,
    titleSmall = mobileHeadline,
    bodyLarge = mobileBody,
    bodyMedium = mobileBody,
    bodySmall = mobileCallout,
    labelLarge = mobileHeadline,
    labelMedium = mobileCaption1,
    labelSmall = mobileCaption2,
  )
