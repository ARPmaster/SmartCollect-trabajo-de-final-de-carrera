// Funciones compartidas de formato de precio y fecha de un ítem, usadas por Home, My Vault y el detalle para que un mismo valor se muestre siempre igual.
package com.example.aicollect.presentation.collection

import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ItemFormatting {

    private val SPANISH = Locale("es", "ES")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", SPANISH)

    /** Valor que puede no existir aún (sin valorar): el texto para ese caso se resuelve en
     * presentación (patrón UiText) en vez de devolverse embebido como String. */
    fun formatValue(value: Double?, currency: String): UiText {
        if (value == null) return UiText.StringResource(R.string.label_not_valued)
        return UiText.DynamicString(formatKnownValue(value, currency))
    }

    /** Igual que [formatValue] pero para un valor que ya se sabe presente (p. ej. un rango de
     * valoración ya comprobado, o un punto de una gráfica), sin pasar por UiText. */
    fun formatKnownValue(value: Double, currency: String): String =
        "%,.0f%s".format(SPANISH, value, currencySymbol(currency))

    fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER)

    fun formatChangePercent(percent: Float?): String? =
        percent?.let { "%+.1f%%".format(SPANISH, it) }

    private fun currencySymbol(currency: String): String = when (currency.uppercase(SPANISH)) {
        "EUR" -> "€"
        "USD" -> "$"
        else -> currency
    }
}
