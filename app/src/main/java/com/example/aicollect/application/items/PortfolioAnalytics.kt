package com.example.aicollect.application.items

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure-Kotlin aggregations over the user's real items, shared by Home/My Vault/Detail so the
 * "evolución (6 meses)" chart and distribution stats are computed the same way everywhere.
 * Nothing here ever extrapolates a value that wasn't actually recorded — an item with no
 * [Item.valoracionActual] simply contributes 0, same principle as `refreshValuation`
 * (PROJECT_CONTEXT roadmap item 2: never invent or extrapolate a market value).
 */
object PortfolioAnalytics {

    private val SPANISH = Locale("es", "ES")

    fun totalValue(items: List<Item>): Double = items.sumOf { it.valoracionActual ?: 0.0 }

    /** Portfolio total at the end of each of the last [monthCount] calendar months (oldest
     * first, current month last) — for each item, carries forward its latest known valuation as
     * of that month from [Item.historialPrecios]; before the item existed, or with no valuation
     * ever recorded, it contributes 0. */
    fun monthlyEvolution(
        items: List<Item>,
        monthCount: Int = 6,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<Float> {
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone))
        return (monthCount - 1 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            val cutoffMillis = month.atEndOfMonth().atTime(23, 59, 59)
                .atZone(zone).toInstant().toEpochMilli()
            items.sumOf { item ->
                item.historialPrecios
                    .filter { it.fecha <= cutoffMillis }
                    .maxByOrNull { it.fecha }
                    ?.precio ?: 0.0
            }.toFloat()
        }
    }

    /** Spanish 3-letter month abbreviations (e.g. "ENE") for the same window as [monthlyEvolution]. */
    fun monthLabels(monthCount: Int = 6, nowMillis: Long = System.currentTimeMillis()): List<String> {
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone))
        return (monthCount - 1 downTo 0).map { offset ->
            currentMonth.minusMonths(offset.toLong()).month
                .getDisplayName(TextStyle.SHORT, SPANISH)
                .removeSuffix(".")
                .uppercase(SPANISH)
        }
    }

    /** Percent change from the earliest non-zero month in [evolution] to the last one, or null
     * if there's no earlier non-zero month to compare against (e.g. a brand new collection). */
    fun changePercent(evolution: List<Float>): Float? {
        val first = evolution.dropLast(1).firstOrNull { it > 0f } ?: return null
        val last = evolution.lastOrNull() ?: return null
        return ((last - first) / first) * 100f
    }

    /** Percent change for a single item from its first to its last recorded valuation, or null
     * with fewer than 2 points (nothing to compare yet — most items today, no eBay integration). */
    fun itemChangePercent(item: Item): Float? {
        val sorted = item.historialPrecios.sortedBy { it.fecha }
        if (sorted.size < 2) return null
        val first = sorted.first().precio
        if (first == 0.0) return null
        val last = sorted.last().precio
        return (((last - first) / first) * 100f).toFloat()
    }

    /** Percent breakdown of [items] by [keySelector] (e.g. deporte, estado), sorted descending. */
    fun distributionBy(items: List<Item>, keySelector: (Item) -> String): List<Pair<String, Int>> {
        if (items.isEmpty()) return emptyList()
        val total = items.size
        return items.groupingBy(keySelector).eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to ((it.value * 100f) / total).roundToInt() }
    }

    fun topValued(items: List<Item>, limit: Int = 3): List<Item> =
        items.filter { it.valoracionActual != null }
            .sortedByDescending { it.valoracionActual }
            .take(limit)
}
