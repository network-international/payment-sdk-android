package payment.sdk.android.demo.data

import android.content.Context
import payment.sdk.android.demo.getPreferences
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.MerchantAttribute
import payment.sdk.android.demo.model.Product
import com.google.gson.Gson
import payment.sdk.android.core.SavedCard
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage

class DataStoreImpl(private val context: Context) : DataStore {
    override fun saveEnvironment(environment: Environment) {
        val environments = Environment.getEnvironments(context)
        Environment.saveEnvironments(context, environments + environment)
    }

    override fun getEnvironments() = Environment.getEnvironments(context)

    override fun setSelectedEnvironment(environment: Environment) {
        Environment.setSelectedEnvironment(context, environmentId = environment.id)
    }

    override fun getSelectedEnvironment() = Environment.getSelectedEnvironment(context)

    override fun getSavedCard() = context.getPreferences().getString(KEY_SAVED_CARD, null)?.let {
        Gson().fromJson(it, SavedCard::class.java)
    }

    override fun deleteEnvironment(environment: Environment) {
        val environments = Environment.getEnvironments(context).toMutableList()
        environments.remove(environment)
        Environment.saveEnvironments(context, environments)
    }

    override fun getMerchantAttributes() = MerchantAttribute.getMerchantAttributes(context)

    override fun saveMerchantAttribute(merchantAttribute: MerchantAttribute) {
        val merchantAttributes = MerchantAttribute.getMerchantAttributes(context).toMutableList()
        merchantAttributes.add(merchantAttribute)
        MerchantAttribute.saveMerchantAttribute(context, merchantAttributes)
    }

    override fun deleteMerchantAttribute(merchantAttribute: MerchantAttribute) {
        val merchantAttributes = MerchantAttribute.getMerchantAttributes(context).toMutableList()
        merchantAttributes.remove(merchantAttribute)
        MerchantAttribute.saveMerchantAttribute(context, merchantAttributes)
    }

    override fun setOrderAction(action: String) {
        context.getPreferences().edit().putString(KEY_ORDER_ACTION, action).apply()
    }

    override fun getOrderAction() =
        context.getPreferences().getString(KEY_ORDER_ACTION, "SALE") ?: "SALE"

    override fun setOrderType(action: String) {
        context.getPreferences().edit().putString(KEY_ORDER_TYPE, action).apply()
    }

    override fun getOrderType() =
        context.getPreferences().getString(KEY_ORDER_TYPE, "SINGLE") ?: "SINGLE"

    override fun addProduct(product: Product) {
        val products = Product.getProducts(context).toMutableList()
        products.add(product)
        Product.saveProducts(context, products)
    }

    override fun deleteProduct(product: Product) {
        val products = Product.getProducts(context).toMutableList()
        products.remove(product)
        Product.saveProducts(context, products)
    }

    override fun getProducts() = Product.getProducts(context) + products

    override fun getSavedCards(): List<SavedCard> {
        val json = context.getPreferences().getString(KEY_SAVED_CARDS, null)
        return if (json != null) {
            Gson().fromJson(json, Array<SavedCard>::class.java).toList()
        } else {
            emptyList()
        }
    }

    override fun setSavedCard(savedCard: SavedCard) {
        context.getPreferences()
            .edit().putString(KEY_SAVED_CARD, Gson().toJson(savedCard)).apply()
    }

    override fun deleteSavedCard(savedCard: SavedCard) {
        val savedCards = getSavedCards().toMutableList()
        if (savedCards.contains(savedCard)) {
            savedCards.remove(savedCard)
            context.getPreferences()
                .edit().putString(KEY_SAVED_CARDS, Gson().toJson(savedCards)).apply()
        }
    }

    override fun updateMerchantAttribute(merchantAttribute: MerchantAttribute) {
        val attributes = MerchantAttribute.getMerchantAttributes(context).toMutableList()
        val index = attributes.indexOfFirst { it.id == merchantAttribute.id }
        if (index != -1) {
            attributes[index] = merchantAttribute
            MerchantAttribute.saveMerchantAttribute(context, attributes)
        }
    }

    override fun getCurrency(): AppCurrency {
        val currency = context.getPreferences().getString(KEY_CURRENCY, "")
        return AppCurrency.entries.firstOrNull { it.code == currency } ?: AppCurrency.AED
    }

    override fun setCurrency(currency: AppCurrency) {
        context.getPreferences().edit().putString(KEY_CURRENCY, currency.code).apply()
    }

    override fun setLanguage(language: AppLanguage) {
        context.getPreferences().edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    override fun getLanguage(): AppLanguage {
        val currency = context.getPreferences().getString(KEY_LANGUAGE, "")
        return AppLanguage.entries.firstOrNull { it.code == currency } ?: AppLanguage.ENGLISH
    }

    override fun saveCard(savedCard: SavedCard) {
        val savedCards = getSavedCards().toMutableList()
        if (savedCards.firstOrNull { it.cardToken == savedCard.cardToken } == null) {
            savedCards.add(savedCard)
            context.getPreferences()
                .edit().putString(KEY_SAVED_CARDS, Gson().toJson(savedCards)).apply()
        }
    }

    companion object {
        const val KEY_CURRENCY = "currency"
        const val KEY_LANGUAGE = "language"
        const val KEY_SAVED_CARDS = "saved_cards"
        const val KEY_SAVED_CARD = "saved_card"
        const val KEY_ORDER_ACTION = "order_action"
        const val KEY_ORDER_TYPE = "order_type"

        private val products = listOf(
            Product(name = "🐊", amount = 1.0),
            Product(name = "🦏", amount = 450.0),
            Product(name = "🐋", amount = 450.12),
            Product(name = "🦠", amount = 700.0),
            Product(name = "🐙", amount = 1500.0),
            Product(name = "🐡", amount = 2200.0),
            Product(name = "🐶", amount = 3000.0),
            Product(name = "🦊", amount = 3000.12),
        )
    }
}
