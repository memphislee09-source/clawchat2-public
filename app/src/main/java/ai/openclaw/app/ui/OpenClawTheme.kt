package ai.openclaw.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val clawChatColorScheme =
  lightColorScheme(
    primary = mobileAccent,
    onPrimary = Color.White,
    primaryContainer = mobileAccentSoft,
    onPrimaryContainer = mobileText,
    secondary = mobileSuccess,
    onSecondary = Color.White,
    secondaryContainer = mobileSuccessSoft,
    onSecondaryContainer = mobileText,
    tertiary = mobileWarning,
    onTertiary = Color.White,
    tertiaryContainer = mobileWarningSoft,
    onTertiaryContainer = mobileText,
    error = mobileDanger,
    onError = Color.White,
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
fun OpenClawTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = clawChatColorScheme,
    typography = mobileTypography,
    content = content,
  )
}

@Composable
fun overlayContainerColor(): Color {
  return MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
}

@Composable
fun overlayIconColor(): Color {
  return MaterialTheme.colorScheme.onSurfaceVariant
}
