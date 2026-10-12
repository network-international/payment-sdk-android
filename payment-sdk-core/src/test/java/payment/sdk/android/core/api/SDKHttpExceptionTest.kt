package payment.sdk.android.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SDKHttpExceptionTest {

    @Test
    fun `401 is recognised as an expired session`() {
        val e = SDKHttpException(401, "HTTP: 401 - {\"error\":\"Unauthorized\"}")
        assertTrue(e.isSessionExpired)
        assertTrue(SDKHttpException.isSessionExpired(e))
        assertEquals(401, SDKHttpException.statusCodeOf(e))
    }

    @Test
    fun `other gateway refusals are not expired sessions`() {
        // 412 invalidInstallments and 415 are provider or request problems, not dead tokens.
        for (code in listOf(400, 403, 404, 412, 415, 422, 500, 503)) {
            assertFalse("status $code", SDKHttpException(code, "HTTP: $code - body").isSessionExpired)
        }
    }

    @Test
    fun `it stays an IllegalStateException so existing callers are unaffected`() {
        assertTrue(SDKHttpException(401, "boom") is IllegalStateException)
    }

    @Test
    fun `the message is passed through untouched`() {
        val msg = "HTTP: 401 - {\"status\":401} (x-correlation-id: abc-123)"
        assertEquals(msg, SDKHttpException(401, msg).message)
    }

    @Test
    fun `unrelated errors report no status and no expiry`() {
        assertNull(SDKHttpException.statusCodeOf(IllegalStateException("timeout")))
        assertNull(SDKHttpException.statusCodeOf(null))
        assertFalse(SDKHttpException.isSessionExpired(IllegalStateException("timeout")))
        assertFalse(SDKHttpException.isSessionExpired(null))
    }
}
