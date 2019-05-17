package payment.sdk.android.demo.product_detail

import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.demo.products.data.ProductDomain
import payment.sdk.android.demo.dependency.repository.ProductRepository
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class ProductDetailsFragmentPresenter @Inject constructor(
        private val view: ProductDetailsFragmentContract.View,
        private val repository: ProductRepository,
        private val scheduler: Scheduler
) : ProductDetailsFragmentContract.Presenter {

    private val subscriptions = CompositeDisposable()

    private lateinit var product: ProductDomain

    override fun init(product: ProductDomain) {
        this.product = product

        subscriptions.add(repository.hasProduct(product.id)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .subscribe { productInBasket ->
                    view.bindData(product, productInBasket)
                })
    }

    override fun addToBasket() {
        subscriptions.add(repository.insertProduct(product)
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .subscribe {
                    view.dismiss()
                })
    }

    override fun cleanup() {
        subscriptions.dispose()
    }
}
