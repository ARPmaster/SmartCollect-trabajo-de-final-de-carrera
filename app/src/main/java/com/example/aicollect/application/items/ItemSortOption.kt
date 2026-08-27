// Opciones de orden aplicables al listado de ítems y la lógica para ordenar una lista según la opción elegida.
package com.example.aicollect.application.items

enum class ItemSortOption {
    DEFAULT,
    ALPHABETICAL,
    PRICE_DESC,
    PRICE_ASC,
    OLDEST_FIRST,
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): ItemSortOption = entries.getOrElse(ordinal) { DEFAULT }
    }
}

fun List<Item>.sortedByOption(option: ItemSortOption): List<Item> = when (option) {
    ItemSortOption.DEFAULT -> this
    ItemSortOption.ALPHABETICAL -> sortedBy { it.nombre.lowercase() }
    ItemSortOption.PRICE_DESC -> sortedByDescending { it.valoracionActual ?: 0.0 }
    ItemSortOption.PRICE_ASC -> sortedBy { it.valoracionActual ?: 0.0 }
    ItemSortOption.OLDEST_FIRST -> sortedBy { it.createdAt }
}
