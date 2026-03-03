package payment.sdk.android.clicktopay

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import payment.sdk.android.clicktopay.model.ClickToPayEffect
import payment.sdk.android.clicktopay.model.ClickToPayState
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.ClickToPayApiInteractor
import payment.sdk.android.core.interactor.ClickToPayCard
import payment.sdk.android.core.interactor.ClickToPayConfigInteractor
import payment.sdk.android.core.interactor.ClickToPayGatewayConfig
import payment.sdk.android.core.interactor.ClickToPayPaymentResult
import payment.sdk.android.core.interactor.ValidationChannel
import payment.sdk.android.payments.requireApplication

/**
 * ViewModel for Click to Pay payment flow.
 * Manages the state of the Click to Pay checkout process including:
 * - SDK initialization
 * - Consumer lookup and card selection
 * - Identity validation (OTP)
 * - Checkout processing
 * - Order polling for PENDING state
 */
@Keep
internal class ClickToPayViewModel(
    private val config: ClickToPayLauncher.Config,
    private val clickToPayApiInteractor: ClickToPayApiInteractor,
    private val clickToPayConfigInteractor: ClickToPayConfigInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel(), ClickToPayJsCallback {

    private val _state = MutableStateFlow<ClickToPayState>(ClickToPayState.Loading())
    val state: StateFlow<ClickToPayState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ClickToPayEffect>()
    val effect = _effect.asSharedFlow()

    private var currentCards: List<ClickToPayCard> = emptyList()
    private var selectedCard: ClickToPayCard? = null
    private var lastCheckoutResponse: String? = null
    private var lastSrcDigitalCardId: String? = null

    /** VCTP gateway config (kid, publicKey, merchantConfig) */
    private var vctpConfig: ClickToPayGatewayConfig? = null

    /**
     * Fetch VCTP config from gateway for encryption keys and merchant config
     */
    fun fetchVctpConfig(onComplete: ((ClickToPayGatewayConfig?) -> Unit)? = null) {
        val outletId = config.outletId ?: return
        // Use pay page host for VCTP config (matching iOS behavior)
        val baseUrl = config.payPageUrl?.let { url ->
            try {
                val uri = android.net.Uri.parse(url)
                "${uri.scheme}://${uri.host}"
            } catch (e: Exception) {
                null
            }
        } ?: config.clickToPayUrl.substringBefore("/transactions/")
            .ifEmpty { config.clickToPayUrl.substringBefore("/api/") }
        val configUrl = "$baseUrl/api/outlets/$outletId/vctp/config"

        viewModelScope.launch(dispatcher) {
            val result = clickToPayConfigInteractor.getConfig(
                configUrl = configUrl,
                accessToken = config.accessToken,
                paymentCookie = config.paymentCookie
            )
            vctpConfig = result
            Log.d(TAG, "VCTP config fetched: kid=${result?.kid?.take(8)}, publicKey=${if (result?.publicKey != null) "present" else "null"}")
            onComplete?.invoke(result)
        }
    }

    /**
     * Get the configuration JSON for initializing the JavaScript SDK
     */
    fun getInitConfigJson(): String {
        return JSONObject().apply {
            put("sdkUrl", config.clickToPayConfig.getSdkUrl())
            put("dpaId", config.clickToPayConfig.dpaId)
            put("dpaClientId", config.clickToPayConfig.dpaClientId ?: JSONObject.NULL)
            put("dpaName", config.clickToPayConfig.dpaName)
            put("cardBrands", config.clickToPayConfig.getCardBrandsParam())
            put("amount", config.amount)
            put("currencyCode", config.currencyCode)
            put("orderReference", config.orderReference ?: JSONObject.NULL)
            put("merchantName", config.merchantName ?: config.clickToPayConfig.dpaName)
            put("locale", config.locale)
            // Add encryption keys from VCTP config
            vctpConfig?.kid?.let { put("kid", it) }
            vctpConfig?.publicKey?.let { put("publicKey", it) }
            // Add merchant config for acquirerBIN etc.
            vctpConfig?.merchantConfig?.let { mc ->
                put("merchantConfig", JSONObject(mc.toJsonString()))
            }
        }.toString()
    }

    /**
     * Select a card for checkout
     */
    fun selectCard(card: ClickToPayCard) {
        selectedCard = card
        _state.update { currentState ->
            if (currentState is ClickToPayState.CardsAvailable) {
                currentState.copy(selectedCard = card)
            } else {
                currentState
            }
        }
    }

    /**
     * Process the checkout after receiving response from JavaScript SDK
     */
    fun processCheckout() {
        val checkoutResponse = lastCheckoutResponse

        if (checkoutResponse == null) {
            viewModelScope.launch {
                _effect.emit(ClickToPayEffect.ShowError("Missing checkout response"))
            }
            return
        }

        _state.update { ClickToPayState.Processing("Completing payment...") }

        viewModelScope.launch(dispatcher) {
            // Use the unified-click-to-pay URL
            val unifiedUrl = config.getUnifiedClickToPayUrl()

            val result = clickToPayApiInteractor.submitClickToPayPayment(
                unifiedClickToPayUrl = unifiedUrl,
                checkoutResponse = checkoutResponse,
                srcDigitalCardId = lastSrcDigitalCardId,
                accessToken = config.accessToken,
                paymentCookie = config.paymentCookie
            )

            handlePaymentResult(result)
        }
    }

    /**
     * Poll order status for PENDING/AWAIT_3DS states
     */
    private fun pollOrderStatus() {
        val orderUrl = config.orderUrl
        if (orderUrl == null) {
            viewModelScope.launch {
                _effect.emit(ClickToPayEffect.ShowError("Order URL not available for polling"))
            }
            return
        }

        _state.update { ClickToPayState.Processing("Completing payment...") }

        viewModelScope.launch(dispatcher) {
            var retries = 0
            val maxRetries = 15
            val pollIntervalMs = 2000L

            while (retries < maxRetries) {
                delay(pollIntervalMs)
                retries++
                Log.d(TAG, "Polling order status, attempt $retries/$maxRetries")

                val result = clickToPayApiInteractor.getOrder(
                    orderUrl = orderUrl,
                    accessToken = config.accessToken
                )

                when (result) {
                    is ClickToPayPaymentResult.Pending -> {
                        // Continue polling
                        continue
                    }
                    else -> {
                        // Got a final state
                        handlePaymentResult(result)
                        return@launch
                    }
                }
            }

            // Exhausted retries
            _effect.emit(ClickToPayEffect.ShowError("Payment is still processing. Please check your order status."))
        }
    }

    private suspend fun handlePaymentResult(result: ClickToPayPaymentResult) {
        when (result) {
            is ClickToPayPaymentResult.Authorised -> {
                _effect.emit(ClickToPayEffect.PaymentAuthorised)
            }
            is ClickToPayPaymentResult.Captured -> {
                _effect.emit(ClickToPayEffect.PaymentCaptured)
            }
            is ClickToPayPaymentResult.Purchased -> {
                _effect.emit(ClickToPayEffect.PaymentSuccess)
            }
            is ClickToPayPaymentResult.PostAuthReview -> {
                _state.update { ClickToPayState.Success(lastCheckoutResponse ?: "") }
            }
            is ClickToPayPaymentResult.Pending -> {
                // Start polling
                pollOrderStatus()
            }
            is ClickToPayPaymentResult.Requires3DS -> {
                _effect.emit(
                    ClickToPayEffect.Requires3DS(
                        acsUrl = result.acsUrl,
                        acsPaReq = result.acsPaReq,
                        acsMd = result.acsMd
                    )
                )
            }
            is ClickToPayPaymentResult.Requires3DSTwo -> {
                _effect.emit(
                    ClickToPayEffect.Requires3DSTwo(
                        threeDSMethodUrl = result.threeDSMethodUrl,
                        threeDSServerTransId = result.threeDSServerTransId,
                        directoryServerId = result.directoryServerId,
                        threeDSMessageVersion = result.threeDSMessageVersion,
                        acsUrl = result.acsUrl,
                        threeDSTwoAuthenticationURL = result.threeDSTwoAuthenticationURL,
                        threeDSTwoChallengeResponseURL = result.threeDSTwoChallengeResponseURL,
                        outletRef = result.outletRef,
                        orderRef = result.orderRef,
                        paymentRef = result.paymentRef,
                        threeDSMethodData = result.threeDSMethodData,
                        threeDSMethodNotificationURL = result.threeDSMethodNotificationURL,
                        paymentCookie = config.paymentCookie,
                        orderUrl = config.orderUrl
                    )
                )
            }
            is ClickToPayPaymentResult.Failed -> {
                _state.update { ClickToPayState.Error(result.message) }
                _effect.emit(ClickToPayEffect.ShowError(result.message))
            }
        }
    }

    // ClickToPayJsCallback implementations

    override fun onSdkInitialized() {
        _state.update { ClickToPayState.LookingUpConsumer }
    }

    override fun onSdkInitError(reason: String, message: String) {
        _state.update { ClickToPayState.Error("SDK initialization failed: $message") }
        viewModelScope.launch {
            _effect.emit(ClickToPayEffect.ShowError("SDK initialization failed: $message"))
        }
    }

    override fun onCardsAvailable(cards: List<ClickToPayCard>) {
        currentCards = cards
        _state.update { ClickToPayState.CardsAvailable(cards = cards) }
    }

    override fun onIdentityValidationRequired(channels: List<ValidationChannel>) {
        _state.update { ClickToPayState.IdentityValidationRequired(validationChannels = channels) }
    }

    override fun onAddCardRequired() {
        _state.update { ClickToPayState.EnterNewCard }
    }

    override fun onOtpSent(maskedDestination: String) {
        _state.update { ClickToPayState.OtpSent(maskedDestination = maskedDestination) }
    }

    override fun onIdentityValidated() {
        _state.update { ClickToPayState.LookingUpConsumer }
    }

    override fun onCheckoutSuccess(checkoutResponse: String, srcDigitalCardId: String?, idToken: String?) {
        lastCheckoutResponse = checkoutResponse
        lastSrcDigitalCardId = srcDigitalCardId
        processCheckout()
    }

    override fun onError(reason: String, message: String) {
        _state.update { ClickToPayState.Error(message) }
        viewModelScope.launch {
            _effect.emit(ClickToPayEffect.ShowError(message))
        }
    }

    override fun onCanceled() {
        viewModelScope.launch {
            _effect.emit(ClickToPayEffect.Canceled)
        }
    }

    override fun onClose() {
        viewModelScope.launch {
            _effect.emit(ClickToPayEffect.Canceled)
        }
    }

    /**
     * Factory for creating ClickToPayViewModel
     */
    @Keep
    internal class Factory(private val config: ClickToPayLauncher.Config) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val httpClient = CoroutinesGatewayHttpClient()
            return ClickToPayViewModel(
                config = config,
                clickToPayApiInteractor = ClickToPayApiInteractor(
                    httpClient = httpClient,
                    app = extras.requireApplication()
                ),
                clickToPayConfigInteractor = ClickToPayConfigInteractor(
                    httpClient = httpClient
                )
            ) as T
        }
    }

    companion object {
        private const val TAG = "ClickToPayViewModel"
    }
}
