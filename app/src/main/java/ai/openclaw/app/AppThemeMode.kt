package ai.openclaw.app

enum class AppThemeMode(val rawValue: String) {
  System("system"),
  Light("light"),
  Dark("dark"),
  ;

  companion object {
    fun fromRawValue(value: String?): AppThemeMode {
      return entries.firstOrNull { it.rawValue == value?.trim()?.lowercase() } ?: System
    }
  }
}
