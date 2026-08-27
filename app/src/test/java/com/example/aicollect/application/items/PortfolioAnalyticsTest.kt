// Test unitario de PortfolioAnalytics: cálculos agregados sobre la colección (valor total, evolución mensual, etiquetas de mes, variación porcentual, distribución por categoría y top valorados).
package com.example.aicollect.application.items

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioAnalyticsTest {

    private val zone = ZoneId.systemDefault()

    private fun millisAt(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun item(
        nombre: String = "Item",
        deporte: String = "Running",
        estado: String = "Nuevo",
        valoracionActual: Double? = null,
        historialPrecios: List<PricePoint> = emptyList(),
        createdAt: Long = 0L,
    ) = Item(
        nombre = nombre,
        descripcion = null,
        marca = "",
        modelo = "",
        edicion = null,
        procedencia = null,
        deporte = deporte,
        estado = estado,
        valoracionActual = valoracionActual,
        valoracionMin = null,
        valoracionMax = null,
        valoracionMoneda = "EUR",
        fuenteValoracion = null,
        historialPrecios = historialPrecios,
        confianzaIdentificacion = null,
        createdAt = createdAt,
    )

    @Test
    fun `totalValue sums valoracionActual treating unvalued items as zero`() {
        val items = listOf(
            item(valoracionActual = 100.0),
            item(valoracionActual = null),
            item(valoracionActual = 50.5),
        )
        assertEquals(150.5, PortfolioAnalytics.totalValue(items), 0.001)
    }

    @Test
    fun `totalValue of an empty collection is zero`() {
        assertEquals(0.0, PortfolioAnalytics.totalValue(emptyList()), 0.001)
    }

    @Test
    fun `monthlyEvolution contributes zero before any price and carries the latest one forward`() {
        val now = millisAt(2026, 3, 20)
        val onlyItem = item(historialPrecios = listOf(PricePoint(fecha = millisAt(2026, 2, 10), precio = 100.0)))

        val evolution = PortfolioAnalytics.monthlyEvolution(listOf(onlyItem), monthCount = 3, nowMillis = now)

        assertEquals(listOf(0f, 100f, 100f), evolution)
    }

    @Test
    fun `monthlyEvolution sums every item independently per month`() {
        val now = millisAt(2026, 2, 15)
        val itemA = item(historialPrecios = listOf(PricePoint(millisAt(2026, 1, 5), 100.0)))
        val itemB = item(historialPrecios = listOf(PricePoint(millisAt(2026, 2, 5), 50.0)))

        val evolution = PortfolioAnalytics.monthlyEvolution(listOf(itemA, itemB), monthCount = 2, nowMillis = now)

        assertEquals(listOf(100f, 150f), evolution)
    }

    @Test
    fun `monthlyEvolution of an empty collection is all zeros`() {
        val now = millisAt(2026, 5, 1)
        assertEquals(listOf(0f, 0f), PortfolioAnalytics.monthlyEvolution(emptyList(), monthCount = 2, nowMillis = now))
    }

    @Test
    fun `monthLabels returns monthCount spanish 3-letter abbreviations ending on the current month`() {
        val now = millisAt(2026, 3, 20)
        val labels = PortfolioAnalytics.monthLabels(monthCount = 3, nowMillis = now)
        assertEquals(listOf("ENE", "FEB", "MAR"), labels)
    }

    @Test
    fun `monthLabels wraps across a year boundary`() {
        val now = millisAt(2026, 1, 10)
        val labels = PortfolioAnalytics.monthLabels(monthCount = 2, nowMillis = now)
        assertEquals(listOf("DIC", "ENE"), labels)
    }

    @Test
    fun `changePercent is null when there is no earlier non-zero month`() {
        assertNull(PortfolioAnalytics.changePercent(listOf(0f, 0f, 100f)))
    }

    @Test
    fun `changePercent is null for an empty evolution`() {
        assertNull(PortfolioAnalytics.changePercent(emptyList()))
    }

    @Test
    fun `changePercent computes change from the first non-zero month to the last`() {
        val percent = PortfolioAnalytics.changePercent(listOf(0f, 100f, 150f))
        assertEquals(50f, percent!!, 0.01f)
    }

    @Test
    fun `distributionBy groups by key and rounds percentages sorted descending`() {
        val items = listOf(
            item(deporte = "Running"),
            item(deporte = "Running"),
            item(deporte = "Baloncesto"),
            item(deporte = "Running"),
        )
        val distribution = PortfolioAnalytics.distributionBy(items) { it.deporte }
        assertEquals(listOf("Running" to 75, "Baloncesto" to 25), distribution)
    }

    @Test
    fun `distributionBy of an empty collection is empty`() {
        assertEquals(emptyList<Pair<String, Int>>(), PortfolioAnalytics.distributionBy(emptyList()) { it.deporte })
    }

    @Test
    fun `topValued excludes unvalued items and returns the top N sorted descending`() {
        val items = listOf(
            item(nombre = "A", valoracionActual = 50.0),
            item(nombre = "B", valoracionActual = null),
            item(nombre = "C", valoracionActual = 200.0),
            item(nombre = "D", valoracionActual = 120.0),
        )
        val top = PortfolioAnalytics.topValued(items, limit = 2)
        assertEquals(listOf("C", "D"), top.map { it.nombre })
    }

    @Test
    fun `topValued defaults to a limit of 3`() {
        val items = (1..5).map { item(nombre = "Item$it", valoracionActual = it.toDouble()) }
        assertEquals(3, PortfolioAnalytics.topValued(items).size)
    }
}
