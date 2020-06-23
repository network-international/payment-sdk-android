package payment.sdk.android.samsungpay

interface SamsungPayResponse {
    fun onSuccess()
    fun onFailure(error: String)
}