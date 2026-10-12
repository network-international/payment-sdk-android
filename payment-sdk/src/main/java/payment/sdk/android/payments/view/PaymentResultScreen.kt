package payment.sdk.android.payments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.SDKConfig
import payment.sdk.android.core.testId
import payment.sdk.android.payments.model.PaymentResultArgs
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.sdk.R
import kotlinx.coroutines.delay

private const val AUTO_DISMISS_DELAY_MS = 1000L

private val SuccessGreen = Color(0xFF2EB852)
private val FailureRed = Color(0xFFE63835)
private val MutedGrey = Color(0xFF8F8F8F)
private val PrimaryText = Color(0xFF1A1A1A)

@Composable
fun PaymentResultScreen(
    args: PaymentResultArgs,
    onDone: () -> Unit
) {
    val accent = if (args.isSuccess) SuccessGreen else FailureRed

    // Show the result for a moment, then hand control back to the merchant app — the merchant
    // confirms/cancels the order off our result, so we must not block on a tap.
    LaunchedEffect(Unit) {
        delay(AUTO_DISMISS_DELAY_MS)
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            // The whole page scrolls — header, status block, footer, and the Done button —
            // so nothing gets clipped when the slice receipt section makes the page taller
            // than the viewport.
            .verticalScroll(rememberScrollState())
    ) {
        // ── Merchant header (shop logo + order summary) — same style as the unified payment page
        MerchantHeader(
            formattedAmount = args.formattedAmount,
            orderItems = args.orderItems
        )

        // ── Status block ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                painter = painterResource(
                    id = if (args.isSuccess) R.drawable.ic_payment_success else R.drawable.ic_payment_failure
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .testId("sdk_result_image_status"),
                tint = Color.Unspecified
            )

            Spacer(Modifier.height(24.dp))

            AedAmountText(
                text = if (args.isSuccess) {
                    if (args.formattedAmount != null) {
                        // On a Slice plan the amount is the plan's total across the term, not what
                        // was debited today, so "You paid" would misdescribe it.
                        val titleRes = if (args.sliceReceipt != null) {
                            R.string.payment_result_success_title_slice
                        } else {
                            R.string.payment_result_success_title
                        }
                        stringResource(titleRes, args.formattedAmount)
                    } else {
                        stringResource(R.string.payment_result_success_title_no_amount)
                    }
                } else {
                    stringResource(R.string.payment_result_failure_title)
                },
                style = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                fontWeight = FontWeight.SemiBold,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.testId("sdk_result_label_title")
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (args.isSuccess) {
                    stringResource(R.string.payment_result_success_subtitle)
                } else {
                    stringResource(R.string.payment_result_failure_subtitle)
                },
                fontSize = 14.sp,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Slice success section - only when the user paid via a Slice installment plan.
            if (args.isSuccess && args.sliceReceipt != null) {
                SliceSuccessSection(receipt = args.sliceReceipt)
                Spacer(Modifier.height(24.dp))
            }

            DetailLine(
                label = if (args.isSuccess)
                    stringResource(R.string.payment_result_transaction_id)
                else
                    stringResource(R.string.payment_result_reference_number),
                value = args.transactionId
            )
            Spacer(Modifier.height(8.dp))
            DetailLine(
                label = stringResource(R.string.payment_result_date_time),
                value = args.dateTime
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Footer + done button ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            PaymentFooterView(supportedCards = args.supportedCards)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SliceSuccessSection(receipt: payment.sdk.android.payments.model.SliceReceipt) {
    val textColor = Color(0xFF070707)
    val lgSize = 16.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testId("sdk_result_slice_receipt"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.slice_receipt_title),
            // FontWeight 350 = "Medium" (rounds to 400 on system fonts that don't expose 350).
            style = androidx.compose.ui.text.TextStyle(
                fontSize = lgSize,
                fontWeight = FontWeight(350),
                color = textColor,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.testId("sdk_result_slice_title"),
        )
        Spacer(Modifier.height(12.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_logo_slice),
            contentDescription = "slice by network",
            modifier = Modifier
                .height(70.dp)
                .width(170.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        SliceReceiptRow(
            label = stringResource(R.string.slice_receipt_tenor),
            value = sliceMonths(R.plurals.slice_receipt_tenor_months, receipt.period),
            textColor = textColor,
            lgSize = lgSize,
        )
        Spacer(Modifier.height(12.dp))
        SliceReceiptRow(
            label = stringResource(sliceRateLabel(receipt.isIslamic)),
            value = receipt.rate,
            textColor = textColor,
            lgSize = lgSize,
        )
        Spacer(Modifier.height(12.dp))
        SliceReceiptRow(label = stringResource(R.string.slice_processing_fees), value = receipt.processingFees, textColor = textColor, lgSize = lgSize)
        // Charged on top of the order total, so it only appears when the offer carried one -
        // exactly as the offer card hides its own row.
        if (receipt.installmentFees != null) {
            Spacer(Modifier.height(12.dp))
            SliceReceiptRow(label = stringResource(R.string.slice_installment_fees), value = receipt.installmentFees, textColor = textColor, lgSize = lgSize)
        }
        Spacer(Modifier.height(12.dp))
        SliceReceiptRow(label = stringResource(R.string.slice_receipt_installment_amount), value = receipt.installmentAmount, textColor = textColor, lgSize = lgSize)
    }
}

@Composable
private fun SliceReceiptRow(
    label: String,
    value: String,
    textColor: Color,
    lgSize: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            // FontWeight 325 = "Book"; system font rounds to nearest available (typically 300/400).
            style = androidx.compose.ui.text.TextStyle(
                fontSize = lgSize,
                fontWeight = FontWeight(325),
                letterSpacing = (lgSize.value * 0.01f).sp,
                color = textColor,
            ),
        )
        AedAmountText(
            text = value,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = lgSize,
                fontWeight = FontWeight(350),
                color = textColor,
            ),
            color = textColor,
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            color = PrimaryText
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = PrimaryText
        )
    }
}

@Composable
private fun MerchantHeader(
    formattedAmount: String?,
    orderItems: List<payment.sdk.android.payments.model.OrderItem>
) {
    val logoResId = if (SDKConfig.merchantLogoResId != 0) SDKConfig.merchantLogoResId
    else R.drawable.network_international_logo

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PgColors.surfaceRow)
            .statusBarsPadding()
    ) {
        // Merchant logo row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Logo",
                modifier = Modifier.height(32.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Order summary block
        if (formattedAmount != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_summary),
                    fontSize = 13.sp,
                    color = MutedGrey
                )
                AedAmountText(
                    text = formattedAmount,
                    style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
            }
        }

        // Optional itemised breakdown
        if (orderItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Item(s)", fontSize = 12.sp, color = MutedGrey)
                    Text(text = "Amount", fontSize = 12.sp, color = MutedGrey)
                }
                orderItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = item.name, fontSize = 13.sp, color = PrimaryText)
                        AedAmountText(
                            text = item.amount,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            color = PrimaryText,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
