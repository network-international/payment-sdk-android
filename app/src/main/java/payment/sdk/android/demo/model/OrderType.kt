package payment.sdk.android.demo.model

enum class OrderType(override val code: String, override val displayValue: String) : PickerItem {
    SINGLE("SINGLE", "Single"),
    RECURRING("RECURRING", "Recurring"),
    UNSCHEDULED("UNSCHEDULED", "Unscheduled"),
    INSTALLMENT("INSTALLMENT", "Installment")
}
