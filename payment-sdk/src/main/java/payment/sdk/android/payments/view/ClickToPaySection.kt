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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.sdk.R

@Composable
fun ClickToPaySection(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    clickToPayConfig: ClickToPayLauncher.Config,
    onToggle: () -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentRadioButton(selected = isExpanded)
                Image(
                    painter = painterResource(R.drawable.ic_click_to_pay),
                    contentDescription = stringResource(R.string.click_to_pay_section_title),
                    modifier = Modifier
                        .height(20.dp)
                        .padding(start = 12.dp)
                )
                Text(
                    text = stringResource(R.string.click_to_pay_section_title),
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = { onClickToPay(clickToPayConfig) },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White,
                            contentColor = Color(0xFF333333),
                        ),
                        border = BorderStroke(width = 1.dp, Color(0xFF8F8F8F)),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_click_to_pay),
                            contentDescription = null,
                            modifier = Modifier.height(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pay_with_click_to_pay),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
