package payment.sdk.android.clicktopay.model

import payment.sdk.android.core.interactor.ClickToPayCard
import payment.sdk.android.core.interactor.ValidationChannel

/**
 * UI State for Click to Pay flow
 */
sealed class ClickToPayState {
    /**
     * Initial loading state - SDK is being initialized
     */
    data class Loading(val message: String = "Initializing Click to Pay...") : ClickToPayState()

    /**
     * Consumer lookup state - checking if consumer is recognized
     */
    object LookingUpConsumer : ClickToPayState()

    /**
     * Consumer has saved cards available
     */
    data class CardsAvailable(
        val cards: List<ClickToPayCard>,
        val selectedCard: ClickToPayCard? = null
    ) : ClickToPayState()

    /**
     * Identity validation is required (OTP)
     */
    data class IdentityValidationRequired(
        val validationChannels: List<ValidationChannel>,
        val selectedChannel: ValidationChannel? = null
    ) : ClickToPayState()

    /**
     * OTP has been sent, waiting for user input
     */
    data class OtpSent(
        val maskedDestination: String,
        val otpValue: String = ""
    ) : ClickToPayState()

    /**
     * No cards found, user needs to enter card details
     */
    object EnterNewCard : ClickToPayState()

    /**
     * Processing the checkout
     */
    data class Processing(val message: String = "Processing payment...") : ClickToPayState()

    /**
     * Checkout completed successfully
     */
    data class Success(val checkoutResponse: String) : ClickToPayState()

    /**
     * Error state
     */
    data class Error(val message: String) : ClickToPayState()
}

/**
 * Side effects for Click to Pay that need to be handled once
 */
sealed class ClickToPayEffect {
    data class ShowError(val message: String) : ClickToPayEffect()
    data object PaymentSuccess : ClickToPayEffect()
    data object PaymentAuthorised : ClickToPayEffect()
    data object PaymentCaptured : ClickToPayEffect()
    data object Canceled : ClickToPayEffect()
    data class Requires3DS(
        val acsUrl: String,
        val acsPaReq: String,
        val acsMd: String
    ) : ClickToPayEffect()
    data class Requires3DSTwo(
        val threeDSMethodUrl: String?,
        val threeDSServerTransId: String?,
        val directoryServerId: String?,
        val threeDSMessageVersion: String?,
        val acsUrl: String?,
        val threeDSTwoAuthenticationURL: String?,
        val threeDSTwoChallengeResponseURL: String?,
        val outletRef: String?,
        val orderRef: String?,
        val paymentRef: String?,
        val threeDSMethodData: String?,
        val threeDSMethodNotificationURL: String?,
        val paymentCookie: String,
        val orderUrl: String?
    ) : ClickToPayEffect()
    data object PaymentPending : ClickToPayEffect()
}
