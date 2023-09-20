package payment.sdk.android.core

import com.google.gson.annotations.SerializedName


class Order {
    @SerializedName(value = "_links")
    var links: Links? = null
    var amount: Amount? = null
    var outletId: String? = null
    var reference: String? = null
    var paymentMethods: PaymentMethods? = null

    var savedCard: SavedCard? = null

    @SerializedName(value = "_embedded")
    var embedded: Embedded? = null

    // Other classes
    class Amount {
        var currencyCode: String? = null
        var value: Int? = 0
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
    }

    class Href {
        var href: String? = null
    }

    class PaymentMethods {
        var card: List<String>? = null
        var wallet: Array<String>? = null
    }
}