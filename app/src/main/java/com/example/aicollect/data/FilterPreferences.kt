// Guarda y lee en SharedPreferences los últimos filtros y el orden aplicados por el usuario en Home, para recordarlos entre sesiones.
package com.example.aicollect.data

import android.content.Context

object FilterPreferences {
    private const val PREFS_NAME = "aicollect_prefs"
    private const val KEY_HAS_SAVED = "filter_has_saved"
    private const val KEY_MIN_PRICE = "filter_min_price"
    private const val KEY_MAX_PRICE = "filter_max_price"
    private const val KEY_SPORT = "filter_sport"
    private const val KEY_CONDITION = "filter_condition"
    private const val KEY_SORT_ORDINAL = "filter_sort_ordinal"

    const val DEFAULT_MIN_PRICE = 0
    const val DEFAULT_MAX_PRICE = 1000

    data class SavedFilters(
        val minPrice: Int,
        val maxPrice: Int,
        val sport: String?,
        val condition: String?,
        val sortOrdinal: Int,
    )

    fun hasSavedFilters(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAS_SAVED, false)

    fun load(context: Context): SavedFilters {
        val p = prefs(context)
        return SavedFilters(
            minPrice = p.getInt(KEY_MIN_PRICE, DEFAULT_MIN_PRICE),
            maxPrice = p.getInt(KEY_MAX_PRICE, DEFAULT_MAX_PRICE),
            sport = p.getString(KEY_SPORT, null),
            condition = p.getString(KEY_CONDITION, null),
            sortOrdinal = p.getInt(KEY_SORT_ORDINAL, 0),
        )
    }

    fun save(context: Context, filters: SavedFilters) {
        prefs(context).edit()
            .putBoolean(KEY_HAS_SAVED, true)
            .putInt(KEY_MIN_PRICE, filters.minPrice)
            .putInt(KEY_MAX_PRICE, filters.maxPrice)
            .putString(KEY_SPORT, filters.sport)
            .putString(KEY_CONDITION, filters.condition)
            .putInt(KEY_SORT_ORDINAL, filters.sortOrdinal)
            .apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
