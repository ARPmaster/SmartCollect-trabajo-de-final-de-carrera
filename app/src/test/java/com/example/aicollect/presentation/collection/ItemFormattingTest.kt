// Test unitario de ItemFormatting: formateo de valores monetarios y fechas para mostrar en pantalla.
package com.example.aicollect.presentation.collection

import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemFormattingTest {

    @Test
    fun `formatValue returns the not-valued resource for a null value`() {
        assertEquals(UiText.StringResource(R.string.label_not_valued), ItemFormatting.formatValue(null, "EUR"))
    }

    @Test
    fun `formatValue formats EUR with the euro symbol after the amount`() {
        assertEquals(UiText.DynamicString("1.234€"), ItemFormatting.formatValue(1234.0, "EUR"))
    }

    @Test
    fun `formatValue formats USD with the dollar symbol after the amount`() {
        assertEquals(UiText.DynamicString("100$"), ItemFormatting.formatValue(100.0, "USD"))
    }

    @Test
    fun `formatValue falls back to the raw currency code for an unknown currency`() {
        assertEquals(UiText.DynamicString("50GBP"), ItemFormatting.formatValue(50.0, "GBP"))
    }

    @Test
    fun `formatKnownValue formats EUR with the euro symbol after the amount`() {
        assertEquals("1.234€", ItemFormatting.formatKnownValue(1234.0, "EUR"))
    }

    @Test
    fun `formatDate formats an epoch millis as dd MM yyyy`() {
        val millis = Instant.parse("2026-01-15T12:00:00Z").toEpochMilli()
        assertEquals("15/01/2026", ItemFormatting.formatDate(millis))
    }
}
