package payment.sdk.android.payments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import payment.sdk.android.sdk.R

/**
 * Renders [text] as a `Text` but substitutes every occurrence of the literal `"AED"` with
 * the UAE Dirham symbol image (`ic_aed_symbol`). Falls back to a plain `Text` when no
 * token is present so existing call sites that pass non-AED strings render unchanged.
 *
 * The placeholder is sized in em units so the symbol scales with the surrounding font.
 */
@Composable
fun AedAmountText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (!text.contains(TOKEN)) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
        )
        return
    }

    val annotated = remember(text) {
        buildAnnotatedString {
            var cursor = 0
            while (cursor < text.length) {
                val idx = text.indexOf(TOKEN, cursor)
                if (idx == -1) {
                    append(text.substring(cursor))
                    break
                }
                append(text.substring(cursor, idx))
                appendInlineContent(INLINE_ID, TOKEN)
                cursor = idx + TOKEN.length
            }
        }
    }

    val inline = mapOf(
        INLINE_ID to InlineTextContent(
            Placeholder(
                width = 0.9.em,
                height = 0.7.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            )
        ) {
            Image(
                painter = painterResource(R.drawable.ic_aed_symbol),
                contentDescription = "AED",
                contentScale = ContentScale.Fit,
                colorFilter = if (color != Color.Unspecified) ColorFilter.tint(color) else null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    )

    Text(
        text = annotated,
        inlineContent = inline,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

private const val TOKEN = "AED"
private const val INLINE_ID = "aed_symbol"
