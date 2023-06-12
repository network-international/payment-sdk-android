package payment.sdk.android.demo.basket.data
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CreateOrderResponseDto (
        @SerializedName("reference") val reference : String,
        @SerializedName("_links") val paymentLinks : PaymentLinks,
        @SerializedName("paymentMethods") val paymentMethods : PaymentMethods
)

@Keep
data class PaymentLinks (
        @SerializedName("payment") val payment : Href,
        @SerializedName("payment-authorization") val paymentAuthorization : Href
)

@Keep
data class Href (
        @SerializedName("href") val href: String
)

@Keep
data class PaymentMethods (
        @SerializedName("card") val card : List<String>
)

