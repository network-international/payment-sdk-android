package payment.sdk.android

import android.app.Activity
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.CardPaymentActivity
import payment.sdk.android.cardpayment.CardPaymentRequest
import payment.sdk.android.cardpayment.savedCard.SavedCardActivityArgs
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoConfig
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoRequest
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.core.Order
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.samsungpay.SamsungPayClient
import payment.sdk.android.samsungpay.SamsungPayResponse
import java.net.URI


class PaymentClient(
        private val context: Activity,
        val serviceId: String
) {

    private val samsungPayClient: SamsungPayClient by lazy {
        SamsungPayClient(context, serviceId, CoroutinesGatewayHttpClient())
    }

    fun getSupportedPaymentMethods(listener: SupportedPaymentTypesListener) {
        GlobalScope.launch(Dispatchers.Main) {

            val supportedPaymentTypes = mutableListOf(PaymentType.CARD_PAYMENT)

            if (samsungPayClient.isSamsungPayAvailable()) {
                supportedPaymentTypes.add(PaymentType.SAMSUNG_PAY)
            }

            listener.onReady(supportedPaymentTypes = supportedPaymentTypes)
        }
    }

    fun launchCardPayment(request: CardPaymentRequest, requestCode: Int) {
        context.startActivityForResult(CardPaymentActivity.getIntent(
                context = context,
                url = request.gatewayUrl,
                code = request.code
            ), requestCode
        )
    }

    /**
     * Please note that this feature is in its early stages, and users may encounter some bugs.
     * Initiates a payment using a saved card retrieved from an order response.
     *
     *
     * To make a payment with a saved card, the 'SavedCard' object must be included in the order request body, as shown below:
     *
     * {
     *     "action": "SALE",
     *     "amount": {
     *         "currencyCode": "AED",
     *         "value": 140
     *     },
     *     "savedCard": {
     *         "maskedPan": "230000******0222",
     *         "expiry": "2025-08",
     *         "cardholderName": "test",
     *         "scheme": "MASTERCARD",
     *         "cardToken": "card_token",
     *         "recaptureCsc": false
     *     }
     * }
     *
     * @param order The response received from the Ngenius order API.
     * @param code A unique code used to receive results for the 'ActivityForResult'.
     *
     * @throws IllegalArgumentException This function will throw an exception if the saved card
     *                                  is not found in the order response or if the provided arguments
     *                                  are incorrect.
     */
    @Throws(IllegalArgumentException::class)
    fun launchSavedCardPayment(
        order: Order,
        code: Int
    ) {
        val intent = SavedCardActivityArgs.getArgs(
            order = order
        ).toIntent(context)
        context.startActivityForResult(
            intent,
            code
        )
    }

    /**
     * Please note that this feature is in its early stages, and users may encounter some bugs.
     * Initiates a payment using a saved card retrieved from an order response. this method accepts
     * cvv as argument to immediately process payment
     *
     * To make a payment with a saved card, the 'SavedCard' object must be included in the order
     * request body, as shown below:
     *
     * {
     *     "action": "SALE",
     *     "amount": {
     *         "currencyCode": "AED",
     *         "value": 140
     *     },
     *     "savedCard": {
     *         "maskedPan": "230000******0222",
     *         "expiry": "2025-08",
     *         "cardholderName": "test",
     *         "scheme": "MASTERCARD",
     *         "cardToken": "card_token",
     *         "recaptureCsc": false
     *     }
     * }
     *
     * @param order The response received from the Ngenius order API.
     * @param cvv CVV (Card Verification Value) code to authorize the payment immediately.
     * @param code A unique code used to receive results for the 'ActivityForResult'.
     *
     * @throws IllegalArgumentException This function will throw an exception if the saved card is
     *                                  not found in the order response or if the provided arguments
     *                                  are incorrect.
     */
    @Throws(IllegalArgumentException::class)
    fun launchSavedCardPayment(
        order: Order,
        cvv: String,
        code: Int
    ) {
        val intent = SavedCardActivityArgs.getArgs(
            order = order,
            cvv
        ).toIntent(context)
        context.startActivityForResult(
            intent,
            code
        )
    }

    fun launchSamsungPay(order: Order, merchantName: String, samsungPayResponse: SamsungPayResponse) {
        samsungPayClient.startSamsungPay(
                order = order,
                merchantName = merchantName,
                samsungPayResponse = samsungPayResponse)
    }

    fun isSamsungPayAvailable(statusListener: StatusListener) {
        samsungPayClient.isSamsungPayAvailable(statusListener)
    }

    fun executeThreeDS(paymentResponse: PaymentResponse, requestCode: Int) {
        val threeDSecureTwoConfig = ThreeDSecureTwoConfig.buildFromPaymentResponse(paymentResponse)
        if (threeDSecureTwoConfig.directoryServerID != null &&
            threeDSecureTwoConfig.threeDSMessageVersion != null &&
            threeDSecureTwoConfig.threeDSTwoAuthenticationURL != null &&
            threeDSecureTwoConfig.threeDSTwoChallengeResponseURL != null) {

            val threedsAuthUri = URI(threeDSecureTwoConfig.threeDSTwoAuthenticationURL)
            val authUrl = "https://${threedsAuthUri.host}/transactions/paymentAuthorization"
            val transactionServiceHttpAdapter = TransactionServiceHttpAdapter()
            val outletRef = paymentResponse.outletId

            val orderRef = paymentResponse.orderReference
            val paymentReference = paymentResponse.reference
            val threeDSTwoChallengeResponseURL = paymentResponse.links?.threeDSChallengeResponseUrl?.href
            val threeDSMessageVersion = paymentResponse.threeDSTwo?.messageVersion
            val directoryServerID = paymentResponse.threeDSTwo?.directoryServerID
            val threeDSTwoAuthenticationURL = paymentResponse.links?.threeDSAuthenticationsUrl?.href
            val threeDSServerTransID = paymentResponse.threeDSTwo?.threeDSServerTransID
            val threeDSMethodURL = paymentResponse.threeDSTwo?.threeDSMethodURL
            val threeDSecureRequest = ThreeDSecureTwoRequest.buildFromPaymentResponse(paymentResponse)
            val threeDSMethodNotificationURL = threeDSecureRequest.threeDSMethodNotificationURL
            val threeDSMethodData = threeDSecureRequest.threeDSMethodData
            transactionServiceHttpAdapter.getAuthTokenFromCode(
                url = authUrl,
                code = threeDSecureTwoConfig.authenticationCode ?: "",
                success = { cookies, orderUrl ->
                    val paymentCookie = cookies.first { it.startsWith("payment-token") }
                    context.startActivityForResult(
                        ThreeDSecureTwoWebViewActivity.getIntent(
                            context = context,
                            threeDSMethodData = threeDSMethodData,
                            threeDSMethodNotificationURL = threeDSMethodNotificationURL,
                            threeDSMethodURL = threeDSMethodURL,
                            threeDSServerTransID = threeDSServerTransID,
                            paymentCookie = paymentCookie,
                            threeDSAuthenticationsUrl = threeDSTwoAuthenticationURL,
                            directoryServerID = directoryServerID,
                            threeDSMessageVersion = threeDSMessageVersion,
                            threeDSTwoChallengeResponseURL = threeDSTwoChallengeResponseURL,
                            outletRef = outletRef,
                            orderRef = orderRef,
                            orderUrl = orderUrl,
                            paymentRef = paymentReference
                        ),
                        requestCode
                    )
                },
                error = {
                    println(it)
                }
            )
        } else {
            context.startActivityForResult(
                ThreeDSecureWebViewActivity.getIntent(
                    context = context,
                    acsUrl = paymentResponse.threeDSOne?.acsUrl,
                    acsPaReq = paymentResponse.threeDSOne?.acsPaReq,
                    acsMd = paymentResponse.threeDSOne?.acsMd,
                    gatewayUrl = paymentResponse.links?.threeDSOneUrl?.href
                ),
                requestCode
            )
        }
    }

    interface SupportedPaymentTypesListener {
        fun onReady(supportedPaymentTypes: List<PaymentType>)
    }

    enum class PaymentType {
        CARD_PAYMENT,
        SAMSUNG_PAY
    }
}