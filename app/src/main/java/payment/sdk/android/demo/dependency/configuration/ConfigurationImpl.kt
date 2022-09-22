package payment.sdk.android.demo.dependency.configuration

import payment.sdk.android.demo.dependency.configuration.Configuration.ConfigurationListener
import payment.sdk.android.demo.dependency.preference.Preferences
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import java.util.*
import javax.inject.Inject


class ConfigurationImpl @Inject constructor(
        private val context: Context) : Configuration {

    private val configurationListeners = mutableListOf<ConfigurationListener>()

    override val locale: Locale
        @WorkerThread
        get() {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return if (preferences.getBoolean(Preferences.LANGUAGE_IN_ARABIC, false)) {
                Locale.getAvailableLocales().first { locale -> locale.country == "AE" } ?: Locale.US
            } else {
                Locale.US
            }
        }

    override val currency: Currency
        @WorkerThread
        get() {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return if (preferences.getBoolean(Preferences.PRICE_IN_AED, false)) {
                Currency.getInstance(CURRENCY_CODE_AED)
            } else {
                Currency.getInstance(CURRENCY_CODE_USD)
            }
        }

    private val listener: SharedPreferences.OnSharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                when (key) {
                    Preferences.PRICE_IN_AED -> {
                        val currency = if (sharedPreferences.getBoolean(Preferences.PRICE_IN_AED, false)) {
                            Currency.getInstance(CURRENCY_CODE_AED)
                        } else {
                            Currency.getInstance(CURRENCY_CODE_USD)
                        }
                        configurationListeners.forEach { listener ->
                            listener.onCurrencyChanged(currency)
                        }
                    }
                    Preferences.LANGUAGE_IN_ARABIC -> {
                        val locale = if (sharedPreferences.getBoolean(Preferences.LANGUAGE_IN_ARABIC, false)) {
                            Locale.getAvailableLocales().first { locale ->
                                locale.country == "AE"
                            } ?: Locale.US
                        } else {
                            Locale.US
                        }
                        configurationListeners.forEach { listener ->
                            listener.onLocaleChanged(locale)
                        }
                    }
                }
            }.apply {
                PreferenceManager.getDefaultSharedPreferences(context)
                        .registerOnSharedPreferenceChangeListener(this)
            }

    override fun addConfigurationListener(listener: ConfigurationListener) {
        configurationListeners.add(element = listener)
    }

    override fun removeConfigurationListener(listener: ConfigurationListener) {
        configurationListeners.remove(element = listener)
    }

    companion object {
        private const val CURRENCY_CODE_AED = "AED"
        private const val CURRENCY_CODE_USD = "USD"
    }
}
