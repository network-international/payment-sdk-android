package payment.sdk.android.cardpayment.partialAuth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.text.TextUtilsCompat
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.cardpayment.partialAuth.model.PartialAuthProperties
import payment.sdk.android.cardpayment.partialAuth.view.PartialAuthView
import payment.sdk.android.core.OrderAmount
import java.util.Locale

class PartialAuthActivity : ComponentActivity() {
    private val inputArgs: PartialAuthActivityArgs? by lazy {
        PartialAuthActivityArgs.fromIntent(intent = intent)
    }

    private val viewModel: PartialAuthViewModel by viewModels { PartialAuthViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = runCatching {
            requireNotNull(inputArgs) {
                "PartialAuthActivity input args not found"
            }
        }.getOrElse {
            finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
            return
        }
        val isLTR =
            TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR
        setContent {
            val state by viewModel.state.collectAsState()
            when (state.state) {
                PartialAuthState.INIT, PartialAuthState.LOADING -> {
                    PartialAuthView(
                        state = state.state,
                        properties = PartialAuthProperties(
                            args.issuingOrg,
                            OrderAmount(
                                args.partialAmount,
                                args.currency
                            ).formattedCurrencyString2Decimal(isLTR),
                            OrderAmount(args.amount, args.currency).formattedCurrencyString2Decimal(
                                isLTR
                            )
                        ),
                        onAccept = { viewModel.accept(args.acceptUrl, args.paymentCookie) },
                        onDecline = { viewModel.decline(args.declineUrl, args.paymentCookie) }
                    )
                }

                PartialAuthState.SUCCESS -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED))
                PartialAuthState.ERROR -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
                PartialAuthState.DECLINED -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PARTIAL_AUTH_DECLINED))
            }
        }
    }

    private fun finishWithData(cardPaymentData: CardPaymentData) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}