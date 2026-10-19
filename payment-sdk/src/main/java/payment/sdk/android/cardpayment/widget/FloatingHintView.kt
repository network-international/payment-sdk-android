package payment.sdk.android.cardpayment.widget

import payment.sdk.android.sdk.R
import android.content.Context
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.util.LayoutDirection
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnLayout

internal class FloatingHintView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttrs: Int = 0
) : androidx.coordinatorlayout.widget.CoordinatorLayout(context, attrs, defStyleAttrs) {

    private val hint: TextView = TextView(context).apply {
        setTextColor(ContextCompat.getColor(context, R.color.payment_sdk_floating_hint_color))
    }

    var text: String? = null
        set(value) {
            hint.text = value
        }

    init {
        addView(hint)
        layoutDirection = LayoutDirection.LTR
    }

    /**
     * Aligns the hint with the start edge of [v]: its left edge in LTR, its right edge in RTL
     * (e.g. Arabic via SDKConfig.setLanguage). Waits for [v] to be laid out, since the first
     * field can gain focus before it has a position.
     */
    fun animateToAlignViewStart(v: View) {
        v.doOnLayout { target ->
            x = if (target.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                hint.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
                target.x + target.width - hint.measuredWidth
            } else {
                target.x
            }
        }
    }
}