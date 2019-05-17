package payment.sdk.android

import payment.sdk.android.cardpayment.CardPaymentActivity
import payment.sdk.android.cardpayment.CardPaymentRequest
import android.app.Activity
import kotlinx.coroutines.*
import payment.sdk.android.samsungpay.SamsungPayClient
import payment.sdk.android.samsungpay.SamsungPayRequest

class PaymentClient(
        private val context: Activity
) {

    private val samsungPayClient: SamsungPayClient by lazy {
        SamsungPayClient(context, "1a7ef7ddf6924777a8676d") //TODO
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

    fun launchSamsungPay(request: SamsungPayRequest) {
        samsungPayClient.startSamsungPay(
                merchantId = request.merchantId,
                merchantName = request.merchantName,
                orderNumber = request.orderNumber,
                supportedCards = request.supportedCards,
                addressInPaymentSheet = request.addressInPaymentSheet,
                controls = request.controls,
                transactionListener = request.transactionListener
        )
    }

    interface SupportedPaymentTypesListener {
        fun onReady(supportedPaymentTypes: List<PaymentType>)
    }

    enum class PaymentType {
        CARD_PAYMENT,
        SAMSUNG_PAY
    }
}