package payment.sdk.android.googlepay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import kotlinx.coroutines.launch
import org.json.JSONObject
import payment.sdk.android.cardpayment.widget.CircularProgressDialog

class GooglePayActivity : AppCompatActivity() {

    private lateinit var args: GooglePayLauncher.Config

    private val viewModel: GooglePayViewModel by viewModels { GooglePayViewModel.Factory(args) }

    private var sheetLaunched = false
    private var finished = false

    private val paymentDataLauncher =
        registerForActivityResult(GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    try {
                        val token = taskResult.result
                            ?.toJson()
                            ?.let { JSONObject(it).getJSONObject("paymentMethodData") }
                            ?.getJSONObject("tokenizationData")
                            ?.getString("token")
                            .orEmpty()
                        if (token.isNotEmpty()) {
                            viewModel.acceptGooglePay(token)
                        } else {
                            finishWith(GooglePayLauncher.Result.Failed("Google Pay token is empty"))
                        }
                    } catch (e: Exception) {
                        finishWith(GooglePayLauncher.Result.Failed("Failed to parse Google Pay result"))
                    }
                }
                CommonStatusCodes.CANCELED -> viewModel.onUserCancelled()
                AutoResolveHelper.RESULT_ERROR,
                CommonStatusCodes.INTERNAL_ERROR -> {
                    finishWith(GooglePayLauncher.Result.Failed("Google Pay error"))
                }
                else -> viewModel.onUserCancelled()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = GooglePayLauncher.Config.fromIntent(intent)
        if (config == null) {
            finishWith(GooglePayLauncher.Result.Failed("Google Pay launcher arguments were not found"))
            return
        }
        args = config

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWith(GooglePayLauncher.Result.Cancelled)
            }
        })

        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is GooglePayVMEffects.LaunchSheet -> {
                        if (sheetLaunched) return@collect
                        sheetLaunched = true
                        effect.uiConfig.paymentsClient
                            .loadPaymentData(effect.uiConfig.paymentDataRequest)
                            .addOnCompleteListener(paymentDataLauncher::launch)
                    }
                    is GooglePayVMEffects.Finished -> finishWith(effect.result)
                }
            }
        }

        setContent {
            val loading by viewModel.loading.collectAsState()
            val message by viewModel.loadingMessage.collectAsState()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressDialog(message = message)
                }
            }
        }

        viewModel.start()
    }

    private fun finishWith(result: GooglePayLauncher.Result) {
        if (finished) return
        finished = true
        val intent = Intent().apply {
            putExtra(GooglePayLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
