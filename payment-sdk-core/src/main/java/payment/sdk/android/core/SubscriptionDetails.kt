package payment.sdk.android.core;

import androidx.annotation.Keep

@Keep
data class SubscriptionDetails(
    val frequency: String,
    val startDate: String,
    val amount: String,
    val lastPaymentDate: String
)

@Keep
data class RecurringDetails(
    val numberOfTenure: Int,
    val recurringType: String,
    val startDate: String,
    val endDate: String,
    val recurringAmount: Order.Amount
)