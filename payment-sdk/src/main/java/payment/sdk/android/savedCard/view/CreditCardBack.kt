package payment.sdk.android.savedCard.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.sdk.R

@Composable
internal fun CreditCardBack(
    modifier: Modifier
) {
    val paymentSdkCardCenterColor = Color(0xFF232527)
    val paymentSdkCardEndColor = Color(0xFF020202)
    val paymentSdkCardStartColor = Color(0xFF43474A)

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            paymentSdkCardStartColor,
            paymentSdkCardCenterColor,
            paymentSdkCardEndColor
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Surface(
        modifier = modifier
            .aspectRatio(16 / 9f),
        shape = RoundedCornerShape(8.dp),
        elevation = 8.dp,
        color = Color.Transparent
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = gradientBrush,
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(48.dp)
                    .background(Color.Black)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(42.dp)
                    .align(Alignment.CenterStart),
            ) {
                val vectorImage =
                    ImageVector.vectorResource(id = R.drawable.ic_card_back_zebra_pattern) // Replace with your vector drawable resource ID

                Image(
                    modifier = Modifier
                        .fillMaxWidth(),
                    imageVector = vectorImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(32.dp)
                        .width(48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(BorderStroke(2.dp, Color.Red), RoundedCornerShape(22.dp))
                        .padding(4.dp) // Optional: Adjust padding to ensure text doesn't touch the border
                ) {
                    Text(
                        text = "000",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center) // Center text within the Box
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CreditCardBackPreview() {
    Box {
        CreditCardBack(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
        )
    }
}