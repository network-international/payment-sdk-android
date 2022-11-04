package payment.sdk.android.samsungpay

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet
import payment.sdk.android.core.Order
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.samsungpay.mapper.SamsungPayCardMapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SamsungPayClient(
    private val context: Context,
    samsungPayServiceId: String,
    private val httpClient: HttpClient
) {

    private val partnerInfo = PartnerInfo(samsungPayServiceId, Bundle().apply {
        putString(SamsungPay.PARTNER_SERVICE_TYPE, SpaySdk.ServiceType.INAPP_PAYMENT.toString())
    })

    private val samsungPay: SamsungPay by lazy {
        SamsungPay(context, partnerInfo)
    }

    private val paymentManager: PaymentManager by lazy {
        PaymentManager(context, partnerInfo)
    }

    suspend fun isSamsungPayAvailable(): Boolean = suspendCoroutine { continuation ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            continuation.resume(false)
        } else {
            samsungPay.getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle) {
                    if(status != SamsungPay.SPAY_READY) {
                        Log.e("SamsungPayClient", "Samsung Pay is not available/ready. It's current status is code $status")
                    }
                    when (status) {
                        SamsungPay.SPAY_READY -> continuation.resume(true)
                        else -> continuation.resume(false)
                    }
                }

                override fun onFail(error: Int, bundle: Bundle?) {
                    Log.e("SamsungPayClient", "Samsung Pay check failed")
                    continuation.resume(false)
                }
            })
        }
    }

    fun isSamsungPayAvailable(statusListener: StatusListener) {
        samsungPay.getSamsungPayStatus(statusListener)
    }

    fun startSamsungPay(
        order: Order,
        merchantName: String,
        samsungPayResponse: SamsungPayResponse
    ) {
        if (order.outletId == null) {
            samsungPayResponse.onFailure("Outlet ID is null in order")
            return
        }

        if (order.reference == null) {
            samsungPayResponse.onFailure("Order reference not found in order")
            return
        }

        if (order.paymentMethods?.card?.size == 0) {
            samsungPayResponse.onFailure("No valid card schemes present")
            return
        }

        if (order.amount == null || order.amount?.currencyCode == null || order.amount?.value == null) {
            samsungPayResponse.onFailure("Order amount is not found")
            return
        }

        if (order.embedded?.payment?.get(0)?.links?.samsungPayLink?.href == null) {
            samsungPayResponse.onFailure("Samsung Pay is not enabled")
            return
        }

        val samsungPaylink = order.embedded!!.payment[0].links!!.samsungPayLink!!.href!!

        val customSheet = CustomSheet()
        customSheet.addControl(makeAmountControl(order.amount!!))

        val transactionServiceHttpAdapter = TransactionServiceHttpAdapter()
        transactionServiceHttpAdapter.authorizePayment(order) { authTokens: HashMap<String, String>?, error: Exception? ->
            if (authTokens?.get("payment-token") == null) {
                samsungPayResponse.onFailure("Could not authorize payment")
            } else {
                val paymentToken = authTokens["payment-token"]!!
                val samsungPayTransactionListener = SamsungPayTransactionListener(
                    samsungPayResponse,
                    samsungPaylink,
                    paymentToken
                ) { card: CardInfo?, customSheet: CustomSheet? ->
                    val amountBoxControl: AmountBoxControl =
                        customSheet?.getSheetControl(AMOUNT_CONTROL_ID) as AmountBoxControl
                    amountBoxControl.setAmountTotal(
                        order.amount!!.value!!.toDouble() / 100,
                        AmountConstants.FORMAT_TOTAL_PRICE_ONLY
                    ) // grand total

                    customSheet.updateControl(amountBoxControl)
                    try {
                        paymentManager.updateSheet(customSheet)
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }

                // Notice that some cards are not supported by Samsung Pay that are already supported by Payment Gateway
                val allowedCards = order.paymentMethods!!.card!!.mapNotNull { cardType ->
                    SamsungPayCardMapper.stringToSamsungPaySdk(cardType)
                }

                val paymentInfo = CustomSheetPaymentInfo.Builder()
                    .setMerchantId(order.outletId)
                    .setMerchantName(merchantName)
                    .setOrderNumber(order.reference)
                    .setAllowedCardBrands(allowedCards)
                    .setCardHolderNameEnabled(true)
                    .setRecurringEnabled(false)
                    .setCustomSheet(customSheet)
                    .build()

                paymentManager.startInAppPayWithCustomSheet(
                    paymentInfo,
                    samsungPayTransactionListener
                )
            }
        }
    }

    private fun makeAmountControl(amount: Order.Amount): AmountBoxControl? {
        val amountBoxControl = AmountBoxControl(AMOUNT_CONTROL_ID, amount.currencyCode)
        amountBoxControl.setAmountTotal(
            amount.value!!.toDouble(),
            AmountConstants.FORMAT_TOTAL_PRICE_ONLY
        )
        return amountBoxControl
    }

    companion object {
        const val AMOUNT_CONTROL_ID: String = "AMOUNT_CONTROL_ID"
    }
}
