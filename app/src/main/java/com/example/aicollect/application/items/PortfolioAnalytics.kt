/**
 * Cálculos agregados sobre la colección del usuario (valor total, evolución mensual, reparto
 * por categoría, ítems mejor valorados) reutilizados por las distintas pantallas.
 * */
package com.example.aicollect.application.items
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

object PortfolioAnalytics {

    private val SPANISH = Locale("es", "ES")

    fun totalValue(items: List<Item>): Double = items.sumOf { it.valoracionActual ?: 0.0 }

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

    fun changePercent(evolution: List<Float>): Float? {
        val first = evolution.dropLast(1).firstOrNull { it > 0f } ?: return null
        val last = evolution.lastOrNull() ?: return null
        return ((last - first) / first) * 100f
    }

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
