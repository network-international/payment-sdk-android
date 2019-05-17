package payment.sdk.android.cardpayment

import payment.sdk.android.sdk.R
import payment.sdk.android.cardpayment.api.CoroutinesGatewayHttpClient
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.core.dependency.StringResourcesImpl
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureRequest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup

class CardPaymentActivity : Activity(), CardPaymentContract.Interactions {

    private lateinit var presenter: CardPaymentContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_payment)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        presenter = CardPaymentPresenter(
                url = intent.getStringExtra(URL_KEY),
                code = intent.getStringExtra(CODE),
                view = CardPaymentView(findViewById(R.id.bottom_sheet)),
                interactions = this,
                paymentApiInteractor = CardPaymentApiInteractor(CoroutinesGatewayHttpClient()),
                stringResources = StringResourcesImpl(this)
        )
        presenter.init()
    }

    override fun onStart3dSecure(threeDSecureRequest: ThreeDSecureRequest) {
        startActivityForResult(
                ThreeDSecureWebViewActivity.getIntent(
                        context = this,
                        acsUrl = threeDSecureRequest.acsUrl,
                        acsPaReq = threeDSecureRequest.acsPaReq,
                        acsMd = threeDSecureRequest.acsMd,
                        gatewayUrl = threeDSecureRequest.gatewayUrl),
                THREE_D_SECURE_REQUEST_KEY
        )
    }

    override fun onPaymentAuthorized() {
        finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED))
    }

    override fun onPaymentCaptured() {
        finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED))
    }

    override fun onPaymentFailed() {
        finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
    }

    override fun onGenericError(message: String?) {
        finishWithData(CardPaymentData(CardPaymentData.STATUS_GENERIC_ERROR, message))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == THREE_D_SECURE_REQUEST_KEY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                presenter.onHandle3DSecurePaymentSate(data.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE))
            } else {
                onPaymentFailed()
            }
        }
    }

    private fun finishWithData(cardPaymentData: CardPaymentData) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    /**
     * Window exit animation (@link {Animation.Design.BottomSheetDialog}) does not seem to work for
     * devices below 21.
     */
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            overridePendingTransition(0, 0)

        }
    }

    companion object {

        private const val THREE_D_SECURE_REQUEST_KEY: Int = 100

        private const val URL_KEY = "gateway-payment-url"
        private const val CODE = "code"


        fun getIntent(context: Context, url: String, code: String) =
                Intent(context, CardPaymentActivity::class.java).apply {
                    putExtra(URL_KEY, url)
                    putExtra(CODE, code)

                }

    }
}
