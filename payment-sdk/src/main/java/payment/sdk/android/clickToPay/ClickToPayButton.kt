package payment.sdk.android.clickToPay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.sdk.R

@Composable
fun ClickToPayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.blue)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.click_to_pay),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.width(18.dp))
            Image(
                painter = painterResource(id = R.drawable.ctp_blue),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .width(1.dp)
                    .background(color = Color.Black.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_logo_visa),
                contentDescription = "Visa",
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(6.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_logo_mastercard),
                contentDescription = "Mastercard",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
