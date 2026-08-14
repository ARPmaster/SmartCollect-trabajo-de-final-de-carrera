package com.example.aicollect.application.collection

/** Parses price labels shown in the collection feed (e.g. "250€") into their numeric value. */
object CollectionPriceFilter {

    fun parsePrice(priceLabel: String): Int = priceLabel.filter { it.isDigit() }.toIntOrNull() ?: 0

    fun isWithinRange(priceLabel: String, minPrice: Int, maxPrice: Int): Boolean =
        parsePrice(priceLabel) in minPrice..maxPrice
}
