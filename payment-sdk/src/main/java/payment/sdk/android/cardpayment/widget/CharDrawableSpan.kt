package payment.sdk.android.cardpayment.widget

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import android.text.style.DynamicDrawableSpan

internal class CharDrawableSpan(
        private val context: Context,
        private val resourceId: Int,
        private val height: Int) : DynamicDrawableSpan() {

    override fun getDrawable() =
            (ContextCompat.getDrawable(context, resourceId) as BitmapDrawable).apply {
                val width = (intrinsicWidth * (height.toFloat() / intrinsicHeight.toFloat())).toInt()
                setBounds(0, 0, width, height)
            }
}