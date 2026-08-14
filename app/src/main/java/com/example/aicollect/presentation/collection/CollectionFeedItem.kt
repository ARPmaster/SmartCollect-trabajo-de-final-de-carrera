package com.example.aicollect.presentation.collection

import androidx.annotation.DrawableRes
import com.example.aicollect.application.collection.CollectionPriceFilter

data class CollectionFeedItem(
    val category: String,
    @param:DrawableRes val image: Int,
    val price: String,
    val description: String,
    val date: String,
) {
    val priceValue: Int get() = CollectionPriceFilter.parsePrice(price)
}
