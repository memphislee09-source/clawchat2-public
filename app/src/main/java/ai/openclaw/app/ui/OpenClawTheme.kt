package ai.openclaw.app.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import ai.openclaw.app.AppThemeMode

private fun clawChatLightColorScheme() =
  lightColorScheme(
    primary = mobileAccent,
    onPrimary = Color(0xFF072012),
    primaryContainer = mobileAccentSoft,
    onPrimaryContainer = mobileText,
    secondary = mobileSuccess,
    onSecondary = Color(0xFF071E18),
    secondaryContainer = mobileSuccessSoft,
    onSecondaryContainer = mobileText,
    tertiary = mobileWarning,
    onTertiary = Color(0xFF081A22),
    tertiaryContainer = mobileWarningSoft,
    onTertiaryContainer = mobileText,
    error = mobileDanger,
    onError = Color(0xFF280808),
    errorContainer = mobileDangerSoft,
    onErrorContainer = mobileText,
    background = mobileBackground,
    onBackground = mobileText,
    surface = mobileSurface,
    onSurface = mobileText,
    surfaceVariant = mobileSurfaceStrong,
    onSurfaceVariant = mobileTextSecondary,
    outline = mobileBorder,
    outlineVariant = mobileBorderStrong,
  )

private fun clawChatDarkColorScheme() =
  darkColorScheme(
    primary = mobileAccent,
    onPrimary = Color(0xFF0F1611),
    primaryContainer = mobileAccentSoft,
    onPrimaryContainer = mobileText,
    secondary = mobileSuccess,
    onSecondary = Color(0xFF0F1611),
    secondaryContainer = mobileSuccessSoft,
    onSecondaryContainer = mobileText,
    tertiary = mobileWarning,
    onTertiary = Color(0xFF1A130B),
    tertiaryContainer = mobileWarningSoft,
    onTertiaryContainer = mobileText,
    error = mobileDanger,
    onError = Color(0xFF1D1111),
    errorContainer = mobileDangerSoft,
    onErrorContainer = mobileText,
    background = mobileBackground,
    onBackground = mobileText,
    surface = mobileSurface,
    onSurface = mobileText,
    surfaceVariant = mobileSurfaceStrong,
    onSurfaceVariant = mobileTextSecondary,
    outline = mobileBorder,
    outlineVariant = mobileBorderStrong,
  )

@Composable
fun OpenClawTheme(
  appThemeMode: AppThemeMode = AppThemeMode.System,
  activity: Activity? = null,
  content: @Composable () -> Unit,
) {
  val systemDarkMode = isSystemInDarkTheme()
  val darkMode =
    when (appThemeMode) {
      AppThemeMode.System -> systemDarkMode
      AppThemeMode.Light -> false
      AppThemeMode.Dark -> true
    }
  setMobileUiPalette(darkMode = darkMode)
  val colorScheme = if (darkMode) clawChatDarkColorScheme() else clawChatLightColorScheme()

  SideEffect {
    activity?.window?.let { window ->
      val controller = WindowCompat.getInsetsController(window, window.decorView)
      controller.isAppearanceLightStatusBars = !darkMode
      controller.isAppearanceLightNavigationBars = !darkMode
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = mobileTypography,
    content = content,
  )
}

@Composable
fun overlayContainerColor(): Color {
  return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
}

@Composable
fun overlayIconColor(): Color {
  return MaterialTheme.colorScheme.onSurfaceVariant
}
