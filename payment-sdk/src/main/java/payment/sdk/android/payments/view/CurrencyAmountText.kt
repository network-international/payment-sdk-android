package payment.sdk.android.payments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import payment.sdk.android.cardpayment.widget.CurrencySymbols

/**
 * Renders [text] as a `Text`, drawing each currency code [CurrencySymbols] knows ("AED",
 * "SAR") as its symbol image, tinted to the text colour. Text without a code renders as a
 * plain `Text`.
 */
@Composable
fun CurrencyAmountText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (CurrencySymbols.symbols.keys.none { text.contains(it) }) {
        Text(text, modifier, color, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, style = style)
        return
    }

    val annotated = remember(text) {
        buildAnnotatedString {
            CurrencySymbols.split(text).forEach { (part, code) ->
                if (code == null) append(part) else appendInlineContent(code, code)
            }
        }
    }
    val tint = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val inline = CurrencySymbols.symbols.mapValues { (code, symbol) ->
        InlineTextContent(
            Placeholder(
                width = (0.75 * symbol.aspectRatio).em,
                height = 0.75.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            )
        ) {
            Image(
                painter = painterResource(symbol.drawable),
                contentDescription = code,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

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
