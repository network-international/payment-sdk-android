package payment.sdk.android.demo.basket


import payment.sdk.android.demo.basket.data.BasketProductDomain
import payment.sdk.android.demo.basket.viewholder.BasketProductViewHolderFactory
import payment.sdk.android.demo.basket.viewholder.BasketProductViewHolderView
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup


class BasketProductsAdapter(
        private val viewHolderFactoryBuilder: BasketProductViewHolderFactory.Builder,
        private val interactions: BasketFragmentContract.Interactions
) : RecyclerView.Adapter<BasketProductViewHolderView>() {

    private var data = emptyList<BasketProductDomain>()

    fun setData(data: List<BasketProductDomain>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasketProductViewHolderView {
        return viewHolderFactoryBuilder.parentView(parent)
                .interactions(interactions)
                .build()
                .createViewHolder()
    }

    override fun onBindViewHolder(holder: BasketProductViewHolderView, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
