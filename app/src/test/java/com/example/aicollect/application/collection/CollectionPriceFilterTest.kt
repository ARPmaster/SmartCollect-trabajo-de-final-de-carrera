package com.example.aicollect.application.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionPriceFilterTest {

    @Test
    fun `parses a normal price with the euro symbol`() {
        assertEquals(250, CollectionPriceFilter.parsePrice("250€"))
    }

    @Test
    fun `parses a price without any currency symbol`() {
        assertEquals(300, CollectionPriceFilter.parsePrice("300"))
    }

    @Test
    fun `price below the minimum is out of range`() {
        assertFalse(CollectionPriceFilter.isWithinRange("100€", minPrice = 200, maxPrice = 500))
    }

    @Test
    fun `price above the maximum is out of range`() {
        assertFalse(CollectionPriceFilter.isWithinRange("600€", minPrice = 200, maxPrice = 500))
    }

    @Test
    fun `price exactly at the minimum or maximum boundary is within range`() {
        assertTrue(CollectionPriceFilter.isWithinRange("200€", minPrice = 200, maxPrice = 500))
        assertTrue(CollectionPriceFilter.isWithinRange("500€", minPrice = 200, maxPrice = 500))
    }
}
