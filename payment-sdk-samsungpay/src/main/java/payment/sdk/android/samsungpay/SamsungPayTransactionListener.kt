package payment.sdk.android.samsungpay

import android.content.Context
import android.os.Bundle
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet
import payment.sdk.android.core.TransactionServiceHttpAdapter

class SamsungPayTransactionListener(
    private val context: Context,
    private val samsungPayResponse: SamsungPayResponse,
    private val samsungPayAcceptLink: String,
    private val paymentToken: String,
    private val onCardInfoUpdate: (card: CardInfo?, customSheet: CustomSheet?) -> Unit
) : PaymentManager.CustomSheetTransactionInfoListener {
    override fun onCardInfoUpdated(cardInfo: CardInfo?, customSheet: CustomSheet?) {
        onCardInfoUpdate(cardInfo, customSheet)
    }

    override fun onFailure(code: Int, bundle: Bundle?) {
        val reason = bundle?.getInt(com.samsung.android.sdk.samsungpay.v2.SpaySdk.EXTRA_ERROR_REASON)
        val reasonMessage = bundle?.getString(com.samsung.android.sdk.samsungpay.v2.SpaySdk.EXTRA_ERROR_REASON_MESSAGE)
        android.util.Log.e(
            "SamsungPayTxnListener",
            "onFailure code=$code reason=$reason reasonMessage=$reasonMessage bundle=$bundle"
        )
        samsungPayResponse.onFailure(
            "Samsung Pay authorization failed with code $code" +
                (reasonMessage?.let { " ($it)" } ?: "")
        )
    }

    override fun onSuccess(customSheetPaymentInfo: CustomSheetPaymentInfo?, encryptedObject: String?, bundle: Bundle?) {
        val transactionServiceHttpAdapter = TransactionServiceHttpAdapter(context)
        if (encryptedObject != null) {
            transactionServiceHttpAdapter.acceptSamsungPay(
                    encryptedObject,
                    samsungPayAcceptLink,
                    paymentToken) { status: Boolean, error: Exception? ->
                if (status || (error != null)) {
                    samsungPayResponse.onSuccess()
                } else {
                    samsungPayResponse.onFailure("Samsung Pay decryption failed")
                }
            }
        } else {
            samsungPayResponse.onFailure("Encrypted object could not be obtained")
        }
    }
}