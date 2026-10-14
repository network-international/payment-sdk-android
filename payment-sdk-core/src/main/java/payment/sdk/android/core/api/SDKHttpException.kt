package payment.sdk.android.core.api

/**
 * A gateway call that came back with an HTTP error, carrying the status code alongside the message.
 *
 * The code used to survive only inside the message string, so nothing could act on it: a 401 —
 * the payment session's access token having expired — was indistinguishable from a provider
 * refusing a checkout, and both ended as "this option is unavailable, choose another". On an
 * expired session every other method fails too, so that advice sends the payer round a page where
 * nothing can work.
 *
 * Extends [IllegalStateException], which is what this client threw before, so existing callers and
 * their `catch` clauses behave exactly as they did; the message is unchanged too.
 */
class SDKHttpException(
    val statusCode: Int,
    message: String
) : IllegalStateException(message) {

    /** The access token is no longer valid, so nothing on this payment session will succeed. */
    val isSessionExpired: Boolean get() = statusCode == 401

    companion object {
        /** The status code of [error] when it came from an HTTP failure, otherwise null. */
        fun statusCodeOf(error: Throwable?): Int? = (error as? SDKHttpException)?.statusCode

        fun isSessionExpired(error: Throwable?): Boolean =
            (error as? SDKHttpException)?.isSessionExpired == true
    }
}
