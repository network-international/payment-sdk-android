package payment.sdk.android.cardpayment

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import org.json.JSONObject
import payment.sdk.android.cardpayment.card.CardDetector
import payment.sdk.android.cardpayment.card.CardFace
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.cardpayment.card.SpacingPatterns
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureRequest
import payment.sdk.android.cardpayment.threedsecuretwo.webview.toIntent
import payment.sdk.android.cardpayment.validation.InputValidationError
import payment.sdk.android.cardpayment.validation.InputValidationError.INVALID_CARD_HOLDER
import payment.sdk.android.cardpayment.validation.Luhn
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.widget.DateFormatter
import payment.sdk.android.cardpayment.widget.NumericMaskInputFilter
import payment.sdk.android.core.CardType
import payment.sdk.android.core.Order
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.dependency.StringResources
import payment.sdk.android.core.interactor.VisaRequest
import payment.sdk.android.sdk.R

internal class CardPaymentPresenter(
        private val url: String,
        private val code: String,
        private val view: CardPaymentContract.View,
        private val interactions: CardPaymentContract.Interactions,
        private val paymentApiInteractor: PaymentApiInteractor,
        private val stringResources: StringResources
) : CardPaymentContract.Presenter {

    private var paymentCard: PaymentCard? = null

    private lateinit var orderReference: String
    private lateinit var outletRef: String
    private lateinit var paymentUrl: String
    private lateinit var paymentCookie: String
    private lateinit var orderAmount: OrderAmount
    private lateinit var orderUrl: String
    private lateinit var paymentRef: String
    private lateinit var selfLink: String
    private lateinit var payPageUrl: String

    private var visRequest: VisaRequest? = null

    @VisibleForTesting
    internal var supportedCards: Set<CardType> = emptySet()

    private val cardDetector: CardDetector by lazy {
        CardDetector(supportedCards)
    }

    init {
        view.setPresenter(this)
    }

    override fun onCardNumberFocusGained() {
        view.setFloatingHintTextVisible(true)
        view.setFloatingHintText(stringResources.getString(CARD_NUMBER_LABEL_RESOURCE))
    }

    override fun onExpireDateFocusGained() {
        view.setFloatingHintTextVisible(true)
        view.setFloatingHintText(stringResources.getString(CARD_EXPIRY_LABEL_RESOURCE))
    }

    override fun onCvvFocusGained() {
        view.setFloatingHintTextVisible(true)
        view.setFloatingHintText(stringResources.getString(CVV_LABEL_RESOURCE))
        paymentCard?.let { card ->
            val maskBuilder = StringBuilder()
            for (i in 1..card.cvv.length) maskBuilder.append(NumericMaskInputFilter.MASK_CHAR)
            view.updateCvvInputMask(maskBuilder.toString())
            when (card.cvv.face) {
                CardFace.BACK -> view.showCardBackFace()
                CardFace.FRONT -> view.showFrontCvvGuide(true)
            }
        }
    }

    override fun onCardNumberFocusLost() {
        view.setFloatingHintTextVisible(false)
    }

    override fun onExpireDateFocusLost() {
        view.setFloatingHintTextVisible(false)
    }

    override fun onCvvFocusLost() {
        view.setFloatingHintTextVisible(false)
        paymentCard?.let { card ->
            when (card.cvv.face) {
                CardFace.BACK -> view.showCardFrontFace()
                CardFace.FRONT -> view.showFrontCvvGuide(false)
            }
        }
    }

    override fun onCardNumberChanged(rawText: String, maskedText: String) {
        if (rawText.isEmpty()) {
            onEmptyCardNumber()
        } else {
            paymentCard = cardDetector.detect(rawText)
            paymentCard?.let { card ->
                view.setCardNumberPreviewText(maskedText)
                view.updateCardInputMask(card.binRange.length.pattern)
                view.updateCardLogo(getCardLogo(card))

                if (view.cardNumber.full) {
                    val valid = onValidateInputs()
                    if (valid) {
                        view.focusInCardExpire()
                    }
                }
            } ?: onCardNoMatched(maskedText)
        }
    }

    private fun onCardNoMatched(maskedText: String) {
        view.updateCardInputMask(SpacingPatterns.Default)
        view.updateCardLogo(null)
        view.setCardNumberPreviewText(maskedText)
        if (view.cardNumber.full) {
            onValidateInputs()
        }
    }

    private fun onEmptyCardNumber() {
        view.updateCardInputMask(SpacingPatterns.Default)
        view.updateCardLogo(null)
        view.setCardNumberPreviewText(stringResources.getString(DEFAULT_CARD_NUMBER_PLACEHOLDER))
    }

    private fun getCardLogo(card: PaymentCard): Int? {
        return when (card.type) {
            CardType.Visa -> LOGO_VISA_RESOURCE
            CardType.MasterCard -> LOGO_MASTERCARD_RESOURCE
            CardType.AmericanExpress -> LOGO_AMEX_RESOURCE
            CardType.JCB -> LOGO_JCB_RESOURCE
            CardType.DinersClubInternational -> LOGO_DINNERS_CLUB_RESOURCE
            CardType.Discover -> LOGO_DISCOVER_RESOURCE
            else -> null
        }
    }

    override fun onExpireDateChanged(rawText: String, maskedText: String) {
        view.setExpireDatePreviewText(maskedText)
        if (rawText.length == 4) {
            view.focusInCvv()
        }
    }

    override fun onCvvChanged(rawText: String, maskedText: String) {
        if (view.cvv.full) {
            view.focusInCardHolder()
        }
    }

    /**
     * Return a set of validation results and show them on different part of the view
     */
    override fun onValidateInputs(): Boolean {
        val errors = validateInputs()

        // If card holder input is invalid, show bottom error message
        errors.firstOrNull { it == INVALID_CARD_HOLDER }?.let {
            view.showBottomErrorMessage(show = true)
            view.setBottomErrorMessage(stringResources.getString(CARD_HOLDER_ERROR_RESOURCE))
        } ?: view.showBottomErrorMessage(show = false)

        // Always filter INVALID_CARD_HOLDER before showing top error message
        errors.filter { it != INVALID_CARD_HOLDER }.also { filteredErrors ->
            if (filteredErrors.isNotEmpty()) {
                view.showTopErrorMessage(show = true)
                view.setTopErrorMessage(getTopValidationErrorMessage(filteredErrors))
            } else {
                view.showTopErrorMessage(show = false)
            }
        }

        return errors.isEmpty()
    }

    /**
     * When card payment sheet is first shown, it makes a couple of calls to get payment cookie and
     * order information. This method is the starting point of card payment.
     */
    override fun init() {
        view.showProgress(true, stringResources.getString(MESSAGE_AUTHORIZING))
        paymentApiInteractor.authorizePayment(
                url = url,
                code = code,
                success = { cookies, getOrderUrl ->
                    view.showProgress(false)
                    orderUrl = getOrderUrl
                    onHandlePaymentAuthorization(cookies, getOrderUrl)
                },
                error = {
                    view.showProgress(false)
                    interactions.onGenericError(it.message)
                })
    }

    /**
     * This method extract payment cookies as an authorization token and gets order details
     * Notice that sdk never use authorization tokens but cookies to authenticate gateway
     * These cookies are one-time tokens and can't be used a second time.
     * When this call is successful, we have all information to complete a payment transaction
     */
    @VisibleForTesting
    internal fun onHandlePaymentAuthorization(cookies: List<String>, orderUrl: String) {
        view.showProgress(true, stringResources.getString(MESSAGE_LOADING_ORDER_DETAILS))

        paymentCookie = cookies.first { it.startsWith("payment-token") }

        paymentApiInteractor.getOrder(
            orderUrl = orderUrl,
            paymentCookie = paymentCookie,
            success = { orderReference, paymentUrl, supportedCards, orderAmount, outletRef, selfLink, orderJson ->
                var paymentRef = ""
                try {
                    val order = Gson().fromJson(orderJson.toString(), Order::class.java)
                    paymentRef = order?.embedded?.payment?.get(0)?.reference ?: ""
                    this.payPageUrl = order?.links?.paymentUrl?.href ?: ""
                } catch (e: Exception) {
                }
                view.showProgress(false)
                view.focusInCardNumber()
                this.selfLink = selfLink
                this.orderReference = orderReference
                this.outletRef = outletRef
                this.paymentUrl = paymentUrl
                this.supportedCards = supportedCards
                this.orderAmount = orderAmount
                this.paymentRef = paymentRef
            },
            error = {
                view.showProgress(false)
                interactions.onGenericError(it.message)
            })
    }

    /**
     * This is called when pay  button on pay sheet is clicked. This method starts the actual
     * card payment flow and returns payment state along with payment json response
     */
    override fun onPayClicked() {
        if (onValidateInputs()) {
            view.showProgress(true, stringResources.getString(LABEL_SUBMITTING_PAYMENT))
            paymentApiInteractor.visaEligibilityCheck(
                cardNumber = view.cardNumber.rawTxt,
                token = paymentCookie,
                url = "$selfLink/vis/eligibility-check",
                success = { isEligible, plans ->
                    if (isEligible) {
                        interactions.launchVisaInstalment(
                            installmentPlans = InstallmentPlan.fromVisaPlans(plans, orderAmount),
                            cardNumber = view.cardNumber.rawTxt
                        )
                    } else {
                        getPayerIp()
                    }
                },
                error = {
                    getPayerIp()
                }
            )
        }
    }

    private fun getPayerIp() {
        paymentApiInteractor.getPayerIp(
            getIpUrl(this.paymentUrl),
            success = {
                doPayment(payerIp = it)
            }, error = {
                doPayment(null)
            })
    }

    override fun makeVisPayment(installmentPlan: InstallmentPlan) {
        view.showProgress(true, stringResources.getString(LABEL_SUBMITTING_PAYMENT))
        if (installmentPlan.frequency != PlanFrequency.PayInFull) {
            visRequest = VisaRequest(
                planSelectionIndicator = true,
                vPlanId = installmentPlan.id,
                acceptedTAndCVersion = installmentPlan.terms?.version ?: 0
            )
        }
        getPayerIp()
    }

    private fun getIpUrl(stringVal: String): String {
        val slug = "/api/requester-ip"
        if (stringVal.contains("-uat", true) ||
            stringVal.contains("sandbox", true)
        ) {
            return "https://paypage.sandbox.ngenius-payments.com$slug"
        }
        if (stringVal.contains("-dev", true)) {
            return "https://paypage-dev.ngenius-payments.com$slug"
        }
        return "https://paypage.ngenius-payments.com$slug"
    }

    private fun doPayment(payerIp: String?) {
        paymentApiInteractor.doPayment(
            paymentUrl = paymentUrl,
            paymentCookie = paymentCookie,
            pan = view.cardNumber.rawTxt,
            expiry = DateFormatter.formatExpireDateForApi(view.expireDate.rawTxt),
            cvv = view.cvv.rawTxt,
            cardHolder = view.cardHolder.rawTxt,
            payerIp = payerIp,
            visRequest = visRequest,
            success = { paymentState, paymentResponse ->
                view.showProgress(false)
                handleCardPaymentResponse(paymentState, response = paymentResponse)
            },
            error = {
                view.showProgress(false)
                interactions.onGenericError(it.message)
            })
    }

    override fun onHandle3DSecurePaymentSate(state: String) {
        handleCardPaymentResponse(state)
    }

    override fun getOrderInfo(): OrderAmount {
        return this.orderAmount
    }

    private fun handleCardPaymentResponse(state: String, response: JSONObject? = null) {
        when (state) {
            STATUS_PAYMENT_AUTHORISED -> interactions.onPaymentAuthorized()
            STATUS_PAYMENT_PURCHASED -> interactions.onPaymentPurchased()
            STATUS_PAYMENT_CAPTURED -> interactions.onPaymentCaptured()
            STATUS_PAYMENT_AWAIT_3DS -> {
                response?.let { orderResponse ->
                    run3DSecure(ThreeDSecureRequest.buildFromOrderResponse(orderResponse))
                } ?: interactions.onPaymentFailed()
            }
            STATUS_PAYMENT_FAILED -> interactions.onPaymentFailed()
            STATUS_POST_AUTH_REVIEW -> interactions.onPaymentPostAuthReview()
            STATUS_AWAITING_PARTIAL_AUTH_APPROVAL -> {
                response?.let {
                    Gson().fromJson(it.toString(), PaymentResponse::class.java)
                        .toIntent(paymentCookie)
                        .let { threeDSChallengeIntent ->
                            interactions.onPartialAuth(threeDSChallengeIntent)
                        }
                } ?: interactions.onGenericError("Failed to parse payment response: $state")
            }
            else -> interactions.onGenericError("Unknown payment state: $state")
        }
    }

    private fun run3DSecure(threeDSecureRequest: ThreeDSecureRequest) {
        view.showProgressTimeOut(text = stringResources.getString(MESSAGE_LAUNCHING_3DS), timeout = {
            if (threeDSecureRequest.threeDSTwo?.directoryServerID != null &&
                    threeDSecureRequest.threeDSTwo.threeDSMessageVersion != null &&
                threeDSecureRequest.threeDSTwoAuthenticationURL != null &&
                threeDSecureRequest.threeDSTwoChallengeResponseURL != null) {
                interactions.onStart3dSecureTwo(
                    threeDSecureRequest = threeDSecureRequest,
                    paymentCookie = paymentCookie,
                    threeDSMessageVersion = threeDSecureRequest.threeDSTwo.threeDSMessageVersion,
                    directoryServerID = threeDSecureRequest.threeDSTwo.directoryServerID,
                    threeDSTwoAuthenticationURL = threeDSecureRequest.threeDSTwoAuthenticationURL,
                    threeDSTwoChallengeResponseURL = threeDSecureRequest.threeDSTwoChallengeResponseURL,
                    outletRef = outletRef,
                    orderRef = orderReference,
                    orderUrl = orderUrl,
                    paymentRef = paymentRef
                )
            } else {
                interactions.onStart3dSecure(threeDSecureRequest)
            }
        })
    }

    private fun validateInputs(): MutableSet<InputValidationError> {
        val errors = mutableSetOf<InputValidationError>()

        view.cardNumber.setErrorWhen {
            !full || !Luhn.isValidPan(rawTxt)
        }.also { invalid ->
            if (invalid) errors.add(InputValidationError.INVALID_CARD_NUMBER)
        }

        view.expireDate.setErrorWhen {
            dirty && !full
        }.also { invalid ->
            if (invalid) errors.add(InputValidationError.INVALID_EXPIRE_DATE)
        }

        view.cvv.setErrorWhen {
            dirty && !full
        }.also { invalid ->
            if (invalid) errors.add(InputValidationError.INVALID_CVV)
        }

        view.cardHolder.setErrorWhen {
            dirty && !full
        }.also { invalid ->
            if (invalid) errors.add(InputValidationError.INVALID_CARD_HOLDER)
        }

        return errors
    }

    private fun getTopValidationErrorMessage(errors: List<InputValidationError>): String {
        if (errors.size > 1) {
            return stringResources.getString(R.string.error_message_card_numbers_multiple)
        }
        return stringResources.getString(
                when (errors.first()) {
                    InputValidationError.INVALID_CARD_NUMBER -> CARD_NUMBER_ERROR_RESOURCE
                    InputValidationError.INVALID_EXPIRE_DATE -> CARD_EXPIRY_ERROR_RESOURCE
                    InputValidationError.INVALID_CVV -> CVV_ERROR_RESOURCE
                    else -> GENERIC_ERROR_RESOURCE
                })
    }

    companion object {
        @VisibleForTesting
        internal val CARD_NUMBER_LABEL_RESOURCE: Int = R.string.card_number_label_title
        @VisibleForTesting
        internal val CARD_EXPIRY_LABEL_RESOURCE: Int = R.string.card_expiry_date_label_title
        @VisibleForTesting
        internal val CVV_LABEL_RESOURCE: Int = R.string.card_cvv_label_title

        @VisibleForTesting
        internal val MESSAGE_AUTHORIZING = R.string.message_authorizing
        @VisibleForTesting
        internal val MESSAGE_LOADING_ORDER_DETAILS = R.string.message_loading_order_details
        @VisibleForTesting
        internal val MESSAGE_LAUNCHING_3DS = R.string.launching_3d_secure

        private val CARD_NUMBER_ERROR_RESOURCE: Int = R.string.error_message_pan_invalid
        private val CARD_EXPIRY_ERROR_RESOURCE: Int = R.string.error_message_card_end_date_invalid
        private val CVV_ERROR_RESOURCE: Int = R.string.error_message_card_cvv_invalid
        private val CARD_HOLDER_ERROR_RESOURCE: Int = R.string.error_message_cardholder_name_invalid
        private val GENERIC_ERROR_RESOURCE: Int = R.string.error_message_default_generic
        private val CARD_NOT_SUPPORTED_ERROR_RESOURCE: Int = R.string.error_card_not_supported

        @VisibleForTesting
        internal val LABEL_SUBMITTING_PAYMENT: Int = R.string.submitting_payment

        @VisibleForTesting
        internal val LOGO_AMEX_RESOURCE: Int = R.drawable.ic_logo_amex
        @VisibleForTesting
        internal val LOGO_VISA_RESOURCE: Int = R.drawable.ic_logo_visa
        @VisibleForTesting
        internal val LOGO_MASTERCARD_RESOURCE: Int = R.drawable.ic_logo_mastercard
        @VisibleForTesting
        internal val LOGO_JCB_RESOURCE: Int = R.drawable.ic_logo_jcb
        @VisibleForTesting
        internal val LOGO_DINNERS_CLUB_RESOURCE: Int = R.drawable.ic_logo_dinners_club
        @VisibleForTesting
        internal val LOGO_DISCOVER_RESOURCE: Int = R.drawable.ic_logo_discover

        @VisibleForTesting
        internal val DEFAULT_CARD_NUMBER_PLACEHOLDER = R.string.placeholder_card_number

        @VisibleForTesting
        internal const val STATUS_PAYMENT_AUTHORISED = "AUTHORISED"
        @VisibleForTesting
        internal const val STATUS_PAYMENT_PURCHASED = "PURCHASED"
        @VisibleForTesting
        internal const val STATUS_PAYMENT_CAPTURED = "CAPTURED"
        @VisibleForTesting
        internal const val STATUS_PAYMENT_AWAIT_3DS = "AWAIT_3DS"
        @VisibleForTesting
        internal const val STATUS_PAYMENT_FAILED = "FAILED"
        @VisibleForTesting
        internal const val STATUS_POST_AUTH_REVIEW = "POST_AUTH_REVIEW"
        internal const val STATUS_AWAITING_PARTIAL_AUTH_APPROVAL = "AWAITING_PARTIAL_AUTH_APPROVAL"
    }
}


