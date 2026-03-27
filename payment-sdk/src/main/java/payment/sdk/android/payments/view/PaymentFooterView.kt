package payment.sdk.android.payments.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.core.CardType
import payment.sdk.android.savedCard.view.getCardImage
import payment.sdk.android.sdk.R

@Composable
fun PaymentFooterView(
    modifier: Modifier = Modifier,
    supportedCards: Set<CardType> = emptySet()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        Spacer(Modifier.height(12.dp))

        // Powered by + Network International logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.powered_by),
                color = Color(0xFF8F8F8F),
                fontSize = 11.sp
            )
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(id = R.drawable.network_international_logo),
                contentDescription = null,
                modifier = Modifier.height(16.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(6.dp))

        // Terms and Conditions | Privacy Policy
        val context = LocalContext.current
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.terms_and_conditions),
                color = Color(0xFF8F8F8F),
                fontSize = 11.sp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.network.ae/en/terms-and-conditions")))
                }
            )
            Text(
                text = "  |  ",
                color = Color(0xFFDADADA),
                fontSize = 11.sp
            )
            Text(
                text = stringResource(R.string.privacy_policy),
                color = Color(0xFF8F8F8F),
                fontSize = 11.sp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.network.ae/en/privacy-notice")))
                }
            )
        }

        // Card security logos
        if (supportedCards.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                supportedCards.forEach { card ->
                    Image(
                        modifier = Modifier
                            .height(18.dp)
                            .width(30.dp)
                            .padding(horizontal = 3.dp),
                        painter = getCardImage(card, isWhiteBackground = true),
                        contentDescription = card.name,
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
