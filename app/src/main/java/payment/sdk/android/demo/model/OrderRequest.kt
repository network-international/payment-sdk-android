package payment.sdk.android.demo.model

import payment.sdk.android.core.SavedCard

data class OrderRequest(
    val action: String,
    val amount: PaymentOrderAmount,
    val language: String = "en",
    val description: String = "Android Demo App",
    val merchantAttributes: Map<String, Any> = mapOf(),
    val savedCard: SavedCard? = null,
    var type: String? = null,
    var frequency: String? = null,
    var recurringDetails: RecurringDetails? = null,
    var installmentDetails: InstallmentDetails? = null
)

data class PaymentOrderAmount(
    val value: Double,
    val currencyCode: String
)

fun OrderRequest.toMap(): MutableMap<String, Any> {
    val bodyMap = mutableMapOf(
        "action" to action,
        "amount" to mapOf(
            "currencyCode" to amount.currencyCode,
            "value" to amount.value * 100
        ),
        "language" to language,
        "description" to description
    )
    if (merchantAttributes.isNotEmpty()) {
        bodyMap["merchantAttributes"] = merchantAttributes
    }
    if (!type.isNullOrEmpty()) {
        bodyMap["type"] = type!!
    }
    if (!frequency.isNullOrEmpty()) {
        bodyMap["frequency"] = frequency!!
    }
    recurringDetails?.let {
        bodyMap["recurringDetails"] = mapOf(
            "recurringType" to it.recurringType,
            "numberOfTenure" to it.numberOfTenure
        )
    }
    installmentDetails?.let {
        bodyMap["installmentDetails"] = mapOf(
            "numberOfTenure" to it.numberOfTenure
        )
    }
    savedCard?.let {
        bodyMap["savedCard"] = mapOf(
            "maskedPan" to it.maskedPan,
            "expiry" to it.expiry,
            "cardholderName" to it.cardholderName,
            "scheme" to it.scheme,
            "cardToken" to it.cardToken,
            "recaptureCsc" to it.recaptureCsc
        )
    }
    return bodyMap
}