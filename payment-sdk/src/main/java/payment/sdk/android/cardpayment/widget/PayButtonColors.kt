package payment.sdk.android.cardpayment.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import payment.sdk.android.sdk.R
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Applies payment_sdk_pay_button_disabled_background_color / _text_color, as iOS does with
 * NISdkColors.payButtonDisabledBackgroundColor / payButtonDisabledTitleColor. Both default to
 * transparent, meaning "not set": a disabled button then keeps its previous look.
 */
internal object PayButtonColors {

    /** Card screen: when no disabled colour is set, a disabled button is the normal one, faded. */
    fun apply(button: Button, enabled: Boolean) {
        val context = button.context
        val background = colorOrNull(context, R.color.payment_sdk_pay_button_disabled_background_color)
        val text = colorOrNull(context, R.color.payment_sdk_pay_button_disabled_text_color)
        if (enabled || (background == null && text == null)) {
            button.alpha = if (enabled) 1f else 0.5f
            ViewCompat.setBackgroundTintList(button, null)
            button.setTextColor(ContextCompat.getColorStateList(context, R.color.payment_sdk_pay_button_text))
        } else {
            button.alpha = 1f
            ViewCompat.setBackgroundTintList(button, background?.let(ColorStateList::valueOf))
            button.setTextColor(text ?: ContextCompat.getColor(context, R.color.payment_sdk_pay_button_text_color))
        }
    }

    private fun colorOrNull(context: Context, id: Int): Int? =
        ContextCompat.getColor(context, id).takeIf { it != Color.TRANSPARENT }
}

/** Compose pay buttons: the disabled background, or the SDK's grey when not set. */
@Composable
internal fun payButtonBackgroundColor(enabled: Boolean): ComposeColor {
    if (enabled) return colorResource(id = R.color.payment_sdk_pay_button_background_color)
    val custom = colorResource(id = R.color.payment_sdk_pay_button_disabled_background_color)
    return if (custom != ComposeColor.Transparent) custom else colorResource(id = R.color.payment_sdk_disabled_color)
}

/** Compose pay buttons: the disabled text colour, or the normal one when not set. */
@Composable
internal fun payButtonTextColor(enabled: Boolean): ComposeColor {
    val normal = colorResource(id = R.color.payment_sdk_pay_button_text_color)
    if (enabled) return normal
    val custom = colorResource(id = R.color.payment_sdk_pay_button_disabled_text_color)
    return if (custom != ComposeColor.Transparent) custom else normal
}
