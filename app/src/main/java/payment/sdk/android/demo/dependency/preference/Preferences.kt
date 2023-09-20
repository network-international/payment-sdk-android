package payment.sdk.android.demo.dependency.preference

import payment.sdk.android.core.SavedCard

interface Preferences {

    fun put(key: String, value: Boolean)

    fun getString(key: String): String?

    fun getSavedCard(): SavedCard?

    fun saveCard(savedCard: SavedCard)

    companion object {
        /** General Preferences **/
        const val PRICE_IN_AED = "price_in_aed"
        const val LANGUAGE_IN_ARABIC = "language_in_arabic"

        /** Samsung Pay Preferences **/
        const val SPAY_ADDRESS_IN_PAYMENT_SHEET = "spay_address_in_payment_sheet"
        const val SPAY_SHOW_DELIVERY_METHOD = "spay_show_delivery_methods"
        const val SPAY_USER_NAME = "spay_user_name"
        const val SPAY_SHIPPING_ADDRESS_LINE_1 = "spay_shipping_address_line_1"
        const val SPAY_SHIPPING_ADDRESS_LINE_2 = "spay_shipping_address_line_2"
        const val SPAY_SHIPPING_CITY = "spay_shipping_city"
        const val SPAY_SHIPPING_STATE = "spay_shipping_state"
        const val SPAY_SHIPPING_COUNTRY = "spay_shipping_country"
        const val SPAY_SHIPPING_POST_CODE = "spay_shipping_post_code"
        const val SPAY_SHIPPING_EMAIL = "spay_shipping_email"
        const val SPAY_SHIPPING_PHONE = "spay_shipping_phone"
    }
}