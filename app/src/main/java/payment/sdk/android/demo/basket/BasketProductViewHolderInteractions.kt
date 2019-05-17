package payment.sdk.android.demo.basket

import payment.sdk.android.demo.basket.data.BasketProductDomain
import javax.inject.Inject

class BasketProductViewHolderInteractions @Inject constructor(
        private val basketPresenter: BasketFragmentContract.Presenter
): BasketFragmentContract.Interactions {

    override fun onAddClicked(basketProduct: BasketProductDomain) {
        basketPresenter.addProduct(basketProduct)
    }

    override fun onRemoveClicked(id: String) {
        basketPresenter.removeProduct(id)
    }

    override fun onDeleteClicked(id: String) {
        basketPresenter.deleteProduct(id)
    }
}
