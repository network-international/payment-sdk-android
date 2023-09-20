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
import kotlin.jvm.Throws


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
        context.startActivityForResult(
            CardPaymentActivity.getIntent(
                context = context,
                url = request.gatewayUrl,
                code = request.code
            ), requestCode
        )
    }

    /**
     *
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

    fun launchSamsungPay(
        order: Order,
        merchantName: String,
        samsungPayResponse: SamsungPayResponse
    ) {
        samsungPayClient.startSamsungPay(
            order = order,
            merchantName = merchantName,
            samsungPayResponse = samsungPayResponse
        )
    }

    fun isSamsungPayAvailable(statusListener: StatusListener) {
        samsungPayClient.isSamsungPayAvailable(statusListener)
    }

    private fun finishWithError(message: String) {
        val cardPaymentData = CardPaymentData(CardPaymentData.STATUS_GENERIC_ERROR, message)
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
        }
        context.setResult(Activity.RESULT_OK)
        context.finish()
    }

    fun executeThreeDS(paymentResponse: PaymentResponse, requestCode: Int) {
        val threeDSecureTwoConfig = ThreeDSecureTwoConfig.buildFromPaymentResponse(paymentResponse)
        if (threeDSecureTwoConfig.directoryServerID != null &&
            threeDSecureTwoConfig.threeDSMessageVersion != null &&
            threeDSecureTwoConfig.threeDSTwoAuthenticationURL != null &&
            threeDSecureTwoConfig.threeDSTwoChallengeResponseURL != null
        ) {

            val threedsAuthUri = URI(threeDSecureTwoConfig.threeDSTwoAuthenticationURL)
            val authUrl = "https://${threedsAuthUri.host}/transactions/paymentAuthorization"
            val transactionServiceHttpAdapter = TransactionServiceHttpAdapter()
            val outletRef = paymentResponse.outletId
            if (outletRef == null) {
                finishWithError("Outlet ref not found")
                return
            }
            val orderRef = paymentResponse.orderReference
            if (orderRef == null) {
                finishWithError("Order reference not found")
                return
            }
            val paymentReference = paymentResponse.reference
            if (paymentReference == null) {
                finishWithError("Payment reference not found")
                return
            }
            val threeDSTwoChallengeResponseURL =
                paymentResponse.links?.threeDSChallengeResponseUrl?.href
            if (threeDSTwoChallengeResponseURL == null) {
                finishWithError("3ds challenge response url not found")
                return
            }
            val threeDSMessageVersion = paymentResponse.threeDSTwo?.messageVersion
            if (threeDSMessageVersion == null) {
                finishWithError("threeDSMessageVersion not found")
                return
            }
            val directoryServerID = paymentResponse.threeDSTwo?.directoryServerID
            if (directoryServerID == null) {
                finishWithError("directoryServerID not found")
                return
            }
            val threeDSTwoAuthenticationURL = paymentResponse.links?.threeDSAuthenticationsUrl?.href
            if (threeDSTwoAuthenticationURL == null) {
                finishWithError("threeDSTwoAuthenticationURL not found")
                return
            }
            val threeDSServerTransID = paymentResponse.threeDSTwo?.threeDSServerTransID
            val threeDSMethodURL = paymentResponse.threeDSTwo?.threeDSMethodURL
            val threeDSecureRequest =
                ThreeDSecureTwoRequest.buildFromPaymentResponse(paymentResponse)
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
            val acsUrl = paymentResponse.threeDSOne?.acsUrl
            if (acsUrl == null) {
                finishWithError("ThreeDS one acs url not found")
                return
            }
            val acsPaReq = paymentResponse.threeDSOne?.acsPaReq
            if (acsPaReq == null) {
                finishWithError("ThreeDS one acsPaReq not found")
                return
            }
            val acsMd = paymentResponse.threeDSOne?.acsMd
            if (acsMd == null) {
                finishWithError("ThreeDS one acsMd not found")
                return
            }
            val threeDSOneUrl = paymentResponse.links?.threeDSOneUrl?.href
            if (threeDSOneUrl == null) {
                finishWithError("ThreeDS one url not found")
                return
            }
            context.startActivityForResult(
                ThreeDSecureWebViewActivity.getIntent(
                    context = context,
                    acsUrl = acsUrl,
                    acsPaReq = acsPaReq,
                    acsMd = acsMd,
                    gatewayUrl = threeDSOneUrl
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