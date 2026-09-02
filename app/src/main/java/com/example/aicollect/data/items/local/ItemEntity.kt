// Entidad Room que refleja localmente los datos escalares de un ítem de Firestore. Las
// colecciones (fotos, búsquedas de valoración, histórico de precios) viven en tablas propias
// (ItemPhotoEntity, ValuationSearchEntity, PricePointEntity) relacionadas por clave foránea.
package com.example.aicollect.data.items.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val itemId: String,
    val ownerId: String,
    val nombre: String,
    val descripcion: String?,
    val marca: String,
    val modelo: String,
    val edicion: String?,
    val procedencia: String?,
    val deporte: String,
    val estado: String,
    val valoracionActual: Double?,
    val valoracionMin: Double?,
    val valoracionMax: Double?,
    val valoracionMoneda: String,
    val fuenteValoracion: String?,
    val confianzaIdentificacion: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)
