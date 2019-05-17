package payment.sdk.android.demo.basket.data

import java.math.BigDecimal
import java.util.*

/**
 * Stateful basket items price details
 */
class AmountDetails {

    private val items: MutableList<AmountItem> = mutableListOf()
    private var totalAmount: BigDecimal = BigDecimal.ZERO
    private var currency: Currency = Currency.getInstance(Locale.UK)

    fun getItems(): List<AmountItem> = items

    fun getCurrency(): Currency = currency

    fun getTotalAmount(): BigDecimal = totalAmount

    fun addItem(id: String, title: String, price: BigDecimal, optionalAmountText: String = "") {
        items.find { it.id == id }?.let {
            items.remove(it)
        }
        items.add(AmountItem(id, title, price, optionalAmountText))
    }

    fun setTotal(totalAmount: BigDecimal, currency: Currency) {
        this.totalAmount = totalAmount
        this.currency = currency

    }

    fun reset() {
        items.clear()
        totalAmount = BigDecimal.ZERO
        currency = Currency.getInstance(Locale.US)
    }

    data class AmountItem internal constructor(
            val id: String,
            val title: String,
            val amount: BigDecimal,
            val optionalAmountText: String
    )
}
