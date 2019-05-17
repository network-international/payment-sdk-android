package payment.sdk.android.cardpayment.widget

import java.lang.StringBuilder

class DateFormatter {

    companion object {

        fun formatExpireDateForApi(rawExpireDate: String): String =
                StringBuilder().apply {
                    append("20").append(rawExpireDate.substring(range = 2..3))
                    append('-')
                    append(rawExpireDate.substring(range = 0..1))
                }.toString()
    }
}
