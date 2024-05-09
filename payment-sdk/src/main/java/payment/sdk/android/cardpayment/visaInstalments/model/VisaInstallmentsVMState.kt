package payment.sdk.android.cardpayment.visaInstalments.model

import payment.sdk.android.cardpayment.savedCard.SavedCardDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.cardpayment.widget.LoadingMessage

sealed class VisaInstallmentsVMState {
    object Init : VisaInstallmentsVMState()

    data class Loading(val message: LoadingMessage) : VisaInstallmentsVMState()

    data class PlanSelection(
        val installmentPlans: List<InstallmentPlan>,
        val paymentCookie: String,
        val accessToken: String,
        val orderUrl: String,
        val savedCardDto: SavedCardDto?,
        val newCardDto: NewCardDto?,
        val paymentUrl: String?,
        val savedCardUrl: String?,
        val selectedPlan: InstallmentPlan? = null,
        val isValid: Boolean = false,
    ) : VisaInstallmentsVMState()

    object Captured : VisaInstallmentsVMState()
    object PaymentAuthorised : VisaInstallmentsVMState()
    object Purchased : VisaInstallmentsVMState()
    object PostAuthReview : VisaInstallmentsVMState()

    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : VisaInstallmentsVMState()

    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) :
        VisaInstallmentsVMState()

    data class Failed(val error: String) : VisaInstallmentsVMState()
}