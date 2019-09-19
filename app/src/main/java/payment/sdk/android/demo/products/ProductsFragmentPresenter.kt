package payment.sdk.android.demo.products

import io.reactivex.disposables.CompositeDisposable
import payment.sdk.android.demo.dependency.configuration.Configuration
import payment.sdk.android.demo.products.data.*
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.collections.ArrayList

class ProductsFragmentPresenter @Inject constructor(
        private val view: ProductsFragmentContract.View,
        private val configuration: Configuration

) : ProductsFragmentContract.Presenter {

    private val subscriptions = CompositeDisposable()

    override fun init() {
        view.showProgress(false)
        view.bindData(this.getProducts())
    }

    fun getProducts(): List<ProductDomain> {
        val settings = Pair(configuration.locale, configuration.currency)
        val locale = settings.first
        val currency = settings.second
//        val language = locale.language.toLowerCase()
        val products = ArrayList<ProductDomain>()
        for (i in 1..6) {
            val price = Price(currency, BigDecimal((0..50).random()), BigDecimal(0))
            products.add(ProductDomain("$i", "Furniture $i", "Just a furniture", listOf(price, price), "file:///android_asset/images/0$i.jpg" ))
        }
        return  products
    }

    override fun cleanup() {
        subscriptions.dispose()
    }
}
