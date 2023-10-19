package payment.sdk.android.core

import com.google.gson.annotations.SerializedName


class PaymentResponse {
    @SerializedName(value = "_links")
    var links: Links? = null

    var amount: Amount? = null
    var outletId: String? = null
    var reference: String? = null
    var orderReference: String? = null
    var paymentMethods: PaymentMethods? = null
    var authenticationCode: String? = null

    @SerializedName(value = "3ds2")
    var threeDSTwo: ThreeDSTwo? = null

    @SerializedName(value = "3ds")
    var threeDSOne: ThreeDSOne? = null

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

        @SerializedName(value = "cnp:3ds2-challenge-response")
        var threeDSChallengeResponseUrl: Href? = null

        @SerializedName(value = "cnp:3ds2-authentication")
        var threeDSAuthenticationsUrl: Href? = null

        @SerializedName(value = "cnp:3ds")
        var threeDSOneUrl: Href? = null
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

    class ThreeDSOne {
        @SerializedName(value = "acsUrl")
        var acsUrl: String? = null

        @SerializedName(value = "acsPaReq")
        var acsPaReq: String? = null

        @SerializedName(value = "acsMd")
        var acsMd: String? = null
    }

    class ThreeDSTwo {
        var messageVersion: String? = null
        var threeDSMethodURL: String? = null
        var threeDSServerTransID: String? = null
        var directoryServerID: String? = null
        var transStatus: String? = null
        var acsTransID: String? = null
        var acsReferenceNumber: String? = null
        var acsSignedContent: String? = null

    }

    fun isThreeDSecureTwo(): Boolean = threeDSTwo != null
}