package payment.sdk.android.demo.data

import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.MerchantAttribute
import payment.sdk.android.demo.model.Product
import payment.sdk.android.core.SavedCard
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage

interface DataStore {
    fun saveEnvironment(environment: Environment)

    fun getEnvironments(): List<Environment>

    fun setSelectedEnvironment(environment: Environment)

    fun getSelectedEnvironment(): Environment?

    fun saveCard(savedCard: SavedCard)

    fun getSavedCard(): SavedCard?

    fun deleteEnvironment(environment: Environment)

    fun getMerchantAttributes(): List<MerchantAttribute>

    fun saveMerchantAttribute(merchantAttribute: MerchantAttribute)

    fun deleteMerchantAttribute(merchantAttribute: MerchantAttribute)

    fun setOrderAction(action: String)

    fun getOrderAction(): String

    fun setOrderType(action: String)

    fun getOrderType(): String

    fun setRecurringType(recurringType: String)

    fun getRecurringType(): String

    fun setFrequency(frequency: String)

    fun getFrequency(): String

    fun setTenure(tenure: Int?)

    fun getTenure(): Int?

    fun addProduct(product: Product)

    fun deleteProduct(product: Product)

    fun getProducts(): List<Product>

    fun getSavedCards(): List<SavedCard>

    fun setSavedCard(savedCard: SavedCard)

    fun deleteSavedCard(savedCard: SavedCard)

    fun updateMerchantAttribute(merchantAttribute: MerchantAttribute)

    fun getCurrency(): AppCurrency

    fun setCurrency(currency: AppCurrency)

    fun setLanguage(language: AppLanguage)

    fun getLanguage(): AppLanguage
}