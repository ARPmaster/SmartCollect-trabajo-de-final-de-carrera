package com.example.aicollect.data.items.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.application.items.ValuationSearch

/**
 * Local mirror of `users/{uid}/items/{itemId}` (brief Sección 4). [ownerId] exists purely so the
 * same on-device table can hold a clean slate per account if the user logs out and into a
 * different one on the same device — every query filters by it, never a cross-account leak.
 */
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
    val imageUrls: List<String>,
    val valoracionActual: Double?,
    val valoracionMin: Double?,
    val valoracionMax: Double?,
    val valoracionMoneda: String,
    val fuenteValoracion: String?,
    val valoracionBusquedas: List<ValuationSearch>,
    val historialPrecios: List<PricePoint>,
    val confianzaIdentificacion: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ItemEntity.toDomain() = Item(
    id = itemId,
    nombre = nombre,
    descripcion = descripcion,
    marca = marca,
    modelo = modelo,
    edicion = edicion,
    procedencia = procedencia,
    deporte = deporte,
    estado = estado,
    imageUrls = imageUrls,
    valoracionActual = valoracionActual,
    valoracionMin = valoracionMin,
    valoracionMax = valoracionMax,
    valoracionMoneda = valoracionMoneda,
    fuenteValoracion = fuenteValoracion,
    valoracionBusquedas = valoracionBusquedas,
    historialPrecios = historialPrecios,
    confianzaIdentificacion = confianzaIdentificacion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Item.toEntity(ownerId: String) = ItemEntity(
    itemId = id,
    ownerId = ownerId,
    nombre = nombre,
    descripcion = descripcion,
    marca = marca,
    modelo = modelo,
    edicion = edicion,
    procedencia = procedencia,
    deporte = deporte,
    estado = estado,
    imageUrls = imageUrls,
    valoracionActual = valoracionActual,
    valoracionMin = valoracionMin,
    valoracionMax = valoracionMax,
    valoracionMoneda = valoracionMoneda,
    fuenteValoracion = fuenteValoracion,
    valoracionBusquedas = valoracionBusquedas,
    historialPrecios = historialPrecios,
    confianzaIdentificacion = confianzaIdentificacion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
