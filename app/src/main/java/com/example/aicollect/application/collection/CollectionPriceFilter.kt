package com.example.aicollect.application.collection

/** Whether a real item's `valoracionActual` (0.0 for unvalued items, never fabricated) falls
 * inside the range picked in the Home filter sheet. */
object CollectionPriceFilter {

    fun isWithinRange(price: Double, minPrice: Int, maxPrice: Int): Boolean =
        price in minPrice.toDouble()..maxPrice.toDouble()
}
