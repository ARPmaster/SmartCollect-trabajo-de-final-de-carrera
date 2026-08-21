package com.example.aicollect.presentation.collection

import com.example.aicollect.application.items.Item

/** UI-shaped view of a real [Item] for the Home feed — not a domain model, just what
 * [CollectionFeedAdapter] needs to render one card. */
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
