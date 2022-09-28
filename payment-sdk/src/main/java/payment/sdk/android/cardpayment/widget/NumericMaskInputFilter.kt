package payment.sdk.android.cardpayment.widget

import androidx.annotation.IntRange
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.Spanned

internal class NumericMaskInputFilter(
        private var mask: String,
        private var placeHolder: String,
        private val listener: MaskListener
) : InputFilter {

    init {
        if (mask.length != placeHolder.length) {
            throw IllegalArgumentException("Mask (${mask.length}) and placeHolder(${placeHolder.length}) sizes should be the same")
        }
    }

    override fun filter(source: CharSequence, sourceStart: Int, sourceEnd: Int,
                        destinationSpanned: Spanned, start: Int, end: Int): CharSequence? {

        if (destinationSpanned !is SpannableStringBuilder) {
            return null
        }

        if (!source.all { chr -> chr.isDigit() }) {
            return NO_TEXT_UPDATE
        }

        val builder = StringBuilder()
        builder.append(destinationSpanned.toString())

        val cursorPosition: Int

        if (source.isNotEmpty()) {
            if (start >= builder.length) {
                // Append
                cursorPosition = appendEnd(builder, start, source)
            } else {
                // Insert + Shift Right
                val newText = StringBuilder()
                newText.append(destinationSpanned.toString())
                newText.replace(start, end, source.toString())
                if (newText.length > mask.length) {
                     return NO_TEXT_UPDATE
                }
                builder.replace(start, end, source.toString())
                cursorPosition = if (mask[start] == ' ' || mask[start] == '/') start + 2 else start + 1
            }
        } else {
            // Delete + Shift Left
            builder.delete(start, end)
            cursorPosition = if (start > 0 && (mask[start - 1] == ' ' || mask[start - 1] == '/')) start - 1 else start
        }
        removeSpace(builder)
        addSpacing(builder)
        listener.onNewText(getRawText(builder), builder.toString(), cursorPosition)

        return NO_TEXT_UPDATE
    }

    fun getRawText(text: CharSequence): String {
        val rawBuilder = StringBuilder()
        text.forEachIndexed { index, chr ->
            if (isMaskChar(index))
                rawBuilder.append(chr)
        }
        return rawBuilder.toString()
    }

    private fun addSpacing(builder: StringBuilder) {
        for ((index, char) in builder.withIndex()) {
            if (index > 0 && mask[index] != MASK_CHAR && char != mask[index]) {
                builder.insert(index, mask[index])
            }
        }
    }

    private fun removeSpace(builder: StringBuilder) {
        for ((index, char) in builder.withIndex()) {
            if (char == ' ' || char == '/') {
                builder.deleteCharAt(index)
            }
        }
    }

    private fun nextMaskCharPosition(@IntRange(from = 0) start: Int): Int {
        return mask.indexOf(MASK_CHAR, start)
    }

    private fun appendEnd(builder: StringBuilder, @IntRange(from = 0) start: Int, source: CharSequence): Int {
        var next = start
        source.forEach { chr ->
            next = appendEnd(builder, next, chr)
        }
        return next
    }

    private fun appendEnd(builder: StringBuilder, @IntRange(from = 0) start: Int, chr: Char): Int {
        val next = nextMaskCharPosition(start)
        if (next == -1) {
            return builder.length
        }
        if (next > builder.length) {
            for (i in builder.length until next) {
                builder.append(placeHolder[i])
            }
        }
        builder.append(chr)
        return builder.length
    }

    private fun isMaskChar(@IntRange(from = 0) index: Int): Boolean {
        return (index < mask.length && mask[index] == MASK_CHAR)
    }

    fun updateMask(newMask: String, newPlaceHolder: String, text: CharSequence) {
        require(value = newMask.length == newPlaceHolder.length)
        val existingRawText = getRawText(text)
        mask = newMask
        placeHolder = newPlaceHolder
        filter(existingRawText, 0, existingRawText.length, SpannableStringBuilder(), 0, 0)
    }

    internal interface MaskListener {
        fun onNewText(rawTextValue: String, maskedTextValue: String, @IntRange(from = 0) cursorPosition: Int)
    }

    companion object {
        private const val NO_TEXT_UPDATE = ""
        const val MASK_CHAR = '#'
        const val PLACEHOLDER_CHAR = '0'
    }
}
