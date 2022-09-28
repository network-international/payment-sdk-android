package payment.sdk.android.demo.products.viewholder

import payment.sdk.android.R
import payment.sdk.android.demo.dependency.formatter.Formatter
import payment.sdk.android.demo.product_detail.ProductDetailActivity
import payment.sdk.android.demo.products.data.ProductDomain
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import java.util.*
import javax.inject.Inject

class ProductsViewHolder @Inject constructor(
        itemView: View,
        private val formatter: Formatter
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

    @BindView(R.id.basket_product_image)
    lateinit var productImageView: ImageView
    @BindView(R.id.basket_product_name)
    lateinit var productNameView: TextView
    @BindView(R.id.product_price)
    lateinit var productPriceView: TextView

    private lateinit var product: ProductDomain

    init {
        ButterKnife.bind(this, itemView)
    }

    fun bind(product: ProductDomain) {
        this.product = product

        productNameView.text = product.name
        productPriceView.text = formatter.formatAmount(product.prices[0].currency, product.prices[0].price, Locale.US)
        Glide.with(itemView.context).load(product.imageUrl).into(productImageView)
    }

    @OnClick(R.id.product)
    fun onProductClicked() {
        itemView.context.apply {
            startActivity(ProductDetailActivity.getIntent(this, product))
        }
    }
}