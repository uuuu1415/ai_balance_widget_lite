package com.example.balancewidget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {
    @Test
    fun deepSeekUsesItsCanonicalEndpointAndFields() {
        val account = Account(
            id = "id",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            path = "ignored",
            apiKey = "key",
            provider = ProviderType.DEEPSEEK.id
        )

        val request = account.effectiveRequest()

        assertEquals("user/balance", request.path)
        assertEquals("balance_infos[0].total_balance", request.balancePath)
        assertEquals("balance_infos[0].currency", request.currencyPath)
        assertEquals("is_available", request.availablePath)
    }
}
