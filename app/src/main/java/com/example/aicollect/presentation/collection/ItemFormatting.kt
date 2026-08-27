// Funciones compartidas de formato de precio y fecha de un ítem, usadas por Home, My Vault y el detalle para que un mismo valor se muestre siempre igual.
package com.example.aicollect.presentation.collection

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ItemFormatting {

    private val SPANISH = Locale("es", "ES")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", SPANISH)

    fun formatValue(value: Double?, currency: String): String {
        if (value == null) return "Sin valorar"
        return "%,.0f%s".format(SPANISH, value, currencySymbol(currency))
    }

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
