package payment.sdk.android.cardpayment.googlePay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.googlepay.GooglePayLauncher
import payment.sdk.android.googlepay.WalletPaymentStateMapper
import payment.sdk.android.payments.UnifiedPaymentPageVMEffects

class WalletPaymentStateMapperTest {

    @Test
    fun `parseState reads state from json`() {
        assertEquals("CAPTURED", WalletPaymentStateMapper.parseState("""{"state":"CAPTURED"}"""))
        assertNull(WalletPaymentStateMapper.parseState(""))
        assertNull(WalletPaymentStateMapper.parseState("not-json"))
    }

    @Test
    fun `maps gateway states to launcher results`() {
        assertEquals(
            GooglePayLauncher.Result.Authorised,
            WalletPaymentStateMapper.toGooglePayResult("AUTHORISED")
        )
        assertEquals(
            GooglePayLauncher.Result.Captured,
            WalletPaymentStateMapper.toGooglePayResult("CAPTURED")
        )
        assertEquals(
            GooglePayLauncher.Result.Success,
            WalletPaymentStateMapper.toGooglePayResult("PURCHASED")
        )
        assertEquals(
            GooglePayLauncher.Result.PostAuthReview,
            WalletPaymentStateMapper.toGooglePayResult("POST_AUTH_REVIEW")
        )
        assertTrue(WalletPaymentStateMapper.toGooglePayResult(null) is GooglePayLauncher.Result.Failed)
    }

    @Test
    fun `maps gateway states to upp effects`() {
        assertTrue(WalletPaymentStateMapper.toUppEffect("AUTHORISED") is UnifiedPaymentPageVMEffects.PaymentAuthorised)
        assertTrue(WalletPaymentStateMapper.toUppEffect("CAPTURED") is UnifiedPaymentPageVMEffects.Captured)
        assertTrue(WalletPaymentStateMapper.toUppEffect("PURCHASED") is UnifiedPaymentPageVMEffects.Purchased)
        assertTrue(WalletPaymentStateMapper.toUppEffect("POST_AUTH_REVIEW") is UnifiedPaymentPageVMEffects.PostAuthReview)
        assertTrue(WalletPaymentStateMapper.toUppEffect("FAILED") is UnifiedPaymentPageVMEffects.Failed)
    }
}
