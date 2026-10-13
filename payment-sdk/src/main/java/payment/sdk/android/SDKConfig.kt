package payment.sdk.android

import android.content.Context
import java.util.Locale

object SDKConfig {
    internal var showOrderAmount: Boolean = false
    internal var showCancelAlert: Boolean = false
    private var sdkVersion: String = "5.0.0"
    private var language: String? = null

    private val supportedLanguages = setOf("en", "ar", "fr")

    fun shouldShowOrderAmount(show: Boolean): SDKConfig {
        this.showOrderAmount = show
        return this
    }

    fun shouldShowCancelAlert(show: Boolean): SDKConfig {
        this.showCancelAlert = show
        return this
    }

    /**
     * Sets the language of the SDK's screens, independent of the app's locale.
     * Supported: "en", "ar", "fr". Unsupported values are ignored. When never set,
     * the SDK follows the app's locale, as before.
     */
    fun setLanguage(language: String): SDKConfig {
        this.language = language.lowercase()
        return this
    }

    /** Wraps [base] in the language from [setLanguage]; returns it unchanged when none is set. */
    internal fun localizedContext(base: Context): Context {
        val lang = language?.takeIf { it in supportedLanguages } ?: return base
        val locale = Locale(lang)
        val config = android.content.res.Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    fun getSDKVersion() =  this.sdkVersion
}