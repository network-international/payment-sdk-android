package payment.sdk.android.googlepay

sealed class GooglePayVMState {
    data object Init : GooglePayVMState()
    class Authorized(val accessToken: String, val paymentCookie: String, val orderUrl: String) : GooglePayVMState()
    class Error(val error: String) : GooglePayVMState()
}