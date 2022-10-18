package payment.sdk.android.core

interface TransactionService {
//    fun getOrder(paymentLink: String, paymentAuthorizationLink: String): Order
    fun getAuthTokenFromCode(url: String, code: String, success: (List<String>, String) -> Unit, error: (Exception) -> Unit)
    fun authorizePayment(order: Order, onResponse: (authTokens: HashMap<String, String>?, error: Exception?) -> Unit)
    fun acceptSamsungPay(encryptedObject: String,
                         samsungPayLink: String,
                         paymentToken: String,
                         onResponse: (status: Boolean, error: Exception?) -> Unit)
}