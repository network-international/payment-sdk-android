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
    val apiKey: String,
    val outletReference: String,
    val realm: String
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
    }

    fun getGatewayUrl(): String {
        return when (type) {
            EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/transactions/outlets/$outletReference/orders"
            EnvironmentType.UAT -> "https://api-gateway-uat.ngenius-payments.com/transactions/outlets/$outletReference/orders"
            EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/transactions/outlets/$outletReference/orders"
        }
    }

    fun getIdentityUrl(): String {
        return when (type) {
            EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/identity/auth/access-token"
            EnvironmentType.UAT -> "https://api-gateway-uat.ngenius-payments.com/identity/auth/access-token"
            EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/identity/auth/access-token"
        }
    }
}