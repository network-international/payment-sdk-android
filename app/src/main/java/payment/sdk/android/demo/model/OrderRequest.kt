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
    var installmentDetails: InstallmentDetails? = null,
    var planReference: String? = null,
    var transactionType: String? = null,
    var tenure: Int? = 0,
    var total: PaymentOrderAmount? = null,
    var orderStartDate: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var paymentAttempts: Int? = 0,
    var invoiceExpiryDate: String? = null,
    var skipInvoiceCreatedEmailNotification: Boolean? = false,
    var notifyPayByLink: Boolean? = false,
    var paymentStructure: String? = null,
    var initialInstallmentAmount: Int? = 0,
    var initialPeriodLength: Int? = 0
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

    planReference?.let { bodyMap["planReference"] = it }

    transactionType?.let { bodyMap["transactionType"] = it }

    tenure?.let { bodyMap["tenure"] = it }

    total?.let {
        bodyMap["total"] = mapOf(
            "currencyCode" to it.currencyCode,
            "value" to it.value
        )
    }

    orderStartDate?.let { bodyMap["orderStartDate"] = it }

    invoiceExpiryDate?.let { bodyMap["invoiceExpiryDate"] = it }

    firstName?.let { bodyMap["firstName"] = it }

    lastName?.let { bodyMap["lastName"] = it }

    email?.let { bodyMap["email"] = it }

    paymentAttempts?.let { bodyMap["paymentAttempts"] = it }

    skipInvoiceCreatedEmailNotification?.let {
        bodyMap["skipInvoiceCreatedEmailNotification"] = it
    }

    notifyPayByLink?.let { bodyMap["notifyPayByLink"] = it }

    paymentStructure?.let { bodyMap["paymentStructure"] = it }

    initialInstallmentAmount?.let {
        bodyMap["initialInstallmentAmount"] = it
    }

    initialPeriodLength?.let {
        bodyMap["initialPeriodLength"] = it
    }

    return bodyMap
}