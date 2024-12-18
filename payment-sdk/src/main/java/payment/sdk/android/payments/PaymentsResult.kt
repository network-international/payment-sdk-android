package payment.sdk.android.payments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * `PaymentsResult` is a sealed class representing the possible outcomes of a payment process.
 * Each subclass of `PaymentsResult` describes a specific result state,
 * mapped to standard payment statuses.
 */
@Parcelize
sealed class PaymentsResult : Parcelable {

    /**
     * Represents a successful authorization of the payment.
     * Corresponds to `STATUS_PAYMENT_AUTHORIZED`,
     * where order creation with the “AUTH” action parameter is successful.
     */
    @Parcelize
    data object Authorised : PaymentsResult()

    /**
     * Represents a successful completion of the payment.
     * Corresponds to either `STATUS_PAYMENT_CAPTURED` or `STATUS_PAYMENT_PURCHASED`,
     * indicating that the order creation was successful with either a “SALE” or “PURCHASE” action parameter.
     */
    @Parcelize
    data object Success : PaymentsResult()

    /**
     * Indicates the payment requires a post-authorization fraud review.
     * Corresponds to `STATUS_POST_AUTH_REVIEW`, where further review is needed before finalizing the payment.
     */
    @Parcelize
    data object PostAuthReview : PaymentsResult()

    /**
     * Indicates that a partial authorization was attempted but declined by the customer.
     * Corresponds to `STATUS_PARTIAL_AUTH_DECLINED`,
     * showing the customer chose not to proceed with the transaction after partial authorization.
     */
    @Parcelize
    data object PartialAuthDeclined : PaymentsResult()

    /**
     * Indicates a partial authorization attempt was declined by the customer,
     * but the authorization reversal failed.
     * Corresponds to `STATUS_PARTIAL_AUTH_DECLINE_FAILED`,
     * indicating downstream system errors prevented successful reversal.
     */
    @Parcelize
    data object PartialAuthDeclineFailed : PaymentsResult()

    /**
     * Indicates that the payment was partially authorized, and the customer accepted the partially authorized transaction.
     * Corresponds to `STATUS_PARTIALLY_AUTHORISED`, showing that the customer chose to proceed with a partially authorized payment.
     */
    @Parcelize
    data object PartiallyAuthorised : PaymentsResult()

    /**
     * Represents a failed payment result with an error message.
     * Can correspond to `STATUS_PAYMENT_FAILED` or `STATUS_GENERIC_ERROR`.
     *
     * @property error The error message describing the reason for failure.
     * Commonly used for cases where payment is unsuccessful on the gateway or client-side issues occur, such as network problems.
     */
    @Parcelize
    data class Failed(val error: String) : PaymentsResult()

    /**
     * Indicates that the payment process was cancelled by the user.
     * This outcome generally represents a user-initiated cancellation, not an error or gateway issue.
     */
    @Parcelize
    data object Cancelled : PaymentsResult()
}