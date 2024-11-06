package payment.sdk.android.core

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
class Order {
    @SerializedName(value = "_links")
    var links: Links? = null
    var amount: Amount? = null
    var outletId: String? = null
    var reference: String? = null
    var paymentMethods: PaymentMethods? = null
    var language: String = "en"

    @SerializedName(value = "visSavedCardMatchedCandidates")
    var savedCardVisMatchedCandidates: SavedCardVisMatchedCandidates? = null

    var savedCard: SavedCard? = null

    var formattedAmount: String? = null

    @SerializedName(value = "_embedded")
    var embedded: Embedded? = null

    // Other classes
    @Keep
    class Amount {
        var currencyCode: String? = null
        var value: Double? = 0.0
    }

    @Keep
    class Links {
        @SerializedName(value = "payment-authorization")
        var paymentAuthorizationUrl: Href? = null

        @SerializedName(value = "payment")
        var paymentUrl: Href? = null
    }

    @Keep
    class Embedded {
        lateinit var payment: Array<Payment>
    }

    @Keep
    class Payment {
        @SerializedName(value = "_links")
        var links: PaymentLinks? = null
        var reference: String? = null
        var savedCard: SavedCard? = null
        var paymentMethod: PaymentMethod? = null
        var state: String? = null
        var amount: Order.Amount? = null
        var authResponse: AuthResponse? = null
    }

    @Keep
    class PaymentLinks {
        @SerializedName(value = "payment:samsung_pay")
        var samsungPayLink: Href? = null

        @SerializedName(value = "payment:saved-card")
        var savedCard: Href? = null

        @SerializedName(value = "payment:card")
        var card: Href? = null

        @SerializedName(value = "self")
        var selfLink: Href? = null

        @SerializedName(value = "payment:google_pay")
        var googlePayLink: Href? = null

        @SerializedName(value = "config:google_pay")
        var googlePayConfigLink: Href? = null

        @SerializedName(value = "payment:partial-auth-accept")
        var partialAuthAccept: Href? = null

        @SerializedName(value = "payment:partial-auth-decline")
        var partialAuthDecline: Href? = null

        @SerializedName(value = "payment:aani")
        var aaniPayment: Href? = null
    }

    @Keep
    class Href {
        var href: String? = null
    }

    @Keep
    class PaymentMethods {
        var card: List<String>? = null
        var wallet: Array<String>? = null
        var apm: Array<String>? = null
    }

    @Keep
    data class SavedCardVisMatchedCandidates(
        val matchedCandidates: List<MatchedCandidates> = listOf()
    )

    @Keep
    @Parcelize
    data class MatchedCandidates(
        val cardToken: String?,
        val eligibilityStatus: String?
    ): Parcelable {
        companion object {
            const val MATCHED_CANDIDATES_ELIGIBLE = "MATCHED"
        }
    }
}

fun Order.getAuthorizationUrl() = links?.paymentAuthorizationUrl?.href

fun Order.getPayPageUrl() = links?.paymentUrl?.href

fun Order.getGooglePayUrl() = embedded?.payment?.firstOrNull()?.links?.googlePayLink?.href

fun Order.getGooglePayConfigUrl() = embedded?.payment?.firstOrNull()?.links?.googlePayConfigLink?.href

fun Order.getCardPaymentUrl() = embedded?.payment?.firstOrNull()?.links?.card?.href

fun Order.getSelfUrl() = embedded?.payment?.firstOrNull()?.links?.selfLink?.href

fun Order.getAaniPayLink() = embedded?.payment?.firstOrNull()?.links?.aaniPayment?.href

fun Order.getSavedCardPaymentUrl() = embedded?.payment?.firstOrNull()?.links?.savedCard?.href
