package payment.sdk.android.core.interactor

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import payment.sdk.android.core.Order

@Keep
data class AuthResponseBody(
    @SerializedName(value = "_links") val links: Links?
)

@Keep
data class Links(
    @SerializedName(value = "cnp:order")
    val orderRef: Order.Href?,
)