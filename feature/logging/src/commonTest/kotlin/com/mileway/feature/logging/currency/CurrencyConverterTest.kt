package com.mileway.feature.logging.currency

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyConverterTest {
    @Test
    fun `identity conversion returns the same amount`() {
        assertEquals(100.0, CurrencyConverter.convert(100.0, "USD", "USD"))
        assertEquals(250.0, CurrencyConverter.convert(250.0, "INR", "INR"))
    }

    @Test
    fun `converts USD to rupees using the static rate`() {
        assertEquals(8300.0, CurrencyConverter.toRupees(100.0, "USD"), absoluteTolerance = 0.001)
    }

    @Test
    fun `converts between two non-INR currencies via the shared table`() {
        // 1 GBP = 105 INR, 1 EUR = 90 INR -> 10 GBP = 1050 INR = 1050/90 EUR
        val expected = 1050.0 / 90.0
        assertEquals(expected, CurrencyConverter.convert(10.0, "GBP", "EUR"), absoluteTolerance = 0.001)
    }

    @Test
    fun `unknown currency code is treated as INR rather than throwing`() {
        assertEquals(50.0, CurrencyConverter.convert(50.0, "XYZ", "INR"))
        assertEquals(50.0, CurrencyConverter.convert(50.0, "INR", "XYZ"))
    }

    @Test
    fun `supported currencies includes the required core set`() {
        val supported = CurrencyConverter.supportedCurrencies
        listOf("INR", "USD", "EUR", "GBP").forEach { code ->
            assert(code in supported) { "$code missing from supportedCurrencies" }
        }
    }
}
