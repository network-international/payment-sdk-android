package payment.sdk.android.cardpayment

import payment.sdk.android.sdk.R
import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.card.SpacingPatterns
import payment.sdk.android.cardpayment.validation.Luhn
import payment.sdk.android.cardpayment.widget.*
import java.util.*

/**
 * Card payment form laid out like the iOS SDK: all fields visible at once in label/field rows,
 * live validation text under the form, and a pay button that enables once every field is valid.
 * Payment flow, 3-D Secure and card detection stay in [CardPaymentPresenter].
 */
internal class CardPaymentView constructor(
        private val root: View
) : CardPaymentContract.View {

    private val context: Context = root.context

    private val logo: ImageView = root.findViewById(R.id.logo_view)
    private val previewNumber: TextView = root.findViewById(R.id.card_number)
    private val previewHolder: TextView = root.findViewById(R.id.card_holder_name)
    private val previewExpiry: TextView = root.findViewById(R.id.card_expire_date)

    private val numberEdit: EditText = root.findViewById(R.id.edit_card_number)
    private val monthEdit: EditText = root.findViewById(R.id.edit_expire_month)
    private val yearEdit: EditText = root.findViewById(R.id.edit_expire_year)
    private val cvvEdit: EditText = root.findViewById(R.id.edit_cvv)
    private val holderEdit: EditText = root.findViewById(R.id.edit_card_holder)

    private val errorView: TextView = root.findViewById(R.id.error_message)
    private val payButton: Button = root.findViewById(R.id.pay_button)

    private lateinit var presenter: CardPaymentContract.Presenter
    private var progressDialog: AlertDialog? = null

    private var numberMask = SpacingPatterns.Default
    private var cvvLength = DEFAULT_CVV_LENGTH

    // Errors reported by the presenter's own validation (shown when the live check has none).
    private var presenterTopError: String? = null
    private var presenterBottomError: String? = null

    private val numberInput = FieldInput(numberEdit) { digits(numberEdit).length == maskDigits(numberMask) }
    private val expiryInput = object : FieldInput(monthEdit, { expiryRaw().length == 4 }) {
        override val rawTxt: String get() = expiryRaw()
        override val txt: String get() = "${digits(monthEdit)}/${digits(yearEdit)}"
        override val dirty: Boolean get() = super.dirty || yearTouched
    }
    private val cvvInput = FieldInput(cvvEdit) { digits(cvvEdit).length == cvvLength }
    private val holderInput = FieldInput(holderEdit) { holderEdit.text.toString().isNotBlank() }
    private var yearTouched = false

    override val cardNumber: CardPaymentContract.StatefulInput get() = numberInput
    override val expireDate: CardPaymentContract.StatefulInput get() = expiryInput
    override val cvv: CardPaymentContract.StatefulInput get() = cvvInput
    override val cardHolder: CardPaymentContract.StatefulInput get() = holderInput

    init {
        listOf(numberEdit, monthEdit, yearEdit, cvvEdit, holderEdit).forEach(FieldColors::applyText)
        numberEdit.filters = arrayOf(InputFilter.LengthFilter(maskDigits(numberMask)))
        cvvEdit.filters = arrayOf(InputFilter.LengthFilter(cvvLength))

        numberEdit.afterTextChanged {
            val raw = digits(numberEdit)
            presenter.onCardNumberChanged(raw, applyMask(raw, numberMask))
            refresh()
        }
        monthEdit.afterTextChanged {
            presenter.onExpireDateChanged(expiryRaw(), expiryInput.txt)
            if (digits(monthEdit).length == 2 && monthEdit.hasFocus()) yearEdit.requestFocus()
            refresh()
        }
        yearEdit.afterTextChanged {
            presenter.onExpireDateChanged(expiryRaw(), expiryInput.txt)
            refresh()
        }
        cvvEdit.afterTextChanged {
            val raw = digits(cvvEdit)
            presenter.onCvvChanged(raw, raw)
            refresh()
        }
        holderEdit.afterTextChanged { refresh() }

        numberEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) presenter.onCardNumberFocusGained() else { numberInput.touched = true; presenter.onCardNumberFocusLost() }
            refresh()
        }
        monthEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) presenter.onExpireDateFocusGained() else { expiryInput.touched = true; presenter.onExpireDateFocusLost() }
            refresh()
        }
        yearEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { yearTouched = true; refresh() }
        }
        cvvEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) presenter.onCvvFocusGained() else { cvvInput.touched = true; presenter.onCvvFocusLost() }
            refresh()
        }
        holderEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { holderInput.touched = true; refresh() }
        }

        payButton.setOnClickListener { presenter.onPayClicked() }
        updatePreview()
        refresh()
    }

    override fun setPresenter(presenter: CardPaymentContract.Presenter) {
        this.presenter = presenter
        payButton.text = context.getString(R.string.pay_button_title, "").trim()
    }

    private fun payButtonTitle(): String {
        val amount = if (SDKConfig.showOrderAmount) {
            val isLTR = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR
            runCatching { presenter.getOrderInfo().formattedCurrencyString(isLTR) }.getOrElse { "" }
        } else ""
        return context.getString(R.string.pay_button_title, amount).trim()
    }

    // --- Card preview -------------------------------------------------------------------

    override fun setCardNumberPreviewText(text: String) = updatePreview()

    override fun setExpireDatePreviewText(text: String) = updatePreview()

    override fun updateCardLogo(resourceId: Int?) {
        resourceId?.let { logo.setImageDrawable(ContextCompat.getDrawable(context, it)) }
            ?: logo.setImageDrawable(null)
    }

    private fun updatePreview() {
        val number = digits(numberEdit)
        previewNumber.text = if (number.isEmpty()) DEFAULT_PREVIEW_NUMBER else number.chunked(4).joinToString(" ")
        val month = digits(monthEdit).ifEmpty { "--" }
        val year = digits(yearEdit).ifEmpty { "--" }
        previewExpiry.text = "$month/$year"
        previewHolder.text = holderEdit.text.toString().ifBlank { DEFAULT_PREVIEW_NAME }
    }

    // --- Masks and focus driven by the presenter ------------------------------------------

    override fun updateCardInputMask(mask: String) {
        if (mask == numberMask) return
        numberMask = mask
        numberEdit.filters = arrayOf(InputFilter.LengthFilter(maskDigits(mask)))
        refresh()
    }

    override fun updateCvvInputMask(mask: String) {
        val length = maskDigits(mask)
        if (length == cvvLength) return
        cvvLength = length
        cvvEdit.filters = arrayOf(InputFilter.LengthFilter(length))
        refresh()
    }

    override fun focusInCardNumber() {
        // Called once the order has loaded; the presenter stores the amount right after this call.
        root.post { payButton.text = payButtonTitle() }
        numberEdit.requestFocus()
        showKeyboard(numberEdit)
    }

    override fun focusInCardExpire() {
        monthEdit.requestFocus()
    }

    override fun focusInCvv() {
        cvvEdit.requestFocus()
    }

    override fun focusInCardHolder() {
        holderEdit.requestFocus()
    }

    // The iOS layout has no floating hint and no card flip; labels sit in each row.
    override fun setFloatingHintText(text: String) = Unit
    override fun setFloatingHintTextVisible(visible: Boolean) = Unit
    override fun showCardBackFace(onAnimationEndCallback: (() -> Unit)?) { onAnimationEndCallback?.invoke() }
    override fun showCardFrontFace(onAnimationEndCallback: (() -> Unit)?) { onAnimationEndCallback?.invoke() }
    override fun showFrontCvvGuide(show: Boolean) = Unit

    // --- Validation -------------------------------------------------------------------

    override fun setTopErrorMessage(error: String) { presenterTopError = error; renderErrors() }
    override fun setBottomErrorMessage(error: String) { presenterBottomError = error; renderErrors() }
    override fun showTopErrorMessage(show: Boolean) { if (!show) presenterTopError = null; renderErrors() }
    override fun showBottomErrorMessage(show: Boolean) { if (!show) presenterBottomError = null; renderErrors() }

    private fun refresh() {
        updatePreview()
        val valid = isNumberValid() && isExpiryValid() && cvvInput.full && holderInput.full
        payButton.isEnabled = valid
        payButton.alpha = if (valid) 1f else 0.5f
        renderErrors()
    }

    /** Like iOS: one message per field the customer has filled in or left, that is not valid. */
    private fun liveErrors(): List<String> = buildList {
        val number = digits(numberEdit)
        if ((number.length == maskDigits(numberMask) || (numberInput.touched && number.isNotEmpty())) && !isNumberValid()) {
            add(context.getString(R.string.error_message_pan_invalid))
        }
        // Moving from month to year is part of entering the date, so wait for a full date
        // or for the customer to leave the year field.
        val expiryStarted = expiryRaw().isNotEmpty()
        if (expiryStarted && (expiryRaw().length == 4 || yearTouched) && !isExpiryValid()) {
            add(context.getString(R.string.error_message_card_end_date_invalid))
        }
        val cvvText = digits(cvvEdit)
        if (cvvText.isNotEmpty() && (cvvInput.touched || cvvText.length > cvvLength) && !cvvInput.full) {
            add(context.getString(R.string.error_message_card_cvv_invalid))
        }
    }

    private fun renderErrors() {
        val messages = liveErrors().ifEmpty { listOfNotNull(presenterTopError, presenterBottomError) }
        errorView.text = messages.joinToString("\n")
    }

    private fun isNumberValid(): Boolean {
        val number = digits(numberEdit)
        return number.length == maskDigits(numberMask) && Luhn.isValidPan(number)
    }

    private fun isExpiryValid(): Boolean {
        val raw = expiryRaw()
        if (raw.length != 4) return false
        val month = raw.substring(0, 2).toInt()
        val year = 2000 + raw.substring(2, 4).toInt()
        if (month !in 1..12) return false
        val now = Calendar.getInstance()
        val thisYear = now.get(Calendar.YEAR)
        val thisMonth = now.get(Calendar.MONTH) + 1
        return year > thisYear || (year == thisYear && month >= thisMonth)
    }

    private fun expiryRaw() = digits(monthEdit) + digits(yearEdit)

    // --- Progress ---------------------------------------------------------------------

    override fun showProgressTimeOut(text: String?, timeout: () -> Unit) {
        showProgress(show = true, text = text)
        root.postDelayed({
            showProgress(show = false)
            timeout()
        }, 1000)
    }

    override fun showProgress(show: Boolean, text: String?) {
        progressDialog = if (show) {
            progressDialog?.dismiss()
            AlertDialog.Builder(context)
                    .setTitle(null)
                    .setCancelable(false)
                    .create().apply {
                        show()
                        setContentView(R.layout.view_progress_dialog)
                        findViewById<TextView>(R.id.text).text = text
                    }
        } else {
            progressDialog?.dismiss()
            null
        }
    }

    private fun showKeyboard(view: View) {
        view.post {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /** A form field as the presenter sees it. No visual error state: errors show as text. */
    private open class FieldInput(
        private val edit: EditText,
        private val isFull: () -> Boolean
    ) : CardPaymentContract.StatefulInput {
        var touched = false
        override val full: Boolean get() = isFull()
        override val rawTxt: String get() = edit.text.toString().filter { it.isDigit() || !edit.isNumeric() }
        override val txt: String get() = edit.text.toString()
        override val dirty: Boolean get() = touched
        override fun setErrorWhen(predicate: (CardPaymentContract.StatefulInput) -> Boolean): Boolean = predicate(this)

        private fun EditText.isNumeric() = inputType and android.text.InputType.TYPE_CLASS_NUMBER != 0
    }

    private fun EditText.afterTextChanged(block: () -> Unit) {
        addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) = block()
        })
    }

    companion object {
        private const val DEFAULT_CVV_LENGTH = 3
        private const val DEFAULT_PREVIEW_NUMBER = "---- ---- ---- ----"
        private const val DEFAULT_PREVIEW_NAME = "---"

        private fun digits(edit: EditText) = edit.text.toString().filter { it.isDigit() }

        private fun maskDigits(mask: String) = mask.count { it == NumericMaskInputFilter.MASK_CHAR }

        /** "4111111111111111" with "#### #### #### ####" -> "4111 1111 1111 1111". */
        internal fun applyMask(raw: String, mask: String): String {
            val out = StringBuilder()
            var i = 0
            for (m in mask) {
                if (i >= raw.length) break
                if (m == NumericMaskInputFilter.MASK_CHAR) out.append(raw[i++]) else out.append(m)
            }
            return out.toString()
        }
    }
}
