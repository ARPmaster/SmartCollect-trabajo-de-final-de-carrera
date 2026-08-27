/** Convierte entre las listas del modelo de dominio (fotos, historial de precios, búsquedas de
 * valoración) y el texto plano que Room guarda en columnas simples, ya que el proyecto no usa
 * ninguna librería de serialización JSON.*/
package com.example.aicollect.data.items.local

import androidx.room.TypeConverter
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.application.items.ValuationSearch

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
        const val ITEM_SEPARATOR = ""
        const val FIELD_SEPARATOR = ""
    }
}
