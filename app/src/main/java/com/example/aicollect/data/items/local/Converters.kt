package com.example.aicollect.data.items.local

import androidx.room.TypeConverter
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.application.items.ValuationSearch

/**
 * No hay ninguna librería JSON en el proyecto (ver PROJECT_CONTEXT.md, decisión consciente de no
 * añadir otro procesador de anotaciones a un toolchain ya frágil) — se codifican las listas con
 * separadores de control ASCII que nunca aparecen en una URL de Storage ni en un número/fecha.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(ITEM_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(ITEM_SEPARATOR)

    @TypeConverter
    fun fromPricePoints(value: List<PricePoint>): String =
        value.joinToString(ITEM_SEPARATOR) { "${it.fecha}$FIELD_SEPARATOR${it.precio}" }

    @TypeConverter
    fun toPricePoints(value: String): List<PricePoint> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(ITEM_SEPARATOR).map { entry ->
                val (fecha, precio) = entry.split(FIELD_SEPARATOR)
                PricePoint(fecha.toLong(), precio.toDouble())
            }
        }

    @TypeConverter
    fun fromValuationSearches(value: List<ValuationSearch>): String =
        value.joinToString(ITEM_SEPARATOR) { "${it.label}$FIELD_SEPARATOR${it.url}" }

    @TypeConverter
    fun toValuationSearches(value: String): List<ValuationSearch> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(ITEM_SEPARATOR).map { entry ->
                val (label, url) = entry.split(FIELD_SEPARATOR)
                ValuationSearch(label, url)
            }
        }

    private companion object {
        // ASCII Unit/Record Separator control chars — never legally appear in a Storage URL,
        // a Long, or a Double, so a plain split() is safe without escaping.
        const val ITEM_SEPARATOR = ""
        const val FIELD_SEPARATOR = ""
    }
}
