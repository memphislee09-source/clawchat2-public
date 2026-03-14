package ai.openclaw.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal fun tr(en: String, zh: String): String {
  val lang = LocalConfiguration.current.locales[0]?.language ?: "en"
  return if (lang.startsWith("zh")) zh else en
}
