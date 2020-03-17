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

    fun formattedCurrencyString(isLTR: Boolean): String {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(this.currencyCode)
        val minorUnit: Int = Currency.getInstance(currencyCode).defaultFractionDigits
        val orderAmount = orderValue / 10.00.pow(minorUnit)
        return if(isLTR) "$orderAmount $currencyCode" else "$currencyCode $orderAmount"
//        return format.format()
    }
}