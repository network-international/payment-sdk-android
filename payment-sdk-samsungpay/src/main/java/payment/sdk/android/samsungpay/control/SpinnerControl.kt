package payment.sdk.android.samsungpay.control

class SpinnerControl(
        val controlId: String,
        val title: String,
        val spinnerType: SpinnerType
) : SamsungPayControl {

    private var selectedId: String? = null

    constructor(
            controlId: String,
            title: String,
            spinnerType: SpinnerType,
            selectedId: String
    ) : this(controlId, title, spinnerType) {
        this.selectedId = selectedId
    }

    private val items = mutableListOf<SpinnerItem>()

    fun addItem(id: String, title: String) {
        items.add(SpinnerItem(id, title))
    }

    fun getItems(): List<SpinnerItem> = items

    fun getSelectedId(): String? = selectedId

    data class SpinnerItem(val id: String, val text: String)

    enum class SpinnerType {
        SHIPPING_METHOD_SPINNER, INSTALLMENT_SPINNER
    }
}
