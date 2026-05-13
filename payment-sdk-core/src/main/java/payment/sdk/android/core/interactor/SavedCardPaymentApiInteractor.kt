package payment.sdk.android.core.interactor

import android.app.Application
import com.google.gson.Gson
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import java.lang.Exception

class SavedCardPaymentApiInteractor(
    private val httpClient: HttpClient,
    private val app: Application
) {
    suspend fun doSavedCardPayment(request: SavedCardPaymentApiRequest): SavedCardResponse {
        // Use a JsonObject directly so we can emit explicit nulls — iOS sends
        // `"vis":null` even when no Visa plan is selected, and the gateway's
        // saved-card route appears to require the key to be present.
        val bodyJson = com.google.gson.JsonObject().apply {
            addProperty(KEY_EXPIRY, request.savedCard.expiry)
            addProperty(KEY_CARD_TOKEN, request.savedCard.cardToken)
            addProperty(KEY_CARDHOLDER_NAME, request.savedCard.cardholderName)
            request.cvv?.let { addProperty(KEY_CVV, it) }
            request.payerIp?.let { addProperty(KEY_PAYER_IP, it) }
            val visa = request.visaRequest
            if (visa != null) {
                add(CardPaymentInteractor.PAYMENT_FIELD_VISA, com.google.gson.JsonObject().apply {
                    addProperty(CardPaymentInteractor.PAYMENT_FIELD_PLAN_SELECTION_INDICATOR, visa.planSelectionIndicator)
                    addProperty(CardPaymentInteractor.PAYMENT_FIELD_VISA_PLAN_ID, visa.vPlanId)
                    addProperty(CardPaymentInteractor.PAYMENT_FIELD_VISA_TERMS, visa.acceptedTAndCVersion)
                })
            } else {
                // Explicit null — matches the iOS body shape.
                add(CardPaymentInteractor.PAYMENT_FIELD_VISA, com.google.gson.JsonNull.INSTANCE)
            }
        }
        // Use StringBody with a Gson-serialized payload so the explicit
        // `vis: null` is preserved. `Body.Json` (org.json.JSONObject) silently
        // drops null values, which would put us right back where we started.
        val bodyString = bodyJson.toString()
        // Auth via Cookie matches the eligibility-check call (see
        // VisaInstallmentPlanInteractor) — the gateway's auth filter on
        // /saved-card rejects `Authorization: payment <jwt>` with 401 before
        // the request reaches the controller (no x-correlation-id on the
        // response). iOS appears to send the `payment` scheme but actually
        // succeeds because NSURLSession auto-attaches the payment-token cookie
        // (the saved-card URL falls under the cookie's Path scope). On
        // Android HttpURLConnection we have to set the cookie ourselves.
        val response = httpClient.put(
            url = request.savedCardUrl,
            headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                TransactionServiceHttpAdapter.HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                "Cookie" to request.paymentCookie
            ),
            body = Body.StringBody(bodyString)
        )

        return when (response) {
            is SDKHttpResponse.Failed -> SavedCardResponse.Error(response.error)
            is SDKHttpResponse.Success -> {
                val paymentResponse = Gson().fromJson(response.body, PaymentResponse::class.java)
                SavedCardResponse.Success(paymentResponse)
            }
        }
    }

    companion object {
        internal const val KEY_EXPIRY = "expiry"
        internal const val KEY_CARD_TOKEN = "cardToken"
        internal const val KEY_CARDHOLDER_NAME = "cardholderName"
        internal const val KEY_CVV = "cvv"
        internal const val KEY_PAYER_IP = "payerIp"
    }
}

data class SavedCardPaymentApiRequest(
    val accessToken: String,
    val paymentCookie: String,
    val savedCardUrl: String,
    val savedCard: SavedCard,
    val payerIp: String?,
    val cvv: String?,
    val visaRequest: VisaRequest? = null
)

sealed class SavedCardResponse {
    data class Success(val paymentResponse: PaymentResponse) : SavedCardResponse()

    data class Error(val error: Exception) : SavedCardResponse()
}