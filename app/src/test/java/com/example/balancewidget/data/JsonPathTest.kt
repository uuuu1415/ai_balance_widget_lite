package com.example.balancewidget.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonPathTest {
    private val response = JSONObject(
        """
        {
          "balance": 17.51,
          "usage": { "today": { "actual_cost": 0.42 } },
          "items": [{ "currency": "USD", "total": "17.51" }]
        }
        """.trimIndent()
    )

    @Test
    fun readsNestedValues() {
        assertEquals("17.51", JsonPath.text(response, "balance"))
        assertEquals("0.42", JsonPath.text(response, "usage.today.actual_cost"))
    }

    @Test
    fun readsArrayIndexes() {
        assertEquals("USD", JsonPath.text(response, "items[0].currency"))
        assertEquals("17.51", JsonPath.text(response, "items[0].total"))
    }

    @Test
    fun returnsNullForMissingOrDisabledPaths() {
        assertNull(JsonPath.text(response, "missing.value"))
        assertNull(JsonPath.text(response, "-"))
    }
}
