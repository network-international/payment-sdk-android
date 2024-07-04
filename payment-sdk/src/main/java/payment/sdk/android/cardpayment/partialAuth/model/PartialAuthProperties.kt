package payment.sdk.android.cardpayment.partialAuth.model

data class PartialAuthProperties(
    val bankName: String?,
    val approvedAmount: String,
    val fullAmount: String
)