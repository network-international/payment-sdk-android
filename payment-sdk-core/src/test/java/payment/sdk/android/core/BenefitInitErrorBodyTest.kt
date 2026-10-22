package payment.sdk.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BenefitInitErrorBodyTest {

    /**
     * Verbatim from a sandbox outlet that lists BENEFIT but has no BENEFIT keys provisioned, wrapped
     * the way the HTTP client reports a non-2xx: the body appended to an exception message. Without
     * digging it back out, the only thing the log can say is "status=null".
     */
    @Test
    fun `reads the reason out of an exception message carrying the body`() {
        val raw = "HTTP: 422 - " + BODY_422 + " (x-correlation-id: c1526b92)"

        assertEquals(
            "Failed to encrypt BENEFIT trandata (invalidBenefitPayment)",
            BenefitInitErrorBody.reasonFrom(raw)
        )
    }

    @Test
    fun `reads the reason out of a bare body`() {
        assertEquals(
            "Failed to encrypt BENEFIT trandata (invalidBenefitPayment)",
            BenefitInitErrorBody.reasonFrom(BODY_422)
        )
    }

    @Test
    fun `falls back to the envelope message when no detail is carried`() {
        assertEquals(
            "Unprocessable Entity",
            BenefitInitErrorBody.reasonFrom("""{"message":"Unprocessable Entity","code":422}""")
        )
    }

    @Test
    fun `a detail without an error code is reported on its own`() {
        assertEquals(
            "Benefit is not enabled for this outlet",
            BenefitInitErrorBody.reasonFrom(
                """{"errors":[{"message":"Benefit is not enabled for this outlet"}]}"""
            )
        )
    }

    /** Not an error envelope, so the caller must fall back to reporting what it has verbatim. */
    @Test
    fun `carries no reason when there is no envelope to read`() {
        assertNull(BenefitInitErrorBody.reasonFrom("""{"paymentUrl":"https://benefit.example.com"}"""))
        assertNull(BenefitInitErrorBody.reasonFrom("Unable to resolve host"))
        assertNull(BenefitInitErrorBody.reasonFrom(null))
    }

    @Test
    fun `empty messages are treated as absent`() {
        assertNull(BenefitInitErrorBody.reasonFrom("""{"message":"","errors":[{"message":""}]}"""))
    }

    @Test
    fun `malformed json does not throw`() {
        assertNull(BenefitInitErrorBody.reasonFrom("HTTP: 502 - {not json at all"))
    }

    companion object {
        private const val BODY_422 =
            """{"message":"Unprocessable Entity","errors":[{"message":"Failed to encrypt BENEFIT trandata",""" +
                """"localizedMessage":"{error.processing.invalidBenefitPayment}",""" +
                """"errorCode":"invalidBenefitPayment","domain":"processing"}],"code":422}"""
    }
}
