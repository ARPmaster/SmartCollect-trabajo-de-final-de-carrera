/** Modelo de dominio de un ítem de la colección: sus datos, el histórico de precios y el
*resultado de la valoración de mercado que se le asocia al publicarlo.*/
package com.example.aicollect.application.items

data class PricePoint(val fecha: Long, val precio: Double)

data class ValuationSearch(val label: String, val url: String)

data class ValuationResult(
    val precio: Double?,
    val min: Double?,
    val max: Double?,
    val moneda: String,
    val fuentes: List<ValuationSearch>,
)

data class Item(
    val id: String = "",
    val nombre: String,
    val descripcion: String?,
    val marca: String,
    val modelo: String,
    val edicion: String?,
    val procedencia: String?,
    val deporte: String,
    val estado: String,
    val imageUrls: List<String> = emptyList(),
    val valoracionActual: Double?,
    val valoracionMin: Double?,
    val valoracionMax: Double?,
    val valoracionMoneda: String,
    val fuenteValoracion: String?,
    val valoracionBusquedas: List<ValuationSearch> = emptyList(),
    val historialPrecios: List<PricePoint> = emptyList(),
    val confianzaIdentificacion: Double?,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
