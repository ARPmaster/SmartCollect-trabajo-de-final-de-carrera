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

    // Proveedores de correo genéricos reales más habituales. Cualquier otro dominio se rechaza a
    // propósito (evita altas con dominios inventados, de un solo uso o con erratas), a costa de no
    // admitir correos corporativos/institucionales propios — es una limitación consciente, no un
    // descuido.
    private val ALLOWED_EMAIL_DOMAINS = setOf(
        "gmail.com", "googlemail.com",
        "outlook.com", "outlook.es", "hotmail.com", "hotmail.es", "live.com", "msn.com",
        "yahoo.com", "yahoo.es",
        "icloud.com", "me.com", "mac.com",
        "protonmail.com", "proton.me",
        "zoho.com",
        "gmx.com", "gmx.es",
        "aol.com",
        "yandex.com",
    )

    /** Devuelve un mensaje de error si el correo no tiene formato válido o su dominio no está en
     * la lista de proveedores genéricos admitidos, o null si es válido. */
    fun emailError(email: String): String? {
        val trimmed = email.trim()
        if (!EMAIL_FORMAT.matches(trimmed)) return "Introduce un correo electrónico válido."
        val domain = trimmed.substringAfterLast('@').lowercase()
        return if (domain !in ALLOWED_EMAIL_DOMAINS) {
            "Usa un correo de un proveedor habitual (Gmail, Outlook, Yahoo, iCloud...)."
        } else {
            null
        }
    }
}
