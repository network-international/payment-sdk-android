package payment.sdk.android.demo.basket.data
import com.google.gson.annotations.SerializedName


data class CreateOrderResponseDto (
        @SerializedName("reference") val reference : String,
        @SerializedName("_links") val paymentLinks : PaymentLinks,
        @SerializedName("paymentMethods") val paymentMethods : PaymentMethods
)

data class PaymentLinks (
        @SerializedName("payment") val payment : Href,
        @SerializedName("payment-authorization") val paymentAuthorization : Href
)

data class Href (
        @SerializedName("href") val href: String
)

data class PaymentMethods (
        @SerializedName("card") val card : List<String>
)

