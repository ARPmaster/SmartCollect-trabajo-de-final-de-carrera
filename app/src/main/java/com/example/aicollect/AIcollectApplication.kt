// Punto de arranque de la aplicación: configura el modo claro/oscuro al iniciar y actúa como raíz del grafo de dependencias de Hilt.
package com.example.aicollect

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.aicollect.data.DarkModePreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIcollectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val nightMode = if (DarkModePreferences.isDarkModeEnabled(this)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
