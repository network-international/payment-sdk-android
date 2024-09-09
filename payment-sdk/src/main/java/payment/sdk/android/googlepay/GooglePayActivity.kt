package payment.sdk.android.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import kotlinx.coroutines.launch

class GooglePayActivity : AppCompatActivity() {

//    private val viewModel: GooglePayViewModel by viewModels { GooglePayViewModel.Factory(args) }

    private lateinit var args: GooglePayLauncher.Config

    private val paymentDataLauncher = registerForActivityResult(GetPaymentDataResult()) { taskResult ->
        when (taskResult.status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                taskResult.result!!.let {
                    Log.i("Google Pay result:", it.toJson())
                }
            }
            CommonStatusCodes.CANCELED -> {
                Log.i("Google Pay result:", "CANCELED")
            }
            AutoResolveHelper.RESULT_ERROR -> {
                Log.i("Google Pay result:", "RESULT_ERROR")
            }
            CommonStatusCodes.INTERNAL_ERROR -> {
                Log.i("Google Pay result:", "INTERNAL_ERROR")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = runCatching {
            requireNotNull(GooglePayLauncher.Config.fromIntent(intent)) {
                "GooglePayActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(GooglePayLauncher.Result.Failed("intent args not found"))
            return
        }

        val client = createPaymentsClient(this)
        lifecycleScope.launch {
//            viewModel.state.collect { state ->
//                if (state is GooglePayVMState.Submit) {
//                    val task = client.loadPaymentData(state.paymentDataRequest)
//                    task.addOnCompleteListener(paymentDataLauncher::launch)
//                }
//            }
        }
        
//        viewModel.handleAuthentication()
    }

    fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }

    private fun finishWithData(result: GooglePayLauncher.Result) {
        val intent = Intent().apply {
            putExtra(GooglePayLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}