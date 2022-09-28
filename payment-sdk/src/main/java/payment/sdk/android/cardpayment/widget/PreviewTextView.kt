package payment.sdk.android.cardpayment.widget

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import android.text.Spannable
import android.util.AttributeSet
import android.widget.TextView

internal class PreviewTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttrs: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttrs) {

    /**
     * https://medium.com/androiddevelopers/spantastic-text-styling-with-spans-17b0c16b4568
     */
    override fun setText(text: CharSequence, type: TextView.BufferType) {
        super.setText(getSpannableText(text), TextView.BufferType.SPANNABLE)
    }

    private fun getSpannableText(chars: CharSequence): Spannable {
        return Spannable.Factory.getInstance().newSpannable(chars).apply {
            for (i in 0 until chars.length) {
                setSpan(charSpan(chars[i]), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun charSpan(chr: Char): CharDrawableSpan =
            CharResources.getDrawableResource(chr)?.let { resourceId ->
                return CharDrawableSpan(context, resourceId, lineHeight)
            } ?: CharDrawableSpan(context, CharResources.getDrawableResource(' ')!!, height)
}


