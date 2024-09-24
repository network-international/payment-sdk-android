package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
class PaymentResponse {
    @SerializedName(value = "_links")
    var links: Links? = null

    var amount: Amount? = null
    var outletId: String? = null
    var reference: String? = null
    var orderReference: String? = null
    var paymentMethods: PaymentMethods? = null
    var authenticationCode: String? = null

    var paymentMethod: PaymentMethod? = null

    @SerializedName(value = "3ds2")
    var threeDSTwo: ThreeDSTwo? = null

    @SerializedName(value = "3ds")
    var threeDSOne: ThreeDSOne? = null

    @SerializedName(value = "_embedded")
    var embedded: Embedded? = null

    var authResponse: AuthResponse? = null

    var state: String? = null
    // Other classes
    @Keep
    class Amount {
        var currencyCode: String? = null
        var value: Int? = 0
    }

    @Keep
    class Links {
        @SerializedName(value = "payment:partial-auth-accept")
        var partialAuthAccept: Href? = null

        @SerializedName(value = "payment:partial-auth-decline")
        var partialAuthDecline: Href? = null

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

    @Keep
    class Embedded {
        lateinit var payment: Array<Payment>
    }

    @Keep
    class Payment {
        @SerializedName(value = "_links")
        var links: PaymentLinks? = null
    }

    @Keep
    class PaymentLinks {
        @SerializedName(value = "payment:samsung_pay")
        var samsungPayLink: Href? = null
    }

    @Keep
    class Href {
        var href: String? = null
    }

    @Keep
    class PaymentMethods {
        var card: List<String>? = null
        var wallet: Array<String>? = null
    }

    @Keep
    class ThreeDSOne {
        @SerializedName(value = "acsUrl")
        var acsUrl: String? = null

        @SerializedName(value = "acsPaReq")
        var acsPaReq: String? = null

        @SerializedName(value = "acsMd")
        var acsMd: String? = null

        var summaryText: String? = null
    }

    @Keep
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