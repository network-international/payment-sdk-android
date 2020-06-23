package payment.sdk.android.core

import com.google.gson.annotations.SerializedName


class Order {
    @SerializedName(value = "_links")
    var links: Links? = null
    var amount: Amount? = null
    var outletId: String? = null
    var reference: String? = null
    var paymentMethods: PaymentMethods? = null

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
    }

    class PaymentLinks {
        @SerializedName(value = "payment:samsung_pay")
        var samsungPayLink: Href? = null
    }

    class Href {
        var href: String? = null
    }

    class PaymentMethods {
        var card: List<String>? = null
        var wallet: Array<String>? = null
    }
}