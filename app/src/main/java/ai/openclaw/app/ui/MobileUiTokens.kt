package ai.openclaw.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ai.openclaw.app.R

internal data class MobileUiPalette(
  val background: Color,
  val surface: Color,
  val surfaceStrong: Color,
  val border: Color,
  val borderStrong: Color,
  val text: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  val accent: Color,
  val accentSoft: Color,
  val success: Color,
  val successSoft: Color,
  val warning: Color,
  val warningSoft: Color,
  val danger: Color,
  val dangerSoft: Color,
  val codeBg: Color,
  val codeText: Color,
)

private val mobileLightPalette =
  MobileUiPalette(
    background = Color(0xFFF4F8F2),
    surface = Color(0xFFFFFFFF),
    surfaceStrong = Color(0xFFEAF3E7),
    border = Color(0xFFD4E1D0),
    borderStrong = Color(0xFFBFD0BC),
    text = Color(0xFF182119),
    textSecondary = Color(0xFF556358),
    textTertiary = Color(0xFF8B978D),
    accent = Color(0xFF49A25A),
    accentSoft = Color(0xFFE6F4E8),
    success = Color(0xFF2E8A51),
    successSoft = Color(0xFFE8F6ED),
    warning = Color(0xFFB67B28),
    warningSoft = Color(0xFFFFF6E7),
    danger = Color(0xFFBF5A5A),
    dangerSoft = Color(0xFFFBECEC),
    codeBg = Color(0xFF182119),
    codeText = Color(0xFFEAF4EB),
  )

private val mobileDarkPalette =
  MobileUiPalette(
    background = Color(0xFF111713),
    surface = Color(0xFF182119),
    surfaceStrong = Color(0xFF202B22),
    border = Color(0xFF314036),
    borderStrong = Color(0xFF435648),
    text = Color(0xFFF1F7F0),
    textSecondary = Color(0xFFB5C3B7),
    textTertiary = Color(0xFF829186),
    accent = Color(0xFF6CCB7C),
    accentSoft = Color(0xFF1F3426),
    success = Color(0xFF65C58A),
    successSoft = Color(0xFF1C3326),
    warning = Color(0xFFD8A24D),
    warningSoft = Color(0xFF362819),
    danger = Color(0xFFE18484),
    dangerSoft = Color(0xFF3A2222),
    codeBg = Color(0xFF0E1410),
    codeText = Color(0xFFE8F2E9),
  )

private object MobileUiPaletteState {
  var currentPalette by mutableStateOf(mobileLightPalette)
}

internal fun setMobileUiPalette(darkMode: Boolean) {
  MobileUiPaletteState.currentPalette = if (darkMode) mobileDarkPalette else mobileLightPalette
}

internal val mobileBackground: Color
  get() = MobileUiPaletteState.currentPalette.background

internal val mobileBackgroundGradient: Brush
  get() = Brush.verticalGradient(listOf(mobileBackground, mobileBackground))

internal val mobileSurface: Color
  get() = MobileUiPaletteState.currentPalette.surface

internal val mobileSurfaceStrong: Color
  get() = MobileUiPaletteState.currentPalette.surfaceStrong

internal val mobileBorder: Color
  get() = MobileUiPaletteState.currentPalette.border

internal val mobileBorderStrong: Color
  get() = MobileUiPaletteState.currentPalette.borderStrong

internal val mobileText: Color
  get() = MobileUiPaletteState.currentPalette.text

internal val mobileTextSecondary: Color
  get() = MobileUiPaletteState.currentPalette.textSecondary

internal val mobileTextTertiary: Color
  get() = MobileUiPaletteState.currentPalette.textTertiary

internal val mobileAccent: Color
  get() = MobileUiPaletteState.currentPalette.accent

internal val mobileAccentSoft: Color
  get() = MobileUiPaletteState.currentPalette.accentSoft

internal val mobileSuccess: Color
  get() = MobileUiPaletteState.currentPalette.success

internal val mobileSuccessSoft: Color
  get() = MobileUiPaletteState.currentPalette.successSoft

internal val mobileWarning: Color
  get() = MobileUiPaletteState.currentPalette.warning

internal val mobileWarningSoft: Color
  get() = MobileUiPaletteState.currentPalette.warningSoft

internal val mobileDanger: Color
  get() = MobileUiPaletteState.currentPalette.danger

internal val mobileDangerSoft: Color
  get() = MobileUiPaletteState.currentPalette.dangerSoft

internal val mobileCodeBg: Color
  get() = MobileUiPaletteState.currentPalette.codeBg

internal val mobileCodeText: Color
  get() = MobileUiPaletteState.currentPalette.codeText

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
