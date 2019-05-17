package payment.sdk.android.demo.home

import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import dagger.Binds
import dagger.Module

@Module
abstract class HomeActivityModule {

    @Binds
    @FragmentViewScope
    abstract fun bindsProductDetailsPresenter(impl: HomeActivityPresenter):
            HomeActivityContract.Presenter
}
