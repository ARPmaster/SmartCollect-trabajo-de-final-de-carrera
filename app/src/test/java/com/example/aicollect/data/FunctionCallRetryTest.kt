// Test unitario de callFunctionWithRetry: reintenta una única vez ante un fallo transitorio y
// nunca traga la cancelación. La clasificación de FirebaseFunctionsException por código
// (UNAVAILABLE, etc.) no se cubre aquí: instanciar esa clase requiere runtime de Android
// (Robolectric o un dispositivo/emulador), que este módulo de tests JVM no usa — igual que el
// resto de tests de este proyecto, que mockean AuthRepository/ItemRepository en vez de tipos del
// SDK de Firebase directamente.
package com.example.aicollect.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionCallRetryTest {

    private suspend fun <T> runAndCatch(block: suspend () -> T): Throwable? = try {
        block()
        null
    } catch (e: Throwable) {
        e
    }

    @Test
    fun `returns the result on first success without retrying`() = runTest {
        var callCount = 0
        val result = callFunctionWithRetry {
            callCount++
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `retries once on a transient IOException and then succeeds`() = runTest {
        var callCount = 0
        val result = callFunctionWithRetry {
            callCount++
            if (callCount == 1) throw IOException("network down")
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `propagates the second IOException without a third attempt`() = runTest {
        var callCount = 0
        val error = runAndCatch {
            callFunctionWithRetry {
                callCount++
                throw IOException("still down")
            }
        }

        assertTrue(error is IOException)
        assertEquals(2, callCount)
    }

    @Test
    fun `does not retry on a plain unrelated exception`() = runTest {
        var callCount = 0
        val error = runAndCatch {
            callFunctionWithRetry {
                callCount++
                throw IllegalStateException("unexpected")
            }
        }

        assertTrue(error is IllegalStateException)
        assertEquals(1, callCount)
    }

    @Test
    fun `never swallows CancellationException`() = runTest {
        var callCount = 0
        val error = runAndCatch {
            callFunctionWithRetry {
                callCount++
                throw CancellationException("cancelled")
            }
        }

        assertTrue(error is CancellationException)
        assertEquals(1, callCount)
    }
}
