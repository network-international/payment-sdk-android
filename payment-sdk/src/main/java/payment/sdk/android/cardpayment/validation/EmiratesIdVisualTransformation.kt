package payment.sdk.android.cardpayment.validation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal class EmiratesIdVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val userInput = if (text.text.length > 12) text.text.substring(0, 12) else text.text
        val out = StringBuilder(FIXED_PREFIX)

        for (i in userInput.indices) {
            out.append(userInput[i])
            if (i == 3 || i == 10) {
                out.append('-')
            }
        }

        val translation = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 3 -> offset + FIXED_PREFIX.length
                    offset <= 10 -> offset + FIXED_PREFIX.length + 1
                    else -> out.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= FIXED_PREFIX.length -> 0
                    offset <= FIXED_PREFIX.length + 4 -> offset - FIXED_PREFIX.length
                    offset <= FIXED_PREFIX.length + 12 -> offset - FIXED_PREFIX.length - 1
                    else -> userInput.length
                }
            }
        }
        return TransformedText(AnnotatedString(out.toString()), translation)
    }

    companion object {
        private const val FIXED_PREFIX = "784-"
    }
}