package payment.sdk.android.demo.product_detail


import payment.sdk.android.demo.App
import payment.sdk.android.R
import payment.sdk.android.demo.products.data.ProductDomain
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import org.parceler.Parcels
import javax.inject.Inject

class ProductDetailsFragment : Fragment(), ProductDetailsFragmentContract.View {

    @BindView(R.id.basket_product_image)
    lateinit var productImage: ImageView

    @BindView(R.id.basket_product_name)
    lateinit var productName: TextView

    @BindView(R.id.product_description)
    lateinit var productDescription: TextView

    @BindView(R.id.add_to_basket_button)
    lateinit var addToBasketButton: View

    @BindView(R.id.already_in_basket_message)
    lateinit var alreadyInBasketMessageView: View


    @Inject
    internal lateinit var presenter: ProductDetailsFragmentContract.Presenter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_product_details, container, false)
        ButterKnife.bind(this, view)

        createProductsComponent(this).inject(this)

        val product: ProductDomain = Parcels.unwrap(
                activity?.intent?.getParcelableExtra(ProductDetailActivity.KEY_PRODUCT))
        presenter.init(product)

        return view
    }

    override fun bindData(product: ProductDomain, productInBasket: Boolean) {
        productName.text = product.name
        productDescription.text = product.description
        addToBasketButton.visibility = if (!productInBasket) View.VISIBLE else View.GONE
        alreadyInBasketMessageView.visibility = if (productInBasket) View.VISIBLE else View.GONE
        Glide.with(this).load(product.imageUrl).into(productImage)
    }

    override fun dismiss() {
        activity?.onBackPressed()
    }

    override fun onDestroyView() {
        presenter.cleanup()
        super.onDestroyView()
    }

    @OnClick(R.id.add_to_basket_button)
    fun onAddToBasketClicked() {
        presenter.addToBasket()
    }

    companion object {
        private fun createProductsComponent(fragment: ProductDetailsFragment)
                : ProductDetailsFragmentComponent {
            val baseComponent = (fragment.activity?.application as App).baseComponent
            return DaggerProductDetailsFragmentComponent.builder()
                    .view(fragment)
                    .baseComponent(baseComponent)
                    .build()
        }
    }

}
