// Doble de prueba de RecognitionRepository para las pruebas de instrumentación: devuelve lo que el test configure, sin llamar a Firebase Functions.
package com.example.aicollect.testutil

import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.application.recognition.RecognitionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRecognitionRepository @Inject constructor() : RecognitionRepository {

    var nextResult: Result<List<RankedCandidate>> = Result.success(emptyList())
    var lastImageBase64: String? = null

    override suspend fun recognizeItem(imageBase64: String): Result<List<RankedCandidate>> {
        lastImageBase64 = imageBase64
        return nextResult
    }
}
