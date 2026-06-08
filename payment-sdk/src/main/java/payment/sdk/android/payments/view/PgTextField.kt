package payment.sdk.android.payments.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import payment.sdk.android.core.testId
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgSize
import payment.sdk.android.payments.theme.PgType
import payment.sdk.android.payments.theme.Radius
import payment.sdk.android.payments.theme.Spacing

// ─── String overload ──────────────────────────────────────────────────────────

@Composable
fun PgTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    trailingIcon: (@Composable () -> Unit)? = null,
    statusLine: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorText: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    testTag: String = ""
) {
    // Preserve cursor across recompositions. A fresh TextFieldValue(value) on every recompose
    // resets selection to TextRange.Zero, which makes backspace appear to do nothing and pushes
    // every new character to position 0. We hold the TextFieldValue ourselves and only resync
    // when the external value diverges (e.g. parent rejected/filtered the input).
    var fieldState by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    if (fieldState.text != value) {
        val newSelection = fieldState.selection.end.coerceAtMost(value.length)
        fieldState = fieldState.copy(text = value, selection = TextRange(newSelection))
    }
    PgTextFieldImpl(
        modifier = modifier,
        value = fieldState,
        onValueChange = { newValue ->
            fieldState = newValue
            if (newValue.text != value) onValueChange(newValue.text)
        },
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        statusLine = statusLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = isError,
        errorText = errorText,
        onFocusChanged = onFocusChanged,
        testTag = testTag
    )
}

// ─── TextFieldValue overload ──────────────────────────────────────────────────

@Composable
fun PgTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String = "",
    trailingIcon: (@Composable () -> Unit)? = null,
    statusLine: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorText: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    testTag: String = ""
) {
    PgTextFieldImpl(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        statusLine = statusLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = isError,
        errorText = errorText,
        onFocusChanged = onFocusChanged,
        testTag = testTag
    )
}

// ─── Shared implementation ────────────────────────────────────────────────────

@Composable
private fun PgTextFieldImpl(
    modifier: Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String,
    trailingIcon: (@Composable () -> Unit)?,
    statusLine: (@Composable () -> Unit)?,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation,
    isError: Boolean,
    errorText: String?,
    onFocusChanged: (Boolean) -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // Error border takes precedence over focused/idle so a wrong value remains
    // visually flagged even while the user is editing it.
    val borderColor = when {
        isError -> PgColors.borderInputError
        isFocused -> PgColors.borderInputFocused
        else -> PgColors.borderInput
    }
    val shape = RoundedCornerShape(Radius.input)

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = label,
            style = PgType.labelField,
            color = PgColors.textPrimary
        )
        Spacer(Modifier.height(Spacing.fieldLabelGap))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PgSize.inputMinHeight)
                .border(1.dp, borderColor, shape)
                .background(Color.White, shape)
                .onFocusChanged { state -> onFocusChanged(state.isFocused) }
                .let { if (testTag.isNotEmpty()) it.testId(testTag) else it },
            textStyle = PgType.bodyInput.copy(color = PgColors.textPrimary),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = PgType.bodyPlaceholder,
                                color = PgColors.textMuted
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            }
        )
        if (isError && !errorText.isNullOrEmpty()) {
            Spacer(Modifier.height(Spacing.fieldLabelGap))
            Text(
                text = errorText,
                style = PgType.bodyRowSubtitle,
                color = PgColors.textError
            )
        }
        statusLine?.invoke()
    }
}
