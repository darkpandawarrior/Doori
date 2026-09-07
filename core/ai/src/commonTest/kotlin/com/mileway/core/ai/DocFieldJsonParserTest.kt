package com.mileway.core.ai

import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocFieldJsonParserTest {
    @Test
    fun `known fields parse with their string value`() {
        val json = """{"merchant": "Cafe Coffee Day", "total": "245.00", "docType": "receipt"}"""

        val fields = DocFieldJsonParser.parseFields(json)

        assertEquals("Cafe Coffee Day", fields[DocField.MERCHANT]?.value)
        assertEquals("245.00", fields[DocField.TOTAL]?.value)
    }

    @Test
    fun `unrecognized key is skipped, not crashed on`() {
        val json = """{"merchant": "Acme", "notARealField": "whatever"}"""

        val fields = DocFieldJsonParser.parseFields(json)

        assertEquals(setOf(DocField.MERCHANT), fields.keys)
    }

    @Test
    fun `blank value is skipped`() {
        val json = """{"merchant": "", "total": "100"}"""

        val fields = DocFieldJsonParser.parseFields(json)

        assertEquals(setOf(DocField.TOTAL), fields.keys)
    }

    @Test
    fun `docType parses case-insensitively`() {
        assertEquals(DocType.INVOICE, DocFieldJsonParser.parseDocType("""{"docType": "INVOICE"}"""))
    }

    @Test
    fun `missing docType key returns null`() {
        assertNull(DocFieldJsonParser.parseDocType("""{"merchant": "Acme"}"""))
    }
}
