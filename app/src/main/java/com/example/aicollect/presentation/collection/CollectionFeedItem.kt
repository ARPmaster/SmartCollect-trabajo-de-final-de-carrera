// Modelo de presentación de un ítem para una tarjeta del feed de Home, y su conversión desde el modelo de dominio Item.
package com.example.aicollect.presentation.collection

import com.example.aicollect.application.items.Item

data class CollectionFeedItem(
    val id: String,
    val category: String,
    val imageUrl: String?,
    val priceLabel: String,
    val priceValue: Double,
    val description: String,
    val dateLabel: String,
)

fun Item.toFeedItem(): CollectionFeedItem = CollectionFeedItem(
    id = id,
    category = deporte.uppercase(),
    imageUrl = imageUrls.firstOrNull(),
    priceLabel = ItemFormatting.formatValue(valoracionActual, valoracionMoneda),
    priceValue = valoracionActual ?: 0.0,
    description = nombre,
    dateLabel = ItemFormatting.formatDate(createdAt),
)
