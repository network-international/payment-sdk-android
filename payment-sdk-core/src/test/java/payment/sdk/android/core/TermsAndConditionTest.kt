package payment.sdk.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Visa sends its terms as a single JSON string with escaped newlines and HTML entities in it.
 * `formattedText` turns that into something a Text composable can show verbatim.
 */
class TermsAndConditionTest {

    private fun terms(text: String) = TermsAndCondition(
        languageCode = "eng",
        text = text,
        url = "https://visa.example.com/terms",
        version = 1
    )

    @Test
    fun `escaped paragraph breaks become real blank lines`() {
        assertEquals("a\n\nb", terms("a\\n\\nb").formattedText())
    }

    @Test
    fun `escaped single newlines become real newlines`() {
        assertEquals("a\nb", terms("a\\nb").formattedText())
    }

    @Test
    fun `remaining backslashes are dropped`() {
        assertEquals("\"Visa installment\"", terms("""\"Visa installment\"""").formattedText())
    }

    @Test
    fun `html entities are decoded`() {
        assertEquals("& \" '", terms("&amp; &quot; &#39;").formattedText())
    }

    @Test
    fun `angle brackets are softened to parentheses so they cannot read as markup`() {
        assertEquals("(or if you default)", terms("<or if you default>").formattedText())
        assertEquals("(or if you default)", terms("&lt;or if you default&gt;").formattedText())
    }

    @Test
    fun `text with nothing to escape is returned unchanged`() {
        assertEquals("Plain terms apply.", terms("Plain terms apply.").formattedText())
    }

    @Test
    fun `a realistic payload renders without escape artefacts`() {
        val formatted = terms(
            "1) Eligibility: \\\"Visa installment\\\" &amp; fees &lt;apply&gt;.\\n\\n2) Interest."
        ).formattedText()

        assertFalse(formatted.contains("\\n"))
        assertFalse(formatted.contains("&amp;"))
        assertFalse(formatted.contains("&lt;"))
        assertTrue(formatted.contains("\n\n"))
        assertTrue(formatted.contains("\"Visa installment\""))
        assertTrue(formatted.contains("(apply)"))
    }
}
