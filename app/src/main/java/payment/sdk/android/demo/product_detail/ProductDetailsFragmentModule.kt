package payment.sdk.android.demo.product_detail

import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import dagger.Binds
import dagger.Module

@Module
abstract class ProductDetailsFragmentModule {

    @Binds
    @FragmentViewScope
    abstract fun bindsProductDetailsPresenter(impl: ProductDetailsFragmentPresenter):
            ProductDetailsFragmentContract.Presenter
}
