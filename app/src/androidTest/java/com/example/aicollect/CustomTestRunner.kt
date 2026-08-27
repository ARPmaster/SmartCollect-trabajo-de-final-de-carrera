// Instrumentation runner de las pruebas: sustituye la Application real por una que permite a Hilt generar sus propios componentes de inyección de dependencias de prueba.
package com.example.aicollect

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
