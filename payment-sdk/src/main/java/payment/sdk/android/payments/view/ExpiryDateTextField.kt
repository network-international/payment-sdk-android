package payment.sdk.android.payments.view

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import payment.sdk.android.core.testId
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.sdk.R
import java.util.Calendar

@Composable
fun ExpiryDateTextField(
    modifier: Modifier = Modifier,
    text: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusCvv: () -> Unit,
    isError: Boolean = false,
    errorText: String? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    PgTextField(
        modifier = modifier.testId("sdk_cardinput_field_expiry"),
        value = text,
        onValueChange = { newValue ->
            // Detect backspace over the auto-inserted slash: previous value
            // ended with "/", user removed only that character. Treat as if
            // they removed the preceding digit too, so the field can shrink
            // back instead of being trapped at "12/".
            val previous = text.text
            if (previous.endsWith("/") && newValue.text == previous.dropLast(1)) {
                val collapsed = previous.dropLast(2)
                onValueChange(newValue.copy(text = collapsed, selection = TextRange(collapsed.length)))
                return@PgTextField
            }

            if (newValue.text.length > 5) return@PgTextField
            val digits = newValue.text.filter { it.isDigit() }
            if (digits.isEmpty()) {
                onValueChange(newValue.copy(text = "", selection = TextRange(0)))
                return@PgTextField
            }
            // Always insert the slash starting from the 2nd digit so the
            // MM/YY mask is visible while typing (matches iOS behavior).
            val formatted = if (digits.length >= 2) {
                digits.take(2) + "/" + digits.drop(2)
            } else {
                digits
            }
            if (isValidExpiryWhileTyping(formatted)) {
                onValueChange(newValue.copy(text = formatted, selection = TextRange(formatted.length)))
                if (formatted.length == 5) focusCvv()
            }
        },
        label = stringResource(R.string.expiration_date_label),
        // Placeholder is empty — the visual mask renders "MM/YY" itself so we
        // don't want the PgTextField placeholder text overlapping it.
        placeholder = "",
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        visualTransformation = ExpiryMaskVisualTransformation,
        isError = isError,
        errorText = errorText,
        onFocusChanged = onFocusChanged
    )
}

/**
 * Always renders the "MM/YY" mask. Typed characters in the field's primary
 * text color, untyped mask positions in the muted placeholder color. The "/"
 * stays in the primary color so the separator reads as a fixed structural
 * element (matching iOS, where it's a separate label between two fields).
 */
private const val EXPIRY_MASK = "MM/YY"

private val ExpiryMaskVisualTransformation = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val typed = text.text
        val builder = AnnotatedString.Builder()
        for (i in EXPIRY_MASK.indices) {
            when {
                i < typed.length -> builder.append(typed[i])
                EXPIRY_MASK[i] == '/' -> builder.append('/')
                else -> builder.withStyle(SpanStyle(color = PgColors.textMuted)) {
                    append(EXPIRY_MASK[i])
                }
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset.coerceAtMost(EXPIRY_MASK.length)
            override fun transformedToOriginal(offset: Int): Int =
                offset.coerceAtMost(typed.length)
        }
        return TransformedText(builder.toAnnotatedString(), offsetMapping)
    }
}

/**
 * Permissive validator used during entry: accepts partial states (e.g. "1",
 * "12/", "12/3") that the production `ExpireDateEditText.isValidExpire` would
 * reject or crash on (empty year piece). The final "MM/YY" form still gets a
 * full date-in-future check.
 */
private fun isValidExpiryWhileTyping(formatted: String): Boolean {
    if (formatted.isEmpty()) return false
    val parts = formatted.split('/')
    val monthStr = parts[0]
    val yearStr = if (parts.size == 2) parts[1] else ""

    val monthOk = when (monthStr.length) {
        1 -> monthStr.toIntOrNull() in 0..1
        2 -> monthStr.toIntOrNull() in 1..12
        else -> false
    }
    if (!monthOk) return false

    return when (yearStr.length) {
        0 -> true
        1 -> yearStr.toIntOrNull() != null
        2 -> {
            val year = yearStr.toIntOrNull() ?: return false
            val month = monthStr.toInt()
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.YEAR, 2000 + year)
            cal > Calendar.getInstance()
        }
        else -> false
    }
}
