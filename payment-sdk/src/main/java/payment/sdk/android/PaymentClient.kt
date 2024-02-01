package payment.sdk.android

import android.app.Activity
import android.content.Intent
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.CardPaymentActivity
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.CardPaymentRequest
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
        ), requestCode)
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

            val orderRef = null
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