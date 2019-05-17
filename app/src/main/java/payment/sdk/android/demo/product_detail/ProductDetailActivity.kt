package payment.sdk.android.demo.product_detail

import payment.sdk.android.R
import payment.sdk.android.demo.products.data.ProductDomain
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import butterknife.BindView
import butterknife.ButterKnife
import org.parceler.Parcels

class ProductDetailActivity : AppCompatActivity() {

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_details)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_activity_product_details)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {

        @VisibleForTesting const val KEY_PRODUCT = "product"

        fun getIntent(context: Context, product: ProductDomain) =
                    Intent(context, ProductDetailActivity::class.java).apply {
                    putExtra(KEY_PRODUCT, Parcels.wrap(product))
                }
    }
}