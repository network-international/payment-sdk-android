package payment.sdk.android.demo.basket

import payment.sdk.android.demo.basket.data.AmountDetails
import payment.sdk.android.demo.dependency.BaseComponent
import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import android.app.Activity
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [BaseComponent::class], modules = [BasketFragmentModule::class])
@FragmentViewScope
interface BasketFragmentComponent {

    fun inject(fragment: BasketFragment)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun view(view: BasketFragmentContract.View): Builder

        @BindsInstance
        fun context(context: Activity): Builder

        @BindsInstance
        fun amountDetails(amountDetails: AmountDetails): Builder

        fun baseComponent(baseComponent: BaseComponent): Builder

        fun build(): BasketFragmentComponent
    }
}