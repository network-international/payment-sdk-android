package payment.sdk.android.cardpayment.widget

import payment.sdk.android.sdk.R
import payment.sdk.android.cardpayment.CardPaymentContract
import android.view.View
import android.widget.EditText

class CardHolderInputDelegate(private val editText: EditText) : CardPaymentContract.StatefulInput {

    override val dirty: Boolean
        get() {
            editText.tag?.let {
                return (editText.tag == true)
            } ?: return false
        }

    override val txt: String
        get() = editText.text.toString()

    override val full: Boolean
        get() = txt.isNotBlank()

    override val rawTxt: String
        get() = txt

    override fun setErrorWhen(predicate: (CardPaymentContract.StatefulInput) -> Boolean): Boolean {
        if (editText.visibility != View.VISIBLE) {
            return false
        }
        return predicate(this).also { hasErrors ->
            if (hasErrors) editText.setBackgroundResource(R.drawable.edittext_error_background)
            else editText.background = null
        }
    }
}