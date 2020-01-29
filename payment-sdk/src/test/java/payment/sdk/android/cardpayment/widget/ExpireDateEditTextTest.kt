package payment.sdk.android.cardpayment.widget

import junitparams.JUnitParamsRunner
import junitparams.NamedParameters
import junitparams.Parameters
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class ExpireDateEditTextTest {


    @Test
    @Parameters(named = "dates")
    fun isValidExpire(masked: String, expected: Boolean) {
        val actual = ExpireDateEditText.isValidExpire(masked)

        assertEquals(expected, actual)
    }

    @NamedParameters("dates")
    fun expiryDates(): Array<Array<Any>> = arrayOf(
            arrayOf("0", true),
            arrayOf("1", true),
            arrayOf("2", false),
            arrayOf("3", false),
            arrayOf("01", true),
            arrayOf("11", true),
            arrayOf("22", false),
            arrayOf("33", false),
            arrayOf("11/1", true),
            arrayOf("11/0", false),
            arrayOf("11/99", true),
            arrayOf("11/12", false),
            arrayOf("11/12", false),
            arrayOf("00/11", false),
            arrayOf("00/00", false)
    )
}