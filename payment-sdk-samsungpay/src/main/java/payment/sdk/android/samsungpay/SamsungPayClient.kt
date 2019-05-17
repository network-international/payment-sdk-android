package payment.sdk.android.samsungpay

import payment.sdk.android.samsungpay.control.SamsungPayControl
import payment.sdk.android.samsungpay.mapper.SamsungPayCardMapper
import payment.sdk.android.samsungpay.mapper.SamsungPayControlMapper
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet
import payment.sdk.android.core.CardType
import payment.sdk.android.core.dependency.StringResourcesImpl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl as NativeAmountBoxControl


class SamsungPayClient(
        private val context: Context,
        samsungPayServiceId: String
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

    private val mapper = SamsungPayControlMapper(paymentManager, StringResourcesImpl(context))

    suspend fun isSamsungPayAvailable(): Boolean = suspendCoroutine { continuation ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            continuation.resume(false)
        } else {
            samsungPay.getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle) {
                    when (status) {
                        SamsungPay.SPAY_READY -> continuation.resume(true)
                        else -> continuation.resume(false)
                    }
                }

                override fun onFail(error: Int, bundle: Bundle?) {
                    continuation.resume(false)
                }
            })
        }
    }

    fun startSamsungPay(merchantId: String,
                        merchantName: String,
                        orderNumber: String,
                        supportedCards: Set<CardType>,
                        addressInPaymentSheet: SamsungPayRequest.AddressInPaymentSheet,
                        controls: List<Pair<SamsungPayControl, SamsungPayRequest.SheetUpdatedListener>>,
                        transactionListener: SamsungPayRequest.TransactionListener
    ) {

        val sheet = CustomSheet().apply {
            controls.forEach { pair ->
                addControl(mapper.mapControl(pair.first, pair.second))
            }
        }

        // Notice that some cards are not supported by Samsung Pay that are already supported by Payment Gateway
        val allowedCards = supportedCards.mapNotNull { cardType -> SamsungPayCardMapper.mapSdkToNative(cardType) }

        val paymentInfo = CustomSheetPaymentInfo.Builder()
                .setMerchantId(merchantId)
                .setMerchantName(merchantName)
                .setOrderNumber(orderNumber)
                .setPaymentProtocol(CustomSheetPaymentInfo.PaymentProtocol.PROTOCOL_3DS)
                .setAddressInPaymentSheet(mapper.mapAddressInPaymentSheet(addressInPaymentSheet))
                .setAllowedCardBrands(allowedCards)
                .setCardHolderNameEnabled(true)
                .setRecurringEnabled(false)
                .setCustomSheet(sheet)
                .build()

        paymentManager.startInAppPayWithCustomSheet(
                paymentInfo, mapper.mapTransactionListener(transactionListener))
    }

}
