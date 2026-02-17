package payment.sdk.android.payments

import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.CardType
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.interactor.MakeCardPaymentRequest
import payment.sdk.android.payments.model.PaymentResultArgs
import payment.sdk.android.sdk.R

sealed class UnifiedPaymentPageVMUiState(val title: Int, val enableBackButton: Boolean = true) {
    data object Init : UnifiedPaymentPageVMUiState(R.string.make_payment)

    data class Loading(val message: LoadingMessage) : UnifiedPaymentPageVMUiState(R.string.make_payment)

    data class Authorized(
        val accessToken: String,
        val paymentCookie: String,
        val orderUrl: String,
        val supportedCards: Set<CardType>,
        val googlePayUiConfig: GooglePayUiConfig? = null,
        val aaniConfig: AaniPayLauncher.Config? = null,
        val clickToPayConfig: ClickToPayLauncher.Config? = null,
        val isSamsungPayAvailable: Boolean = false,
        val showWallets: Boolean,
        val selfUrl: String,
        val orderAmount: String,
        val cardPaymentUrl: String,
        val amount: Double,
        val currencyCode: String,
        val locale: String,
        val payerIp: String
    ) : UnifiedPaymentPageVMUiState(R.string.make_payment)

    data class ShowVisaPlans(
        val makeCardPaymentRequest: MakeCardPaymentRequest,
        val visaPlans: VisaPlans,
        val orderUrl: String,
        val orderAmount: OrderAmount
    ) : UnifiedPaymentPageVMUiState(R.string.title_activity_visa_instalments)

    data class InitiatePartialAuth(val partialAuthIntent: PartialAuthIntent) :
        UnifiedPaymentPageVMUiState(R.string.paypage_title_awaiting_partial_auth_approval, enableBackButton = false)

    data class ShowPaymentResult(
        val args: PaymentResultArgs,
        val pendingResult: UnifiedPaymentPageResult
    ) : UnifiedPaymentPageVMUiState(R.string.make_payment, enableBackButton = false)
}

sealed class UnifiedPaymentPageVMEffects {
    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : UnifiedPaymentPageVMEffects()
    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) : UnifiedPaymentPageVMEffects()

    data object Captured : UnifiedPaymentPageVMEffects()
    data object PaymentAuthorised : UnifiedPaymentPageVMEffects()
    data object Purchased : UnifiedPaymentPageVMEffects()
    data object PostAuthReview : UnifiedPaymentPageVMEffects()

    data class Failed(val error: String) : UnifiedPaymentPageVMEffects()
}

data class GooglePayUiConfig(
    val allowedPaymentMethods: String,
    val googlePayAcceptUrl: String,
    val canUseGooglePay: Boolean,
    val task: Task<PaymentData>
)