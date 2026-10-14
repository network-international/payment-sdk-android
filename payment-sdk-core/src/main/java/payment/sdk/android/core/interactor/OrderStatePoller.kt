package payment.sdk.android.core.interactor

import android.util.Log
import kotlinx.coroutines.delay
import org.json.JSONObject
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Re-reads an order over the payment cookie and reports the payment's authoritative state.
 *
 * A payment call that never comes back (read timeout, dropped connection, gateway 5xx) says
 * nothing about what happened on the backend. Long-running fraud screening on the 3DS challenge
 * response routinely outlives the client's read timeout while the payment goes on to succeed, so
 * treating the failed call as a failed payment shows the shopper the opposite of the truth. The
 * order is the only source that knows, so it is polled until it settles.
 */
class OrderStatePoller(
    private val httpClient: HttpClient,
    private val sleep: suspend (Long) -> Unit = { delay(it) }
) {

    sealed class Result {
        /** The order reports a state the payment can no longer move away from. */
        data class Terminal(val state: String, val order: JSONObject) : Result()

        /**
         * The poll budget ran out with the payment still in flight, or the order could not be
         * read at all. [lastState] is the last state seen, null when no read ever succeeded.
         */
        data class Inconclusive(val lastState: String?) : Result()
    }

    /** One read of the order. Returns null when the read itself failed. */
    suspend fun fetchOrder(orderUrl: String, paymentCookie: String): JSONObject? {
        return when (val response = httpClient.get(
            url = orderUrl,
            headers = mapOf(HEADER_COOKIE to paymentCookie),
            body = Body.Empty()
        )) {
            is SDKHttpResponse.Success -> runCatching { JSONObject(response.body) }
                .onFailure { Log.e(TAG, "Order response was not JSON: ${it.message}") }
                .getOrNull()

            is SDKHttpResponse.Failed -> {
                Log.e(TAG, "Order read failed: ${response.error.message}")
                null
            }
        }
    }

    /**
     * Polls [orderUrl] until the payment reaches one of [stopStates] or the attempts run out.
     * The first read happens immediately, so a payment that has already settled costs one call.
     *
     * [stopStates] defaults to the states the payment cannot move away from. A caller recovering
     * a lost payment *initiation* also stops at AWAIT_3DS, because there the payment has settled
     * into something it can act on — the challenge still has to be run.
     */
    suspend fun awaitTerminalState(
        orderUrl: String,
        paymentCookie: String,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        stopStates: Set<String> = TERMINAL_STATES
    ): Result {
        var lastState: String? = null
        repeat(maxAttempts) { attempt ->
            if (attempt > 0) sleep(intervalMs)
            val order = fetchOrder(orderUrl, paymentCookie)
            val state = order?.let { paymentState(it) }
            Log.d(TAG, "poll attempt=${attempt + 1}/$maxAttempts state=$state")
            if (state != null) {
                lastState = state
                if (state in stopStates) {
                    return Result.Terminal(state, order)
                }
            }
        }
        return Result.Inconclusive(lastState)
    }

    private fun paymentState(order: JSONObject): String? = runCatching {
        order.getJSONObject("_embedded")
            .getJSONArray("payment")
            .getJSONObject(0)
            .getString("state")
            .uppercase()
    }.getOrNull()

    companion object {
        private const val TAG = "OrderStatePoller"

        /**
         * The order's embedded payment. It carries the same fields as the payment resource
         * itself — state, 3DS data, links — so it deserialises straight into a PaymentResponse.
         */
        fun embeddedPayment(order: JSONObject): JSONObject? = runCatching {
            order.getJSONObject("_embedded").getJSONArray("payment").getJSONObject(0)
        }.getOrNull()

        private const val HEADER_COOKIE = "Cookie"

        /** ~30s of polling: enough to outlast slow fraud screening without stranding the shopper. */
        const val DEFAULT_MAX_ATTEMPTS = 10
        const val DEFAULT_INTERVAL_MS = 3_000L

        /** The payment exists and is waiting on the cardholder challenge. */
        const val STATE_AWAIT_3DS = "AWAIT_3DS"

        val TERMINAL_SUCCESS_STATES = setOf("CAPTURED", "AUTHORISED", "PURCHASED", "VERIFIED")
        val TERMINAL_FAILURE_STATES = setOf("FAILED", "DECLINED", "CANCELLED", "REVERSED")

        /**
         * States the payment cannot move away from on its own. POST_AUTH_REVIEW and
         * AWAITING_PARTIAL_AUTH_APPROVAL are terminal for the SDK: both hand the outcome back to
         * the caller, which drives the next step.
         */
        val TERMINAL_STATES: Set<String> = TERMINAL_SUCCESS_STATES + TERMINAL_FAILURE_STATES +
                setOf("POST_AUTH_REVIEW", "AWAITING_PARTIAL_AUTH_APPROVAL")
    }
}
