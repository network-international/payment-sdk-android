package payment.sdk.android.core.interactor

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * Configuration for Click to Pay (Unified Click to Pay) integration.
 * Used by PSPs to enable Click to Pay checkout flow.
 */
@Keep
@Parcelize
data class ClickToPayConfig(
    /**
     * The DPA (Digital Payment Application) ID assigned by Visa during onboarding.
     */
    val dpaId: String,

    /**
     * The DPA Client ID for multi-merchant setups. Optional.
     */
    val dpaClientId: String? = null,

    /**
     * Supported card brands (e.g., "visa", "mastercard")
     */
    val cardBrands: List<String> = listOf("visa", "mastercard"),

    /**
     * The DPA name shown to consumers during checkout
     */
    val dpaName: String,

    /**
     * Whether to use sandbox environment
     */
    val isSandbox: Boolean = false,

    /**
     * When true, skip SDK init and show OTP page directly (for testing OTP UI)
     */
    val testOtpMode: Boolean = false
) : Parcelable {

    companion object {
        const val SANDBOX_SDK_URL = "https://sandbox.secure.checkout.visa.com/checkout-widget/resources/js/integration/v2/sdk.js"
        const val PRODUCTION_SDK_URL = "https://secure.checkout.visa.com/checkout-widget/resources/js/integration/v2/sdk.js"
    }

    fun getSdkUrl(): String = if (isSandbox) SANDBOX_SDK_URL else PRODUCTION_SDK_URL

    fun getCardBrandsParam(): String = cardBrands.joinToString(",")
}
