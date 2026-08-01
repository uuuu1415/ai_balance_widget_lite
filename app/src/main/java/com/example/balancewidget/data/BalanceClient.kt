package com.example.balancewidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches and normalizes responses from built-in and user-defined providers. */
class BalanceClient {
    suspend fun fetch(account: Account): BalanceSnapshot = withContext(Dispatchers.IO) {
        val requested = account.effectiveRequest()
        val fetchedAt = System.currentTimeMillis()

        runCatching {
            val connection = openConnection(requested)
            try {
                val responseCode = connection.responseCode
                val body = readBody(connection, responseCode)
                if (responseCode !in HTTP_SUCCESS_RANGE) {
                    error("HTTP $responseCode${body.takeIf { it.isNotBlank() }?.let { ": ${it.take(MAX_ERROR_BODY_LENGTH)}"}.orEmpty()}")
                }
                parseSnapshot(account, requested, JSONObject(body), fetchedAt)
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            BalanceSnapshot(
                accountId = account.id,
                accountName = account.name,
                balance = null,
                currency = null,
                available = null,
                todayCost = null,
                totalCost = null,
                fetchedAt = fetchedAt,
                error = error.message ?: "请求失败"
            )
        }
    }

    private fun openConnection(account: Account): HttpURLConnection {
        require(account.baseUrl.startsWith("https://", ignoreCase = true)) { "Base URL 必须使用 HTTPS" }
        val endpoint = account.baseUrl.trimEnd('/') + "/" + account.path.trimStart('/')
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer ${account.apiKey}")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun readBody(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in HTTP_SUCCESS_RANGE) connection.inputStream else connection.errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun parseSnapshot(original: Account, requested: Account, json: JSONObject, fetchedAt: Long) = BalanceSnapshot(
        accountId = original.id,
        accountName = original.name,
        balance = JsonPath.text(json, requested.balancePath),
        currency = JsonPath.text(json, requested.currencyPath),
        available = JsonPath.text(json, requested.availablePath)?.toBooleanStrictOrNull(),
        todayCost = JsonPath.text(json, requested.todayCostPath),
        totalCost = JsonPath.text(json, requested.totalCostPath),
        fetchedAt = fetchedAt
    )

    private companion object {
        val HTTP_SUCCESS_RANGE = 200..299
        const val NETWORK_TIMEOUT_MS = 12_000
        const val MAX_ERROR_BODY_LENGTH = 160
    }
}
