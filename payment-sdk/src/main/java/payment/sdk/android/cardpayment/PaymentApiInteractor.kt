package payment.sdk.android.cardpayment

import payment.sdk.android.core.CardType
import payment.sdk.android.core.OrderAmount
import org.json.JSONObject
import payment.sdk.android.cardpayment.threedsecuretwo.webview.BrowserData
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.interactor.VisaRequest

interface PaymentApiInteractor {

    fun authorizePayment(
            url: String,
            code: String,
            success: (List<String>, String) -> Unit,
            error: (Exception) -> Unit)

    fun getOrder(
            orderUrl: String,
            paymentCookie: String,
            success: (String, String, Set<CardType>, OrderAmount, String, String, JSONObject) -> Unit,
            error: (Exception) -> Unit)

    fun doPayment(
            paymentUrl: String,
            paymentCookie: String,
            pan: String,
            expiry: String,
            cvv: String,
            cardHolder: String,
            payerIp: String?,
            visRequest: VisaRequest?,
            success: (state: String, response: JSONObject) -> Unit,
            error: (Exception) -> Unit)

    fun postThreeDSTwoBrowserAuthentications(
            browserData: BrowserData,
            threeDSCompInd: String,
            threeDSAuthenticationsUrl: String,
            paymentCookie: String,
            notificationUrl: String,
            success: (response: JSONObject) -> Unit,
            error: (Exception) -> Unit
    )

    fun postThreeDSTwoChallengeResponse(
            threeDSTwoChallengeResponseURL: String,
            paymentCookie: String,
            success: (state: String, response: JSONObject) -> Unit,
            error: (Exception) -> Unit
    )

    fun getPayerIP(
            requestIpUrl: String,
            paymentCookie: String,
            success: (response: JSONObject) -> Unit,
            error: (Exception) -> Unit
    )

    fun getPayerIp(
        url: String,
        success: (payerIp: String?) -> Unit,
        error: (Exception) -> Unit
    )

    fun visaEligibilityCheck(
        url: String,
        token: String,
        cardNumber: String,
        success: (isEligible: Boolean, plans: VisaPlans) -> Unit,
        error: (Exception) -> Unit
    )
}