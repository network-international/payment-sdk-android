package payment.sdk.android.cardpayment.validation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class EmiratesIdVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > 15) text.text.substring(0, 15) else text.text
        val out = StringBuilder()

        for (i in trimmed.indices) {
            out.append(trimmed[i])
            if (i == 2 || i == 6 || i == 13) {
                out.append('-')
            }
        }

        val translation = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 6 -> offset + 1
                    offset <= 13 -> offset + 2
                    offset <= 15 -> offset + 3
                    else -> out.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 3 -> offset
                    offset <= 8 -> offset - 1
                    offset <= 16 -> offset - 2
                    offset <= 19 -> offset - 3
                    else -> trimmed.length
                }
            }
        }

        return TransformedText(AnnotatedString(out.toString()), translation)
    }
}