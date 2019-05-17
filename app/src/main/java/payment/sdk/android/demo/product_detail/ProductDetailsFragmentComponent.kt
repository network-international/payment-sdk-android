package payment.sdk.android.demo.product_detail

import payment.sdk.android.demo.dependency.BaseComponent
import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [BaseComponent::class], modules = [ProductDetailsFragmentModule::class])
@FragmentViewScope
interface ProductDetailsFragmentComponent {

    fun inject(fragment: ProductDetailsFragment)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun view(view: ProductDetailsFragmentContract.View): Builder

        fun baseComponent(baseComponent: BaseComponent): Builder

        fun build(): ProductDetailsFragmentComponent
    }
}