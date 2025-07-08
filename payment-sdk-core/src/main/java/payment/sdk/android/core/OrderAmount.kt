package payment.sdk.android.core

import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

class OrderAmount(private val orderValue: Double, private val currencyCode: String) {

    fun formattedCurrencyString(isLTR: Boolean): String {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(this.currencyCode)
        val minorUnit: Int = Currency.getInstance(currencyCode).defaultFractionDigits
        val orderAmount = orderValue / 10.00.pow(minorUnit)
        return if (isLTR) "$orderAmount $currencyCode" else "$currencyCode $orderAmount"
    }

    fun formattedCurrencyString2Decimal(isLTR: Boolean): String {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(this.currencyCode)
        val minorUnit: Int = Currency.getInstance(currencyCode).defaultFractionDigits
        val orderAmount = orderValue / 10.00.pow(minorUnit)
        val orderAmountFormatted = String.format(Locale.ENGLISH, "%.2f", orderAmount)
        return if (isLTR) "$orderAmountFormatted $currencyCode" else "$currencyCode $orderAmountFormatted"
    }

    fun isRiyalCurrency(): Boolean {
        return currencyCode == "SAR"
    }

    fun getOrderValue(): String {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(this.currencyCode)
        val minorUnit: Int = Currency.getInstance(currencyCode).defaultFractionDigits
        val orderAmount = orderValue / 10.00.pow(minorUnit)
        val orderAmountFormatted = String.format(Locale.ENGLISH, "%.2f", orderAmount)
        return orderAmountFormatted
    }
}