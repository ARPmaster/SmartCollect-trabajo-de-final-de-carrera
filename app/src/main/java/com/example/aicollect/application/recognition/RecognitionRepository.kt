// Modelo de un candidato devuelto por el reconocimiento automático de imágenes y el contrato del repositorio que lo invoca.
package com.example.aicollect.application.recognition

data class RankedCandidate(
    val nombre: String,
    val marca: String?,
    val modelo: String?,
    val edicion: String?,
    val procedencia: String?,
    val confianza: Double,
    val numeroFuentes: Int,
    val score: Double,
)

interface RecognitionRepository {

    suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>>
}
