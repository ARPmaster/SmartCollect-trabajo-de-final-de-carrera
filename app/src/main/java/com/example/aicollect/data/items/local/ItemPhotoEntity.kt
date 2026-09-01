// Tabla propia para las fotografías de un ítem (antes una lista serializada en ItemEntity).
// Relacionada con ItemEntity por clave foránea con borrado en cascada; "position" conserva el
// orden original de las fotos.
package com.example.aicollect.data.items.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_photos",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class ItemPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val position: Int,
    val url: String,
)
