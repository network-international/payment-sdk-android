package payment.sdk.android.demo.products


import payment.sdk.android.demo.App
import payment.sdk.android.R
import payment.sdk.android.demo.products.data.ProductDomain
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import javax.inject.Inject

class ProductsFragment : Fragment(), ProductsFragmentContract.View {

    @BindView(R.id.progress)
    internal lateinit var progressView: ProgressBar

    @Inject
    internal lateinit var adapter: ProductsAdapter

    @Inject
    internal lateinit var presenter: ProductsFragmentContract.Presenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_products, container, false)
        ButterKnife.bind(this, view)

        createProductsComponent(this).inject(this)

        val productsView: RecyclerView = view.findViewById(R.id.products)
        productsView.adapter = adapter

        presenter.init()

        return view
    }

    override fun bindData(products: List<ProductDomain>) {
        adapter.setData(products)
    }

    override fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String?) {
        Snackbar.make(view!!, message ?: "No error message supplied", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        presenter.cleanup()
        super.onDestroyView()
    }

    companion object {
        private fun createProductsComponent(fragment: ProductsFragment)
                : ProductsFragmentComponent {
            val baseComponent = (fragment.activity?.application as App).baseComponent
            return DaggerProductsFragmentComponent.builder()
                    .view(fragment)
                    .baseComponent(baseComponent)
                    .build()
        }
    }

}
