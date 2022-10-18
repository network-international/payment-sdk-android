package payment.sdk.android.demo.home


import payment.sdk.android.demo.App
import payment.sdk.android.R
import payment.sdk.android.demo.basket.BasketFragment
import payment.sdk.android.demo.dependency.configuration.Configuration
import payment.sdk.android.demo.dependency.configuration.Configuration.ConfigurationListener
import payment.sdk.android.demo.products.ProductsFragment
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.demo.settings.SettingsFragment
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import java.util.*
import javax.inject.Inject

class HomeActivity : AppCompatActivity(),
        HomeActivityContract.View, OnNavigationItemSelectedListener, ConfigurationListener {

    @Inject
    lateinit var presenter: HomeActivityContract.Presenter
    @Inject
    lateinit var configuration: Configuration

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar
    @BindView(R.id.navigation)
    lateinit var navigation: BottomNavigationView

    private lateinit var badge: ViewGroup
    private lateinit var badgeContent: TextView

    private var lastSelectedItemId: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ButterKnife.bind(this)

        setSupportActionBar(toolbar)
        navigation.setOnNavigationItemSelectedListener(this)

        if (intent.getIntExtra("tab", 0) != 0) {
            navigation.selectedItemId = intent.getIntExtra("tab", 0)
        } else if (savedInstanceState == null) {
            navigation.selectedItemId = R.id.navigate_basket
        }
        addBadgeView()

        createComponent(this).inject(this)

        configuration.addConfigurationListener(this)
        presenter.init()
    }

    override fun onNavigationItemSelected(menu: MenuItem): Boolean {
        val itemId = menu.itemId
        if (itemId != lastSelectedItemId) {
            changeFragment(itemId)
            lastSelectedItemId = itemId
        }
        return true
    }

    override fun basketSizeChanged(size: Int) {
        if (size > 0) {
            badge.visibility = View.VISIBLE
            badgeContent.text = "$size"
        } else {
            badge.visibility = View.GONE
        }
    }


    private fun addBadgeView() {
        val bottomMenu = navigation.getChildAt(0) as? BottomNavigationMenuView
        val basketItemView = bottomMenu?.getChildAt(1) as? BottomNavigationItemView

        badge = LayoutInflater.from(this)
                .inflate(R.layout.view_navigation_badge, bottomMenu, false) as ViewGroup
        basketItemView?.addView(badge)
        badgeContent = badge.getChildAt(0) as TextView
    }

    private fun changeFragment(itemId: Int) {
        val arguments = Bundle()
        val fragment = when (itemId) {
            R.id.navigate_furniture -> {
                setTitle(R.string.fragment_title_furniture)
                ProductsFragment()
            }
            R.id.navigate_basket -> {
                setTitle(R.string.fragment_title_basket)
                BasketFragment()
            }
            R.id.navigate_more -> {
                setTitle(R.string.fragment_title_settings)
                SettingsFragment()
            }
            else -> throw IllegalArgumentException("Id is not supported")
        }
        fragment.arguments = arguments

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_content, fragment, itemId.toString())
                .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        configuration.removeConfigurationListener(this)
        presenter.cleanup()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BasketFragment.CARD_PAYMENT_REQUEST_CODE ||
            requestCode == BasketFragment.THREE_DS_TWO_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> basketFragment?.onCardPaymentResponse(
                        CardPaymentData.getFromIntent(data!!)
                )
                Activity.RESULT_CANCELED -> basketFragment?.onCardPaymentCancelled()
            }
        }
    }

    private val basketFragment: BasketFragment?
        get() = supportFragmentManager.fragments.firstOrNull { it is BasketFragment }?.let {
            it as BasketFragment
        }

    override fun onCurrencyChanged(currency: Currency) {
        // We wil not use this in the activity
    }

    override fun onLocaleChanged(locale: Locale) {
        Locale.setDefault(locale)
        val osConfiguration = android.content.res.Configuration().apply {
            setLocale(locale)
        }
        applicationContext.apply {
            resources.updateConfiguration(osConfiguration, resources.displayMetrics)
        }
        recreate()
    }

    companion object {
        private fun createComponent(activity: HomeActivity)
                : HomeActivityComponent {
            val baseComponent = (activity.application as App).baseComponent
            return DaggerHomeActivityComponent.builder()
                    .view(activity)
                    .baseComponent(baseComponent)
                    .build()
        }
    }
}
