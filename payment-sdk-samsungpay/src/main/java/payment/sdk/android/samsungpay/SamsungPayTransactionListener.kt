package payment.sdk.android.samsungpay

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
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
        if (code == SpaySdk.ERROR_USER_CANCELED) {
            samsungPayResponse.onCancelled()
        } else {
            // Samsung often reports the real cause via bundle extras (e.g. invalid service id,
            // merchant not registered, signing-cert mismatch, no eligible card) rather than `code`.
            val reasonMessage = bundle?.getString(SpaySdk.EXTRA_ERROR_REASON_MESSAGE)
            val reasonCode = bundle?.get(SpaySdk.EXTRA_ERROR_REASON)
            val allExtras = bundle?.keySet()?.joinToString(", ") { key ->
                "$key=${bundle.get(key)}"
            }
            Log.e(
                "SamsungPayTxnListener",
                "onFailure code=$code reasonCode=$reasonCode reasonMessage=$reasonMessage extras=[$allExtras]"
            )
            val detail = reasonMessage ?: reasonCode?.let { "reason $it" } ?: "code $code"
            samsungPayResponse.onFailure("Samsung Pay authorization failed: $detail")
        }
    }

    override fun onSuccess(customSheetPaymentInfo: CustomSheetPaymentInfo?, encryptedObject: String?, bundle: Bundle?) {
        val transactionServiceHttpAdapter = TransactionServiceHttpAdapter(context)
        if (encryptedObject != null) {
            transactionServiceHttpAdapter.acceptSamsungPay(
                    encryptedObject,
                    samsungPayAcceptLink,
                    paymentToken) { status: Boolean, error: Exception? ->
                // Only a genuinely successful accept call is a success. The previous condition
                // `status || (error != null)` reported success whenever the call ERRORED, masking
                // real failures (e.g. the gateway returning 400 "Padding error in decryption" on a
                // Samsung Pay certificate mismatch) as completed payments.
                if (status) {
                    samsungPayResponse.onSuccess()
                } else {
                    Log.e("SamsungPayTxnListener", "acceptSamsungPay failed", error)
                    samsungPayResponse.onFailure(
                        "Samsung Pay accept failed: ${error?.message ?: "unknown error"}"
                    )
                }
            }
        } else {
            samsungPayResponse.onFailure("Encrypted object could not be obtained")
        }
    }
}