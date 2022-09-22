package payment.sdk.android.cardpayment.widget

import payment.sdk.android.sdk.R
import payment.sdk.android.cardpayment.CardPaymentContract
import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import java.lang.IllegalArgumentException


open class NumericMaskedEditText : ConstraintLayout, CardPaymentContract.StatefulInput, NumericMaskInputFilter.MaskListener {
    private lateinit var editView: AppCompatEditText
    private lateinit var hintView: AppCompatTextView
    private lateinit var placeHolder: String
    private lateinit var mask: String

    override val full: Boolean
        get() = editView.text?.length == mask.length

    override var dirty: Boolean = false

    override val txt: String
        get() = editView.text.toString()

    override val rawTxt: String
        get() = inputFilter.getRawText(editView.text.toString())

    var onTextChangeListener: OnTextChangeListener? = null

    private val inputFilter: NumericMaskInputFilter by lazy {
        NumericMaskInputFilter(mask, placeHolder, this)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context) : super(context) {
        init(null)
    }

    private fun init(attributes: AttributeSet?) {
        attributes.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.NumericMaskedEditText)
            this.mask = typedArray.getString(R.styleable.NumericMaskedEditText_mask)
                    ?: throw IllegalArgumentException("mask property should be defined")
            this.placeHolder = typedArray.getString(R.styleable.NumericMaskedEditText_placeHolder)
                    ?: createPlaceHolderFromMask(this.mask)
            typedArray.recycle()
        }
    }

    override fun setErrorWhen(predicate: (CardPaymentContract.StatefulInput) -> Boolean): Boolean {
        if (visibility != View.VISIBLE) {
            return false
        }
        return predicate(this).also { hasErrors ->
            if (hasErrors) setBackgroundResource(R.drawable.edittext_error_background)
            else background = null
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        hintView = getChildAt(0) as AppCompatTextView
        hintView.text = placeHolder

        editView = getChildAt(1) as AppCompatEditText
        editView.inputType = InputType.TYPE_CLASS_NUMBER
        editView.onFocusChangeListener = View.OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                dirty = true
            }
            onEditFocusChanged(v, hasFocus)
        }
        editView.filters = arrayOf(inputFilter)
    }


    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (!editView.text.isNullOrEmpty()) {
            hintView.text = editView.text
        }
    }

    private fun onEditFocusChanged(view: View?, hasFocus: Boolean) {
        onFocusChangeListener?.onFocusChange(this, hasFocus)
    }

    @SuppressLint("SetTextI18n")
    override fun onNewText(rawTextValue: String, maskedTextValue: String, cursorPosition: Int) {
        val padded = maskedTextValue + placeHolder.substring(maskedTextValue.length)
        hintView.text = padded
        editView.setText(maskedTextValue)
        editView.setSelection(cursorPosition)

        onTextChangeListener?.onTextChangeListener(this, rawTextValue, maskedTextValue)
    }

    fun setInputMask(newMask: String) {
        if (mask != newMask) {
            mask = newMask
            placeHolder = createPlaceHolderFromMask(mask)
            inputFilter.updateMask(mask, placeHolder, editView.text ?: "")
        }
    }

    private fun createPlaceHolderFromMask(mask: String) =
            mask.replace(NumericMaskInputFilter.MASK_CHAR, NumericMaskInputFilter.PLACEHOLDER_CHAR)


    interface OnTextChangeListener {
        fun onTextChangeListener(view: View, rawTextValue: String, maskedTextValue: String)
    }
}
