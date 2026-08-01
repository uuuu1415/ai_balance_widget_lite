package com.example.balancewidget.data

import android.content.Context
import org.json.JSONObject

/** Caches the latest result so widgets remain useful when the network is unavailable. */
class SnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(accountId: String): BalanceSnapshot? {
        val raw = preferences.getString(accountId, null) ?: return null
        return runCatching { JSONObject(raw).toSnapshot() }.getOrNull()
    }

    fun save(snapshot: BalanceSnapshot) {
        preferences.edit().putString(snapshot.accountId, snapshot.toJson().toString()).apply()
    }

    private fun JSONObject.toSnapshot() = BalanceSnapshot(
        accountId = getString("id"),
        accountName = getString("name"),
        balance = nullableString("balance"),
        currency = nullableString("currency"),
        available = if (isNull("available")) null else optBoolean("available"),
        todayCost = nullableString("today"),
        totalCost = nullableString("total"),
        fetchedAt = getLong("time"),
        error = nullableString("error")
    )

    private fun JSONObject.nullableString(key: String): String? = optString(key, "").takeIf { it.isNotBlank() }

    private fun BalanceSnapshot.toJson() = JSONObject().apply {
        put("id", accountId)
        put("name", accountName)
        put("balance", balance)
        put("currency", currency)
        put("available", available)
        put("today", todayCost)
        put("total", totalCost)
        put("time", fetchedAt)
        put("error", error)
    }

    private companion object {
        const val PREFERENCES_NAME = "snapshots"
    }
}
