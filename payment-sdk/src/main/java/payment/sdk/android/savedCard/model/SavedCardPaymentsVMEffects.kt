package payment.sdk.android.savedCard.model

import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto

internal sealed class SavedCardPaymentsVMEffects {
    data object Captured : SavedCardPaymentsVMEffects()
    data object PaymentAuthorised : SavedCardPaymentsVMEffects()
    data object Purchased : SavedCardPaymentsVMEffects()
    data object PostAuthReview : SavedCardPaymentsVMEffects()

    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : SavedCardPaymentsVMEffects()

    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) :
        SavedCardPaymentsVMEffects()

    data class Failed(val error: String) : SavedCardPaymentsVMEffects()
}