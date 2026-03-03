package payment.sdk.android

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import java.util.Locale

object SDKConfig {
    internal var showOrderAmount: Boolean = false
    internal var showCancelAlert: Boolean = false
    @DrawableRes
    internal var merchantLogoResId: Int = 0
    private var sdkVersion: String = "5.0.0"
    private var language: String? = null

    private val supportedLanguages = setOf("en", "ar", "fr")

    // Runtime color overrides: resource ID -> ARGB color int
    internal val colorOverrides: MutableMap<Int, Int> = mutableMapOf()

    fun shouldShowOrderAmount(show: Boolean): SDKConfig {
        this.showOrderAmount = show
        return this
    }

    fun shouldShowCancelAlert(show: Boolean): SDKConfig {
        this.showCancelAlert = show
        return this
    }

    /**
     * Sets a drawable resource ID for the merchant logo displayed at the top of the payment screen.
     *
     * @param resId The drawable resource ID for the merchant logo. Pass 0 to hide the logo.
     */
    fun setMerchantLogo(@DrawableRes resId: Int): SDKConfig {
        this.merchantLogoResId = resId
        return this
    }

    /**
     * Sets a runtime color override for the given color resource ID.
     * This takes precedence over the XML color resource value.
     *
     * @param resId The color resource ID to override (e.g. R.color.payment_sdk_pay_button_background_color)
     * @param color The ARGB color int value
     */
    fun setColor(@ColorRes resId: Int, @ColorInt color: Int): SDKConfig {
        colorOverrides[resId] = color
        return this
    }

    /**
     * Returns the runtime color override for the given resource ID, or null if not set.
     */
    @ColorInt
    fun getColorOverride(@ColorRes resId: Int): Int? = colorOverrides[resId]

    /**
     * Sets the SDK language for Click to Pay and other localized components.
     * Supported languages: "en", "ar", "fr".
     *
     * @param language The language code (e.g. "en", "ar", "fr")
     */
    fun setLanguage(language: String): SDKConfig {
        this.language = language
        return this
    }

    /**
     * Returns the SDK language.
     * Priority: explicit language > device language > "en" (fallback).
     * Only returns a supported language ("en", "ar", "fr").
     */
    internal fun getLanguage(): String {
        language?.let {
            if (it in supportedLanguages) return it
        }
        val deviceLanguage = Locale.getDefault().language
        if (deviceLanguage in supportedLanguages) return deviceLanguage
        return "en"
    }

    fun getSDKVersion() =  this.sdkVersion
}