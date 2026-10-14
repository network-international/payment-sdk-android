package payment.sdk.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Terms-and-conditions text arrives as one blob with bare URLs in it; the view splits it into
 * runs so the URLs can be rendered as links and everything else as plain text.
 */
class StringUtilsTest {

    @Test
    fun `plain text is returned as a single non-url run`() {
        assertEquals(
            listOf("no links here" to false),
            "no links here".extractUrlsAndText()
        )
    }

    @Test
    fun `a url is separated from the text around it`() {
        assertEquals(
            listOf(
                "See " to false,
                "https://example.com/terms" to true,
                " for details" to false
            ),
            "See https://example.com/terms for details".extractUrlsAndText()
        )
    }

    @Test
    fun `multiple urls each become their own run`() {
        val runs = "a http://x.com b https://y.com c".extractUrlsAndText()

        assertEquals(5, runs.size)
        assertEquals(listOf("http://x.com", "https://y.com"), runs.filter { it.second }.map { it.first })
    }

    @Test
    fun `a url at the very start has no leading text run`() {
        val runs = "https://example.com is our site".extractUrlsAndText()

        assertEquals("https://example.com" to true, runs.first())
        assertEquals(2, runs.size)
    }

    @Test
    fun `a url at the very end has no trailing text run`() {
        val runs = "visit https://example.com".extractUrlsAndText()

        assertEquals("https://example.com" to true, runs.last())
        assertEquals(2, runs.size)
    }

    @Test
    fun `matching is case insensitive on the scheme`() {
        val runs = "go to HTTPS://EXAMPLE.COM now".extractUrlsAndText()

        assertTrue(runs.any { it.second && it.first.equals("HTTPS://EXAMPLE.COM", ignoreCase = true) })
    }

    @Test
    fun `non http schemes the regex covers are also linked`() {
        assertTrue("see ftp://files.example.com/a".extractUrlsAndText().any { it.second })
    }

    @Test
    fun `an empty string yields no runs`() {
        assertTrue("".extractUrlsAndText().isEmpty())
    }

    @Test
    fun `the runs concatenate back to the original text`() {
        val original = "Terms: https://a.com and https://b.com apply."
        assertEquals(original, original.extractUrlsAndText().joinToString("") { it.first })
    }
}
