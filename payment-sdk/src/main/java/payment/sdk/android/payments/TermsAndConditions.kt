import android.view.View
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.text.TextUtilsCompat
import payment.sdk.android.payments.resolveTermsConfig
import payment.sdk.android.sdk.R
import java.util.Locale

@Composable
fun TermsAndConditionsConsent(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isSubscriptionOrder: Boolean,
    termsUrl: String,
    orderType: String
) {
    val isRtl = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL

    val termsConfig = resolveTermsConfig(orderType, isSubscriptionOrder)

    val annotatedText = buildAnnotatedString {
        append(termsConfig.termsText)

        var startIndex = termsConfig.termsText.indexOf(termsConfig.linkText)

        while (startIndex >= 0) {
            val endIndex = startIndex + termsConfig.linkText.length

            addLink(
                LinkAnnotation.Url(
                    url = termsUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = colorResource(
                                id = R.color.payment_sdk_pay_button_background_color
                            ),
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                ),
                start = startIndex,
                end = endIndex
            )

            startIndex = termsConfig.termsText.indexOf(
                termsConfig.linkText,
                startIndex = endIndex
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (isRtl) {
            TermsText(annotatedText)
            Spacer(Modifier.width(6.dp))
            TermsCheckbox(checked, onCheckedChange)
        } else {
            TermsCheckbox(checked, onCheckedChange)
            Spacer(Modifier.width(6.dp))
            TermsText(annotatedText)
        }
    }
}

@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = colorResource(
                id = R.color.payment_sdk_pay_button_background_color
            )
        )
    )
}

@Composable
private fun RowScope.TermsText(text: AnnotatedString) {
    Text(
        text = text,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.caption.copy(color = Color.Gray),
        textAlign = TextAlign.Start
    )
}

