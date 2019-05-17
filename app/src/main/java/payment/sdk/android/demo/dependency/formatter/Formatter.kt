package payment.sdk.android.demo.dependency.formatter

import java.math.BigDecimal
import java.util.*


interface Formatter {

    fun formatAmount(currency: Currency, amount: BigDecimal, locale: Locale): String
}