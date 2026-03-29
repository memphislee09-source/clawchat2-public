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
    background = Color(0xFFF7F8FB),
    surface = Color(0xFFFFFFFF),
    surfaceStrong = Color(0xFFF1F4F8),
    border = Color(0xFFE4E9F0),
    borderStrong = Color(0xFFD6DDE7),
    text = Color(0xFF2E3644),
    textSecondary = Color(0xFF768297),
    textTertiary = Color(0xFFA0A9B8),
    accent = Color(0xFF67C979),
    accentSoft = Color(0xFFDDF3E2),
    success = Color(0xFF5CCB79),
    successSoft = Color(0xFFD9F3E0),
    warning = Color(0xFF7ACEEA),
    warningSoft = Color(0xFFE3F5FB),
    danger = Color(0xFFEF6C66),
    dangerSoft = Color(0xFFFFE3E1),
    codeBg = Color(0xFF171D27),
    codeText = Color(0xFFEAF0F7),
  )

private val mobileDarkPalette =
  MobileUiPalette(
    background = Color(0xFF0A111A),
    surface = Color(0xFF111927),
    surfaceStrong = Color(0xFF1A2231),
    border = Color(0xFF232D3E),
    borderStrong = Color(0xFF313E54),
    text = Color(0xFFE2E8F2),
    textSecondary = Color(0xFFA1ABBD),
    textTertiary = Color(0xFF6E788C),
    accent = Color(0xFF67CC79),
    accentSoft = Color(0xFF223B2B),
    success = Color(0xFF67CC79),
    successSoft = Color(0xFF1E3928),
    warning = Color(0xFF6BC7E8),
    warningSoft = Color(0xFF1B3141),
    danger = Color(0xFFFF736A),
    dangerSoft = Color(0xFF3A2326),
    codeBg = Color(0xFF0E1520),
    codeText = Color(0xFFE8EDF7),
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
  get() = Brush.verticalGradient(listOf(mobileBackground, mobileSurfaceStrong))

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
