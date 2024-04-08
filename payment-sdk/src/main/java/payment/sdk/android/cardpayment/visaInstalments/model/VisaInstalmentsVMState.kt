package payment.sdk.android.cardpayment.visaInstalments.model

import payment.sdk.android.cardpayment.savedCard.SavedCardDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto

sealed class VisaInstalmentsVMState {
    object Init : VisaInstalmentsVMState()

    data class Loading(val message: String) : VisaInstalmentsVMState()

    data class PlanSelection(
        val installmentPlans: List<InstalmentPlan>,
        val paymentCookie: String,
        val orderUrl: String,
        val savedCardDto: SavedCardDto?,
        val newCardDto: NewCardDto?,
        val paymentUrl: String?,
        val savedCardUrl: String?,
        val selectedPlan: InstalmentPlan? = null,
        val isValid: Boolean = false,
    ) : VisaInstalmentsVMState()

    object Captured : VisaInstalmentsVMState()
    object PaymentAuthorised : VisaInstalmentsVMState()
    object Purchased : VisaInstalmentsVMState()
    object PostAuthReview : VisaInstalmentsVMState()

    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : VisaInstalmentsVMState()

    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) :
        VisaInstalmentsVMState()

    data class Failed(val error: String) : VisaInstalmentsVMState()
}