package payment.sdk.android.payments

import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.CardType
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.interactor.MakeCardPaymentRequest
import payment.sdk.android.sdk.R

sealed class PaymentsVMUiState(val title: Int, val enableBackButton: Boolean = true) {
    data object Init : PaymentsVMUiState(R.string.make_payment)

    data class Loading(val message: LoadingMessage) : PaymentsVMUiState(R.string.make_payment)

    data class Authorized(
        val accessToken: String,
        val paymentCookie: String,
        val orderUrl: String,
        val supportedCards: Set<CardType>,
        val googlePayUiConfig: GooglePayUiConfig? = null,
        val aaniConfig: AaniPayLauncher.Config? = null,
        val showWallets: Boolean,
        val selfUrl: String,
        val orderAmount: String,
        val cardPaymentUrl: String,
        val amount: Double,
        val currencyCode: String,
        val locale: String,
        val payerIp: String
    ) : PaymentsVMUiState(R.string.make_payment)

    data class ShowVisaPlans(
        val makeCardPaymentRequest: MakeCardPaymentRequest,
        val visaPlans: VisaPlans,
        val orderUrl: String,
        val orderAmount: OrderAmount
    ) : PaymentsVMUiState(R.string.title_activity_visa_instalments)

    data class InitiatePartialAuth(val partialAuthIntent: PartialAuthIntent) :
        PaymentsVMUiState(R.string.paypage_title_awaiting_partial_auth_approval, enableBackButton = false)
}

sealed class PaymentsVMEffects {
    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : PaymentsVMEffects()
    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) : PaymentsVMEffects()

    data object Captured : PaymentsVMEffects()
    data object PaymentAuthorised : PaymentsVMEffects()
    data object Purchased : PaymentsVMEffects()
    data object PostAuthReview : PaymentsVMEffects()

    data class Failed(val error: String) : PaymentsVMEffects()
}

data class GooglePayUiConfig(
    val allowedPaymentMethods: String,
    val googlePayAcceptUrl: String,
    val canUseGooglePay: Boolean,
    val task: Task<PaymentData>
)