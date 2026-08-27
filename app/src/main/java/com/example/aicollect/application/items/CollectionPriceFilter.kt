// Comprueba si el precio de un ítem cae dentro del rango de precios elegido en el filtro de Home.
package com.example.aicollect.application.items

object CollectionPriceFilter {

    fun isWithinRange(price: Double, minPrice: Int, maxPrice: Int): Boolean =
        price in minPrice.toDouble()..maxPrice.toDouble()
}
