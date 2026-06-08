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
        val raw = text.text

        // Format with the pattern up to its capacity, then append any extra
        // digits unformatted. ISO/IEC 7812 allows PANs up to 19 digits, but
        // most BIN patterns only describe the common 16-digit layout — we
        // must still display whatever the user typed beyond that, not
        // silently truncate it.
        val maskedText = buildString {
            var rawIdx = 0
            for (patternChar in pattern) {
                if (rawIdx >= raw.length) break
                if (patternChar == '#') {
                    append(raw[rawIdx])
                    rawIdx++
                } else {
                    append(patternChar)
                }
            }
            if (rawIdx < raw.length) {
                append(raw.substring(rawIdx))
            }
        }

        // Number of separator (non-'#') characters from the pattern that are
        // actually rendered before raw position `n` (separators only show once
        // they're "reached" by typed digits).
        fun separatorsBeforeRaw(n: Int): Int {
            var separators = 0
            var rawCounted = 0
            for (patternChar in pattern) {
                if (rawCounted >= n) break
                if (patternChar == '#') rawCounted++ else separators++
            }
            return separators
        }

        // Number of separators inserted before visual position `n`.
        fun separatorsBeforeVisual(n: Int): Int {
            var separators = 0
            var visualCounted = 0
            for (patternChar in pattern) {
                if (visualCounted >= n) break
                visualCounted++
                if (patternChar != '#') separators++
            }
            return separators
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val capped = offset.coerceAtMost(raw.length)
                return (capped + separatorsBeforeRaw(capped))
                    .coerceAtMost(maskedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val capped = offset.coerceAtMost(maskedText.length)
                return (capped - separatorsBeforeVisual(capped))
                    .coerceAtLeast(0)
                    .coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(maskedText), offsetTranslator)
    }
}