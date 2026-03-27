package payment.sdk.android.demo.model

enum class OrderAction(override val code: String, override val displayValue: String) : PickerItem {
    AUTH("AUTH", "Auth"),
    SALE("SALE", "Sale"),
    PURCHASE("PURCHASE", "Purchase")
}
