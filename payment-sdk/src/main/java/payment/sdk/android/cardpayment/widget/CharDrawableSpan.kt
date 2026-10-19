package payment.sdk.android.cardpayment.widget

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import payment.sdk.android.sdk.R
import androidx.core.content.ContextCompat
import android.text.style.DynamicDrawableSpan

internal class CharDrawableSpan(
        private val context: Context,
        private val resourceId: Int,
        private val height: Int) : DynamicDrawableSpan() {

    override fun getDrawable() =
            (ContextCompat.getDrawable(context, resourceId)!!.mutate() as BitmapDrawable).apply {
                colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(context, R.color.payment_sdk_card_text_color),
                    PorterDuff.Mode.MULTIPLY
                )
                val width = (intrinsicWidth * (height.toFloat() / intrinsicHeight.toFloat())).toInt()
                setBounds(0, 0, width, height)
            }
}