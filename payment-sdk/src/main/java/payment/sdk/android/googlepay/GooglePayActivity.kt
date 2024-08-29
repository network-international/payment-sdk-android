package payment.sdk.android.googlepay

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class GooglePayActivity : AppCompatActivity() {

    private val viewModel: GooglePayViewModel by viewModels { GooglePayViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}