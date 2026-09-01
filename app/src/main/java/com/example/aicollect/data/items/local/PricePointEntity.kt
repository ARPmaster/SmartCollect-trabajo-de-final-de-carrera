// Tabla propia para el histórico de precios de un ítem (antes una lista serializada en
// ItemEntity, que se acumulaba sin límite en una sola celda). Relacionada con ItemEntity por
// clave foránea con borrado en cascada; indexada por fecha para soportar consultas ordenadas.
package com.example.aicollect.data.items.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_points",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId"), Index("fecha")],
)
data class PricePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val fecha: Long,
    val precio: Double,
)
