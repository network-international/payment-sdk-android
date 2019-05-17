package payment.sdk.android.cardpayment.widget

import junitparams.JUnitParamsRunner
import junitparams.NamedParameters
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class DateFormatterTest {

    @Test
    @Parameters(named = "dates")
    fun formatExpireDateForApi(rawDate: String, expected: String) {
        val actual = DateFormatter.formatExpireDateForApi(rawDate)
        assertEquals(expected, actual)
    }

    @NamedParameters("dates")
    fun dates() = arrayOf(
            arrayOf("1119", "2019-11"),
            arrayOf("0125", "2025-01"),
            arrayOf("1221", "2021-12"),
            arrayOf("0101", "2001-01"),
            arrayOf("1212", "2012-12")
    )
}