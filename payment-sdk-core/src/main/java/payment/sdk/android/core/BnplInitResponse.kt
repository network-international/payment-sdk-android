package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Response of `POST .../payments/{paymentRef}/{tamara|tabby}`.
 *
 * Observed verbatim from the gateway for Tamara:
 * `{"webUrl": "https://checkout-sandbox.tamara.co/checkout/…", "tamaraOrderId": "761d053b-…"}`
 *
 * `webUrl` is the field both providers use and the one the hosted paypage reads. The sibling APMs
 * spell it differently — Benefit returns `paymentUrl`, QPay `redirectUri` — so those spellings are
 * accepted too rather than leaving the payer on a blank screen if the gateway ever answers in kind.
 */
@Keep
class BnplInitResponse {
    var webUrl: String? = null
    var redirectUrl: String? = null
    var paymentUrl: String? = null
    var checkoutUrl: String? = null

    /** True when the gateway aborted the checkout itself, e.g. the order was already paid. */
    var cancelled: Boolean? = null

    /**
     * True when the gateway refused the checkout while still answering 200 — an over-large basket
     * comes back as `{"isError": true, "errorMessage": "order amount too high"}`. Read explicitly
     * rather than inferred from a missing URL: the two have only ever coincided by luck, and a
     * response carrying both would otherwise send the payer to a checkout the gateway rejected.
     */
    var isError: Boolean? = null
    var errorMessage: String? = null

    @SerializedName(value = "tamaraOrderId")
    var tamaraOrderId: String? = null

    @SerializedName(value = "tabbyPaymentId")
    var tabbyPaymentId: String? = null

    var orderId: String? = null
    var paymentId: String? = null

    /** The hosted page to load, or null when there is none to load or the gateway flagged an error. */
    val hostedCheckoutUrl: String?
        get() = if (isError == true) null else listOfNotNull(webUrl, redirectUrl, paymentUrl, checkoutUrl)
            .firstOrNull { it.isNotBlank() }

    /**
     * The provider's own reference for this checkout, returned at initiation. It is what `/accept`
     * needs, so capturing it here means a return leg that arrives without it in the query string can
     * still be finalised. Provider-specific names win over the generic ones: an order id and a
     * payment id are different things, and taking the wrong one fails the accept call.
     */
    val providerReference: String?
        get() = listOfNotNull(tamaraOrderId, tabbyPaymentId, orderId, paymentId)
            .firstOrNull { it.isNotBlank() }
}
