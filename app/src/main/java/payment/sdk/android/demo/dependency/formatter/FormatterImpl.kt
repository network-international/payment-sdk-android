package payment.sdk.android.demo.dependency.formatter

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

class FormatterImpl @Inject constructor() : Formatter {

    override fun formatAmount(currency: Currency, amount: BigDecimal, locale: Locale): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = currency
        return format.format(amount)
    }
}
