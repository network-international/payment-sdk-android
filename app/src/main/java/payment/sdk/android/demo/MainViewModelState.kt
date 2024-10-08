package payment.sdk.android.demo

import payment.sdk.android.core.Order
import payment.sdk.android.demo.model.Product
import payment.sdk.android.core.SavedCard

data class MainViewModelState(
    val state: MainViewModelStateType = MainViewModelStateType.INIT,
    val products: List<Product> = listOf(),
    val selectedProducts: List<Product> = listOf(),
    val isSamsungPayAvailable: Boolean = false,
    val message: String = "",
    val total: Double = 0.0,
    val orderReference: String? = null,
    val savedCard: SavedCard? = null,
    val currency: String = "",
    val savedCards: List<SavedCard> = listOf(),
    val order: Order = Order(),
    val paymentType: PaymentType = PaymentType.CARD
)

enum class PaymentType {
    SAMSUNG_PAY,
    CARD,
    SAVED_CARD,
    AANI_PAY
}

enum class MainViewModelStateType {
    INIT,
    LOADING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    PAYMENT_PROCESSING,
    PAYMENT_POST_AUTH_REVIEW,
    ERROR,
    PAYMENT_PARTIAL_AUTH_DECLINED,
    PAYMENT_PARTIAL_AUTH_DECLINE_FAILED,
    PAYMENT_PARTIALLY_AUTHORISED
}

fun MainViewModelStateType.getAlertMessage(message: String = ""): Pair<String, String> {
    return when (this) {
        MainViewModelStateType.INIT -> Pair("", "")
        MainViewModelStateType.LOADING -> Pair("", "")
        MainViewModelStateType.PAYMENT_SUCCESS -> Pair("Payment Success", "Payment was successful")
        MainViewModelStateType.PAYMENT_FAILED -> Pair("Payment Failed", "Payment was Failed")
        MainViewModelStateType.PAYMENT_CANCELLED -> Pair(
            "Payment Cancelled",
            "Payment was cancelled by user"
        )

        MainViewModelStateType.PAYMENT_POST_AUTH_REVIEW -> Pair(
            "Payment in Review",
            "Payment is in  post auth review"
        )

        MainViewModelStateType.ERROR -> Pair("Error", message)
        MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINED -> Pair(
            "Partial Auth Declined",
            "Customer declined partial auth"
        )
        MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINE_FAILED -> Pair(
            "Sorry, your payment has not been accepted.",
            "Due to technical error, the refund was not processed. Please contact merchant for refund."
        )
        MainViewModelStateType.PAYMENT_PARTIALLY_AUTHORISED -> Pair(
            "Payment Partially Authorized",
            "Payment Partially Authorized"
        )

        MainViewModelStateType.PAYMENT_PROCESSING -> Pair("", "")
    }
}