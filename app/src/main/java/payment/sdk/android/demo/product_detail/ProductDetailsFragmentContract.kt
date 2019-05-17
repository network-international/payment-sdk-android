package payment.sdk.android.demo.product_detail

import payment.sdk.android.demo.products.data.ProductDomain


class ProductDetailsFragmentContract {

    interface View {
        fun bindData(product: ProductDomain, productInBasket: Boolean)

        fun dismiss()
    }

    interface Presenter {
        fun init(product: ProductDomain)

        fun cleanup()

        fun addToBasket()
    }
}
