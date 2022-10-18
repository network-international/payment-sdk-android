package payment.sdk.android.demo.basket.cardpayment

import payment.sdk.android.demo.basket.BasketFragment
import payment.sdk.android.demo.basket.BasketFragmentContract
import payment.sdk.android.demo.basket.data.AmountDetails
import payment.sdk.android.demo.basket.data.PaymentOrderAction
import payment.sdk.android.demo.basket.data.PaymentOrderApiInteractor
import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.PaymentClient
import payment.sdk.android.cardpayment.CardPaymentRequest
import android.annotation.SuppressLint
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import javax.inject.Inject

class CardPaymentPresenter @Inject constructor(
        private val view: BasketFragmentContract.View,
        private val orderApiInteractor: PaymentOrderApiInteractor,
        private val paymentClient: PaymentClient,
        private val scheduler: Scheduler,
        private val amountDetails: AmountDetails
) {
    private val subscriptions = CompositeDisposable()

    @SuppressLint("CheckResult")
    fun launchCardPayment() {
        view.showProgress(true)
        subscriptions.add(
                orderApiInteractor.createPaymentOrder(
                        action = PaymentOrderAction.SALE, // or PaymentOrderAction.AUTH
                        amount = totalAmount(),
                        currency = amountDetails.getCurrency().currencyCode)
                        .subscribeOn(scheduler.io())
                        .observeOn(scheduler.main())
                        .subscribe({ paymentOrderInfo ->
                            view.showProgress(false)
                            paymentClient.launchCardPayment(
                                    request = CardPaymentRequest.builder()
                                            .gatewayUrl(paymentOrderInfo.paymentAuthorizationUrl)
                                            .code(paymentOrderInfo.code)
                                            .build(),
                                    requestCode = BasketFragment.CARD_PAYMENT_REQUEST_CODE)
                        }, { error ->
                            view.showProgress(false)
                            view.showSnackBar(error.message)
                        }))
    }

    @SuppressLint("CheckResult")
    fun makeSavedCardPayment() {
        view.showProgress(true)
        subscriptions.add(
            orderApiInteractor.createSavedCardOrder(
                action = PaymentOrderAction.SALE,
                amount = totalAmount(),
                currency = amountDetails.getCurrency().currencyCode)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .subscribe({ paymentResponse ->
                    view.showProgress(false)
                    println("Payment Order Info $paymentResponse")
                    paymentClient.executeThreeDS(
                        paymentResponse = paymentResponse,
                        requestCode = BasketFragment.THREE_DS_TWO_REQUEST_CODE
                    )
                }, { error ->
                    view.showProgress(false)
                    view.showSnackBar(error.message)
                }))
    }

    private fun totalAmount() =
            amountDetails.getItems().map { it.amount }.fold(BigDecimal.ZERO) { acc, e -> acc + e }

    fun cleanup() {
        subscriptions.dispose()
    }
}
