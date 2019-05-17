package payment.sdk.android.demo.basket

import android.os.Bundle
import android.support.annotation.WorkerThread
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONObject
import payment.sdk.android.PaymentClient
import payment.sdk.android.core.CardType
import payment.sdk.android.demo.basket.data.AmountDetails
import payment.sdk.android.demo.basket.data.PaymentOrderAction
import payment.sdk.android.demo.basket.data.PaymentOrderApiInteractor
import payment.sdk.android.demo.dependency.preference.Preferences
import payment.sdk.android.demo.dependency.repository.ProductRepository
import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.samsungpay.control.AddressControl
import payment.sdk.android.samsungpay.SamsungPayRequest
import payment.sdk.android.samsungpay.control.AmountBoxControl
import payment.sdk.android.samsungpay.control.SpinnerControl
import payment.sdk.android.samsungpay.transaction.CardInfo
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import javax.inject.Inject


class SamsungPayPresenter @Inject constructor(
        private val view: BasketFragmentContract.View,
        private val orderApiInteractor: PaymentOrderApiInteractor,
        private val repository: ProductRepository,
        private val preferences: Preferences,
        private val paymentClient: PaymentClient,
        private val scheduler: Scheduler,
        private val amountDetails: AmountDetails
) {

    private val subscriptions = CompositeDisposable()

    fun launchSamsungPay() {
        view.showProgress(true)
        subscriptions.add(
                orderApiInteractor.createPaymentOrder(
                        action = PaymentOrderAction.SALE, // or PaymentOrderAction.AUTH
                        amount = totalAmount(),
                        currency = amountDetails.getCurrency().currencyCode)
                        .flatMap { response ->
                            Single.fromCallable { buildSamsungPayRequest(response.orderReference, response.supportedCards) }
                        }
                        .subscribeOn(scheduler.io())
                        .observeOn(scheduler.main())
                        .subscribe(
                                { request ->
                                    view.showProgress(false)
                                    paymentClient.launchSamsungPay(request)
                                },
                                { error ->
                                    view.showProgress(false)
                                    view.showSnackBar(error.message)
                                }))
    }

    private fun totalAmount() =
            amountDetails.getItems().map { it.amount }.fold(BigDecimal.ZERO) { acc, e -> acc + e }

    fun cleanup() {
        subscriptions.dispose()
    }

    /**
     * At first glance, building a SamsungPayRequest can be seen complicated in this demo app but
     * indeed it is not. We are constructing a request using values under settings page, so there is
     * lots of preferences to construct a request.
     *
     * Depending on merchant requirements, a simple and complete request can be built as follows:
     *
     *     SamsungPayRequest.builder()
     *          .merchantId("1")
     *          .merchantName("Furniture Store")
     *          .orderNumber("1")
     *          .addressInPaymentSheet(SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING)
     *          .addAmountBoxControl(makeAmountControl(amountDetails))
     *          .addSpinnerControl(makeDeliveryMethodSpinnerControl(), spinnerControlListener)
     *          .addSpinnerControl(makeInstallmentSpinnerControl(), spinnerControlListener)
     *          .addAddressControl(makeBillingAddressControl(), addressControlListener)
     *          .addAddressControl(makeShippingAddressControl(), addressControlListener)
     *          .build()
     *
     */
    @WorkerThread
    private fun buildSamsungPayRequest(orderNumber: String, supportedCards: Set<CardType>): SamsungPayRequest {
        val requestBuilder = SamsungPayRequest.builder()
                .merchantId(merchantId = "1") // TODO: Replace with actual merchant Id - NI merchant Id
                .merchantName(merchantName = "Furniture Store") // TODO: Replace
                .orderNumber(orderNumber = orderNumber)
                .supportedCards(supportedCards = supportedCards)

        val addressInPaymentSheet = preferences.getString(Preferences.SPAY_ADDRESS_IN_PAYMENT_SHEET)?.let {
            SamsungPayRequest.AddressInPaymentSheet.valueOf(it)
        } ?: SamsungPayRequest.AddressInPaymentSheet.DO_NOT_SHOW

        requestBuilder.addressInPaymentSheet(addressInPaymentSheet = addressInPaymentSheet)
        requestBuilder.addAmountBoxControl(amountBoxControl = makeAmountControl(amountDetails))

        /**
         * Billing address comes from Samsung Pay
         * If we see one of the options below, create a dummy billing address control and let
         * Samsung Pay fill in with data
         */
        when (addressInPaymentSheet) {
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_SEND_SHIPPING,
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING,
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_SPAY -> {
                makeBillingAddressControl().apply {
                    if (address.countryCode.isEmpty()) throw IllegalArgumentException(
                            "Country can't be empty. Please update the country from settings")
                }
            }
            else -> null
        }?.let { billingAddress ->
            requestBuilder.addAddressControl(billingAddress, addressControlListener)
        }

        /**
         * Create shipping address depending on @code {SamsungPayRequest.AddressInPaymentSheet}
         */
        when (addressInPaymentSheet) {
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_SEND_SHIPPING,
            SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING,
            SamsungPayRequest.AddressInPaymentSheet.NEED_SHIPPING_SPAY,
            SamsungPayRequest.AddressInPaymentSheet.SEND_SHIPPING -> {
                makeShippingAddressControl().apply {
                    if (address.countryCode.isEmpty()) throw IllegalArgumentException(
                            "Country can't be empty. Please update the country from settings")
                }
            }
            else -> null
        }?.let { shippingAddress ->
            requestBuilder.addAddressControl(shippingAddress, addressControlListener)
        }

        /**
         * Shipping method can be used when the shipping address comes from Samsung Pay app;
         * i.e., when the CustomSheetPaymentInfo.AddressInPaymentSheet option is set to
         * NEED_BILLING_AND_SHIPPING or NEED_SHIPPING_SPAY
         */
        preferences.getString(Preferences.SPAY_SHOW_DELIVERY_METHOD)?.let {
            val showShippingMethods = it.toLowerCase() == "yes"
            if (showShippingMethods &&
                    (addressInPaymentSheet == SamsungPayRequest.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING ||
                            addressInPaymentSheet == SamsungPayRequest.AddressInPaymentSheet.NEED_SHIPPING_SPAY)) {
                requestBuilder.addSpinnerControl(makeDeliveryMethodSpinnerControl(), spinnerControlListener)
            }
        }

        requestBuilder.transactionListener(transactionListener)

        return requestBuilder.build()

    }

    private val transactionListener = object : SamsungPayRequest.TransactionListener {
        override fun onCardInfoUpdated(selectedCardInfo: CardInfo?, customSheetDelegate: SamsungPayRequest.CustomSheetDelegate) {
            selectedCardInfo?.let {
                updateAmount(customSheetDelegate)
            } ?: updateAmount(customSheetDelegate, "Card type is not supported by payment gateway")
        }

        override fun onSuccess(paymentCredential: String, extraPaymentData: Bundle) {
//            val data = JSONObject(paymentCredential).getJSONObject("3DS").getString("data")
            repository.removeAll()
                    .subscribeOn(scheduler.io())
                    .observeOn(scheduler.main())
                    .subscribe()
            view.showOrderSuccessful()
        }

        override fun onFailure(errorCode: Int, errorData: Bundle) {
            view.showError("Payment is not successful. Code: $errorCode")
        }
    }

    private val addressControlListener = object : SamsungPayRequest.AddressControlUpdatedListener {
        override fun onResult(controlId: String, address: AddressControl.Address, customSheetDelegate: SamsungPayRequest.CustomSheetDelegate) {
            // Samsung Pay wait timeout is 5 seconds, and this should return in this period
            if (controlId == "ShippingAddressControl") {
                subscriptions.add(orderApiInteractor.updateShippingAddress(address.postalCode)
                        .subscribeOn(scheduler.io())
                        .observeOn(scheduler.main())
                        .doOnSuccess {
                            amountDetails.addItem("shippingFee", "Shipping Fee", it)
                        }
                        .subscribe({
                            updateAmount(customSheetDelegate)
                        }, {
                            updateAmount(
                                    customSheetDelegate,
                                    it?.message
                                            ?: "Error while updating amount for shipping address")
                        }))
            }
        }
    }

    private val spinnerControlListener = object : SamsungPayRequest.SpinnerControlUpdatedListener {
        override fun onResult(controlId: String, selectedId: String, customSheetDelegate: SamsungPayRequest.CustomSheetDelegate) {
            // Samsung Pay wait timeout is 5 seconds, and this should return in this period
            if (controlId == "DeliveryMethodSpinnerControl") {
                subscriptions.add(orderApiInteractor.updateDeliveryMethod(selectedId)
                        .subscribeOn(scheduler.io())
                        .observeOn(scheduler.main())
                        .doOnSuccess {
                            amountDetails.addItem("deliveryMethodFee", "Delivery Method Fee", it.fee, it.optionalFeeText
                                    ?: "")
                        }
                        .subscribe({
                            updateAmount(customSheetDelegate)
                        }, {
                            updateAmount(
                                    customSheetDelegate,
                                    it?.message
                                            ?: "Error while updating amount for delivery method")
                        }))
            }
        }
    }

    private fun updateAmount(customSheetDelegate: SamsungPayRequest.CustomSheetDelegate, errorMessage: String? = null) {
        val breakdown = amountDetails.getItems().map { detail ->
            AmountBoxControl.AmountItem(detail.id, detail.title, detail.amount)
        }

        val totalAmount = breakdown.sumByDouble { it.price.toDouble() }

        if (errorMessage == null) {
            customSheetDelegate.update(totalAmount.toBigDecimal(), breakdown)
        } else {
            customSheetDelegate.updateWithError(totalAmount.toBigDecimal(), breakdown, errorMessage)
        }

    }

    private fun makeAmountControl(amountDetails: AmountDetails): AmountBoxControl =
            AmountBoxControl(amountDetails.getTotalAmount(), amountDetails.getCurrency().currencyCode).apply {
                amountDetails.getItems().forEach { detail ->
                    addItem(detail.id, detail.title, detail.amount)
                }
            }

    @WorkerThread
    private fun makeShippingAddressControl() =
            AddressControl(
                    controlId = "ShippingAddressControl",
                    addressType = AddressControl.AddressType.SHIPPING_ADDRESS,
                    title = "Shipping Address",
                    address = AddressControl.Address(
                            addressee = preferences.getString(Preferences.SPAY_USER_NAME).orEmpty(),
                            addressLine1 = preferences.getString(Preferences.SPAY_SHIPPING_ADDRESS_LINE_1).orEmpty(),
                            addressLine2 = preferences.getString(Preferences.SPAY_SHIPPING_ADDRESS_LINE_2).orEmpty(),
                            city = preferences.getString(Preferences.SPAY_SHIPPING_CITY).orEmpty(),
                            state = preferences.getString(Preferences.SPAY_SHIPPING_STATE).orEmpty(),
                            countryCode = preferences.getString(Preferences.SPAY_SHIPPING_COUNTRY).orEmpty(),
                            postalCode = preferences.getString(Preferences.SPAY_SHIPPING_POST_CODE).orEmpty(),
                            phoneNumber = preferences.getString(Preferences.SPAY_SHIPPING_PHONE).orEmpty(),
                            email = preferences.getString(Preferences.SPAY_SHIPPING_EMAIL).orEmpty()
                    )
            )

    @WorkerThread
    private fun makeBillingAddressControl() =
            AddressControl(
                    controlId = "BillingAddressControl",
                    addressType = AddressControl.AddressType.BILLING_ADDRESS,
                    title = "Billing Address",
                    address = AddressControl.Address(
                            addressee = "",
                            addressLine1 = "",
                            addressLine2 = "",
                            city = "",
                            state = "",
                            countryCode = preferences.getString(Preferences.SPAY_SHIPPING_COUNTRY)
                                    ?: "GBR", // Can't be empty
                            postalCode = "",
                            phoneNumber = "",
                            email = ""
                    )
            )

    private fun makeDeliveryMethodSpinnerControl() =
            SpinnerControl(
                    "DeliveryMethodSpinnerControl",
                    "Delivery Method", SpinnerControl.SpinnerType.SHIPPING_METHOD_SPINNER,
                    "DeliveryMethodSpinnerControl-Item-1").apply {
                addItem("DeliveryMethodSpinnerControl-Item-1", "Free Delivery")
                addItem("DeliveryMethodSpinnerControl-Item-2", "1-Day Delivery - £4")
                addItem("DeliveryMethodSpinnerControl-Item-3", "2-Days Delivery - £2")
            }
}
