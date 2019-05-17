package payment.sdk.android.demo.basket.viewholder

import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import dagger.Module
import dagger.Provides

@Module(includes = [ViewHolderLayoutModule::class])
abstract class BasketProductViewHolderModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @FragmentViewScope
        fun provideViewHolder(impl: BasketProductViewHolderView): BasketProductViewHolderView {
            return impl
        }

    }
}