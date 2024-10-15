package payment.sdk.android.cardpayment.visaInstalments.view

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.payments.theme.SDKTheme
import payment.sdk.android.cardpayment.util.extractUrls
import payment.sdk.android.cardpayment.visaInstalments.model.InstallmentPlan
import payment.sdk.android.cardpayment.visaInstalments.model.PlanFrequency
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.sdk.R

@Composable
fun VisaPlanTermsView(
    isTermsAccepted: Boolean,
    isSelected: Boolean,
    frequency: PlanFrequency,
    termsExpanded: Boolean,
    termsAndCondition: TermsAndCondition,
    onTermsAccepted: (Boolean) -> Unit,
    onTermsExpanded: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = isSelected && frequency != PlanFrequency.PayInFull,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (termsExpanded || isTermsAccepted) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = {
                            onTermsAccepted(it)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF1D33C3),
                            uncheckedColor = Color(0xFF808080),
                            disabledColor = Color.Gray,
                        ),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(13.dp)
                            .size(20.dp)
                            .clip(shape = RoundedCornerShape(2.dp))
                            .background(Color.Gray),
                    ) {}
                }

                Text(
                    modifier = Modifier,
                    text = stringResource(id = R.string.visa_terms_and_conditions)
                )

                TextButton(onClick = {
                    onTermsExpanded(!termsExpanded)
                }) {
                    Text(
                        text = if (termsExpanded) stringResource(id = R.string.visa_read_less) else stringResource(
                            id = R.string.visa_read_more
                        ),
                        style = TextStyle(
                            color = Color(0xFF1D33C3),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            val annotatedString = buildAnnotatedString {
                val text = termsAndCondition.formattedText()
                append(text)
                text.extractUrls().forEach { (url, startIndex, endIndex) ->
                    addStyle(
                        style = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = Color.Blue
                        ), start = startIndex, end = endIndex
                    )
                    addStringAnnotation(
                        url,
                        url,
                        startIndex,
                        endIndex
                    )
                }
            }

            AnimatedVisibility(
                visible = termsExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                ClickableText(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    text = annotatedString,
                    style = TextStyle(fontSize = 12.sp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(offset, offset)
                            .firstOrNull()?.let { span ->
                                context.startActivity(Intent(Intent.ACTION_VIEW).also {
                                    it.setData(Uri.parse(span.item))
                                })
                            }
                    }
                )
            }
        }
    }
}


@Preview(name = "Terms View", device = Devices.PIXEL_4_XL)
@Composable
fun VisaPlanTermsViewView_Preview() {
    SDKTheme {
        Box(modifier = Modifier.background(Color.White)) {
            VisaPlanTermsView(
                isTermsAccepted = false,
                termsExpanded = true,
                onTermsExpanded = {
                },
                onTermsAccepted = {},
                frequency = PlanFrequency.MONTHLY,
                isSelected = true,
                termsAndCondition = InstallmentPlan.dummyInstallmentPlan.terms!!
            )
        }
    }
}