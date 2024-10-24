package payment.sdk.android.visaInstalments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import payment.sdk.android.visaInstalments.view.VisaInstalmentsView
import payment.sdk.android.sdk.R

class VisaInstallmentsActivity : ComponentActivity() {
    private val inputArgs: VisaInstallmentsLauncher.Config? by lazy {
        VisaInstallmentsLauncher.Config.fromIntent(intent = intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = runCatching {
            requireNotNull(inputArgs) {
                "VisaInstalmentsActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(VisaInstallmentsLauncher.Result.Cancelled)
            return
        }

        setContent {
            Scaffold(
                backgroundColor = Color(0xFFD6D6D6),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.title_activity_visa_instalments),
                                color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                            )
                        },
                        backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
                        navigationIcon = {
                            IconButton(onClick = {
                                finishWithData(VisaInstallmentsLauncher.Result.Cancelled)
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    tint = colorResource(id = R.color.payment_sdk_toolbar_icon_color),
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { contentPadding ->
                VisaInstalmentsView(
                    modifier = Modifier.padding(contentPadding),
                    instalmentPlans = args.installmentPlans,
                    cardNumber = args.cardNumber
                ) { plan ->
                    finishWithData(VisaInstallmentsLauncher.Result.Success(plan))
                }
            }
        }
    }

    private fun finishWithData(result: VisaInstallmentsLauncher.Result) {
        val intent = Intent().apply {
            putExtra(VisaInstallmentsLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}