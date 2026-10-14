package payment.sdk.android.demo.model

import android.content.Context
import androidx.core.content.edit
import payment.sdk.android.demo.getPreferences

/**
 * QA override: one switch that replaces the selected environment's region, currency, order
 * action and order type for every order, so a combination can be tried without editing (or
 * duplicating) an environment. Off by default; saved environments are never changed.
 *
 * Region here only feeds what reads [payment.sdk.android.demo.data.DataStore.getRegion]. The
 * gateway host and the outlet still come from the environment record — an outlet does not move
 * between the UAE and KSA deployments.
 *
 * Stored under its own keys, apart from the legacy app-wide ones, and the switch is a string so
 * the preference reads stay `getString` like the rest of [Environment].
 */
object EnvironmentOverride {
    private const val KEY_ENABLED = "override_enabled"
    private const val KEY_REGION = "override_region"
    private const val KEY_CURRENCY = "override_currency"
    private const val KEY_ORDER_ACTION = "override_order_action"
    private const val KEY_ORDER_TYPE = "override_order_type"

    fun isEnabled(context: Context): Boolean =
        context.getPreferences().getString(KEY_ENABLED, null) == "true"

    /**
     * Turning it on for the first time starts from the selected environment's values, so ticking
     * the box changes nothing until a value is picked.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getPreferences()
        if (enabled && prefs.getString(KEY_REGION, null) == null) {
            val env = Environment.getSelectedEnvironment(context)
            prefs.edit {
                putString(KEY_REGION, env?.region?.code ?: Region.UAE.code)
                putString(KEY_CURRENCY, env?.currencyCode(context) ?: Environment.legacyCurrency(context))
                putString(KEY_ORDER_ACTION, env?.orderActionCode(context) ?: Environment.legacyOrderAction(context))
                putString(KEY_ORDER_TYPE, env?.orderTypeCode(context) ?: Environment.legacyOrderType(context))
            }
        }
        prefs.edit { putString(KEY_ENABLED, enabled.toString()) }
    }

    fun region(context: Context): String =
        context.getPreferences().getString(KEY_REGION, null) ?: Region.UAE.code

    fun currency(context: Context): String =
        context.getPreferences().getString(KEY_CURRENCY, null) ?: Environment.legacyCurrency(context)

    fun orderAction(context: Context): String =
        context.getPreferences().getString(KEY_ORDER_ACTION, null) ?: Environment.legacyOrderAction(context)

    fun orderType(context: Context): String =
        context.getPreferences().getString(KEY_ORDER_TYPE, null) ?: Environment.legacyOrderType(context)

    fun setRegion(context: Context, code: String) = put(context, KEY_REGION, code)
    fun setCurrency(context: Context, code: String) = put(context, KEY_CURRENCY, code)
    fun setOrderAction(context: Context, code: String) = put(context, KEY_ORDER_ACTION, code)
    fun setOrderType(context: Context, code: String) = put(context, KEY_ORDER_TYPE, code)

    private fun put(context: Context, key: String, value: String) =
        context.getPreferences().edit { putString(key, value) }
}
