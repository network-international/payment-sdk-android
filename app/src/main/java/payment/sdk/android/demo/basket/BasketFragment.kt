package payment.sdk.android.demo.basket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.snackbar.Snackbar
import payment.sdk.android.R
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.core.SavedCard
import payment.sdk.android.demo.App
import payment.sdk.android.demo.basket.data.AmountDetails
import payment.sdk.android.demo.basket.data.BasketProductDomain
import payment.sdk.android.demo.basket.viewholder.BasketProductViewHolderFactory
import payment.sdk.android.demo.home.HomeActivity
import javax.inject.Inject


class BasketFragment : androidx.fragment.app.Fragment(), BasketFragmentContract.View {
    @BindView(R.id.basket_products)
    lateinit var basketProductsView: androidx.recyclerview.widget.RecyclerView

    @BindView(R.id.pay_button_layout)
    lateinit var payButtonLayout: View

    @BindView(R.id.pay)
    lateinit var cardPaymentButton: View

    @BindView(R.id.pay_with_samsung_pay)
    lateinit var samsungPayButton: View

    @BindView(R.id.amount_layout)
    lateinit var amountLayout: View

    @BindView(R.id.empty_basket)
    lateinit var emptyBasketMessageView: TextView

    @BindView(R.id.subtotal_amount)
    lateinit var subTotalAmountView: TextView

    @BindView(R.id.tax_amount)
    lateinit var taxAmountView: TextView

    @BindView(R.id.progress)
    lateinit var progressView: ProgressBar

    @BindView(R.id.order_successful)
    lateinit var orderSuccessful: View

    @BindView(R.id.saved_card_view)
    lateinit var savedCardView: View

    @BindView(R.id.iv_card_logo)
    lateinit var cardLogo: ImageView

    @BindView(R.id.tv_cardholder_name)
    lateinit var tvCardHolerName: TextView

    @BindView(R.id.tv_cardholder_number)
    lateinit var tvCardHolerNumber: TextView

    @Inject
    internal lateinit var viewHolderFactoryBuilder: BasketProductViewHolderFactory.Builder

    @Inject
    internal lateinit var presenter: BasketFragmentContract.Presenter

    @Inject
    internal lateinit var interactions: BasketFragmentContract.Interactions


    private lateinit var adapter: BasketProductsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_basket, container, false)
        ButterKnife.bind(this, view)

        createBasketComponent(this, activity!!).inject(this)

        adapter = BasketProductsAdapter(viewHolderFactoryBuilder, interactions)
        basketProductsView.adapter = adapter
        basketProductsView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                context,
                androidx.recyclerview.widget.DividerItemDecoration.HORIZONTAL
            )
        )
        presenter.init()

        return view
    }

    override fun bindData(basketProducts: List<BasketProductDomain>) {
        adapter.setData(basketProducts)
    }

    override fun showBasketEmptyMessage() {
        emptyBasketMessageView.visibility = View.VISIBLE
    }

    override fun showPayButtonLayout(show: Boolean) {
        payButtonLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showSamsungPayButton(show: Boolean) {
        samsungPayButton.visibility = if(show) View.VISIBLE else View.GONE
    }

    override fun showSavedCardView(savedCard: SavedCard?) {
        savedCard?.let {
            savedCardView.visibility = View.VISIBLE
            cardLogo.setBackgroundResource(getCardLogo(savedCard.scheme))
            tvCardHolerName.text = savedCard.cardholderName
            tvCardHolerNumber.text = savedCard.maskedPan
            savedCardView.setOnClickListener {
                presenter.onSavedCardPayment(savedCard)
            }
        } ?: run {
            savedCardView.visibility = View.GONE
        }
    }

    fun getCardLogo(scheme: String) = when (scheme) {
        "MASTERCARD" -> payment.sdk.android.sdk.R.drawable.ic_logo_mastercard
        "VISA" -> payment.sdk.android.sdk.R.drawable.ic_logo_visa
        "AMERICAN_EXPRESS" -> payment.sdk.android.sdk.R.drawable.ic_logo_amex
        "DINERS_CLUB_INTERNATIONAL" -> payment.sdk.android.sdk.R.drawable.ic_logo_dinners_clup
        "JCB" -> payment.sdk.android.sdk.R.drawable.ic_logo_jcb
        else -> payment.sdk.android.sdk.R.drawable.ic_card_back_chip
    }

    override fun showAmountLayout(show: Boolean) {
        amountLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String?) {
        Snackbar.make(view!!, message ?: "No error message supplied", Snackbar.LENGTH_SHORT).show()
    }

    override fun setSubTotalAmount(amount: String) {
        subTotalAmountView.text = amount
    }

    override fun setTaxAmount(amount: String) {
        taxAmountView.text = amount
    }

    @OnClick(R.id.pay)
    fun onCardPaymentClicked() {
        presenter.onPayWithCard()
    }

    @OnClick(R.id.pay_with_samsung_pay)
    fun onSamsungPayClicked() {
        presenter.onPayWithSamsungPay()
    }

    @OnClick(R.id.continue_shopping)
    fun onContinueClicked() {
        context?.let {
            val intent = Intent(it, HomeActivity::class.java)
            intent.putExtra("tab", R.id.navigate_furniture)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.startActivity(intent)
        }
    }

    override fun showCardPaymentButton(show: Boolean) {
        cardPaymentButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showSnackBar(message: String?) {
        message?.let {
            Snackbar.make(progressView, it, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun showOrderSuccessful() {
        bindData(listOf())
        payButtonLayout.visibility = View.GONE
        amountLayout.visibility = View.GONE
        orderSuccessful.visibility = View.VISIBLE
        savedCardView.visibility = View.GONE
    }

    fun onCardPaymentResponse(data: CardPaymentData) {
        showProgress(false)
        savedCardView.visibility = View.GONE
        presenter.onCardPaymentResponse(data)
    }

    fun onCardPaymentCancelled() {
        showProgress(false)
        savedCardView.visibility = View.GONE
        presenter.onCardPaymentCancelled()
    }

    companion object {

        internal const val CARD_PAYMENT_REQUEST_CODE: Int = 0
        internal const val THREE_DS_TWO_REQUEST_CODE: Int = 2

        private fun createBasketComponent(fragment: BasketFragment, context: Activity)
                : BasketFragmentComponent {
            val baseComponent = (fragment.activity?.application as App).baseComponent
            return DaggerBasketFragmentComponent.builder()
                    .amountDetails(AmountDetails())
                    .context(context)
                    .view(fragment)
                    .baseComponent(baseComponent)
                    .build()
        }
    }
}
