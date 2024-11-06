package payment.sdk.android.payments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class PaymentsResult : Parcelable {
    @Parcelize
    data object Authorised : PaymentsResult()

    @Parcelize
    data object Success : PaymentsResult()

    @Parcelize
    data object PostAuthReview : PaymentsResult()

    @Parcelize
    data object PartialAuthDeclined : PaymentsResult()

    @Parcelize
    data object PartialAuthDeclineFailed : PaymentsResult()

    @Parcelize
    data object PartiallyAuthorised : PaymentsResult()

    @Parcelize
    data class Failed(val error: String) : PaymentsResult()

    @Parcelize
    data object Cancelled : PaymentsResult()
}