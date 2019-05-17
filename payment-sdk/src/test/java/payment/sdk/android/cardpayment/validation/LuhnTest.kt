package payment.sdk.android.cardpayment.validation

import junitparams.JUnitParamsRunner
import junitparams.NamedParameters
import junitparams.Parameters
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class LuhnTest {

    @Test
    @Parameters(named = "validPans")
    fun isValidPan(pan: String) {
        val valid = Luhn.isValidPan(pan)
        assertTrue(valid)
    }

    @Test
    @Parameters(named = "invalidPans")
    fun isInValidPan(pan: String) {
        val valid = Luhn.isValidPan(pan)
        assertFalse(valid)
    }

    @NamedParameters("validPans")
    fun validPans() = arrayOf(
            "5555555555554444",
            "5105105105105100",
            "378282246310005",
            "371449635398431",
            "4111111111111111"
    )

    @NamedParameters("invalidPans")
    fun invalidPans() = arrayOf(
            "4111111111111112",
            "4444333322221110"
    )
}