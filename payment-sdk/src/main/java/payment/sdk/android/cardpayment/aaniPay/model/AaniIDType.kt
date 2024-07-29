package payment.sdk.android.cardpayment.aaniPay.model

import androidx.compose.ui.text.input.KeyboardType
import payment.sdk.android.sdk.R

enum class AaniIDType(
    val resourceId: Int,
    val regex: Regex,
    val keyboardType: KeyboardType,
    val length: Int,
    val sample: String
) {
    MOBILE_NUMBER(
        resourceId = R.string.aani_mobile_number,
        regex = "^[0-9]{10}$".toRegex(),
        keyboardType = KeyboardType.Phone,
        length = 10,
        sample = "8888888888"
    ),
    EMIRATES_ID(
        resourceId = R.string.aani_emirates_id,
        regex = "^784-[0-9]{4}-[0-9]{7}-[0-9]$".toRegex(),
        keyboardType = KeyboardType.Number,
        length = 18,
        sample = "XXX-XXXX-XXXXXXX-X"
    ),
    PASSPORT_ID(
        resourceId = R.string.aani_passport_id,
        regex = "^[0-9CFGHJKLMNPRTVWXYZ]{9}\$".toRegex(),
        keyboardType = KeyboardType.Text,
        length = 9,
        sample = "X12345678"
    ),
    EMAIL_ID(
        resourceId = R.string.aani_email_id,
        regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex(),
        keyboardType = KeyboardType.Email,
        length = Int.MAX_VALUE,
        sample = "example@example.com"
    );

    fun validate(input: String) = regex.matches(input)
}