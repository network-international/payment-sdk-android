package payment.sdk.android.core

import org.junit.Assert.assertEquals
import org.junit.Test
import payment.sdk.android.core.Utils.getQueryParameter

class UtilsTest {

    @Test
    fun `test get params`() {
        val urlString = "https://example.com/path?param1=value1&param2=value2"

        val parsedParams = urlString.getQueryParameter("param1")

        assertEquals(parsedParams, "value1")
    }
}