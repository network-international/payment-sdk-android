package payment.sdk.android.core.interactor

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * Response models for Click to Pay SDK operations
 */

/**
 * Consumer identity used for Click to Pay recognition
 */
@Keep
@Parcelize
data class ConsumerIdentity(
    val identityType: IdentityType,
    val identityValue: String
) : Parcelable {
    enum class IdentityType {
        EMAIL,
        MOBILE_NUMBER
    }
}

/**
 * Digital card information returned from Click to Pay
 */
@Keep
@Parcelize
data class ClickToPayCard(
    val srcDigitalCardId: String,
    val panLastFour: String,
    val digitalCardData: DigitalCardData,
    val panExpirationMonth: String?,
    val panExpirationYear: String?,
    val paymentCardDescriptor: String?,
    val paymentCardType: String?,
    val paymentCardNetwork: String?
) : Parcelable

@Keep
@Parcelize
data class DigitalCardData(
    val descriptorName: String?,
    val artUri: String?,
    val artHeight: Int?,
    val artWidth: Int?
) : Parcelable

/**
 * Checkout response from Click to Pay
 */
@Keep
@Parcelize
data class ClickToPayCheckoutResponse(
    val checkoutResponse: String,  // JWS signed response
    val idToken: String?,
    val bindingStatus: String?
) : Parcelable

/**
 * Click to Pay action codes returned by the SDK
 */
@Keep
enum class ClickToPayActionCode {
    SUCCESS,
    PENDING_CONSUMER_IDV,  // Identity validation required
    ADD_CARD,              // No cards found, add new card
    ERROR
}

/**
 * Validation channel for OTP delivery
 */
@Keep
@Parcelize
data class ValidationChannel(
    val id: String,
    val type: String,  // EMAIL or MOBILE_NUMBER
    val maskedValue: String
) : Parcelable

/**
 * Click to Pay SDK response wrapper
 */
@Keep
sealed class ClickToPaySdkResponse : Parcelable {
    @Parcelize
    data class CardsAvailable(
        val cards: List<ClickToPayCard>,
        val consumerPresent: Boolean
    ) : ClickToPaySdkResponse()

    @Parcelize
    data class IdentityValidationRequired(
        val validationChannels: List<ValidationChannel>
    ) : ClickToPaySdkResponse()

    @Parcelize
    object AddCardRequired : ClickToPaySdkResponse()

    @Parcelize
    data class CheckoutSuccess(
        val response: ClickToPayCheckoutResponse
    ) : ClickToPaySdkResponse()

    @Parcelize
    data class Error(
        val reason: String,
        val message: String
    ) : ClickToPaySdkResponse()
}
