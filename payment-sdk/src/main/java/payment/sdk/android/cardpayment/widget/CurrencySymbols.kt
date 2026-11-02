package payment.sdk.android.cardpayment.widget

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import payment.sdk.android.sdk.R

/**
 * The currency codes drawn as their symbol instead of the letters: UAE Dirham and Saudi Riyal.
 * Compose screens use [payment.sdk.android.payments.view.CurrencyAmountText]; views use [replace].
 */
internal object CurrencySymbols {

    class Symbol(val drawable: Int, val aspectRatio: Float)

    /**
     * Left-to-right mark placed on both sides of a symbol. Text layout treats an image span as a
     * direction-neutral object, so in Arabic it would join the word before it ("إدفع ⃁ 450.0" read
     * right to left); the marks keep it with the amount, as the Latin code "SAR" was.
     */
    const val LTR_MARK = "\u200E"

    val symbols = mapOf(
        "AED" to Symbol(R.drawable.ic_aed_symbol, 330f / 288f),
        "SAR" to Symbol(R.drawable.btn_riyal, 1124.1f / 1256.4f),
    )

    /** [text] split into plain parts (code = null) and currency codes. */
    fun split(text: String): List<Pair<String, String?>> {
        val parts = mutableListOf<Pair<String, String?>>()
        var cursor = 0
        while (cursor < text.length) {
            val next = symbols.keys
                .map { it to text.indexOf(it, cursor) }
                .filter { it.second >= 0 }
                .minByOrNull { it.second }
            if (next == null) {
                parts += text.substring(cursor) to null
                break
            }
            val (code, index) = next
            if (index > cursor) parts += text.substring(cursor, index) to null
            parts += code to code
            cursor = index + code.length
        }
        return parts
    }

    /** [text] with each currency code drawn as its symbol in [color], sized to [textSizePx]. */
    fun replace(context: Context, text: CharSequence, color: Int, textSizePx: Float): CharSequence {
        val builder = SpannableStringBuilder()
        split(text.toString()).forEach { (part, code) ->
            val symbol = code?.let { symbols[it] }
            val drawable = symbol?.let { ContextCompat.getDrawable(context, it.drawable)?.mutate() }
            if (symbol == null || drawable == null) {
                builder.append(part)
                return@forEach
            }
            val height = (textSizePx * 0.75f).toInt()
            drawable.setBounds(0, 0, (height * symbol.aspectRatio).toInt(), height)
            drawable.setTint(color)
            builder.append(LTR_MARK)
            val start = builder.length
            builder.append(code)
            builder.setSpan(ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE), start, builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append(LTR_MARK)
        }
        return builder
    }
}
