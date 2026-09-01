// Implementación de RecognitionRepository que llama a la Cloud Function de reconocimiento de imágenes y traduce su respuesta a los candidatos del modelo de dominio.
package com.example.aicollect.data.recognition

import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.application.recognition.RecognitionRepository
import com.example.aicollect.data.callFunctionWithRetry
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirebaseRecognitionRepository @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
) : RecognitionRepository {

    override suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>> = runCatching {
        val response = callFunctionWithRetry {
            firebaseFunctions
                .getHttpsCallable(RECOGNIZE_ITEM_FUNCTION)
                .call(mapOf("imageBase64" to imageBase64))
                .await()
        }

        @Suppress("UNCHECKED_CAST")
        val body = response.data as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val rawCandidates = body["candidates"] as? List<Map<String, Any?>> ?: emptyList()
        rawCandidates.map { it.toRankedCandidate() }
    }

    private fun Map<String, Any?>.toRankedCandidate() = RankedCandidate(
        nombre = this["nombre"] as? String ?: "",
        marca = this["marca"] as? String,
        modelo = this["modelo"] as? String,
        edicion = this["edicion"] as? String,
        procedencia = this["procedencia"] as? String,
        confianza = (this["confianza"] as? Number)?.toDouble() ?: 0.0,
        numeroFuentes = (this["numeroFuentes"] as? Number)?.toInt() ?: 0,
        score = (this["score"] as? Number)?.toDouble() ?: 0.0,
    )

    private companion object {
        const val RECOGNIZE_ITEM_FUNCTION = "recognizeItem"
    }
}
