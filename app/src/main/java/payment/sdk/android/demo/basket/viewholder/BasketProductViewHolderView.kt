package payment.sdk.android.demo.basket.viewholder

import payment.sdk.android.R
import payment.sdk.android.demo.basket.BasketFragmentContract
import payment.sdk.android.demo.basket.data.BasketProductDomain
import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import javax.inject.Inject

class BasketProductViewHolderView @Inject constructor(
        itemView: View,
        private val interactions: BasketFragmentContract.Interactions
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

    @BindView(R.id.basket_product_image)
    lateinit var basketProductImage: ImageView
    @BindView(R.id.basket_product_name)
    lateinit var basketProductName: TextView

    private lateinit var basketProduct: BasketProductDomain

    init {
        ButterKnife.bind(this, itemView)
    }

    @SuppressLint("SetTextI18n")
    fun bind(basketProduct: BasketProductDomain) {
        this.basketProduct = basketProduct
        basketProductName.text = "${basketProduct.amount} x ${basketProduct.name}"
        Glide.with(itemView.context).load(basketProduct.imageUrl).into(basketProductImage)
    }

    @OnClick(R.id.add_button)
    fun onAddClicked() {
        interactions.onAddClicked(basketProduct)
    }

    @OnClick(R.id.remove_button)
    fun onRemoveClicked() {
        interactions.onRemoveClicked(basketProduct.id)
    }

    @OnClick(R.id.delete_button)
    fun onDeleteClicked() {
        interactions.onDeleteClicked(basketProduct.id)
    }

}