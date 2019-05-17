package payment.sdk.android.demo.products

import payment.sdk.android.demo.dependency.BaseComponent
import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [BaseComponent::class], modules = [ProductsFragmentModule::class])
@FragmentViewScope
interface ProductsFragmentComponent {

    fun inject(fragment: ProductsFragment)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun view(view: ProductsFragmentContract.View): Builder

        fun baseComponent(baseComponent: BaseComponent): Builder

        fun build(): ProductsFragmentComponent
    }
}