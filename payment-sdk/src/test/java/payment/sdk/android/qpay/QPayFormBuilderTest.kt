package payment.sdk.android.qpay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.QPayInitResponse

class QPayFormBuilderTest {

    private fun response(
        redirectUri: String? = "https://qcb.example.com/pay",
        amount: String? = null,
        pun: String? = null,
        paymentDescription: String? = null
    ) = QPayInitResponse(
        redirectUri = redirectUri,
        amount = amount,
        pun = pun,
        paymentDescription = paymentDescription
    )

    @Test
    fun `normalises every unicode dash variant to ASCII`() {
        val dashes = listOf('‐', '‑', '‒', '–', '—', '―', '−', '﹘')
        dashes.forEach { dash ->
            assertEquals(
                "https://qcb-gateway.example.com/pay",
                QPayFormBuilder.normalizeRedirectUri("https://qcb${dash}gateway.example.com/pay")
            )
        }
    }

    @Test
    fun `an already clean uri is unchanged`() {
        val uri = "https://qcb-gateway.example.com/pay?a=1"
        assertEquals(uri, QPayFormBuilder.normalizeRedirectUri(uri))
    }

    @Test
    fun `an empty uri normalises to empty`() {
        assertEquals("", QPayFormBuilder.normalizeRedirectUri(""))
    }

    @Test
    fun `form posts to the normalised action`() {
        val html = QPayFormBuilder.buildAutoSubmitHTML(
            response(redirectUri = "https://qcb–gw.example.com/pay")
        )

        assertNotNull(html)
        assertTrue(html!!.contains("""action="https://qcb-gw.example.com/pay""""))
        assertTrue(html.contains("""method="post""""))
        assertTrue(html.contains("document.getElementById('QPayRedirectForm').submit()"))
    }

    @Test
    fun `every gateway field is carried as a hidden input`() {
        val html = QPayFormBuilder.buildAutoSubmitHTML(response(amount = "500", pun = "PUN123"))!!

        assertTrue(html.contains("""<input type="hidden" name="Amount" value="500" />"""))
        assertTrue(html.contains("""<input type="hidden" name="PUN" value="PUN123" />"""))
        assertEquals(14, html.split("<input").size - 1)
    }

    @Test
    fun `missing values are sent as empty strings, not omitted`() {
        val html = QPayFormBuilder.buildAutoSubmitHTML(response(amount = "500"))!!

        assertTrue(html.contains("""name="SecureHash" value="" """.trim()))
    }

    @Test
    fun `values are html escaped so they cannot break out of the attribute`() {
        val html = QPayFormBuilder.buildAutoSubmitHTML(
            response(paymentDescription = """a"><script>x</script> & 'b'""")
        )!!

        assertTrue(!html.contains("<script>x</script>"))
        assertTrue(html.contains("&quot;"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&amp;"))
        assertTrue(html.contains("&#39;"))
    }

    @Test
    fun `no redirect uri means no form`() {
        assertNull(QPayFormBuilder.buildAutoSubmitHTML(response(redirectUri = null)))
        assertNull(QPayFormBuilder.buildAutoSubmitHTML(response(redirectUri = "")))
    }
}
