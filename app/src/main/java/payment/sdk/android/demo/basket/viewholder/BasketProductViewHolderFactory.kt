package payment.sdk.android.demo.basket.viewholder

import payment.sdk.android.demo.basket.BasketFragmentContract
import payment.sdk.android.demo.dependency.BaseComponent
import payment.sdk.android.demo.dependency.scope.ViewHolderScope
import android.view.ViewGroup
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [BaseComponent::class], modules = [ViewHolderLayoutModule::class])
@ViewHolderScope
interface BasketProductViewHolderFactory {

    fun createViewHolder(): BasketProductViewHolderView

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun interactions(interactions: BasketFragmentContract.Interactions): Builder

        @BindsInstance
        fun parentView(parentView: ViewGroup): Builder

        fun baseComponent(baseComponent: BaseComponent): Builder

        fun layoutModule(layoutModule: ViewHolderLayoutModule): Builder

        fun build(): BasketProductViewHolderFactory
    }
}