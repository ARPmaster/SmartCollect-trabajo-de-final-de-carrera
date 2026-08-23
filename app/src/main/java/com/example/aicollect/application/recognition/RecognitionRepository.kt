package com.example.aicollect.application.recognition

/**
 * One candidate returned by the `recognizeItem` Cloud Function (europe-west1), already ranked.
 * Mirrors `RankedCandidate` in the deployed `recognizeItem.ts` exactly — [score] is computed
 * server-side (0.4·visión + 0.35·consenso de fuentes + 0.25·confianza de Gemini) and must never
 * be recalculated on the client.
 */
data class RankedCandidate(
    val nombre: String,
    val marca: String?,
    val modelo: String?,
    val edicion: String?,
    val procedencia: String?,
    /** Gemini's own reported confidence, 0.0–1.0 — distinct from [score]. */
    val confianza: Double,
    val numeroFuentes: Int,
    val score: Double,
)

interface RecognitionRepository {

    /** Calls `recognizeItem` with a base64-encoded JPEG. An empty list is a valid result (no
     * reliable candidates found), not an error — the caller should offer manual entry either way. */
    suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>>
}
