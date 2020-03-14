package payment.sdk.android.core

import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

class OrderAmount {
    private val orderValue: Double
    private val currencyCode: String

    constructor(orderValue : Double, currencyCode: String) {
        this.orderValue = orderValue
        this.currencyCode = currencyCode
    }

    fun formattedCurrencyString(): String {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(this.currencyCode)
        val minorUnit: Int = Currency.getInstance(currencyCode).defaultFractionDigits
        val orderAmount = orderValue / 10.00.pow(minorUnit)
        return "$orderAmount $currencyCode"
//        return format.format()
    }
}