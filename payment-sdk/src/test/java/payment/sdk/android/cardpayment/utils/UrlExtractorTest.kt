package payment.sdk.android.cardpayment.utils

import org.junit.Assert.*
import org.junit.Test
import payment.sdk.android.util.extractUrlsAndText

class UrlExtractorTest {

    @Test
    fun `test extractUrlsAndText with multiple URLs`() {
        val input = "Here is a link to https://example.com and another one to https://kotlinlang.org"
        val expected = listOf(
            Pair("Here is a link to ", false),
            Pair("https://example.com", true),
            Pair(" and another one to ", false),
            Pair("https://kotlinlang.org", true),
        )
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }

    @Test
    fun `test extractUrlsAndText with no URL`() {
        val input = "This is a simple sentence with no URLs."
        val expected = listOf(Pair(input, false))
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }

    @Test
    fun `test extractUrlsAndText with text starting with a URL`() {
        val input = "https://start.com begins the sentence."
        val expected = listOf(
            Pair("https://start.com", true),
            Pair(" begins the sentence.", false)
        )
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }

    @Test
    fun `test extractUrlsAndText with text ending with a URL`() {
        val input = "The sentence ends with a link https://end.com"
        val expected = listOf(
            Pair("The sentence ends with a link ", false),
            Pair("https://end.com", true)
        )
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }

    @Test
    fun `test extractUrlsAndText with only a URL`() {
        val input = "https://only-url.com"
        val expected = listOf(Pair(input, true))
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }

    @Test
    fun `test extractUrlsAndText with mixed URL and non-URL text`() {
        val input = "Text before https://link.com and more text."
        val expected = listOf(
            Pair("Text before ", false),
            Pair("https://link.com", true),
            Pair(" and more text.", false)
        )
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }

    @Test
    fun `test extractUrlsAndText with adjacent URLs`() {
        val input = "Two URLs: https://first.com and https://second.com"
        val expected = listOf(
            Pair("Two URLs: ", false),
            Pair("https://first.com", true),
            Pair(" and ", false),
            Pair("https://second.com", true)
        )
        val result = input.extractUrlsAndText()
        assertEquals(expected, result)
    }
}