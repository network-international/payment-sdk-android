package payment.sdk.android.cardpayment.widget

import android.support.annotation.IntRange
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

        if (destinationSpanned.length >= mask.length && source.isNotEmpty()) {
            return NO_TEXT_UPDATE
        }

        if (!source.all { chr -> chr.isDigit() }) {
            return NO_TEXT_UPDATE
        }

        val builder = StringBuilder()
        builder.append(destinationSpanned.toString())

        val cursorPosition: Int

        if (source.isNotEmpty()) {
            val nextMaskPosition = nextMaskCharPosition(start)

            if (nextMaskPosition >= builder.length) {
                // Append
                cursorPosition = appendEnd(builder, nextMaskPosition, source)
            } else {
                // Insert + Shift Right
                val keep = builder.substring(nextMaskPosition)

                builder.replace(nextMaskPosition, nextMaskPosition + source.length, source.toString())
                builder.delete(nextMaskPosition + source.length, builder.length)

                var lastAvailableIndex = nextMaskCharPosition(nextMaskPosition + 1)
                for ((index, chr) in keep.withIndex()) {
                    if (isMaskChar(nextMaskPosition + index)) {
                        lastAvailableIndex = appendEnd(builder, lastAvailableIndex, chr)
                    }
                }
                cursorPosition = nextMaskPosition + source.length
            }
        } else {
            // Delete + Shift Left
            val keep = builder.substring(end, builder.length)
            builder.delete(start, builder.length)
            dropLastUntil(builder) { index ->
                mask[index] == MASK_CHAR
            }

            var lastAvailableIndex = builder.length
            for ((index, chr) in keep.withIndex()) {
                if (isMaskChar(end + index)) {
                    lastAvailableIndex = appendEnd(builder, lastAvailableIndex, chr)
                }
            }
            cursorPosition = lastAvailableIndex
        }

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

    private fun nextMaskCharPosition(@IntRange(from = 0) start: Int): Int {
        return mask.indexOf(MASK_CHAR, start)
    }

    private inline fun dropLastUntil(builder: StringBuilder, predicate: (Int) -> Boolean) {
        loop@ for (index in builder.lastIndex downTo 0) {
            if (predicate(index)) {
                break@loop
            } else {
                builder.deleteCharAt(builder.lastIndex)
            }
        }
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
