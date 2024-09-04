package payment.sdk.android.googlepay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class GooglePayActivity : AppCompatActivity() {

    private val viewModel: GooglePayViewModel by viewModels { GooglePayViewModel.Factory(args) }

    private lateinit var args: GooglePayLauncher.Config

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
    }

    private fun finishWithData(result: GooglePayLauncher.Result) {
        val intent = Intent().apply {
            putExtra(GooglePayLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}

