package ai.openclaw.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object AppLanguageManager {
  private const val key = "app.language"
  private const val prefsName = "openclaw.node"

  fun readPreferredLanguage(context: Context): String {
    val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    return prefs.getString(key, "system")?.trim().orEmpty().ifBlank { "system" }
  }

  fun wrapContext(base: Context, language: String): Context {
    if (language == "system") return base
    val locale = Locale.forLanguageTag(language)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    config.setLocales(LocaleList(locale))
    return base.createConfigurationContext(config)
  }
}
