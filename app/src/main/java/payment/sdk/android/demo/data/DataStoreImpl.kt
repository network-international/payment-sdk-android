package payment.sdk.android.demo.data

import android.content.Context
import androidx.core.content.edit
import payment.sdk.android.demo.getPreferences
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentOverride
import payment.sdk.android.demo.model.MerchantAttribute
import payment.sdk.android.demo.model.Product
import com.google.gson.Gson
import payment.sdk.android.core.SavedCard
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage
import payment.sdk.android.demo.model.Region

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

    override fun updateEnvironment(environment: Environment) {
        val environments = Environment.getEnvironments(context).toMutableList()
        val index = environments.indexOfFirst { it.id == environment.id }
        if (index != -1) {
            environments[index] = environment
            Environment.saveEnvironments(context, environments)
        }
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

    // Order action and type belong to the selected outlet, like the currency above: which
    // actions and order types an outlet accepts is outlet configuration, so one app-wide
    // value was necessarily wrong for every outlet but the one it was set for. Setting
    // either rewrites the selected outlet; the legacy preference keys survive only as the
    // migration source for environments saved before the move.
    override fun setOrderAction(action: String) {
        val selected = Environment.getSelectedEnvironment(context)
        if (selected != null) {
            updateEnvironment(selected.copy(orderAction = action))
        } else {
            context.getPreferences().edit { putString(KEY_ORDER_ACTION, action) }
        }
    }

    override fun getOrderAction() = Environment.selectedOrderAction(context)

    override fun setOrderType(action: String) {
        val selected = Environment.getSelectedEnvironment(context)
        if (selected != null) {
            updateEnvironment(selected.copy(orderType = action))
        } else {
            context.getPreferences().edit { putString(KEY_ORDER_TYPE, action) }
        }
    }

    override fun getOrderType() = Environment.selectedOrderType(context)

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
        context.getPreferences().edit { putString(KEY_SAVED_CARD, Gson().toJson(savedCard)) }
    }

    override fun deleteSavedCard(savedCard: SavedCard) {
        val savedCards = getSavedCards().toMutableList()
        if (savedCards.contains(savedCard)) {
            savedCards.remove(savedCard)
            context.getPreferences().edit { putString(KEY_SAVED_CARDS, Gson().toJson(savedCards)) }
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

    /**
     * The currency now belongs to the selected outlet rather than the app as a whole, so that
     * picking the Benefit outlet cannot leave the currency on AED and silently hide the option.
     * Callers are unchanged; only where the value is stored has moved.
     */
    override fun getCurrency(): AppCurrency {
        val code = Environment.selectedCurrency(context)
        return AppCurrency.entries.firstOrNull { it.code == code } ?: AppCurrency.AED
    }

    override fun setCurrency(currency: AppCurrency) {
        val selected = Environment.getSelectedEnvironment(context)
        if (selected != null) {
            updateEnvironment(selected.copy(currency = currency.code))
        } else {
            // Nothing selected yet: keep it in the legacy slot so the choice is not lost, and the
            // first outlet added inherits it through Environment.legacyCurrency.
            context.getPreferences().edit { putString(KEY_CURRENCY, currency.code) }
        }
    }

    override fun setLanguage(language: AppLanguage) {
        context.getPreferences().edit { putString(KEY_LANGUAGE, language.code) }
    }

    override fun getLanguage(): AppLanguage {
        val currency = context.getPreferences().getString(KEY_LANGUAGE, "")
        return AppLanguage.entries.firstOrNull { it.code == currency } ?: AppLanguage.ENGLISH
    }

    override fun saveCard(savedCard: SavedCard) {
        val savedCards = getSavedCards().toMutableList()
        if (savedCards.firstOrNull { it.cardToken == savedCard.cardToken } == null) {
            savedCards.add(savedCard)
            context.getPreferences().edit { putString(KEY_SAVED_CARDS, Gson().toJson(savedCards)) }
        }
    }

    override fun setRegion(region: Region) {
        context.getPreferences().edit { putString(KEY_REGION, region.code) }
    }

    override fun getRegion(): Region {
        if (EnvironmentOverride.isEnabled(context)) return getOverrideRegion()
        val code = context.getPreferences().getString(KEY_REGION, "")
        return Region.entries.firstOrNull { it.code == code } ?: Region.UAE
    }

    override fun isOverrideEnabled() = EnvironmentOverride.isEnabled(context)

    override fun setOverrideEnabled(enabled: Boolean) =
        EnvironmentOverride.setEnabled(context, enabled)

    override fun getOverrideRegion(): Region {
        val code = EnvironmentOverride.region(context)
        return Region.entries.firstOrNull { it.code == code } ?: Region.UAE
    }

    override fun setOverrideRegion(region: Region) =
        EnvironmentOverride.setRegion(context, region.code)

    override fun getOverrideCurrency(): AppCurrency {
        val code = EnvironmentOverride.currency(context)
        return AppCurrency.entries.firstOrNull { it.code == code } ?: AppCurrency.AED
    }

    override fun setOverrideCurrency(currency: AppCurrency) =
        EnvironmentOverride.setCurrency(context, currency.code)

    override fun getOverrideOrderAction() = EnvironmentOverride.orderAction(context)

    override fun setOverrideOrderAction(action: String) =
        EnvironmentOverride.setOrderAction(context, action)

    override fun getOverrideOrderType() = EnvironmentOverride.orderType(context)

    override fun setOverrideOrderType(type: String) =
        EnvironmentOverride.setOrderType(context, type)

    override fun setSDKColor(key: String, hex: String) {
        context.getPreferences().edit { putString(key, hex) }
    }

    override fun getSDKColor(key: String, default: String): String {
        return context.getPreferences().getString(key, default) ?: default
    }

    companion object {
        const val KEY_CURRENCY = "currency"
        const val KEY_LANGUAGE = "language"
        const val KEY_SAVED_CARDS = "saved_cards"
        const val KEY_SAVED_CARD = "saved_card"
        const val KEY_ORDER_ACTION = "order_action"
        const val KEY_REGION = "region"
        const val KEY_ORDER_TYPE = "order_type"

        private val products = listOf(
            Product(name = "Quick Test", amount = 0.10),
            Product(name = "Micro", amount = 0.50),
            Product(name = "Small", amount = 1.0),
            Product(name = "Basic", amount = 2.0),
            Product(name = "Standard", amount = 5.0),
            Product(name = "Medium", amount = 10.0),
            Product(name = "Large", amount = 50.0),
            Product(name = "Premium", amount = 100.0),
            Product(name = "Pro", amount = 450.0),
            Product(name = "Business", amount = 1000.0),
            Product(name = "Enterprise", amount = 2200.0),
            Product(name = "Ultimate", amount = 3000.0),
        )
    }
}
