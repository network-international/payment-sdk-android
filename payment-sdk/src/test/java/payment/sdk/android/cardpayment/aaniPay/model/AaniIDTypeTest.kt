package payment.sdk.android.cardpayment.aaniPay.model

import org.junit.Assert.*
import org.junit.Test

class AaniIDTypeTest {

    @Test
    fun emailIDValidation() {
        val validEmails = listOf(
            "username@example.com",
            "user+name@example.com",
            "user.name+tag+sorting@example.com",
            "user@domain.com",
            "user.name@domain.com"
        )

        validEmails.forEach { assertTrue(AaniIDType.EMAIL_ID.validate(it)) }
    }

    @Test
    fun emiratesIDValidation() {
        val validEmiratesIDs = listOf(
            "784-1234-1234567-1",
            "784-5678-9876543-2",
            "784-1111-2222333-3",
            "784-3333-4444555-4",
            "784-5555-6666777-5"
        )

        validEmiratesIDs.forEach { assertTrue(AaniIDType.EMIRATES_ID.validate(it)) }
    }

    @Test
    fun passportIDValidation() {
        val validPassportIDs = listOf(
            "C12345678",
            "Z98765432",
            "G42H63820",
            "G1234567H",
            "F0T2V4W6X"
        )

        validPassportIDs.forEach { assertTrue(AaniIDType.PASSPORT_ID.validate(it)) }
    }

    @Test
    fun mobileNumberValidation() {
        val validMobileNumbers = listOf(
            "1234567890",
            "0987654321",
            "1122334455",
            "5566778899",
            "6677889900"
        )

        validMobileNumbers.forEach { assertTrue(AaniIDType.MOBILE_NUMBER.validate(it)) }
    }
}