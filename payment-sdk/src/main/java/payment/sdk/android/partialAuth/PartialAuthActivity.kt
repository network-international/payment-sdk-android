package payment.sdk.android.partialAuth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.partialAuth.view.PartialAuthView
import payment.sdk.android.sdk.R

class PartialAuthActivity : ComponentActivity() {
    private val inputArgs: PartialAuthActivityArgs? by lazy {
        PartialAuthActivityArgs.fromIntent(intent = intent)
    }

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
        setContent {
            Scaffold(
                backgroundColor = Color.White,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.paypage_title_awaiting_partial_auth_approval),
                                color = colorResource(id = R.color.payment_sdk_pay_button_text_color),
                                textAlign = TextAlign.Center
                            )
                        },
                        backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
                    )
                },
            ) { contentPadding ->
                PartialAuthView(
                    modifier = Modifier.padding(contentPadding),
                    args = args,
                    onResult = { result ->
                        finishWithData(result)
                    }
                )
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