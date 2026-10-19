package payment.sdk.android.cardpayment.widget

import android.graphics.Color
import android.widget.TextView
import androidx.core.content.ContextCompat
import payment.sdk.android.sdk.R

/**
 * Applies payment_sdk_text_field_text_color / payment_sdk_text_field_placeholder_color.
 * Both default to transparent, meaning "not set": the field then keeps its default colour.
 */
internal object FieldColors {

    fun applyText(view: TextView) {
        colorOrNull(view, R.color.payment_sdk_text_field_text_color)?.let(view::setTextColor)
    }

    fun applyPlaceholder(view: TextView) {
        colorOrNull(view, R.color.payment_sdk_text_field_placeholder_color)?.let(view::setTextColor)
    }

    fun applyPlaceholderAsHint(view: TextView) {
        colorOrNull(view, R.color.payment_sdk_text_field_placeholder_color)?.let(view::setHintTextColor)
    }

    private fun colorOrNull(view: TextView, id: Int): Int? =
        ContextCompat.getColor(view.context, id).takeIf { it != Color.TRANSPARENT }
}
