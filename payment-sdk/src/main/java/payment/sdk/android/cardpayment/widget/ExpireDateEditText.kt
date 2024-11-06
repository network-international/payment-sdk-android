package payment.sdk.android.cardpayment.widget

import android.content.Context
import android.util.AttributeSet
import java.util.*

open class ExpireDateEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttrs: Int = 0
) : NumericMaskedEditText(context, attrs, defStyleAttrs) {

    override fun onNewText(rawTextValue: String, maskedTextValue: String, cursorPosition: Int) {
        if (maskedTextValue.isEmpty() || isValidExpire(maskedTextValue)) {
            super.onNewText(rawTextValue, maskedTextValue, cursorPosition)
        } else {
            super.onNewText(this.rawTxt, this.txt, this.txt.length)
        }
    }

    companion object {

        internal fun isValidExpire(maskedText: String): Boolean {
            if (maskedText.filter { it.isDigit() || it == '/' }.isBlank()) {
                return false
            }
            return maskedText.split('/').run {
                when (size) {
                    1 -> {
                        if (this[0].length == 1)
                            (0..1).contains(this[0].toInt())
                        else
                            (1..12).contains(this[0].toInt())
                    }

                    2 -> {
                        if (this[1].length == 1)
                            (1..9).contains(this[1].toInt())
                        else {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                            calendar.set(Calendar.MONTH, this[0].toInt() - 1)
                            calendar.set(Calendar.YEAR, 2000 + this[1].toInt())
                            calendar > Calendar.getInstance()
                        }
                    }

                    else -> false
                }
            }
        }
    }
}
