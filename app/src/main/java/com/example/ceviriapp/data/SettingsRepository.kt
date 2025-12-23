package com.example.ceviriapp.data

import android.content.Context

class SettingsRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    fun saveThemePreference(isDark: Boolean) {
        sharedPreferences.edit().putBoolean(THEME_KEY, isDark).apply()
    }

    fun getThemePreference(): Boolean? {
        return if (sharedPreferences.contains(THEME_KEY)) {
            sharedPreferences.getBoolean(THEME_KEY, false)
        } else {
            null // Hiçbir ayar kaydedilmemişse null döndür
        }
    }

    companion object {
        private const val THEME_KEY = "is_dark_theme"
    }
}
