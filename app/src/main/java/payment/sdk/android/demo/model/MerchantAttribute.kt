package payment.sdk.android.demo.model

import android.content.Context
import payment.sdk.android.demo.getPreferences
import com.google.gson.Gson
import java.util.UUID

data class MerchantAttribute(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val date: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    companion object {
        private const val KEY_MERCHANT_ATTRIBUTES = "merchant_attributes"

        fun saveMerchantAttribute(context: Context, environments: List<MerchantAttribute>) {
            val json = Gson().toJson(environments)
            context.getPreferences()
                .edit().putString(KEY_MERCHANT_ATTRIBUTES, json).apply()
        }

        fun getMerchantAttributes(context: Context): List<MerchantAttribute> {
            val json = context.getPreferences()
                .getString(KEY_MERCHANT_ATTRIBUTES, null)
            return if (json != null) {
                Gson().fromJson(json, Array<MerchantAttribute>::class.java).toList()
            } else {
                emptyList()
            }
        }
    }
}
