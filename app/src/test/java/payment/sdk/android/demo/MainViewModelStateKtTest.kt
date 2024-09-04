package payment.sdk.android.demo

import junit.framework.TestCase.assertEquals
import org.junit.Test

class MainViewModelStateKtTest {
    @Test
    fun `getAlertMessage returns correct messages for all states`() {
        val initState = MainViewModelStateType.INIT
        val loadingState = MainViewModelStateType.LOADING
        val successState = MainViewModelStateType.PAYMENT_SUCCESS
        val failedState = MainViewModelStateType.PAYMENT_FAILED
        val cancelledState = MainViewModelStateType.PAYMENT_CANCELLED
        val reviewState = MainViewModelStateType.PAYMENT_POST_AUTH_REVIEW
        val errorState = MainViewModelStateType.ERROR
        val partialAuthDeclinedState = MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINED
        val partialAuthDeclinedFailedState =
            MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINE_FAILED
        val partiallyAuthorised = MainViewModelStateType.PAYMENT_PARTIALLY_AUTHORISED
        val errorMessage = "Some error message"

        assertEquals(Pair("", ""), initState.getAlertMessage())
        assertEquals(Pair("", ""), loadingState.getAlertMessage())
        assertEquals(
            Pair("Payment Success", "Payment was successful"),
            successState.getAlertMessage()
        )
        assertEquals(Pair("Payment Failed", "Payment was Failed"), failedState.getAlertMessage())
        assertEquals(
            Pair("Payment Cancelled", "Payment was cancelled by user"),
            cancelledState.getAlertMessage()
        )
        assertEquals(
            Pair("Payment in Review", "Payment is in  post auth review"),
            reviewState.getAlertMessage()
        )
        assertEquals(Pair("Error", errorMessage), errorState.getAlertMessage(errorMessage))
        assertEquals(
            Pair("Partial Auth Declined", "Customer declined partial auth"),
            partialAuthDeclinedState.getAlertMessage()
        )
        assertEquals(
            Pair(
                "Sorry, your payment has not been accepted.",
                "Due to technical error, the refund was not processed. Please contact merchant for refund."
            ), partialAuthDeclinedFailedState.getAlertMessage()
        )
        assertEquals(
            Pair(
                "Payment Partially Authorized",
                "Payment Partially Authorized"
            ), partiallyAuthorised.getAlertMessage()
        )
    }
}