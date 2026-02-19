package payment.sdk.android.util

import android.view.View
import androidx.core.text.TextUtilsCompat
import payment.sdk.android.core.Order
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.SubscriptionDetails
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SubscriptionUtils {

    fun getSubscriptionDetails(order: Order): SubscriptionDetails? {
        if (!isSubscriptionOrder(order) || order.type != "RECURRING") return null
        return SubscriptionDetails(
            frequency = order.frequency.orEmpty(),
            startDate = formatSimpleDate(order.recurringDetails?.startDate!!),
            lastPaymentDate = formatSimpleDate(order.recurringDetails?.endDate!!),
            amount = getSubscriptionAmount(order.recurringDetails?.recurringAmount)
        )
    }

    private fun getSubscriptionAmount(amount: Order.Amount?): String {
        val isLTR =
            TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR
        val orderAmount = OrderAmount(
            amount?.value ?: return "",
            amount.currencyCode.orEmpty()
        )
        return orderAmount.formattedCurrencyString2Decimal(isLTR)
    }

    fun isSubscriptionOrder(order: Order): Boolean {
        return (order.type == "RECURRING" || order.type == "INSTALLMENT") &&
                order.merchantAttributes["paymentModel"] == "subscription"
    }

    fun formatSimpleDate(input: String): String {
        return try {
            val normalizedInput = if (input.contains(".")) {
                input.substringBefore(".") + "Z"
            } else {
                input
            }

            val inputFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val outputFormat = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )

            val date = inputFormat.parse(normalizedInput)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            input
        }
    }
}
