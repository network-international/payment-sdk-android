package payment.sdk.android.payments.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.sdk.R

@Composable
fun OtherPaymentOptionsSection(
    modifier: Modifier = Modifier,
    selectedOption: PaymentOption?,
    googlePayUiConfig: GooglePayUiConfig?,
    isSamsungPayAvailable: Boolean,
    aaniConfig: AaniPayLauncher.Config?,
    clickToPayConfig: ClickToPayLauncher.Config?,
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit,
    onOptionSelected: (PaymentOption) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.select_other_payment_options),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF070707),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        var isFirst = true

        if (googlePayUiConfig != null) {
            if (!isFirst) {
                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            }
            isFirst = false
            val icon: @Composable () -> Unit = {
                Text(
                    text = "G Pay",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF070707),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            PaymentOptionItem(
                selected = selectedOption == PaymentOption.GOOGLE_PAY,
                label = stringResource(R.string.google_pay_button),
                buttonLabel = stringResource(R.string.pay_with_google_pay),
                iconContent = icon,
                buttonIconContent = icon,
                onSelect = { onOptionSelected(PaymentOption.GOOGLE_PAY) },
                onPayClick = { onGooglePay() }
            )
        }

        if (isSamsungPayAvailable) {
            if (!isFirst) {
                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            }
            isFirst = false
            val icon: @Composable () -> Unit = {
                Image(
                    painter = painterResource(R.drawable.samsung_pay_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(8.dp))
            }
            PaymentOptionItem(
                selected = selectedOption == PaymentOption.SAMSUNG_PAY,
                label = stringResource(R.string.samsung_pay_button),
                buttonLabel = stringResource(R.string.pay_with_samsung_pay),
                iconContent = icon,
                buttonIconContent = icon,
                onSelect = { onOptionSelected(PaymentOption.SAMSUNG_PAY) },
                onPayClick = { onSamsungPay() }
            )
        }

        if (aaniConfig != null) {
            if (!isFirst) {
                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            }
            isFirst = false
            val icon: @Composable () -> Unit = {
                Image(
                    painter = painterResource(R.drawable.aani_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(8.dp))
            }
            PaymentOptionItem(
                selected = selectedOption == PaymentOption.AANI,
                label = stringResource(R.string.aani),
                buttonLabel = stringResource(R.string.pay_with_aani),
                iconContent = icon,
                buttonIconContent = icon,
                onSelect = { onOptionSelected(PaymentOption.AANI) },
                onPayClick = { onClickAaniPay(aaniConfig) }
            )
        }

        if (clickToPayConfig != null) {
            if (!isFirst) {
                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            }
            val icon: @Composable () -> Unit = {
                Image(
                    painter = painterResource(R.drawable.ic_click_to_pay),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(8.dp))
            }
            PaymentOptionItem(
                selected = selectedOption == PaymentOption.CLICK_TO_PAY,
                label = stringResource(R.string.click_to_pay_section_title),
                buttonLabel = stringResource(R.string.pay_with_click_to_pay),
                iconContent = icon,
                buttonIconContent = icon,
                onSelect = { onOptionSelected(PaymentOption.CLICK_TO_PAY) },
                onPayClick = { onClickToPay(clickToPayConfig) }
            )
        }
    }
}

@Composable
private fun PaymentOptionItem(
    selected: Boolean,
    label: String,
    buttonLabel: String,
    iconContent: @Composable () -> Unit,
    buttonIconContent: @Composable () -> Unit,
    onSelect: () -> Unit,
    onPayClick: () -> Unit
) {
    Column {
        // Radio row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(start = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaymentRadioButton(selected = selected)
            Spacer(Modifier.width(12.dp))
            iconContent()
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF070707)
            )
        }

        // Expandable "Pay with X" button
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250)),
            modifier = Modifier.clip(RectangleShape)
        ) {
            OutlinedButton(
                onClick = onPayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, bottom = 12.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, Color(0xFF8F8F8F)),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.White,
                    contentColor = Color(0xFF070707)
                )
            ) {
                buttonIconContent()
                Text(
                    text = buttonLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF070707)
                )
            }
        }
    }
}
