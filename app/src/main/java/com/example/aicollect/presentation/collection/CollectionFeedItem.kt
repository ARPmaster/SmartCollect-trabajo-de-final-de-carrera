package com.example.aicollect.presentation.collection

import androidx.annotation.DrawableRes

data class CollectionFeedItem(
    val category: String,
    @param:DrawableRes val image: Int,
    val price: String,
    val description: String,
    val date: String,
)
