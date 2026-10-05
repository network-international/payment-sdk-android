package payment.sdk.android.demo.model

import android.content.Context
import payment.sdk.android.demo.getPreferences
import com.google.gson.Gson
import java.util.*

enum class EnvironmentType(val value: String) {
    DEV("DEV"),
    UAT("UAT"),
    PROD("PROD")
}

data class Environment(
    val type: EnvironmentType,
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nickname: String? = null,
    val apiKey: String,
    val outletReference: String,
    val realm: String,
    /**
     * Gateway region this outlet lives on. UAE and KSA are physically separate deployments with
     * their own identity endpoints, so an outlet's credentials only work against one of them.
     */
    val region: Region,
    /**
     * Currency this outlet transacts in. Alternative payment methods follow the acquirer, so the
     * currency and the available methods travel together: a BHD outlet is the Benefit one, a QAR
     * outlet is the QPay one. Pairing them here makes a mismatched combination unrepresentable.
     *
     * Null on environments saved before the currency moved onto the outlet; [currencyCode]
     * resolves those against the old app-wide setting.
     */
    val currency: String? = null,
    /**
     * Order action (SALE / PURCHASE / AUTH) this outlet is exercised with. Which actions an
     * outlet accepts is outlet configuration — Slice, for one, only supports PURCHASE — so
     * pairing it with the outlet stops one global picker from being wrong for every other.
     *
     * Null on environments saved before it moved onto the outlet; [orderActionCode] resolves
     * those against the old app-wide setting.
     */
    val orderAction: String? = null,
    /**
     * Order type (SINGLE / RECURRING / UNSCHEDULED / INSTALLMENT), enabled per outlet like
     * the action. Null on environments saved before the move; see [orderTypeCode].
     */
    val orderType: String? = null,
    /**
     * Payment methods the gateway offered for this outlet, captured on import.
     * Informational: shown on the tile so an outlet's capabilities are visible without
     * opening it or starting a payment. Null for an outlet added by hand.
     */
    val paymentMethods: List<String>? = null,
    /**
     * Merchant identifier used by the Click to Pay config endpoint
     * (`/config/merchants/{merchantId}/configs/vctp`). Distinct from [outletReference].
     */
    val clickToPayMerchantId: String? = null
) {
    companion object {
        private const val KEY_SAVED_ENVIRONMENT_ID = "saved_env_id"
        private const val KEY_SAVED_ENVIRONMENTS = "saved_environments"

        fun saveEnvironments(context: Context, environments: List<Environment>) {
            val json = Gson().toJson(environments)
            context.getPreferences().edit().putString(KEY_SAVED_ENVIRONMENTS, json).apply()
        }

        fun getEnvironments(context: Context): List<Environment> {
            val json = context.getPreferences().getString(KEY_SAVED_ENVIRONMENTS, null)
            return if (json != null) {
                Gson().fromJson(json, Array<Environment>::class.java).toList()
            } else {
                emptyList()
            }
        }

        fun getSelectedEnvironment(context: Context): Environment? {
            val envId = context.getPreferences().getString(KEY_SAVED_ENVIRONMENT_ID, null)
            return getEnvironments(context).firstOrNull { it.id == envId }
        }

        fun setSelectedEnvironment(context: Context, environmentId: String) {
            context.getPreferences().edit().putString(KEY_SAVED_ENVIRONMENT_ID, environmentId)
                .apply()
        }

        /**
         * Currency of the currently selected outlet — the single source of truth for an order's
         * currency. Falls back to [legacyCurrency] for outlets saved before the migration, and to
         * AED when nothing is selected yet.
         */
        fun selectedCurrency(context: Context): String =
            getSelectedEnvironment(context)?.currencyCode(context) ?: legacyCurrency(context)

        fun selectedOrderAction(context: Context): String =
            getSelectedEnvironment(context)?.orderActionCode(context) ?: legacyOrderAction(context)

        fun selectedOrderType(context: Context): String =
            getSelectedEnvironment(context)?.orderTypeCode(context) ?: legacyOrderType(context)

        /**
         * The app-wide currency setting that used to be independent of the selected outlet — which
         * is what forced a tester to set BHD by hand before Benefit would appear. Read only so
         * environments saved by an older build can inherit it.
         */
        fun legacyCurrency(context: Context): String =
            context.getPreferences().getString(KEY_CURRENCY, null) ?: "AED"

        fun legacyOrderAction(context: Context): String =
            context.getPreferences().getString(KEY_ORDER_ACTION, null) ?: "SALE"

        fun legacyOrderType(context: Context): String =
            context.getPreferences().getString(KEY_ORDER_TYPE, null) ?: "SINGLE"

        private const val KEY_CURRENCY = "currency"
        private const val KEY_ORDER_ACTION = "order_action"
        private const val KEY_ORDER_TYPE = "order_type"
    }

    /** This outlet's currency, resolving a pre-migration null against the old app-wide setting. */
    fun currencyCode(context: Context): String = currency ?: legacyCurrency(context)

    fun orderActionCode(context: Context): String = orderAction ?: legacyOrderAction(context)

    fun orderTypeCode(context: Context): String = orderType ?: legacyOrderType(context)

    fun getGatewayUrl(): String {
        if (region == Region.UAE) {
            return when (type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/transactions/outlets/$outletReference/orders"
                EnvironmentType.UAT -> "https://api-gateway.sandbox.ngenius-payments.com/transactions/outlets/$outletReference/orders"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/transactions/outlets/$outletReference/orders"
            }
        } else {
            return when (type) {
                EnvironmentType.DEV -> "https://api-gateway.dev.ksa.ngenius-payments.com/transactions/outlets/$outletReference/orders"
                EnvironmentType.UAT -> "https://api-gateway.uat.ksa.ngenius-payments.com/transactions/outlets/$outletReference/orders"
                EnvironmentType.PROD -> "https://api-gateway.ksa.ngenius-payments.com/transactions/outlets/$outletReference/orders"
            }
        }
    }

    /**
     * Bare API gateway host (no trailing path), used by the Click to Pay merchant config
     * endpoint among other host-level calls. Derived from the same region/env logic as
     * [getGatewayUrl].
     */
    fun getApiGatewayBaseUrl(): String {
        if (region == Region.UAE) {
            return when (type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com"
                EnvironmentType.UAT -> "https://api-gateway.sandbox.ngenius-payments.com"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com"
            }
        }
        return when (type) {
            EnvironmentType.DEV -> "https://api-gateway.dev.ksa.ngenius-payments.com"
            EnvironmentType.UAT -> "https://api-gateway.uat.ksa.ngenius-payments.com"
            EnvironmentType.PROD -> "https://api-gateway.ksa.ngenius-payments.com"
        }
    }

    fun getIdentityUrl(): String {
        if (region == Region.UAE) {
            return when (type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.UAT -> "https://api-gateway.sandbox.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/identity/auth/access-token"
            }
        } else {
            return when (type) {
                EnvironmentType.DEV -> "https://api-gateway.dev.ksa.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.UAT -> "https://api-gateway.uat.ksa.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.PROD -> "https://api-gateway.ksa.ngenius-payments.com/identity/auth/access-token"
            }
        }
    }
}