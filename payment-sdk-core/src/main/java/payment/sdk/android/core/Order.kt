package payment.sdk.android.core

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


class Order {
    @SerializedName(value = "_links")
    var links: Links? = null
    var amount: Amount? = null
    var outletId: String? = null
    var reference: String? = null
    var paymentMethods: PaymentMethods? = null

    var savedCardVisMatchedCandidates: SavedCardVisMatchedCandidates? = null

    var savedCard: SavedCard? = null

    @SerializedName(value = "_embedded")
    var embedded: Embedded? = null

    // Other classes
    class Amount {
        var currencyCode: String? = null
        var value: Double? = 0.0
    }

    class Links {
        @SerializedName(value = "payment-authorization")
        var paymentAuthorizationUrl: Href? = null

        @SerializedName(value = "payment")
        var paymentUrl: Href? = null
    }

    class Embedded {
        lateinit var payment: Array<Payment>
    }

    class Payment {
        @SerializedName(value = "_links")
        var links: PaymentLinks? = null
        var reference: String? = null
        var savedCard: SavedCard? = null
    }

    class PaymentLinks {
        @SerializedName(value = "payment:samsung_pay")
        var samsungPayLink: Href? = null

        @SerializedName(value = "payment:saved-card")
        var savedCard: Href? = null

        @SerializedName(value = "payment:card")
        var card: Href? = null

        @SerializedName(value = "self")
        var selfLink: Href? = null
    }

    class Href {
        var href: String? = null
    }

    class PaymentMethods {
        var card: List<String>? = null
        var wallet: Array<String>? = null
    }

    data class SavedCardVisMatchedCandidates(
        val matchedCandidates: List<MatchedCandidates> = listOf()
    )

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