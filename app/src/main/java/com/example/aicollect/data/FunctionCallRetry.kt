// Reintento único con backoff corto para llamadas a Cloud Functions, usado por
// FirebaseRecognitionRepository y FirestoreItemRepository. Solo reintenta fallos transitorios
// (red caída, function sobrecargada/caída un instante, timeout) — nunca errores de
// autenticación o de validación de argumentos, que fallarán exactamente igual en el reintento.
package com.example.aicollect.data

import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val RETRY_BACKOFF_MS = 1_500L

// El set de códigos vive dentro de la función (no a nivel de fichero) para no forzar la carga de
// FirebaseFunctionsException.Code al cargar esta clase: eso rompe los tests unitarios JVM (sin
// Android runtime) incluso para las llamadas que nunca tocan un error de Firebase.
private fun Throwable.isTransientFunctionFailure(): Boolean = when (this) {
    is FirebaseFunctionsException -> code in setOf(
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
        FirebaseFunctionsException.Code.INTERNAL,
        FirebaseFunctionsException.Code.UNKNOWN,
        FirebaseFunctionsException.Code.ABORTED,
    )
    is IOException -> true
    else -> false
}

/** Ejecuta [call]; si falla con un error transitorio, espera [RETRY_BACKOFF_MS] y lo intenta una
 * única vez más. Cualquier otro fallo (auth, validación, o el segundo intento) se propaga tal
 * cual. */
suspend fun <T> callFunctionWithRetry(call: suspend () -> T): T = try {
    call()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    if (e.isTransientFunctionFailure()) {
        delay(RETRY_BACKOFF_MS)
        call()
    } else {
        throw e
    }
}
