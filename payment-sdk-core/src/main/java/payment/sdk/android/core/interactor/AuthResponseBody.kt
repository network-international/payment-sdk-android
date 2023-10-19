package payment.sdk.android.core.interactor
import com.google.gson.annotations.SerializedName
import payment.sdk.android.core.Order

data class AuthResponseBody(
    @SerializedName(value = "_links") val links: Links?
)

data class Links(
    @SerializedName(value = "cnp:order")
    val orderRef: Order.Href?,
)