package payment.sdk.android.clicktopay

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.google.gson.Gson
import org.json.JSONObject
import payment.sdk.android.core.interactor.ClickToPayCard
import payment.sdk.android.core.interactor.DigitalCardData
import payment.sdk.android.core.interactor.ValidationChannel

/**
 * JavaScript interface for communication between WebView and Android for Click to Pay SDK.
 * This bridge enables the Visa JavaScript SDK to communicate with native Android code.
 */
@Keep
class ClickToPayJsBridge(
    private val callback: ClickToPayJsCallback
) {
    private val gson = Gson()

    /**
     * Called when SDK initialization is complete
     */
    @JavascriptInterface
    fun onSdkInitialized() {
        callback.onSdkInitialized()
    }

    /**
     * Called when SDK initialization fails
     */
    @JavascriptInterface
    fun onSdkInitError(errorJson: String) {
        val (reason, message) = parseError(errorJson)
        callback.onSdkInitError(reason, message)
    }

    /**
     * Called when getCards returns available cards
     */
    @JavascriptInterface
    fun onCardsAvailable(cardsJson: String) {
        try {
            val jsCards = gson.fromJson(cardsJson, Array<JsCard>::class.java)
            val cards = jsCards.map { it.toClickToPayCard() }
            callback.onCardsAvailable(cards)
        } catch (e: Exception) {
            callback.onError("PARSE_ERROR", "Failed to parse cards: ${e.message}")
        }
    }

    /**
     * Called when identity validation is required
     */
    @JavascriptInterface
    fun onIdentityValidationRequired(channelsJson: String) {
        try {
            val jsChannels = gson.fromJson(channelsJson, Array<JsValidationChannel>::class.java)
            val channels = jsChannels.map { it.toValidationChannel() }
            callback.onIdentityValidationRequired(channels)
        } catch (e: Exception) {
            callback.onError("PARSE_ERROR", "Failed to parse validation channels: ${e.message}")
        }
    }

    /**
     * Called when user needs to add a new card
     */
    @JavascriptInterface
    fun onAddCardRequired() {
        callback.onAddCardRequired()
    }

    /**
     * Called when OTP is sent successfully
     */
    @JavascriptInterface
    fun onOtpSent(maskedDestination: String) {
        callback.onOtpSent(maskedDestination)
    }

    /**
     * Called when identity validation is complete
     */
    @JavascriptInterface
    fun onIdentityValidated() {
        callback.onIdentityValidated()
    }

    /**
     * Called when checkout is successful
     */
    @JavascriptInterface
    fun onCheckoutSuccess(checkoutResponseJson: String) {
        try {
            val response = gson.fromJson(checkoutResponseJson, JsCheckoutResponse::class.java)
            callback.onCheckoutSuccess(
                checkoutResponse = response.checkoutResponse,
                srcDigitalCardId = response.srcDigitalCardId,
                idToken = response.idToken
            )
        } catch (e: Exception) {
            callback.onError("PARSE_ERROR", "Failed to parse checkout response: ${e.message}")
        }
    }

    /**
     * Called when any error occurs
     */
    @JavascriptInterface
    fun onError(errorJson: String) {
        val (reason, message) = parseError(errorJson)
        callback.onError(reason, message)
    }

    /**
     * Called when user cancels the flow
     */
    @JavascriptInterface
    fun onCanceled() {
        callback.onCanceled()
    }

    /**
     * Called when user taps the close button
     */
    @JavascriptInterface
    fun onClose() {
        callback.onClose()
    }

    /**
     * Log messages from JavaScript for debugging
     */
    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("ClickToPayJS", message)
    }

    /**
     * Parse error JSON where reason/message can be strings or objects.
     * The Visa SDK sometimes returns objects for these fields.
     */
    private fun parseError(errorJson: String): Pair<String, String> {
        return try {
            val json = JSONObject(errorJson)
            val reason = json.opt("reason")?.let {
                if (it is String) it else it.toString()
            } ?: "UNKNOWN"
            val message = json.opt("message")?.let {
                if (it is String) it else it.toString()
            } ?: json.optString("error", errorJson)
            Pair(reason, message)
        } catch (e: Exception) {
            Pair("PARSE_ERROR", errorJson)
        }
    }

    // JSON data classes for parsing
    @Keep
    data class JsCard(
        val srcDigitalCardId: String,
        val panLastFour: String?,
        val digitalCardData: JsDigitalCardData?,
        val panExpirationMonth: String?,
        val panExpirationYear: String?,
        val paymentCardDescriptor: String?,
        val paymentCardType: String?,
        val paymentCardNetwork: String?
    ) {
        fun toClickToPayCard() = ClickToPayCard(
            srcDigitalCardId = srcDigitalCardId,
            panLastFour = panLastFour ?: "",
            digitalCardData = DigitalCardData(
                descriptorName = digitalCardData?.descriptorName,
                artUri = digitalCardData?.artUri,
                artHeight = digitalCardData?.artHeight,
                artWidth = digitalCardData?.artWidth
            ),
            panExpirationMonth = panExpirationMonth,
            panExpirationYear = panExpirationYear,
            paymentCardDescriptor = paymentCardDescriptor,
            paymentCardType = paymentCardType,
            paymentCardNetwork = paymentCardNetwork
        )
    }

    @Keep
    data class JsDigitalCardData(
        val descriptorName: String?,
        val artUri: String?,
        val artHeight: Int?,
        val artWidth: Int?
    )

    @Keep
    data class JsValidationChannel(
        val id: String,
        val type: String,
        val maskedValue: String?
    ) {
        fun toValidationChannel() = ValidationChannel(
            id = id,
            type = type,
            maskedValue = maskedValue ?: ""
        )
    }

    @Keep
    data class JsCheckoutResponse(
        val checkoutResponse: String,
        val srcDigitalCardId: String,
        val idToken: String?
    )

    @Keep
    data class JsError(
        val reason: String,
        val message: String
    )
}

/**
 * Callback interface for Click to Pay JavaScript bridge events
 */
interface ClickToPayJsCallback {
    fun onSdkInitialized()
    fun onSdkInitError(reason: String, message: String)
    fun onCardsAvailable(cards: List<ClickToPayCard>)
    fun onIdentityValidationRequired(channels: List<ValidationChannel>)
    fun onAddCardRequired()
    fun onOtpSent(maskedDestination: String)
    fun onIdentityValidated()
    fun onCheckoutSuccess(checkoutResponse: String, srcDigitalCardId: String?, idToken: String?)
    fun onError(reason: String, message: String)
    fun onCanceled()
    fun onClose()
}
