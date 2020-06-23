package payment.sdk.android.demo.basket

import android.support.annotation.WorkerThread
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import payment.sdk.android.PaymentClient
import payment.sdk.android.core.Order
import payment.sdk.android.demo.basket.data.AmountDetails
import payment.sdk.android.demo.basket.data.PaymentOrderAction
import payment.sdk.android.demo.basket.data.PaymentOrderApiInteractor
import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.samsungpay.SamsungPayResponse
import java.math.BigDecimal
import javax.inject.Inject


class SamsungPayPresenter @Inject constructor(
        private val view: BasketFragmentContract.View,
        private val orderApiInteractor: PaymentOrderApiInteractor,
        private val paymentClient: PaymentClient,
        private val scheduler: Scheduler,
        private val amountDetails: AmountDetails
): SamsungPayResponse {

    private val subscriptions = CompositeDisposable()

    fun launchSamsungPay() {
        view.showProgress(true)
        subscriptions.add(
                orderApiInteractor.createOrder(
                        action = PaymentOrderAction.SALE,
                        amount = totalAmount(),
                        currency = amountDetails.getCurrency().currencyCode)
                        .flatMap { order ->
                            Single.fromCallable {
                                initiateSamungPay(order)
                            }
                        }
                        .subscribeOn(scheduler.io())
                        .observeOn(scheduler.main())
                        .subscribe { _ ->
                            view.showProgress(true)

                        })
    }

    private fun totalAmount() =
            amountDetails.getItems().map { it.amount }.fold(BigDecimal.ZERO) { acc, e -> acc + e }

    fun cleanup() {
        subscriptions.dispose()
    }

    @WorkerThread
    fun initiateSamungPay(order: Order) {
        paymentClient.launchSamsungPay(order, "Merchant Name", this)
    }

    override fun onSuccess() {
        view.showProgress(false)
        view.showSnackBar("Transaction successful")
    }

    override fun onFailure(error: String) {
        view.showProgress(false)
        view.showSnackBar("Transaction Failed $error")
    }
}
