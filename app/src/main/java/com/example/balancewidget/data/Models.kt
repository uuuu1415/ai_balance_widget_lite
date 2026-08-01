package com.example.balancewidget.data

/** A locally configured balance endpoint. */
data class Account(
    val id: String,
    val name: String,
    val baseUrl: String,
    val path: String,
    val apiKey: String,
    val provider: String = ProviderType.CUSTOM.id,
    val balancePath: String = "balance",
    val currencyPath: String = "unit",
    val availablePath: String = "isValid",
    val todayCostPath: String = "usage.today.actual_cost",
    val totalCostPath: String = "usage.total.actual_cost"
)

enum class ProviderType(val id: String) {
    CUSTOM("custom"),
    DEEPSEEK("deepseek"),
    RELAY("relay");

    companion object {
        fun fromId(id: String): ProviderType = when (id.lowercase()) {
            // Preserve existing local configurations from the initial SevnX-oriented release.
            "sevnx" -> RELAY
            else -> entries.firstOrNull { it.id == id.lowercase() } ?: CUSTOM
        }
    }
}

/** The last successful or failed response shown by the app and widgets. */
data class BalanceSnapshot(
    val accountId: String,
    val accountName: String,
    val balance: String?,
    val currency: String?,
    val available: Boolean?,
    val todayCost: String?,
    val totalCost: String?,
    val fetchedAt: Long,
    val error: String? = null
)

fun Account.effectiveRequest(): Account = when (ProviderType.fromId(provider)) {
    ProviderType.DEEPSEEK -> copy(
        path = "user/balance",
        balancePath = "balance_infos[0].total_balance",
        currencyPath = "balance_infos[0].currency",
        availablePath = "is_available"
    )
    else -> this
}
