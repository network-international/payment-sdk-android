package payment.sdk.android.payments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.qpay.QPayLauncher
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgSize
import payment.sdk.android.payments.theme.PgType
import payment.sdk.android.payments.theme.Radius
import payment.sdk.android.payments.theme.Spacing
import payment.sdk.android.sdk.R
import payment.sdk.android.core.testId

// Figma: "Or select your payment options" section
@Composable
fun OtherPaymentOptionsSection(
    modifier: Modifier = Modifier,
    title: String? = stringResource(R.string.or_select_payment_options),
    selectedOption: PaymentOption?,
    googlePayUiConfig: GooglePayUiConfig?,
    isSamsungPayAvailable: Boolean,
    aaniConfig: AaniPayLauncher.Config?,
    clickToPayConfig: ClickToPayLauncher.Config?,
    qpayConfig: QPayLauncher.Config? = null,
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit,
    onClickQPay: (QPayLauncher.Config) -> Unit = {},
    onOptionSelected: (PaymentOption) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = PgType.headingSection,
                color = PgColors.textPrimary,
                modifier = Modifier.padding(horizontal = Spacing.pageH)
            )
        }

        if (googlePayUiConfig != null) {
            PaymentOptionRow(
                selected = selectedOption == PaymentOption.GOOGLE_PAY,
                label = stringResource(R.string.google_pay_button),
                trailingIcon = {
                    Text(
                        text = "G Pay",
                        style = PgType.bodyRowTitle,
                        color = PgColors.textPrimary
                    )
                },
                onSelect = { onOptionSelected(PaymentOption.GOOGLE_PAY) }
            )
        }

        if (isSamsungPayAvailable) {
            PaymentOptionRow(
                selected = selectedOption == PaymentOption.SAMSUNG_PAY,
                label = stringResource(R.string.samsung_pay_button),
                trailingIcon = {
                    Image(
                        painter = painterResource(R.drawable.samsung_pay_logo),
                        contentDescription = null,
                        modifier = Modifier.size(PgSize.providerLogoHeight),
                        contentScale = ContentScale.Fit
                    )
                },
                onSelect = { onOptionSelected(PaymentOption.SAMSUNG_PAY) }
            )
        }

        if (aaniConfig != null) {
            PaymentOptionRow(
                selected = selectedOption == PaymentOption.AANI,
                label = stringResource(R.string.aani),
                trailingIcon = {
                    Image(
                        painter = painterResource(R.drawable.aani_logo),
                        contentDescription = null,
                        modifier = Modifier.height(40.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                onSelect = { onOptionSelected(PaymentOption.AANI) }
            )
        }

        if (clickToPayConfig != null) {
            PaymentOptionRow(
                selected = selectedOption == PaymentOption.CLICK_TO_PAY,
                label = stringResource(R.string.click_to_pay_section_title),
                trailingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_click_to_pay),
                        contentDescription = null,
                        modifier = Modifier.size(PgSize.providerLogoHeight),
                        contentScale = ContentScale.Fit
                    )
                },
                onSelect = { onOptionSelected(PaymentOption.CLICK_TO_PAY) }
            )
        }

        if (qpayConfig != null) {
            PaymentOptionRow(
                selected = selectedOption == PaymentOption.QPAY,
                label = stringResource(R.string.qpay),
                trailingIcon = {
                    Image(
                        painter = painterResource(R.drawable.qpay_logo),
                        contentDescription = null,
                        modifier = Modifier.size(PgSize.providerLogoHeight),
                        contentScale = ContentScale.Fit
                    )
                },
                onSelect = { onOptionSelected(PaymentOption.QPAY) }
            )
        }
    }
}

// QPay express button — a prominent full-width black CTA pinned above all other
// payment options. Shown (in place of the QPay radio row) only when QPay is enabled.
// Tapping it launches QPay directly, mirroring the web express button.
@Composable
fun QPayExpressButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageH)
            .clip(RoundedCornerShape(Radius.button))
            .background(Color.Black)
            .clickable { onClick() }
            .padding(vertical = 16.dp)
            .testId("sdk_paymentpage_qpay_express"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.qpay_express_pay_with),
            style = PgType.buttonPrimary,
            color = Color.White
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.naps_logo),
            contentDescription = "NAPS",
            // NAPS wordmark is ~4.63:1; size by height and let width follow the aspect ratio.
            modifier = Modifier
                .height(16.dp)
                .aspectRatio(2224f / 480f),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun PaymentOptionRow(
    selected: Boolean,
    label: String,
    trailingIcon: @Composable () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageH)
            .clip(RoundedCornerShape(Radius.row))
            .border(1.dp, PgColors.borderRow, RoundedCornerShape(Radius.row))
            .background(PgColors.surfaceRow)
            .clickable { onSelect() }
            .padding(horizontal = Spacing.rowPaddingH, vertical = Spacing.rowPaddingV)
            .testId("sdk_paymentpage_option_${label.replace(" ", "")}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaymentRadioButton(selected = selected)
        Spacer(Modifier.width(Spacing.rowGap))
        Text(
            text = label,
            style = PgType.bodyRowTitle,
            color = PgColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        trailingIcon()
    }
}
