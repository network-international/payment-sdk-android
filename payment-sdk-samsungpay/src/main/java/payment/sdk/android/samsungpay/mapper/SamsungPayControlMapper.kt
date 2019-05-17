package payment.sdk.android.samsungpay.mapper

import payment.sdk.android.samsungpay.SamsungPayRequest
import payment.sdk.android.samsungpay.control.*
import payment.sdk.android.samsungpay.transaction.CardInfo
import android.os.Bundle
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo as NativeCardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo as NativeCustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetControl as NativeSheetControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl as NativeAmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AddressControl as NativeAddressControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SpinnerControl as NativeSpinnerControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.PlainTextControl as NativePlainTextControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet as NativeCustomSheet
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetItemType
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetUpdatedListener
import payment.sdk.android.core.dependency.StringResources
import payment.sdk.android.samsungpay.R
import java.lang.IllegalArgumentException
import java.math.BigDecimal


class SamsungPayControlMapper constructor(
        private val paymentManager: PaymentManager,
        private val stringResources: StringResources
) {

    fun mapControl(control: SamsungPayControl, listener: SamsungPayRequest.SheetUpdatedListener): NativeSheetControl {
        return when (control) {
            is AmountBoxControl -> mapAmountBoxControl(control)
            is PlainTextControl -> mapPlainTextControl(control)
            is AddressControl -> mapAddressControl(control, listener as SamsungPayRequest.AddressControlUpdatedListener)
            is SpinnerControl -> mapSpinnerControl(control, listener as SamsungPayRequest.SpinnerControlUpdatedListener)
            else -> throw IllegalArgumentException(stringResources.getString(R.string.control_type_not_supported) + control.toString())
        }
    }

    fun mapAddressInPaymentSheet(addressInPaymentSheet: SamsungPayRequest.AddressInPaymentSheet): NativeCustomSheetPaymentInfo.AddressInPaymentSheet {
        return when (addressInPaymentSheet) {
            SamsungPayRequest.AddressInPaymentSheet.DO_NOT_SHOW -> NativeCustomSheetPaymentInfo.AddressInPaymentSheet.DO_NOT_SHOW
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_SPAY -> NativeCustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_SPAY
            SamsungPayRequest.AddressInPaymentSheet.NEED_SHIPPING_SPAY -> NativeCustomSheetPaymentInfo.AddressInPaymentSheet.NEED_SHIPPING_SPAY
            SamsungPayRequest.AddressInPaymentSheet.SEND_SHIPPING -> NativeCustomSheetPaymentInfo.AddressInPaymentSheet.SEND_SHIPPING
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_SEND_SHIPPING -> NativeCustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_SEND_SHIPPING
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING -> NativeCustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING
        }
    }

    fun mapTransactionListener(transactionListener: SamsungPayRequest.TransactionListener): PaymentManager.CustomSheetTransactionInfoListener =
            object : PaymentManager.CustomSheetTransactionInfoListener {

                override fun onCardInfoUpdated(selectedCardInfo: NativeCardInfo, customSheet: NativeCustomSheet) {
                    transactionListener.onCardInfoUpdated(mapCardInfo(selectedCardInfo), createCustomSheetDelegate(customSheet))
                }

                override fun onSuccess(response: NativeCustomSheetPaymentInfo, paymentCredential: String, extraPaymentData: Bundle) {
                    transactionListener.onSuccess(paymentCredential, extraPaymentData)
                }

                override fun onFailure(errorCode: Int, errorData: Bundle) {
                    transactionListener.onFailure(errorCode, errorData)
                }
            }

    private fun mapCardInfo(nativeCardInfo: NativeCardInfo): CardInfo? =
            SamsungPayCardMapper.mapNativeToSdk(nativeCardInfo.brand)?.let { cardType ->
                CardInfo(cardType, nativeCardInfo.cardMetaData)
            }

    private fun mapAmountBoxControl(amountBoxControl: AmountBoxControl): NativeAmountBoxControl =
            NativeAmountBoxControl(AmountBoxControl.CONTROL_ID, amountBoxControl.currency).apply {
                amountBoxControl.getItems().forEach { item ->
                    addItem(item.id, item.title, item.price.toDouble(), item.optionalPriceText)
                }
                setAmountTotal(amountBoxControl.totalAmount.toDouble(), amountBoxControl.currency)
            }

    private fun mapAddressControl(addressControl: AddressControl, listener: SamsungPayRequest.AddressControlUpdatedListener): NativeAddressControl {
        val nativeType = when (addressControl.addressType) {
            AddressControl.AddressType.BILLING_ADDRESS -> SheetItemType.BILLING_ADDRESS
            AddressControl.AddressType.SHIPPING_ADDRESS -> SheetItemType.SHIPPING_ADDRESS
        }
        return NativeAddressControl(addressControl.controlId, nativeType).apply {
            addressTitle = addressControl.title
            address = mapAddress(addressControl.address)
            sheetUpdatedListener = SheetUpdatedListener { controlId, nativeCustomSheet ->
                val nativeAddressControl = nativeCustomSheet.getSheetControl(controlId) as NativeAddressControl
                listener.onResult(controlId, mapNativeAddress(nativeAddressControl.address), createCustomSheetDelegate(nativeCustomSheet))
            }
        }
    }

    private fun mapSpinnerControl(spinnerControl: SpinnerControl, listener: SamsungPayRequest.SheetUpdatedListener): NativeSpinnerControl {
        val nativeType = when (spinnerControl.spinnerType) {
            SpinnerControl.SpinnerType.SHIPPING_METHOD_SPINNER -> SheetItemType.SHIPPING_METHOD_SPINNER
            SpinnerControl.SpinnerType.INSTALLMENT_SPINNER -> SheetItemType.INSTALLMENT_SPINNER
        }
        return NativeSpinnerControl(spinnerControl.controlId, spinnerControl.title, nativeType).apply {
            spinnerControl.getItems().forEach { item ->
                addItem(item.id, item.text)
            }
            spinnerControl.getSelectedId()?.let {
                selectedItemId = it
            }
            sheetUpdatedListener = SheetUpdatedListener { controlId, nativeCustomSheet ->
                val nativeSpinnerControl = nativeCustomSheet.getSheetControl(controlId) as NativeSpinnerControl
                val spinnerControlListener = listener as SamsungPayRequest.SpinnerControlUpdatedListener
                spinnerControlListener.onResult(controlId, nativeSpinnerControl.selectedItemId, createCustomSheetDelegate(nativeCustomSheet))
            }
        }
    }

    private fun createCustomSheetDelegate(nativeCustomSheet: NativeCustomSheet) =
            object : SamsungPayRequest.CustomSheetDelegate {
                override fun update(total: BigDecimal, breakdown: List<AmountBoxControl.AmountItem>) {
                    updateAmount(total, breakdown)
                    paymentManager.updateSheet(nativeCustomSheet)
                }

                override fun updateWithError(total: BigDecimal, breakdown: List<AmountBoxControl.AmountItem>, errorMessage: String) {
                    updateAmount(total, breakdown)
                    paymentManager.updateSheet(nativeCustomSheet, PaymentManager.CUSTOM_MESSAGE, errorMessage)
                }

                private fun updateAmount(total: BigDecimal, breakdown: List<AmountBoxControl.AmountItem>) {
                    val nativeAmountBoxControl = nativeCustomSheet.getSheetControl(AmountBoxControl.CONTROL_ID) as NativeAmountBoxControl

                    breakdown.forEach { item ->
                        if (nativeAmountBoxControl.existItem(item.id)) {
                            nativeAmountBoxControl.updateValue(item.id, item.price.toDouble(), item.optionalPriceText)
                        } else {
                            nativeAmountBoxControl.addItem(item.id, item.title, item.price.toDouble(), item.optionalPriceText)
                        }
                    }
                    nativeAmountBoxControl.setAmountTotal(total.toDouble(), "Total") //TODO
                    nativeCustomSheet.updateControl(nativeAmountBoxControl)
                }
            }

    private fun mapPlainTextControl(plainTextControl: PlainTextControl): NativePlainTextControl =
            NativePlainTextControl(plainTextControl.controlId).apply {
                setText(plainTextControl.title, plainTextControl.text)
            }

    private fun mapAddress(address: AddressControl.Address): NativeCustomSheetPaymentInfo.Address? =
            NativeCustomSheetPaymentInfo.Address.Builder()
                    .setAddressee(address.addressee)
                    .setAddressLine1(address.addressLine1)
                    .setAddressLine2(address.addressLine2)
                    .setCity(address.city)
                    .setState(address.state)
                    .setCountryCode(address.countryCode)
                    .setPostalCode(address.postalCode)
                    .setPhoneNumber(address.phoneNumber)
                    .setEmail(address.email)
                    .build()

    private fun mapNativeAddress(nativeAddress: NativeCustomSheetPaymentInfo.Address): AddressControl.Address =
            AddressControl.Address(
                    nativeAddress.addressee.orEmpty(),
                    nativeAddress.addressLine1.orEmpty(),
                    nativeAddress.addressLine2.orEmpty(),
                    nativeAddress.city.orEmpty(),
                    nativeAddress.state.orEmpty(),
                    nativeAddress.countryCode, // Can't be empty at all
                    nativeAddress.postalCode.orEmpty(),
                    nativeAddress.phoneNumber.orEmpty(),
                    nativeAddress.email.orEmpty()
            )
}
