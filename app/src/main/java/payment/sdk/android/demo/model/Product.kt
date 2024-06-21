package payment.sdk.android.demo.model

import android.content.Context
import payment.sdk.android.demo.getPreferences
import com.google.gson.Gson
import java.util.UUID

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val isLocal: Boolean = false
) {
    companion object {
        private const val KEY_PRODUCTS = "products"

        fun saveProducts(context: Context, environments: List<Product>) {
            val json = Gson().toJson(environments)
            context.getPreferences()
                .edit().putString(KEY_PRODUCTS, json).apply()
        }

        fun getProducts(context: Context): List<Product> {
            val json = context.getPreferences()
                .getString(KEY_PRODUCTS, null)
            return if (json != null) {
                Gson().fromJson(json, Array<Product>::class.java).toList()
            } else {
                emptyList()
            }
        }
    }
}
