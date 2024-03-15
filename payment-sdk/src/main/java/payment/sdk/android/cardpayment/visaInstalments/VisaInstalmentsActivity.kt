package payment.sdk.android.cardpayment.visaInstalments

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class VisaInstalmentsActivity : ComponentActivity() {

    private val viewModel: VisaInstalmentsViewModel by viewModels { VisaInstalmentsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
        }
    }
}