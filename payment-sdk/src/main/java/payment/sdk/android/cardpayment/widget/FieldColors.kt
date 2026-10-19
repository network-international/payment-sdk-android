package payment.sdk.android.cardpayment.widget

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import payment.sdk.android.sdk.R

/**
 * Applies payment_sdk_text_field_text_color / payment_sdk_text_field_placeholder_color.
 * Both default to transparent, meaning "not set": the field then keeps its default colour.
 */
internal object FieldColors {

    /** Typed text colour; when not set, the label colour, as iOS does. */
    fun applyText(view: TextView) {
        view.setTextColor(
            colorOrNull(view, R.color.payment_sdk_text_field_text_color)
                ?: ContextCompat.getColor(view.context, R.color.payment_sdk_floating_hint_color)
        )
    }

    fun applyPlaceholder(view: TextView) {
        colorOrNull(view, R.color.payment_sdk_text_field_placeholder_color)?.let(view::setTextColor)
    }

    fun applyPlaceholderAsHint(view: TextView) {
        colorOrNull(view, R.color.payment_sdk_text_field_placeholder_color)?.let(view::setHintTextColor)
    }

    /**
     * The masked fields draw the placeholder underneath the typed text, typed characters
     * included. With a custom placeholder colour that shows as doubled digits, so hide the
     * already-typed part of the placeholder. Unchanged when no placeholder colour is set.
     */
    fun placeholderText(view: TextView, padded: String, typedLength: Int): CharSequence {
        if (colorOrNull(view, R.color.payment_sdk_text_field_placeholder_color) == null) return padded
        return SpannableString(padded).apply {
            setSpan(
                ForegroundColorSpan(Color.TRANSPARENT), 0, typedLength.coerceAtMost(padded.length),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun colorOrNull(view: TextView, id: Int): Int? =
        ContextCompat.getColor(view.context, id).takeIf { it != Color.TRANSPARENT }
}
