package payment.sdk.android.cardpayment.aaniPay.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import payment.sdk.android.sdk.R

enum class AaniIDType {
    MOBILE_NUMBER,
    EMIRATES_ID,
    PASSPORT_ID,
    EMAIL_ID
}

@Composable
fun AaniIDType.getName() = when (this) {
    AaniIDType.MOBILE_NUMBER -> stringResource(R.string.aani_mobile_number)
    AaniIDType.EMIRATES_ID -> stringResource(R.string.aani_emirates_id)
    AaniIDType.PASSPORT_ID -> stringResource(R.string.aani_passport_id)
    AaniIDType.EMAIL_ID -> stringResource(R.string.aani_email_id)
}