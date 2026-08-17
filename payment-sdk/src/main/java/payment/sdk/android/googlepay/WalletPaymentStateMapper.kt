package payment.sdk.android.googlepay

import com.google.gson.Gson
import org.json.JSONObject
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.payments.UnifiedPaymentPageVMEffects

/**
 * Maps a gateway payment `state` from a Google Pay accept (or order poll) response.
 */
internal object WalletPaymentStateMapper {

    fun parseState(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val fromModel = Gson().fromJson(body, PaymentResponse::class.java)?.state
            if (!fromModel.isNullOrBlank()) return fromModel
            JSONObject(body).optString("state").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun toUppEffect(state: String?): UnifiedPaymentPageVMEffects = when (state) {
        "AUTHORISED" -> UnifiedPaymentPageVMEffects.PaymentAuthorised
        "PURCHASED" -> UnifiedPaymentPageVMEffects.Purchased
        "CAPTURED" -> UnifiedPaymentPageVMEffects.Captured
        "POST_AUTH_REVIEW" -> UnifiedPaymentPageVMEffects.PostAuthReview
        "FAILED" -> UnifiedPaymentPageVMEffects.Failed("Google Pay payment failed")
        null, "" -> UnifiedPaymentPageVMEffects.Failed("Unknown payment state")
        else -> UnifiedPaymentPageVMEffects.Failed("Unknown payment state: $state")
    }

    fun toGooglePayResult(state: String?): GooglePayLauncher.Result = when (state) {
        "AUTHORISED" -> GooglePayLauncher.Result.Authorised
        "PURCHASED" -> GooglePayLauncher.Result.Success
        "CAPTURED" -> GooglePayLauncher.Result.Captured
        "POST_AUTH_REVIEW" -> GooglePayLauncher.Result.PostAuthReview
        "FAILED" -> GooglePayLauncher.Result.Failed("Google Pay payment failed")
        null, "" -> GooglePayLauncher.Result.Failed("Unknown payment state")
        else -> GooglePayLauncher.Result.Failed("Unknown payment state: $state")
    }
}
