package payment.sdk.android.core

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * The buy-now-pay-later providers the gateway offers as alternative payment methods.
 *
 * Tamara and Tabby are the same integration end to end — same request body, same hosted-checkout
 * redirect, same accept-then-poll finish — and differ only in the nouns below. Modelling that as
 * data rather than as two near-identical activities is what keeps the two flows from drifting apart
 * as either provider changes.
 *
 * Only gateway-facing facts live here; the row label and brand mark belong to the UI module.
 */
@Keep
@Parcelize
enum class BnplProvider(
    /** Name in the order's `paymentMethods.apm` array. */
    val apmName: String,
    /** Last path segment of the checkout endpoint, and the `payment:{segment}` rel that carries it. */
    val pathSegment: String,
    /**
     * Field name the provider's `/accept` endpoint expects its own reference under. The gateway
     * named these itself and they follow no shared convention: Tamara wants the checkout's order
     * id, Tabby the payment id, so swapping them fails every accept call.
     */
    val acceptIdField: String
) : Parcelable {
    TAMARA("TAMARA", "tamara", "tamaraOrderId"),
    TABBY("TABBY", "tabby", "tabbyPaymentId");

    val linkRel: String get() = "payment:$pathSegment"

    companion object {
        /** The only product the APM endpoint accepts; every other value is rejected on `type`. */
        const val CHECKOUT_TYPE = "INSTALLMENTS"

        /**
         * Marks the return legs so the WebView can tell them apart. The providers append their own
         * parameters to whatever URL they are given, so an extra one of ours rides along untouched.
         */
        const val RESULT_PARAM = "ni_sdk_result"

        fun fromApmName(name: String): BnplProvider? =
            entries.firstOrNull { it.apmName.equals(name, ignoreCase = true) }
    }
}
