package payment.sdk.android.demo.dependency.configuration

import java.util.*

interface Configuration {

    val locale: Locale

    val currency: Currency

    fun addConfigurationListener(listener: ConfigurationListener)

    fun removeConfigurationListener(listener: ConfigurationListener)

    interface ConfigurationListener {
        /**
         * Locale changed from app's settings menu
         */
        fun onLocaleChanged(locale: Locale)

        /**
         * Currency changed from app's settings menu
         */
        fun onCurrencyChanged(currency: Currency)
    }
}