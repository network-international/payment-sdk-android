package payment.sdk.android.core

import androidx.annotation.Keep
import payment.sdk.android.core.api.Body

@Keep
data class AaniPayRequest(
    val aliasType: String,
    val mobileNumber: MobileNumber? = null,
    val emiratesId: String? = null,
    val passportId: String? = null,
    val emailId: String? = null,
    val backLink: String = "niannipay://open",
    val payerIp: String,
    val source: String = "MOBILE_APP"
) {
    fun toBody(): Body {
        val bodyMap = mutableMapOf<String, Any>().apply {
            put(KEY_ALIAS_TYPE, aliasType)
            put(KEY_BACK_LINK, backLink)
            put(KEY_SOURCE, source)
            put(KEY_PAYER_IP, payerIp)
        }

        mobileNumber?.let {
            bodyMap[KEY_MOBILE_NUMBER] = mapOf(
                KEY_NUMBER to it.number,
                KEY_COUNTRY_CODE to it.countryCode
            )
        }

        emiratesId?.let {
            bodyMap[KEY_EMIRATES_ID] = it
        }

        passportId?.let {
            bodyMap[KEY_PASSPORT_ID] = it
        }

        emailId?.let {
            bodyMap[KEY_EMAIL_ID] = it
        }

        return Body.Json(bodyMap)
    }

    companion object {
        const val KEY_ALIAS_TYPE = "aliasType"
        const val KEY_MOBILE_NUMBER = "mobileNumber"
        const val KEY_COUNTRY_CODE = "countryCode"
        const val KEY_NUMBER = "number"
        const val KEY_EMIRATES_ID = "emiratesId"
        const val KEY_PASSPORT_ID = "passportId"
        const val KEY_EMAIL_ID = "emailId"
        const val KEY_SOURCE = "source"
        const val KEY_BACK_LINK = "backLink"
        const val KEY_PAYER_IP = "payerIp"
    }
}

@Keep
data class MobileNumber(
    val countryCode: String = "+971",
    val number: String
)
