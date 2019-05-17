package payment.sdk.android.demo.basket

import payment.sdk.android.demo.basket.data.BasketProductDomain
import payment.sdk.android.cardpayment.CardPaymentData


interface BasketFragmentContract {

    interface View {
        fun bindData(basketProducts: List<BasketProductDomain>)

        fun showError(message: String?)

        fun showBasketEmptyMessage()

        fun showPayButtonLayout(show: Boolean)

        fun showAmountLayout(show: Boolean)

        fun showProgress(show: Boolean)

        fun showSnackBar(message: String?)

        fun setSubTotalAmount(amount: String)

        fun setTaxAmount(amount: String)

        fun showCardPaymentButton(show: Boolean)

        fun showOrderSuccessful()

        fun showSamsungPayButton(show: Boolean)
    }

    interface Presenter {
        fun init()

        fun cleanup()

        fun addProduct(basketProduct: BasketProductDomain)

        fun removeProduct(id: String)

        fun deleteProduct(id: String)

        fun onPayWithCard()

        fun onCardPaymentResponse(data: CardPaymentData)

        fun onCardPaymentCancelled()

        fun onPayWithSamsungPay()
    }

    interface Interactions {

        fun onAddClicked(basketProduct: BasketProductDomain)

        fun onRemoveClicked(id: String)

        fun onDeleteClicked(id: String)
    }
}
