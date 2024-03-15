package payment.sdk.android.cardpayment.visaInstalments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient

class VisaInstalmentsViewModel: ViewModel() {

    private var _state: MutableStateFlow<VisaInstalmentsVMState> =
        MutableStateFlow(VisaInstalmentsVMState.Init)

    val state: StateFlow<VisaInstalmentsVMState> = _state.asStateFlow()

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val httpClient = CoroutinesGatewayHttpClient()
                return VisaInstalmentsViewModel() as T
            }
        }
    }
}

sealed class VisaInstalmentsVMState {
    object Init : VisaInstalmentsVMState()

    data class Loading(val message: String) : VisaInstalmentsVMState()
}