package com.example.aicollect.presentation.collection

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared display formatting for [com.example.aicollect.application.items.Item], used by
 * Home/My Vault/Detail so a value/date reads the same everywhere. */
object ItemFormatting {

    private val SPANISH = Locale("es", "ES")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", SPANISH)

    /** "Sin valorar" when null — never a fabricated number (see PortfolioAnalytics kdoc). */
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
