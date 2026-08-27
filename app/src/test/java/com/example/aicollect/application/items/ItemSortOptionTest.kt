// Test unitario de ItemSortOption: comprueba cada criterio de ordenación de la colección y el mapeo desde/hacia su ordinal.
package com.example.aicollect.application.items

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemSortOptionTest {

    private fun item(nombre: String, valoracionActual: Double?, createdAt: Long) = Item(
        nombre = nombre,
        descripcion = null,
        marca = "",
        modelo = "",
        edicion = null,
        procedencia = null,
        deporte = "Running",
        estado = "Nuevo",
        valoracionActual = valoracionActual,
        valoracionMin = null,
        valoracionMax = null,
        valoracionMoneda = "EUR",
        fuenteValoracion = null,
        confianzaIdentificacion = null,
        createdAt = createdAt,
    )

    private val items = listOf(
        item("Nike", valoracionActual = 50.0, createdAt = 300L),
        item("adidas", valoracionActual = null, createdAt = 100L),
        item("Hoka", valoracionActual = 200.0, createdAt = 200L),
    )

    @Test
    fun `DEFAULT keeps the original order`() {
        assertEquals(items, items.sortedByOption(ItemSortOption.DEFAULT))
    }

    @Test
    fun `ALPHABETICAL sorts case-insensitively by nombre`() {
        val sorted = items.sortedByOption(ItemSortOption.ALPHABETICAL)
        assertEquals(listOf("adidas", "Hoka", "Nike"), sorted.map { it.nombre })
    }

    @Test
    fun `PRICE_DESC sorts descending, treating unvalued items as zero`() {
        val sorted = items.sortedByOption(ItemSortOption.PRICE_DESC)
        assertEquals(listOf("Hoka", "Nike", "adidas"), sorted.map { it.nombre })
    }

    @Test
    fun `PRICE_ASC sorts ascending, treating unvalued items as zero`() {
        val sorted = items.sortedByOption(ItemSortOption.PRICE_ASC)
        assertEquals(listOf("adidas", "Nike", "Hoka"), sorted.map { it.nombre })
    }

    @Test
    fun `OLDEST_FIRST sorts ascending by createdAt`() {
        val sorted = items.sortedByOption(ItemSortOption.OLDEST_FIRST)
        assertEquals(listOf("adidas", "Hoka", "Nike"), sorted.map { it.nombre })
    }

    @Test
    fun `fromOrdinal falls back to DEFAULT for an out-of-range ordinal`() {
        assertEquals(ItemSortOption.DEFAULT, ItemSortOption.fromOrdinal(-1))
        assertEquals(ItemSortOption.DEFAULT, ItemSortOption.fromOrdinal(999))
    }

    @Test
    fun `fromOrdinal maps a valid ordinal to the matching option`() {
        assertEquals(ItemSortOption.PRICE_DESC, ItemSortOption.fromOrdinal(ItemSortOption.PRICE_DESC.ordinal))
    }
}
