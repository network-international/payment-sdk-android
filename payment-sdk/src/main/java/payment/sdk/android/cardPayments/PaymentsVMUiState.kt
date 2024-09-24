package payment.sdk.android.cardPayments

import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.visaInstalments.model.NewCardDto
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.CardType
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.VisaPlans

sealed class PaymentsVMUiState {
    data object Init : PaymentsVMUiState()

    data class Loading(val message: LoadingMessage) : PaymentsVMUiState()

    data class Authorized(
        val accessToken: String,
        val paymentCookie: String,
        val orderUrl: String,
        val supportedCards: Set<CardType>,
        val googlePayConfig: GooglePayConfig? = null,
        val showWallets: Boolean,
        val orderAmount: OrderAmount
    ) : PaymentsVMUiState()
}

sealed class PaymentsVMEffects {
    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : PaymentsVMEffects()
    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) : PaymentsVMEffects()
    data class InitiatePartialAuth(val partialAuthIntent: PartialAuthIntent) : PaymentsVMEffects()

    data object Captured : PaymentsVMEffects()
    data object PaymentAuthorised : PaymentsVMEffects()
    data object Purchased : PaymentsVMEffects()
    data object PostAuthReview : PaymentsVMEffects()

    data class Failed(val error: String) : PaymentsVMEffects()

    data class ShowVisaPlans(
        val visaPlans: VisaPlans,
        val paymentCookie: String,
        val accessToken: String,
        val orderUrl: String,
        val cvv: String?,
        val newCardDto: NewCardDto
    ) : PaymentsVMEffects()
}

data class GooglePayConfig(
    val allowedPaymentMethods: String,
    val canUseGooglePay: Boolean,
    val task: Task<PaymentData>
)