package com.example.balancewidget.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Small JSON-backed store for the user's local endpoint definitions. */
class AccountStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun accounts(): List<Account> {
        val raw = preferences.getString(KEY_ACCOUNTS, EMPTY_JSON_ARRAY) ?: EMPTY_JSON_ARRAY
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index -> array.getJSONObject(index).toAccount() }
        }.getOrElse { emptyList() }
    }

    fun save(account: Account) {
        val updated = accounts().filterNot { it.id == account.id } + account
        write(updated)
    }

    fun delete(id: String) {
        write(accounts().filterNot { it.id == id })
    }

    private fun write(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    }

    private fun JSONObject.toAccount() = Account(
        id = getString("id"),
        name = getString("name"),
        baseUrl = getString("baseUrl"),
        path = getString("path"),
        apiKey = getString("apiKey"),
        provider = optString("provider", ProviderType.CUSTOM.id),
        balancePath = optString("balancePath", "balance"),
        currencyPath = optString("currencyPath", "unit"),
        availablePath = optString("availablePath", "isValid"),
        todayCostPath = optString("todayCostPath", "usage.today.actual_cost"),
        totalCostPath = optString("totalCostPath", "usage.total.actual_cost")
    )

    private fun Account.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("baseUrl", baseUrl)
        put("path", path)
        put("apiKey", apiKey)
        put("provider", provider)
        put("balancePath", balancePath)
        put("currencyPath", currencyPath)
        put("availablePath", availablePath)
        put("todayCostPath", todayCostPath)
        put("totalCostPath", totalCostPath)
    }

    companion object {
        private const val PREFERENCES_NAME = "accounts"
        private const val KEY_ACCOUNTS = "items"
        private const val EMPTY_JSON_ARRAY = "[]"

        fun newId(): String = UUID.randomUUID().toString()
    }
}
