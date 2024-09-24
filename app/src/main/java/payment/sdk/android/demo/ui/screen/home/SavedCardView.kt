package payment.sdk.android.demo.ui.screen.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import payment.sdk.android.core.SavedCard
import payment.sdk.android.sdk.R

@Composable
fun SavedCardView(
    savedCard: SavedCard,
    isSelected: Boolean = false,
    isEditing: Boolean = false,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPay: () -> Unit = {}
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                modifier = Modifier
                    .height(24.dp)
                    .width(70.dp),
                painter = getCardImage(scheme = savedCard.scheme),
                contentDescription = "",
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text(text = savedCard.maskedPan, style = MaterialTheme.typography.labelMedium)

                Spacer(Modifier.height(2.dp))

                Text(text = savedCard.expiry, style = MaterialTheme.typography.labelMedium)

                Spacer(Modifier.height(2.dp))

                Text(text = savedCard.cardholderName, style = MaterialTheme.typography.labelMedium)
            }

            if (isEditing && !isSelected) {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF93000A))
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            if (!isEditing) {
                Button(modifier = Modifier.padding(8.dp), onClick = onPay) {
                    Text(text = "Pay", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun getCardImage(scheme: String): Painter {
    return painterResource(
        id = when (scheme) {
            "MASTERCARD" -> R.drawable.ic_logo_mastercard
            "VISA" -> R.drawable.ic_logo_visa
            "AMERICAN_EXPRESS" -> R.drawable.ic_logo_amex
            "DINERS_CLUB_INTERNATIONAL" -> R.drawable.ic_logo_dinners_club
            "JCB" -> R.drawable.ic_logo_jcb
            "DISCOVER" -> R.drawable.ic_logo_discover
            else -> R.drawable.ic_card_back_chip
        }
    )
}