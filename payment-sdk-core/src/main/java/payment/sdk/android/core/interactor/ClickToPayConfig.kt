package payment.sdk.android.core.interactor

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * Configuration for Click to Pay (Unified Click to Pay) integration.
 *
 * Two ways to construct:
 *   1. Preferred: pass [merchantId] and call [ClickToPayMerchantConfigService.fetch]
 *      (or [ClickToPayConfig.Companion.fetchFromGateway]) to resolve [dpaId] /
 *      [dpaClientId] / [dpaName] from `GET /config/merchants/{merchantId}/configs/vctp`.
 *   2. Legacy: pass [dpaId] / [dpaClientId] / [dpaName] directly if your backend
 *      already provides them.
 */
@Keep
@Parcelize
data class ClickToPayConfig(
    /** The DPA (Digital Payment Application) ID. Resolved via the gateway when null. */
    val dpaId: String? = null,

    /** The DPA Client ID for multi-merchant setups. Optional. */
    val dpaClientId: String? = null,

    /** Supported card brands (e.g., "visa", "mastercard") */
    val cardBrands: List<String> = listOf("visa", "mastercard"),

    /** The DPA name shown to consumers during checkout. Resolved via the gateway when null. */
    val dpaName: String? = null,

    /** Whether to use sandbox environment */
    val isSandbox: Boolean = false,

    /** When true, skip SDK init and show OTP page directly (for testing OTP UI) */
    val testOtpMode: Boolean = false,

    /**
     * Merchant identifier used by the gateway VCTP config endpoint. The SDK uses this to
     * resolve [dpaId] / [dpaClientId] / [dpaName] so the merchant never has to hardcode them.
     */
    val merchantId: String? = null
) : Parcelable {

    companion object {
        const val SANDBOX_SDK_URL = "https://sandbox.secure.checkout.visa.com/checkout-widget/resources/js/integration/v2/sdk.js"
        const val PRODUCTION_SDK_URL = "https://secure.checkout.visa.com/checkout-widget/resources/js/integration/v2/sdk.js"
    }

    fun getSdkUrl(): String = if (isSandbox) SANDBOX_SDK_URL else PRODUCTION_SDK_URL

    fun getCardBrandsParam(): String = cardBrands.joinToString(",")

    /** True if the credentials needed by the Click-to-Pay launch are populated. */
    fun isResolved(): Boolean = !dpaId.isNullOrEmpty()
}
