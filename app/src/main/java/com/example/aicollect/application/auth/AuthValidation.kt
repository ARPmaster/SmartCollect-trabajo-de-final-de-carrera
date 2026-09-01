/** Reglas de validación de contraseña y correo compartidas por registro y cambio de credenciales
 * (Seguridad). Centralizadas aquí para que ambas pantallas exijan exactamente lo mismo. */
package com.example.aicollect.application.auth

/** Código de requisito incumplido, sin texto embebido: el mapeo a mensaje se hace en
 * presentación (patrón UiText) para que el dominio no decida el idioma de la interfaz. */
sealed interface PasswordError {
    data class TooShort(val minLength: Int) : PasswordError
    data object MissingUppercase : PasswordError
    data object MissingLowercase : PasswordError
    data object MissingDigit : PasswordError
    data object MissingSymbol : PasswordError
}

sealed interface EmailError {
    data object InvalidFormat : EmailError
}

object AuthValidation {

    const val MIN_PASSWORD_LENGTH = 8

    private val UPPERCASE = Regex("[A-ZÁÉÍÓÚÑ]")
    private val LOWERCASE = Regex("[a-záéíóúñ]")
    private val DIGIT = Regex("[0-9]")
    private val SYMBOL = Regex("[^A-Za-zÁÉÍÓÚÑáéíóúñ0-9]")

    /** Devuelve el primer requisito incumplido, o null si la contraseña es válida. */
    fun passwordError(password: String): PasswordError? = when {
        password.length < MIN_PASSWORD_LENGTH -> PasswordError.TooShort(MIN_PASSWORD_LENGTH)
        !UPPERCASE.containsMatchIn(password) -> PasswordError.MissingUppercase
        !LOWERCASE.containsMatchIn(password) -> PasswordError.MissingLowercase
        !DIGIT.containsMatchIn(password) -> PasswordError.MissingDigit
        !SYMBOL.containsMatchIn(password) -> PasswordError.MissingSymbol
        else -> null
    }

    private val EMAIL_FORMAT = Regex("^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$")

    /** Devuelve un error si el correo no tiene formato válido (RF-02), o null si es válido. No
     * restringe por dominio: una lista blanca de proveedores rechazaría correos
     * institucionales/corporativos legítimos (incluidos los del tribunal evaluador). */
    fun emailError(email: String): EmailError? {
        val trimmed = email.trim()
        return if (!EMAIL_FORMAT.matches(trimmed)) EmailError.InvalidFormat else null
    }
}
