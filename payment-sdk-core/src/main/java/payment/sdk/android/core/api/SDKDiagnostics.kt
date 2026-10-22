package payment.sdk.android.core.api

/**
 * The SDK's most recent gateway calls, kept in memory so a host app can show *why* a payment
 * failed, not just that it did.
 *
 * Every launcher reports a failure to the host as a single string ("HTTP: 422 - …", or less),
 * while the gateway's actual answer — `errors[0].errorCode`, the payment's final `state` —
 * only reaches logcat. This keeps the last [MAX_EXCHANGES] responses so a "technical details"
 * view can include them. Nothing is persisted or sent anywhere, and response bodies are stored
 * as received: anything shown or shared from here must be redacted by the caller first (bodies
 * can carry access tokens and card tokens).
 */
object SDKDiagnostics {

    data class Exchange(
        val timestampMs: Long,
        val method: String,
        val url: String,
        /** HTTP status, or null when the call never got one (timeout, no connection). */
        val statusCode: Int?,
        /** The response body, or the exception message when there was no response. */
        val body: String
    )

    private const val MAX_EXCHANGES = 20

    private val exchanges = ArrayDeque<Exchange>()

    @JvmStatic
    fun record(method: String, url: String, statusCode: Int?, body: String) {
        synchronized(exchanges) {
            exchanges.addLast(Exchange(System.currentTimeMillis(), method, url, statusCode, body))
            while (exchanges.size > MAX_EXCHANGES) exchanges.removeFirst()
        }
    }

    /** Oldest first. */
    @JvmStatic
    fun recentExchanges(): List<Exchange> = synchronized(exchanges) { exchanges.toList() }

    /** Call when a new payment starts, so its report holds only its own calls. */
    @JvmStatic
    fun clear() = synchronized(exchanges) { exchanges.clear() }
}
