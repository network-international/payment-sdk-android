package payment.sdk.android.cardpayment.aaniPay.model

data class AaniPayRequest(
    val aliasType: String,
    val mobileNumber: MobileNumber? = null,
    val emiratesId: String? = null,
    val passportId: String? = null,
    val emailId: String? = null,
    val source: String,
    val backLink: String,
    val payerIp: String
)

data class MobileNumber(
    val countryCode: String,
    val number: String
)
