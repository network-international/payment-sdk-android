package payment.sdk.android

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.CardPaymentActivity
import payment.sdk.android.cardpayment.CardPaymentRequest
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoActivity
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoConfig
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

    fun executeThreeDS(paymentResponse: PaymentResponse, requestCode: Int) {
        val threeDSecureTwoConfig = ThreeDSecureTwoConfig.buildFromPaymentResponse(paymentResponse)
        if (threeDSecureTwoConfig.directoryServerID != null &&
            threeDSecureTwoConfig.threeDSMessageVersion != null &&
            threeDSecureTwoConfig.threeDSTwoAuthenticationURL != null &&
            threeDSecureTwoConfig.threeDSTwoChallengeResponseURL != null) {

            val threedsAuthUri = URI(threeDSecureTwoConfig.threeDSTwoAuthenticationURL)
            val authUrl = "https://${threedsAuthUri.host}/transactions/paymentAuthorization"
            val transactionServiceHttpAdapter = TransactionServiceHttpAdapter()
            transactionServiceHttpAdapter.getAuthTokenFromCode(
                url = authUrl,
                code = threeDSecureTwoConfig.authenticationCode ?: "",
                success = { cookies, _ ->
                    val paymentCookie = cookies.first { it.startsWith("payment-token") }
                    context.startActivityForResult(
                        ThreeDSecureTwoActivity.getIntent(
                            context = context,
                            directoryServerID = threeDSecureTwoConfig.directoryServerID,
                            threeDSMessageVersion = threeDSecureTwoConfig.threeDSMessageVersion,
                            paymentCookie = paymentCookie,
                            threeDSTwoAuthenticationURL= threeDSecureTwoConfig.threeDSTwoAuthenticationURL,
                            threeDSTwoChallengeResponseURL = threeDSecureTwoConfig.threeDSTwoChallengeResponseURL),
                        requestCode
                    )
                },
                error = {
                    println(it)
                }
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