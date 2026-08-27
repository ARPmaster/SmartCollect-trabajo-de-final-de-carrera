// Guarda y lee la preferencia de modo oscuro/claro del usuario en SharedPreferences.
package com.example.aicollect.data

import android.content.Context

object DarkModePreferences {
    private const val PREFS_NAME = "aicollect_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkModeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DARK_MODE, false)

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }
}
