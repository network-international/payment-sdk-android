package payment.sdk.android.samsungpay

import payment.sdk.android.samsungpay.control.*
import payment.sdk.android.samsungpay.transaction.CardInfo
import android.os.Bundle
import payment.sdk.android.core.CardType
import java.lang.IllegalArgumentException
import java.math.BigDecimal

class SamsungPayRequest private constructor(
        val merchantId: String,
        val merchantName: String,
        val orderNumber: String,
        val supportedCards: Set<CardType>,
        val addressInPaymentSheet: AddressInPaymentSheet,
        val controls: List<Pair<SamsungPayControl, SheetUpdatedListener>>,
        val transactionListener: TransactionListener
) {

    class Builder {
        private var merchantId: String? = null
        private var merchantName: String? = null
        private var orderNumber: String? = null
        private var supportedCards: Set<CardType>? = null
        private var addressInPaymentSheet: AddressInPaymentSheet? = null
        private val controls: MutableList<Pair<SamsungPayControl, SheetUpdatedListener>> = mutableListOf()
        private var transactionListener: TransactionListener? = null

        fun merchantId(merchantId: String) = this.apply {
            this.merchantId = merchantId
        }

        fun merchantName(merchantName: String) = this.apply {
            this.merchantName = merchantName
        }

        fun orderNumber(orderNumber: String) = this.apply {
            this.orderNumber = orderNumber
        }

        fun supportedCards(supportedCards: Set<CardType>) = this.apply {
            this.supportedCards = supportedCards
        }

        fun addressInPaymentSheet(addressInPaymentSheet: AddressInPaymentSheet) = this.apply {
            this.addressInPaymentSheet = addressInPaymentSheet
        }

        fun addSpinnerControl(spinnerControl: SpinnerControl, listener: SheetUpdatedListener) = this.apply {
            this.controls.add(Pair(spinnerControl, listener))
        }

        fun addAmountBoxControl(amountBoxControl: AmountBoxControl) = this.apply {
            this.controls.add(Pair(amountBoxControl, NullSheetUpdatedListener))
        }

        fun addAddressControl(addressControl: AddressControl, listener: AddressControlUpdatedListener) = this.apply {
            this.controls.add(Pair(addressControl, listener))
        }

        fun addPlainTextControl(plainTextControl: PlainTextControl) = this.apply {
            this.controls.add(Pair(plainTextControl, NullSheetUpdatedListener))
        }

        fun transactionListener(transactionListener: TransactionListener) = this.apply {
            this.transactionListener = transactionListener
        }

        fun build(): SamsungPayRequest {
            requireNotNull(merchantId) {
                "Merchant Id can't be null"
            }
            requireNotNull(merchantName) {
                "Merchant name can't be null"
            }
            requireNotNull(orderNumber) {
                "Order number can't be null"
            }
            if (supportedCards.isNullOrEmpty()) {
                throw IllegalArgumentException("Supported cards can't be null or empty")
            }
            requireNotNull(transactionListener) {
                "Transaction listener can't be null"
            }
            return SamsungPayRequest(merchantId!!, merchantName!!, orderNumber!!, supportedCards!!, addressInPaymentSheet!!, controls, transactionListener!!)
        }
    }

    companion object {
        fun builder() = Builder()
    }

    interface SheetUpdatedListener

    object NullSheetUpdatedListener : SheetUpdatedListener

    interface AddressControlUpdatedListener : SheetUpdatedListener {
        fun onResult(controlId: String, address: AddressControl.Address, customSheetDelegate: SamsungPayRequest.CustomSheetDelegate)
    }

    interface SpinnerControlUpdatedListener : SheetUpdatedListener {
        fun onResult(controlId: String, selectedId: String, customSheetDelegate: SamsungPayRequest.CustomSheetDelegate)
    }

    interface TransactionListener {
        fun onCardInfoUpdated(selectedCardInfo: CardInfo?, customSheetDelegate: CustomSheetDelegate)

        fun onSuccess(paymentCredential: String, extraPaymentData: Bundle)

        fun onFailure(errorCode: Int, errorData: Bundle)
    }

    interface CustomSheetDelegate {
        fun update(total: BigDecimal, breakdown: List<AmountBoxControl.AmountItem>)

        fun updateWithError(total: BigDecimal, breakdown: List<AmountBoxControl.AmountItem>, errorMessage: String)
    }

    enum class AddressInPaymentSheet {
        /**
         * No address is displayed on the payment sheet;
         * typically reserved for digital items, such as music, software, and so forth
         */
        DO_NOT_SHOW,

        /**
         * Displays the billing address on file in Samsung Pay
         */
        NEED_BILLING_SPAY,

        /**
         * Displays only shipping address on file in Samsung Pay
         */
        NEED_SHIPPING_SPAY,

        /**
         * Displays the shipping address sent by the merchant app only
         */
        SEND_SHIPPING,

        /**
         * Displays billing address from Samsung Pay and shipping address sent by this merchant
         */
        NEED_BILLING_SEND_SHIPPING,

        /**
         * Displays both shipping address and billing address on the Payment Sheet from Samsung Pay
         */
        NEED_BILLING_AND_SHIPPING
    }
}
