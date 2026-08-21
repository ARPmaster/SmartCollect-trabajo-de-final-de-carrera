package com.example.aicollect.application.collection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionPriceFilterTest {

    @Test
    fun `price below the minimum is out of range`() {
        assertFalse(CollectionPriceFilter.isWithinRange(100.0, minPrice = 200, maxPrice = 500))
    }

    @Test
    fun `price above the maximum is out of range`() {
        assertFalse(CollectionPriceFilter.isWithinRange(600.0, minPrice = 200, maxPrice = 500))
    }

    @Test
    fun `price exactly at the minimum or maximum boundary is within range`() {
        assertTrue(CollectionPriceFilter.isWithinRange(200.0, minPrice = 200, maxPrice = 500))
        assertTrue(CollectionPriceFilter.isWithinRange(500.0, minPrice = 200, maxPrice = 500))
    }
}
