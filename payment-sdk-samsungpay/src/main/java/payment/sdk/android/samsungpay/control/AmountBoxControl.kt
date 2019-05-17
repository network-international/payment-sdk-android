package payment.sdk.android.samsungpay.control

import java.math.BigDecimal

class AmountBoxControl(
        val totalAmount: BigDecimal = BigDecimal.ZERO,
        val currency: String
) : SamsungPayControl {
    private val items: MutableList<AmountItem> = mutableListOf()

    fun addItem(id: String, title: String, price: BigDecimal, optionalPriceText: String = "") {
        items.add(AmountItem(id, title, price, optionalPriceText))
    }

    fun getItems(): List<AmountItem> = items

    data class AmountItem(val id: String, val title: String, val price: BigDecimal, val optionalPriceText: String = "")

    companion object {
        const val CONTROL_ID = "AmountBoxControl"
    }
}