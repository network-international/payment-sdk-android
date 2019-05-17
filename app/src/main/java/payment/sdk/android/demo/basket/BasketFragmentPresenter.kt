package payment.sdk.android.demo.basket

import payment.sdk.android.demo.basket.cardpayment.CardPaymentPresenter
import payment.sdk.android.demo.basket.data.AmountDetails
import payment.sdk.android.demo.basket.data.BasketProductDomain
import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.demo.dependency.repository.ProductRepository
import payment.sdk.android.demo.dependency.formatter.Formatter
import payment.sdk.android.demo.products.data.ProductDomain
import payment.sdk.android.PaymentClient
import payment.sdk.android.cardpayment.CardPaymentData
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

class BasketFragmentPresenter @Inject constructor(
        private val view: BasketFragmentContract.View,
        private val cardPaymentPresenter: CardPaymentPresenter,
        private val samsungPayPresenter: SamsungPayPresenter,
        private val repository: ProductRepository,
        private val scheduler: Scheduler,
        private val formatter: Formatter,
        private val paymentClient: PaymentClient,
        private val amountDetails: AmountDetails
) : BasketFragmentContract.Presenter {
    private val subscriptions = CompositeDisposable()

    /**
     *
     * Init method is called from different places to refresh data on basket
     *
     * - WARNING - Avoid leak whilst using flowable. Take first data from stream and unsubscribe
     */
    override fun init() {
        val disposable = repository.getBasketProducts().take(1)
                .flatMapSingle { basketProducts ->
                    val priceSum = basketProducts.map { it.prices[0].price.multiply(it.amount.toBigDecimal()) }
                            .fold(BigDecimal.ZERO) { acc, e -> acc + e }
                    val taxSum = basketProducts.map { it.prices[0].tax.multiply(it.amount.toBigDecimal()) }
                            .fold(BigDecimal.ZERO) { acc, e -> acc + e }

                    return@flatMapSingle Single.just(Pair(basketProducts, Pair(priceSum, taxSum)))
                }
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .subscribe({ pair ->
                    amountDetails.reset() // Reset all details

                    if (pair.first.isEmpty()) {
                        view.showBasketEmptyMessage()
                        view.bindData(emptyList())
                        view.showPayButtonLayout(false)
                        view.showAmountLayout(false)
                    } else {
                        val (subTotalSum, taxSum) = pair.second
                        val currency = pair.first[0].prices[0].currency
                        with(amountDetails) {
                            setTotal(subTotalSum.add(taxSum), currency)
                            addItem("subTotal", "Sub Total", subTotalSum)
                            addItem("tax", "Tax", taxSum)
                        }
                        view.bindData(pair.first)
                        view.showPayButtonLayout(true)
                        view.showAmountLayout(true)
                        view.setSubTotalAmount(formatter.formatAmount(amountDetails.getCurrency(), subTotalSum, Locale.US))
                        view.setTaxAmount(formatter.formatAmount(amountDetails.getCurrency(), taxSum, Locale.US))
                        getSupportedPaymentMethods()
                    }
                }, { error ->
                    view.showError(error.message)
                })
        subscriptions.add(disposable)

    }

    override fun onPayWithCard() {
        cardPaymentPresenter.launchCardPayment()
    }

    override fun onPayWithSamsungPay() {
        samsungPayPresenter.launchSamsungPay()
    }

    override fun onCardPaymentResponse(data: CardPaymentData) {
        when (data.code) {
            CardPaymentData.STATUS_PAYMENT_AUTHORIZED,
            CardPaymentData.STATUS_PAYMENT_CAPTURED -> {
                repository.removeAll()
                        .subscribeOn(scheduler.io())
                        .observeOn(scheduler.main())
                        .subscribe()
                view.showOrderSuccessful()
            }
            CardPaymentData.STATUS_PAYMENT_FAILED -> {
                view.showError("Payment failed")
            }
            CardPaymentData.STATUS_GENERIC_ERROR -> {
                view.showError("Generic error(${data.reason})")
            }
            else -> IllegalArgumentException("Unknown payment response (${data.reason})")
        }
    }

    override fun onCardPaymentCancelled() {
        view.showSnackBar("User dismissed pay page")
    }

    override fun addProduct(basketProduct: BasketProductDomain) {
        val product = ProductDomain(
                basketProduct.id,
                basketProduct.name,
                basketProduct.description,
                basketProduct.prices,
                basketProduct.imageUrl
        )

        subscriptions.add(repository.insertProduct(product)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .doOnComplete {
                }.subscribe {
                    init()
                })
    }

    override fun removeProduct(id: String) {
        subscriptions.add(repository.removeProduct(id)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .doOnComplete {
                    init()
                }.subscribe())
    }

    override fun deleteProduct(id: String) {
        subscriptions.add(repository.deleteProduct(id)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .doOnComplete {
                    init()
                }.subscribe())
    }

    override fun cleanup() {
        subscriptions.dispose()
        cardPaymentPresenter.cleanup()
    }

    private fun getSupportedPaymentMethods() {
        paymentClient.getSupportedPaymentMethods(object : PaymentClient.SupportedPaymentTypesListener {
            override fun onReady(supportedPaymentTypes: List<PaymentClient.PaymentType>) {
                supportedPaymentTypes.forEach { type ->
                    when (type) {
                        PaymentClient.PaymentType.CARD_PAYMENT -> view.showCardPaymentButton(true)
                        PaymentClient.PaymentType.SAMSUNG_PAY -> view.showSamsungPayButton(true)
                    }
                }
            }
        })
    }
}
