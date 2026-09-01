// Mapea los códigos de error de AuthValidation (dominio) a UiText (presentación), compartido
// entre las pantallas de registro y seguridad para que ambas muestren el mismo texto.
package com.example.aicollect.presentation.auth

import com.example.aicollect.R
import com.example.aicollect.application.auth.EmailError
import com.example.aicollect.application.auth.PasswordError
import com.example.aicollect.presentation.UiText

fun PasswordError.asUiText(): UiText = when (this) {
    is PasswordError.TooShort -> UiText.StringResource(R.string.error_password_too_short, listOf(minLength))
    PasswordError.MissingUppercase -> UiText.StringResource(R.string.error_password_missing_uppercase)
    PasswordError.MissingLowercase -> UiText.StringResource(R.string.error_password_missing_lowercase)
    PasswordError.MissingDigit -> UiText.StringResource(R.string.error_password_missing_digit)
    PasswordError.MissingSymbol -> UiText.StringResource(R.string.error_password_missing_symbol)
}

fun EmailError.asUiText(): UiText = when (this) {
    EmailError.InvalidFormat -> UiText.StringResource(R.string.error_email_invalid_format)
}
