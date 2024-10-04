package payment.sdk.android.payments

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import payment.sdk.android.cardpayment.card.SpacingPatterns

internal class CreditCardVisualTransformation(
    private val pattern: String = SpacingPatterns.Default
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val maxLength = pattern.count { it == '#' }
        val trimmedText = if (text.text.length > maxLength) {
            text.text.substring(0, maxLength)
        } else {
            text.text
        }

        val maskedText = buildString {
            var index = 0
            for (char in pattern) {
                if (char == '#') {
                    if (index < trimmedText.length) {
                        append(trimmedText[index])
                        index++
                    } else {
                        break
                    }
                } else {
                    append(char)
                }
            }
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset > trimmedText.length) return maskedText.length
                var transformedOffset = offset
                var transformedIndex = 0
                for (i in pattern.indices) {
                    if (pattern[i] == '#') {
                        if (transformedIndex == offset) break
                        transformedIndex++
                    } else {
                        transformedOffset++
                    }
                }
                return transformedOffset.coerceAtMost(maskedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset > maskedText.length) return trimmedText.length
                var originalOffset = offset
                var originalIndex = 0
                for (i in pattern.indices) {
                    if (pattern[i] == '#') {
                        if (originalIndex == offset) break
                        originalIndex++
                    } else {
                        originalOffset--
                    }
                }
                return originalOffset.coerceAtMost(trimmedText.length)
            }
        }

        return TransformedText(AnnotatedString(maskedText), offsetTranslator)
    }
}