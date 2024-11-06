package payment.sdk.android.cardpayment

import payment.sdk.android.sdk.R
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.appcompat.widget.AppCompatTextView
import android.text.Editable
import android.view.View
import android.widget.*
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.widget.*
import java.util.*


internal class CardPaymentView constructor(
        private val root: View
) : CardPaymentContract.View, View.OnFocusChangeListener, NumericMaskedEditText.OnTextChangeListener {

    private val cardFlipper: HorizontalViewFlipper =
            root.findViewById(R.id.view_flipper)
    private val logo: ImageView =
            root.findViewById(R.id.logo_view)
    private val cardNumberView: AppCompatTextView =
            root.findViewById(R.id.card_number)
    private val expiryDateView: AppCompatTextView =
            root.findViewById(R.id.card_expire_date)
    private val cardHolderNameView: AppCompatTextView =
            root.findViewById(R.id.card_holder_name)
    private val floatingHint: FloatingHintView =
            root.findViewById(R.id.floating_hint_view)
    private val cardNumberEdit: NumericMaskedEditText =
            findNumericMaskedEditById(R.id.edit_card_number)
    private val cardExpireEdit: NumericMaskedEditText =
            findNumericMaskedEditById(R.id.edit_expire_date)
    private val cardCvvEdit: NumericMaskedEditText =
            findNumericMaskedEditById(R.id.edit_cvv)
    private val cardHolderHintView: TextView =
            root.findViewById(R.id.card_holder_name_hint_view)
    private val frontCvvGuideView: View =
            root.findViewById(R.id.card_front_cvv_guide)
    private val frontCvvIndicatorView: View =
            root.findViewById(R.id.card_front_cvv_indicator)

    private val topErrorMessageView: TextView by lazy {
        root.findViewById<TextView>(R.id.top_error_message)
    }
    private val bottomErrorMessageView: TextView by lazy {
        root.findViewById<TextView>(R.id.bottom_error_message)
    }
    private val payButton: Button by lazy {
        root.findViewById<Button>(R.id.pay_button).apply {
            setOnClickListener {
                presenter.onPayClicked()
            }
            if(SDKConfig.showOrderAmount) {
                val isLTR = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR
                text = root.context.getString(
                        R.string.pay_button_title,
                        presenter.getOrderInfo().formattedCurrencyString(isLTR)
                )
            } else {
                text = root.context.getString(
                        R.string.pay_button_title,
                        ""
                )
            }
        }
    }

    private val cardHolderEditContainer: View  by lazy {
        root.findViewById<View>(R.id.edit_card_holder_container)
    }

    private val cardHolderEdit: EditText =
            root.findViewById<EditText>(R.id.edit_card_holder).apply {
                addTextChangedListener(
                        object : TextWatcherAdapter() {
                            override fun afterTextChanged(s: Editable?) {
                                cardHolderNameView.text = s?.toString()
                            }
                        })
                onFocusChangeListener = View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                    tag = true
                    if (!hasFocus) {
                        onFocusChange(v, hasFocus)
                    }

                }
            }

    /**
     * @link {CardPaymentContract.StatefulMaskedInput} abstractions
     */
    override val cardNumber: CardPaymentContract.StatefulInput
        get() = cardNumberEdit

    override val expireDate: CardPaymentContract.StatefulInput
        get() = cardExpireEdit

    override val cvv: CardPaymentContract.StatefulInput
        get() = cardCvvEdit

    override val cardHolder: CardPaymentContract.StatefulInput
        get() = CardHolderInputDelegate(cardHolderEdit)


    private lateinit var presenter: CardPaymentContract.Presenter

    private var progressDialog: AlertDialog? = null

    override fun setPresenter(presenter: CardPaymentContract.Presenter) {
        this.presenter = presenter
    }

    override fun setFloatingHintText(text: String) {
        floatingHint.text = text
    }

    override fun setFloatingHintTextVisible(visible: Boolean) {
        floatingHint.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun setCardNumberPreviewText(text: String) {
        cardNumberView.text = text
    }

    override fun setExpireDatePreviewText(text: String) {
        expiryDateView.text = text
    }

    override fun updateCardInputMask(mask: String) {
        cardNumberEdit.setInputMask(mask)
    }

    override fun updateCvvInputMask(mask: String) {
        cardCvvEdit.setInputMask(mask)
    }

    override fun focusInCardNumber() {
        cardNumberEdit.requestFocus()
    }

    override fun focusInCardExpire() {
        cardExpireEdit.visibility = View.VISIBLE
        cardExpireEdit.requestFocus()
    }

    override fun focusInCvv() {
        cardCvvEdit.visibility = View.VISIBLE
        cardCvvEdit.requestFocus()
    }

    override fun focusInCardHolder() {
        payButton.visibility = View.VISIBLE
        cardHolderEditContainer.visibility = View.VISIBLE
        cardHolderHintView.visibility = View.VISIBLE
        cardHolderEdit.requestFocus()
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            when (view.id) {
                R.id.edit_card_number -> presenter.onCardNumberFocusGained()
                R.id.edit_expire_date -> presenter.onExpireDateFocusGained()
                R.id.edit_cvv -> presenter.onCvvFocusGained()
            }
            floatingHint.animateToAlignViewStart(view)
        } else {
            when (view.id) {
                R.id.edit_card_number -> presenter.onCardNumberFocusLost()
                R.id.edit_expire_date -> presenter.onExpireDateFocusLost()
                R.id.edit_cvv -> presenter.onCvvFocusLost()
            }
            presenter.onValidateInputs()
        }
    }

    override fun updateCardLogo(resourceId: Int?) {
        resourceId?.let { id ->
            logo.setImageDrawable(ContextCompat.getDrawable(logo.context, id))
        } ?: logo.setImageDrawable(null)
    }

    override fun onTextChangeListener(view: View, rawTextValue: String, maskedTextValue: String) {
        view.let {
            when (view.id) {
                R.id.edit_card_number -> presenter.onCardNumberChanged(rawTextValue, maskedTextValue)
                R.id.edit_expire_date -> presenter.onExpireDateChanged(rawTextValue, maskedTextValue)
                R.id.edit_cvv -> presenter.onCvvChanged(rawTextValue, maskedTextValue)
            }
        }
    }

    override fun showCardBackFace(onAnimationEndCallback: (() -> Unit)?) {
        cardFlipper.flipRightToLeft(onAnimationEndCallback)
    }

    override fun showCardFrontFace(onAnimationEndCallback: (() -> Unit)?) {
        cardFlipper.flipLeftToRight(onAnimationEndCallback)
    }

    override fun showFrontCvvGuide(show: Boolean) {
        val viewVisibility = if (show) View.VISIBLE else View.GONE
        frontCvvGuideView.visibility = viewVisibility
        frontCvvIndicatorView.visibility = viewVisibility
    }

    override fun setTopErrorMessage(error: String) {
        topErrorMessageView.visibility = View.VISIBLE
        topErrorMessageView.text = error
    }

    override fun setBottomErrorMessage(error: String) {
        bottomErrorMessageView.visibility = View.VISIBLE
        bottomErrorMessageView.text = error
    }

    override fun showTopErrorMessage(show: Boolean) {
        topErrorMessageView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showBottomErrorMessage(show: Boolean) {
        bottomErrorMessageView.visibility = if (show) View.VISIBLE else View.GONE
    }

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
            AlertDialog.Builder(root.context)
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

    private fun findNumericMaskedEditById(viewId: Int) =
            root.findViewById<NumericMaskedEditText>(viewId).apply {
                onTextChangeListener = this@CardPaymentView
                onFocusChangeListener = this@CardPaymentView

            }
}