package payment.sdk.android.core

import androidx.annotation.Keep

/**
 * Response of `POST .../payments/{paymentRef}/benefit`.
 *
 * [paymentUrl] is a ready-to-load hosted page on the Benefit gateway. [status] is `Initiated` or
 * `Failed`.
 */
@Keep
data class BenefitInitResponse(
    val paymentId: String? = null,
    val paymentUrl: String? = null,
    val status: String? = null,
    val errorMessage: String? = null
) {
    val isInitiated: Boolean
        get() = status.equals(STATUS_INITIATED, ignoreCase = true)

    companion object {
        private const val STATUS_INITIATED = "Initiated"
    }
}

/**
 * The gateway's own error envelope, which a refused initiation answers with instead of a
 * [BenefitInitResponse].
 *
 * A non-2xx becomes an exception whose message carries the raw body (`HTTP: 422 - {...}`), so the
 * only readable reason — "Failed to encrypt BENEFIT trandata" on an outlet whose BENEFIT keys are
 * not provisioned — is buried in JSON inside a string. [reasonFrom] digs it back out.
 */
@Keep
data class BenefitInitErrorBody(
    val message: String? = null,
    val errors: List<Detail>? = null,
    val code: Int? = null
) {
    @Keep
    data class Detail(
        val message: String? = null,
        val errorCode: String? = null,
        val domain: String? = null
    )

    /** The most specific reason the body carries, qualified by its error code for a support ticket. */
    val reason: String?
        get() {
            val detail = errors?.firstOrNull()
            val text = detail?.message
            if (!text.isNullOrBlank()) {
                val code = detail.errorCode
                return if (code.isNullOrBlank()) text else "$text ($code)"
            }
            return message?.takeIf { it.isNotBlank() }
        }

    companion object {
        /**
         * Reads the reason out of anything that might contain the envelope — an exception message
         * with the body appended, or the body on its own. Null when there is no envelope to read,
         * so callers fall back to reporting what they have verbatim.
         */
        fun reasonFrom(raw: String?): String? {
            val start = raw?.indexOf('{') ?: return null
            if (start < 0) return null
            val json = raw.substring(start).substringBeforeLast('}') + "}"
            return runCatching {
                com.google.gson.Gson().fromJson(json, BenefitInitErrorBody::class.java)
            }.getOrNull()?.reason
        }
    }
}
