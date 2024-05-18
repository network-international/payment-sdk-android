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
        val errorMessage = "Some error message"

        assertEquals(Pair("", ""), initState.getAlertMessage())
        assertEquals(Pair("", ""), loadingState.getAlertMessage())
        assertEquals(Pair("Payment Success", "Payment was successful"), successState.getAlertMessage())
        assertEquals(Pair("Payment Failed", "Payment was Failed"), failedState.getAlertMessage())
        assertEquals(Pair("Payment Cancelled", "Payment was cancelled by user"), cancelledState.getAlertMessage())
        assertEquals(Pair("Payment in Review", "Payment is in  post auth review"), reviewState.getAlertMessage())
        assertEquals(Pair("Error", errorMessage), errorState.getAlertMessage(errorMessage))
    }
}