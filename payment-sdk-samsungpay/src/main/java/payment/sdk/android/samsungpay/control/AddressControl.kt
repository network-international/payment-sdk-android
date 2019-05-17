package payment.sdk.android.samsungpay.control

class AddressControl(
        val controlId: String,
        val addressType: AddressType,
        val title: String,
        val address: Address
) : SamsungPayControl {

    enum class AddressType {
        BILLING_ADDRESS, SHIPPING_ADDRESS
    }

    data class Address(
            val addressee: String?,
            val addressLine1: String,
            val addressLine2: String,
            val city: String,
            val state: String,
            val countryCode: String,
            val postalCode: String,
            val phoneNumber: String,
            val email: String
    )
}