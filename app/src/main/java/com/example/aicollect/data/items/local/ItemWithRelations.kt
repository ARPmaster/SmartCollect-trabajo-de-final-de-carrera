// Composición de ItemEntity con sus tablas hijas (fotos, búsquedas de valoración, histórico de
// precios), y las conversiones entre esta composición y el modelo de dominio Item.
package com.example.aicollect.data.items.local

import androidx.room.Embedded
import androidx.room.Relation
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.application.items.ValuationSearch

data class ItemWithRelations(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "itemId", entityColumn = "itemId")
    val photos: List<ItemPhotoEntity>,
    @Relation(parentColumn = "itemId", entityColumn = "itemId")
    val valuationSearches: List<ValuationSearchEntity>,
    @Relation(parentColumn = "itemId", entityColumn = "itemId")
    val pricePoints: List<PricePointEntity>,
)

fun ItemWithRelations.toDomain() = Item(
    id = item.itemId,
    nombre = item.nombre,
    descripcion = item.descripcion,
    marca = item.marca,
    modelo = item.modelo,
    edicion = item.edicion,
    procedencia = item.procedencia,
    deporte = item.deporte,
    estado = item.estado,
    imageUrls = photos.sortedBy { it.position }.map { it.url },
    valoracionActual = item.valoracionActual,
    valoracionMin = item.valoracionMin,
    valoracionMax = item.valoracionMax,
    valoracionMoneda = item.valoracionMoneda,
    fuenteValoracion = item.fuenteValoracion,
    valoracionBusquedas = valuationSearches.sortedBy { it.position }.map { ValuationSearch(it.label, it.url) },
    historialPrecios = pricePoints.sortedBy { it.fecha }.map { PricePoint(it.fecha, it.precio) },
    confianzaIdentificacion = item.confianzaIdentificacion,
    createdAt = item.createdAt,
    updatedAt = item.updatedAt,
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
    valoracionActual = valoracionActual,
    valoracionMin = valoracionMin,
    valoracionMax = valoracionMax,
    valoracionMoneda = valoracionMoneda,
    fuenteValoracion = fuenteValoracion,
    confianzaIdentificacion = confianzaIdentificacion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Item.toPhotoEntities(): List<ItemPhotoEntity> =
    imageUrls.mapIndexed { index, url -> ItemPhotoEntity(itemId = id, position = index, url = url) }

fun Item.toValuationSearchEntities(): List<ValuationSearchEntity> =
    valoracionBusquedas.mapIndexed { index, search ->
        ValuationSearchEntity(itemId = id, position = index, label = search.label, url = search.url)
    }

fun Item.toPricePointEntities(): List<PricePointEntity> =
    historialPrecios.map { PricePointEntity(itemId = id, fecha = it.fecha, precio = it.precio) }
