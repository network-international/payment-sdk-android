package payment.sdk.android.demo.home

import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.demo.dependency.repository.ProductRepository
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class HomeActivityPresenter @Inject constructor(
        private val view: HomeActivityContract.View,
        private val repository: ProductRepository,
        private val scheduler: Scheduler
) : HomeActivityContract.Presenter {

    private val subscriptions = CompositeDisposable()

    override fun init() {
        subscriptions.add(repository.getBasketProducts()
                .subscribeOn(scheduler.io())
                .observeOn(scheduler.main())
                .subscribe { productsInBasket ->
                    view.basketSizeChanged(productsInBasket.size)
                })
    }

    override fun cleanup() {
        subscriptions.dispose()
    }
}
