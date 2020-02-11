package payment.sdk.android.cardpayment

import payment.sdk.android.cardpayment.CardPaymentContract.StatefulInput
import payment.sdk.android.cardpayment.CardPaymentPresenter.Companion.STATUS_PAYMENT_AUTHORISED
import payment.sdk.android.cardpayment.CardPaymentPresenter.Companion.STATUS_PAYMENT_CAPTURED
import payment.sdk.android.cardpayment.CardPaymentPresenter.Companion.STATUS_PAYMENT_FAILED
import payment.sdk.android.core.dependency.StringResources
import payment.sdk.android.core.CardType
import payment.sdk.android.core.CardType.*
import com.flextrade.jfixture.FixtureAnnotations
import com.flextrade.jfixture.JFixture
import com.flextrade.jfixture.annotations.Fixture
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.NamedParameters
import junitparams.Parameters
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString

import org.mockito.Mockito.*
import payment.sdk.android.cardpayment.card.SpacingPatterns
import payment.sdk.android.core.OrderAmount

@RunWith(JUnitParamsRunner::class)
class CardPaymentPresenterTest {

    @Fixture
    private lateinit var url: String

    @Fixture
    private lateinit var code: String

    @Mock
    private lateinit var mockView: CardPaymentContract.View
    @Mock
    private lateinit var mockInteractions: CardPaymentContract.Interactions
    @Mock
    private lateinit var mockPaymentApiInteractor: PaymentApiInteractor
    @Mock
    private lateinit var mockStringResources: StringResources
    @Mock
    private lateinit var mockCardNumberInput: StatefulInput
    @Mock
    private lateinit var mockExpireDateInput: StatefulInput
    @Mock
    private lateinit var mockCvvInput: StatefulInput
    @Mock
    private lateinit var mockCardHolderInput: StatefulInput

    private lateinit var sut: CardPaymentPresenter

    private lateinit var fixture: JFixture

    @Before
    fun setUp() {
        fixture = JFixture()
        MockitoAnnotations.initMocks(this)
        FixtureAnnotations.initFixtures(this, fixture)
        sut = CardPaymentPresenter(url, code, mockView, mockInteractions, mockPaymentApiInteractor, mockStringResources)

        whenever(mockView.cardNumber).thenReturn(mockCardNumberInput)
        whenever(mockView.expireDate).thenReturn(mockExpireDateInput)
        whenever(mockView.cvv).thenReturn(mockCvvInput)
        whenever(mockView.cardHolder).thenReturn(mockCardHolderInput)

        clearInvocations(mockView)
    }

    @Test
    fun onCardNumberFocusGained() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CARD_NUMBER_LABEL_RESOURCE)).thenReturn(fixtLabel)

        sut.onCardNumberFocusGained()

        verify(mockView).setFloatingHintText(fixtLabel)

    }

    @Test
    fun onExpireDateFocusGained() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CARD_EXPIRY_LABEL_RESOURCE)).thenReturn(fixtLabel)

        sut.onExpireDateFocusGained()

        verify(mockView).setFloatingHintText(fixtLabel)
    }

    @Test
    fun `onCvvFocusGained when visa`() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CVV_LABEL_RESOURCE)).thenReturn(fixtLabel)
        whenever(mockCardNumberInput.full).thenReturn(false)
        supportAllCards()
        sut.onCardNumberChanged(VISA_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)
        verifyCardNumberChange()

        sut.onCvvFocusGained()

        verify(mockView).updateCvvInputMask("###")
    }

    @Test
    fun `onCvvFocusGained when mastercard`() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CVV_LABEL_RESOURCE)).thenReturn(fixtLabel)
        whenever(mockCardNumberInput.full).thenReturn(false)
        supportAllCards()
        sut.onCardNumberChanged(MASTERCARD_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)
        verifyCardNumberChange()

        sut.onCvvFocusGained()

        verify(mockView).updateCvvInputMask("###")
    }

    @Test
    fun `onCvvFocusGained when amex`() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CVV_LABEL_RESOURCE)).thenReturn(fixtLabel)
        whenever(mockCardNumberInput.full).thenReturn(false)
        supportAllCards()
        sut.onCardNumberChanged(AMEX_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)
        verifyCardNumberChange()

        sut.onCvvFocusGained()

        verify(mockView).updateCvvInputMask("####")
    }

    @Test
    fun `onCvvFocusGained when jcb`() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CVV_LABEL_RESOURCE)).thenReturn(fixtLabel)
        whenever(mockCardNumberInput.full).thenReturn(false)
        supportAllCards()
        sut.onCardNumberChanged(JCB_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)
        verifyCardNumberChange()

        sut.onCvvFocusGained()

        verify(mockView).updateCvvInputMask("###")
    }

    @Test
    fun `onCvvFocusGained when dinners club`() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CVV_LABEL_RESOURCE)).thenReturn(fixtLabel)
        whenever(mockCardNumberInput.full).thenReturn(false)
        supportAllCards()
        sut.onCardNumberChanged(DISCOVER_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)
        verifyCardNumberChange()

        sut.onCvvFocusGained()

        verify(mockView).updateCvvInputMask("###")
    }

    @Test
    fun `onCvvFocusGained when discover`() {
        val fixtLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.CVV_LABEL_RESOURCE)).thenReturn(fixtLabel)
        whenever(mockCardNumberInput.full).thenReturn(false)
        supportAllCards()
        sut.onCardNumberChanged(DISCOVER_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)
        verifyCardNumberChange()

        sut.onCvvFocusGained()

        verify(mockView).updateCvvInputMask("###")
    }


    @Test
    fun `onCvvFocusLost when visa`() {
        supportAllCards()
        sut.onCardNumberChanged(VISA_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)

        sut.onCvvFocusLost()

        verify(mockView).showCardFrontFace()
    }

    @Test
    fun `onCvvFocusLost when mastercard`() {
        supportAllCards()
        sut.onCardNumberChanged(MASTERCARD_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)

        sut.onCvvFocusLost()

        verify(mockView).showCardFrontFace()
    }

    private fun supportAllCards() {
        sut.supportedCards = setOf(Visa, MasterCard, AmericanExpress, Discover, JCB, DinersClubInternational)
    }

    @Test
    fun `onCvvFocusLost when amex`() {
        supportAllCards()
        sut.onCardNumberChanged(AMEX_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_6_5)

        sut.onCvvFocusLost()

        verify(mockView).showFrontCvvGuide(false)
    }

    @Test
    fun `onCvvFocusLost when jcb`() {
        supportAllCards()
        sut.onCardNumberChanged(JCB_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_4_4_4)

        sut.onCvvFocusLost()

        verify(mockView).showCardFrontFace()
    }

    @Test
    fun `onCvvFocusLost when dinners club`() {
        supportAllCards()
        sut.onCardNumberChanged(DINNERS_CLUB_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_6_4)

        sut.onCvvFocusLost()

        verify(mockView).showCardFrontFace()
    }

    @Test
    fun `onCvvFocusLost when discover`() {
        supportAllCards()
        sut.onCardNumberChanged(DISCOVER_TEST_NUMBERS.first(), SpacingPatterns.Pattern_4_6_4)

        sut.onCvvFocusLost()

        verify(mockView).showCardFrontFace()
    }

    @Test
    fun `onCardNumberChanged when empty`() {
        val fixtPlaceHolder = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.DEFAULT_CARD_NUMBER_PLACEHOLDER))
                .thenReturn(fixtPlaceHolder)

        sut.onCardNumberChanged("", fixture.create(String::class.java))

        verify(mockView).updateCardInputMask(SpacingPatterns.Default)
        verify(mockView).updateCardLogo(null)
        verify(mockView).setCardNumberPreviewText(fixtPlaceHolder)
    }

    @Test
    fun `onCardNumberChanged when not matched and not full`() {
        val fixtPlaceHolder = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.DEFAULT_CARD_NUMBER_PLACEHOLDER))
                .thenReturn(fixtPlaceHolder)
        whenever(mockView.cardNumber.full).thenReturn(false)
        val fixtMaskedText = fixture.create(String::class.java)

        sut.onCardNumberChanged("0000", fixtMaskedText)

        verify(mockView).updateCardInputMask(SpacingPatterns.Default)
        verify(mockView).updateCardLogo(null)
        verify(mockView).setCardNumberPreviewText(fixtMaskedText)
    }

    @Test
    fun `onCardNumberChanged when not matched and full`() {
        val fixtPlaceHolder = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.DEFAULT_CARD_NUMBER_PLACEHOLDER))
                .thenReturn(fixtPlaceHolder)
        whenever(mockView.cardNumber.full).thenReturn(true)
        val fixtMaskedText = fixture.create(String::class.java)

        sut.onCardNumberChanged(INVALID_CARD_NUMBER, fixtMaskedText)

        verify(mockView).updateCardInputMask(SpacingPatterns.Default)
        verify(mockView).updateCardLogo(null)
        verify(mockView).setCardNumberPreviewText(fixtMaskedText)
    }

    @Test
    @Parameters(named = PARAMS_PARTIAL_PAN_LOGOS_AND_PATTERNS)
    fun `onCardNumberChanged when matched and not full`(pan: String, logoResource: Int, pattern: String) {
        val fixtPlaceHolder = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.DEFAULT_CARD_NUMBER_PLACEHOLDER))
                .thenReturn(fixtPlaceHolder)
        whenever(mockView.cardNumber.full).thenReturn(true)
        val fixtMaskedText = fixture.create(String::class.java)

        supportAllCards()
        sut.onCardNumberChanged(pan, fixtMaskedText)

        verify(mockView).setCardNumberPreviewText(fixtMaskedText)
        verify(mockView).updateCardInputMask(pattern)
        verify(mockView).updateCardLogo(logoResource)
    }

    @Test
    @Parameters(named = PARAMS_FULL_PAN_LOGOS_AND_PATTERNS)
    fun `onCardNumberChanged when matched and full`(pan: String, logoResource: Int, pattern: String) {
        val fixtPlaceHolder = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.DEFAULT_CARD_NUMBER_PLACEHOLDER))
                .thenReturn(fixtPlaceHolder)
        whenever(mockView.cardNumber.full).thenReturn(true)
        val fixtMaskedText = fixture.create(String::class.java)

        supportAllCards()
        sut.onCardNumberChanged(pan, fixtMaskedText)

        verify(mockView).setCardNumberPreviewText(fixtMaskedText)
        verify(mockView).updateCardInputMask(pattern)
        verify(mockView).updateCardLogo(logoResource)
        verify(mockView).focusInCardExpire()
    }

    @Test
    @Parameters(named = PARAMS_PARTIAL_EXPIRY)
    fun `onExpireDateChanged when not full`(rawExpiry: String, maskedText: String) {
        sut.onExpireDateChanged(rawExpiry, maskedText)

        verify(mockView).setExpireDatePreviewText(maskedText)
        verify(mockView, never()).focusInCvv()
    }

    @Test
    @Parameters(named = PARAMS_FULL_EXPIRY)
    fun `onExpireDateChanged when full`(rawExpiry: String, maskedText: String) {
        sut.onExpireDateChanged(rawExpiry, maskedText)

        verify(mockView).setExpireDatePreviewText(maskedText)
        verify(mockView).focusInCvv()
    }

    @Test
    fun `onCvvChanged when not full`() {
        whenever(mockView.cvv.full).thenReturn(false)
        sut.onCvvChanged(fixture.create(String::class.java), fixture.create(String::class.java))

        verify(mockView, never()).focusInCardHolder()
    }

    @Test
    fun `onCvvChanged when full`() {
        whenever(mockView.cvv.full).thenReturn(true)
        sut.onCvvChanged(fixture.create(String::class.java), fixture.create(String::class.java))

        verify(mockView).focusInCardHolder()
    }

    @Test
    fun `onValidateInputs when card number not full`() {
        whenever(mockView.cardNumber)
                .thenReturn(createStateFulInputMock(dirty = false, full = false))

        val actual = sut.onValidateInputs()

        assertFalse(actual)
    }

    @Test
    fun `onValidateInputs when card number full and not valid number`() {
        whenever(mockView.cardNumber)
                .thenReturn(createStateFulInputMock(dirty = false, full = true, rawTxt = INVALID_CARD_NUMBER))

        val actual = sut.onValidateInputs()

        assertFalse(actual)
    }

    @Test
    fun `onValidateInputs when expiry not dirty`() {
        whenever(mockView.expireDate)
                .thenReturn(createStateFulInputMock(dirty = false))

        val actual = sut.onValidateInputs()

        assertTrue(actual)
    }

    @Test
    fun `onValidateInputs when expiry dirty and not full`() {
        whenever(mockView.expireDate)
                .thenReturn(createStateFulInputMock(dirty = true, full = false))

        val actual = sut.onValidateInputs()

        assertFalse(actual)
    }

    @Test
    fun `onValidateInputs when expiry dirty and full`() {
        whenever(mockView.expireDate)
                .thenReturn(createStateFulInputMock(dirty = true, full = true))

        val actual = sut.onValidateInputs()

        assertTrue(actual)
    }

    @Test
    fun `onValidateInputs when cvv not dirty`() {
        whenever(mockView.cvv)
                .thenReturn(createStateFulInputMock(dirty = false))

        val actual = sut.onValidateInputs()

        assertTrue(actual)
    }

    @Test
    fun `onValidateInputs when cvv dirty and not full`() {
        whenever(mockView.cvv)
                .thenReturn(createStateFulInputMock(dirty = true, full = false))

        val actual = sut.onValidateInputs()

        assertFalse(actual)
    }

    @Test
    fun `onValidateInputs when cvv dirty and full`() {
        whenever(mockView.cvv)
                .thenReturn(createStateFulInputMock(dirty = true, full = true))

        val actual = sut.onValidateInputs()

        assertTrue(actual)
    }

    @Test
    fun `onValidateInputs when cardholder not dirty`() {
        whenever(mockView.cardHolder)
                .thenReturn(createStateFulInputMock(dirty = false))

        val actual = sut.onValidateInputs()

        assertTrue(actual)
    }

    @Test
    fun `onValidateInputs when cardholder dirty and not full`() {
        whenever(mockView.cardHolder)
                .thenReturn(createStateFulInputMock(dirty = true, full = false))

        val actual = sut.onValidateInputs()

        assertFalse(actual)
    }

    @Test
    fun `onValidateInputs when cardholder dirty and full`() {
        whenever(mockView.cardHolder)
                .thenReturn(createStateFulInputMock(dirty = true, full = true))

        val actual = sut.onValidateInputs()

        assertTrue(actual)
    }

    @Test
    fun `onPayClicked return AUTHORIZED`() {
        // init
        val fixtSubmittingLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.LABEL_SUBMITTING_PAYMENT)).thenReturn(fixtSubmittingLabel)
        initPresenterForPayment(STATUS_PAYMENT_AUTHORISED)

        // run
        sut.onPayClicked()

        // verify
        val inOrder = inOrder(mockView, mockInteractions)
        inOrder.verify(mockView).showProgress(true, fixtSubmittingLabel)
        inOrder.verify(mockView).showProgress(false)
        inOrder.verify(mockInteractions).onPaymentAuthorized()
    }

    @Test
    fun `onPayClicked return FAILED`() {
        // init
        val fixtSubmittingLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.LABEL_SUBMITTING_PAYMENT)).thenReturn(fixtSubmittingLabel)
        initPresenterForPayment(STATUS_PAYMENT_FAILED)

        // run
        sut.onPayClicked()

        // verify
        val inOrder = inOrder(mockView, mockInteractions)
        inOrder.verify(mockView).showProgress(true, fixtSubmittingLabel)
        inOrder.verify(mockView).showProgress(false)
        inOrder.verify(mockInteractions).onPaymentFailed()
    }

    @Test
    fun `onPayClicked return CAPTURED`() {
        // init
        val fixtSubmittingLabel = fixture.create(String::class.java)
        whenever(mockStringResources.getString(CardPaymentPresenter.LABEL_SUBMITTING_PAYMENT)).thenReturn(fixtSubmittingLabel)
        initPresenterForPayment(STATUS_PAYMENT_CAPTURED)

        // run
        sut.onPayClicked()

        // verify
        val inOrder = inOrder(mockView, mockInteractions)
        inOrder.verify(mockView).showProgress(true, fixtSubmittingLabel)
        inOrder.verify(mockView).showProgress(false)
        inOrder.verify(mockInteractions).onPaymentCaptured()
    }

    @Test
    fun `onPayClicked when errors`() {
        val spiedSut = spy(sut)

        doReturn(false).whenever(spiedSut).onValidateInputs()

        spiedSut.onPayClicked()

        verifyZeroInteractions(mockView, mockPaymentApiInteractor)
    }

    @Test
    fun `onHandle3DSecurePaymentSate when 3d secure state captured`() {
        sut.onHandle3DSecurePaymentSate("CAPTURED")

        verify(mockInteractions).onPaymentCaptured()
    }

    @Test
    fun `onHandle3DSecurePaymentSate when 3d secure state auth`() {
        sut.onHandle3DSecurePaymentSate("AUTHORISED")

        verify(mockInteractions).onPaymentAuthorized()
    }

    @Test
    fun `onHandle3DSecurePaymentSate when 3d secure state failed`() {
        sut.onHandle3DSecurePaymentSate("FAILED")

        verify(mockInteractions).onPaymentFailed()
    }

    @Test
    fun `onHandle3DSecurePaymentSate when 3d secure state unknown`() {
        val fixtState = fixture.create(String::class.java)
        sut.onHandle3DSecurePaymentSate(fixtState)

        verify(mockInteractions).onGenericError("Unknown payment state: $fixtState")
    }


    private fun initPresenterForPayment(state: String) {
        whenever(mockCardNumberInput.rawTxt).thenReturn(VISA_TEST_NUMBERS.first())
        whenever(mockExpireDateInput.rawTxt).thenReturn("1220")
        whenever(mockCvvInput.rawTxt).thenReturn("123")
        whenever(mockCardHolderInput.rawTxt).thenReturn(fixture.create(String::class.java))

        val fixtOrderReference = fixture.create(String::class.java)
        val fixtPaymentUrl = fixture.create(String::class.java)
        val fixtCookie = fixture.create(String::class.java)
        whenever(mockPaymentApiInteractor.getOrder(
                anyString(), anyString(), anyObject(), anyObject())).then {
            it.getArgument<((String, String, Set<CardType>, OrderAmount) -> Unit)>(2)(fixtOrderReference, fixtPaymentUrl, setOf(Visa, MasterCard, AmericanExpress), OrderAmount(2000.00, "AED"))
        }
        whenever(mockPaymentApiInteractor.doPayment(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyObject(), anyObject())).then {
            it.getArgument<((String, JSONObject) -> Unit)>(6)(state, JSONObject())
        }

        sut.onHandlePaymentAuthorization(listOf("payment-token: $fixtCookie"), fixture.create(String::class.java))
    }

    private fun <T> anyObject(): T {
        return Mockito.anyObject<T>()
    }

    private fun createStateFulInputMock(dirty: Boolean = false, full: Boolean = false, rawTxt: String = "", txt: String = "")
            : StatefulInput = object : StatefulInput {
        @Suppress("UNUSED_EXPRESSION")
        override fun setErrorWhen(predicate: StatefulInput.() -> Boolean): Boolean = predicate()

        override val dirty: Boolean = dirty
        override val full: Boolean = full
        override val rawTxt: String = rawTxt
        override val txt: String = txt
    }

    /**
     * As sut.onCardNumberChanged is called to update presenter internal paymentCard value almost in every call,
     * we have to consume this invocations and focus on actual invocation
     */
    private fun verifyCardNumberChange() {
        verify(mockView).setCardNumberPreviewText(anyString())
        verify(mockView).updateCardInputMask(anyString())
        verify(mockView).updateCardLogo(anyInt())
    }

    @NamedParameters(PARAMS_PARTIAL_PAN_LOGOS_AND_PATTERNS)
    fun partialPanParams(): Array<Array<Any>> {
        return arrayOf(
                arrayOf("4", CardPaymentPresenter.LOGO_VISA_RESOURCE, SpacingPatterns.Pattern_4_4_4_4),
                arrayOf("52", CardPaymentPresenter.LOGO_MASTERCARD_RESOURCE, SpacingPatterns.Pattern_4_4_4_4),
                arrayOf("34", CardPaymentPresenter.LOGO_AMEX_RESOURCE, SpacingPatterns.Pattern_4_6_5),
                arrayOf("3528", CardPaymentPresenter.LOGO_JCB_RESOURCE, SpacingPatterns.Pattern_4_4_4_4),
                arrayOf("36", CardPaymentPresenter.LOGO_DINNERS_CLUB_RESOURCE, SpacingPatterns.Pattern_4_6_4),
                arrayOf("65", CardPaymentPresenter.LOGO_DISCOVER_RESOURCE, SpacingPatterns.Pattern_4_4_4_4)
        )
    }

    @NamedParameters(PARAMS_FULL_PAN_LOGOS_AND_PATTERNS)
    fun fullPanParams(): Array<Array<Any>> {
        return arrayOf(
                arrayOf(VISA_TEST_NUMBERS.first(), CardPaymentPresenter.LOGO_VISA_RESOURCE, SpacingPatterns.Pattern_4_4_4_4),
                arrayOf(MASTERCARD_TEST_NUMBERS.first(), CardPaymentPresenter.LOGO_MASTERCARD_RESOURCE, SpacingPatterns.Pattern_4_4_4_4),
                arrayOf(AMEX_TEST_NUMBERS.first(), CardPaymentPresenter.LOGO_AMEX_RESOURCE, SpacingPatterns.Pattern_4_6_5),
                arrayOf(JCB_TEST_NUMBERS.first(), CardPaymentPresenter.LOGO_JCB_RESOURCE, SpacingPatterns.Pattern_4_4_4_4),
                arrayOf(DINNERS_CLUB_TEST_NUMBERS.first(), CardPaymentPresenter.LOGO_DINNERS_CLUB_RESOURCE, SpacingPatterns.Pattern_4_6_4),
                arrayOf(DISCOVER_TEST_NUMBERS.first(), CardPaymentPresenter.LOGO_DISCOVER_RESOURCE, SpacingPatterns.Pattern_4_4_4_4)
        )
    }

    @NamedParameters(PARAMS_PARTIAL_EXPIRY)
    fun partialExpiry(): Array<Array<String>> {
        return arrayOf(
                arrayOf("", ""),
                arrayOf("1", "1"),
                arrayOf("11", "11"),
                arrayOf("111", "11/1")
        )
    }

    @NamedParameters(PARAMS_FULL_EXPIRY)
    fun fullExpiry(): Array<Array<String>> {
        return arrayOf(
                arrayOf("1111", "11/11"),
                arrayOf("0912", "09/12"),
                arrayOf("1225", "12/25")
        )
    }

    companion object {
        private const val INVALID_CARD_NUMBER = "1111111111111111"
        private val VISA_TEST_NUMBERS = listOf("4111111111111111", "4012888888881881")
        private val MASTERCARD_TEST_NUMBERS = listOf("5555555555554444", "5105105105105100")
        private val AMEX_TEST_NUMBERS = listOf("378282246310005", "371449635398431")
        private val JCB_TEST_NUMBERS = listOf("3566002020360505", "3530111333300000")
        private val DINNERS_CLUB_TEST_NUMBERS = listOf("36700102000000", "36148900647913")
        private val DISCOVER_TEST_NUMBERS = listOf("6011111111111117", "6011000990139424")

        private const val PARAMS_PARTIAL_PAN_LOGOS_AND_PATTERNS = "PARAMS_PARTIAL_PAN_LOGOS_AND_PATTERNS"
        private const val PARAMS_FULL_PAN_LOGOS_AND_PATTERNS = "PARAMS_FULL_PAN_LOGOS_AND_PATTERNS"
        private const val PARAMS_PARTIAL_EXPIRY = "PARAMS_PARTIAL_EXPIRY"
        private const val PARAMS_FULL_EXPIRY = "PARAMS_FULL_EXPIRY"
    }
}