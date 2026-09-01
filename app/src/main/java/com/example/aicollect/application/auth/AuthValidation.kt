/** Reglas de validación de contraseña y correo compartidas por registro y cambio de credenciales
 * (Seguridad). Centralizadas aquí para que ambas pantallas exijan exactamente lo mismo. */
package com.example.aicollect.application.auth

object AuthValidation {

    const val MIN_PASSWORD_LENGTH = 8

    private val UPPERCASE = Regex("[A-ZÁÉÍÓÚÑ]")
    private val LOWERCASE = Regex("[a-záéíóúñ]")
    private val DIGIT = Regex("[0-9]")
    private val SYMBOL = Regex("[^A-Za-zÁÉÍÓÚÑáéíóúñ0-9]")

    /** Devuelve el primer requisito incumplido, o null si la contraseña es válida. */
    fun passwordError(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH ->
            "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
        !UPPERCASE.containsMatchIn(password) ->
            "La contraseña debe incluir al menos una letra mayúscula."
        !LOWERCASE.containsMatchIn(password) ->
            "La contraseña debe incluir al menos una letra minúscula."
        !DIGIT.containsMatchIn(password) ->
            "La contraseña debe incluir al menos un número."
        !SYMBOL.containsMatchIn(password) ->
            "La contraseña debe incluir al menos un signo (por ejemplo: !?#\$%&*)."
        else -> null
    }

    private val EMAIL_FORMAT = Regex("^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$")

    /** Devuelve un mensaje de error si el correo no tiene formato válido (RF-02), o null si es
     * válido. No restringe por dominio: una lista blanca de proveedores rechazaría correos
     * institucionales/corporativos legítimos (incluidos los del tribunal evaluador). */
    fun emailError(email: String): String? {
        val trimmed = email.trim()
        return if (!EMAIL_FORMAT.matches(trimmed)) "Introduce un correo electrónico válido." else null
    }
}
