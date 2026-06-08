package payment.sdk.android.cardpayment.aaniPay.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.aaniPay.model.AaniIDType

class AaniIDTypeTest {

    // region MOBILE_NUMBER — regex: \d{5,13}$

    @Test
    fun `mobile number accepts minimum length of 5 digits`() {
        assertTrue(AaniIDType.MOBILE_NUMBER.validate("12345"))
    }

    @Test
    fun `mobile number accepts maximum length of 13 digits`() {
        assertTrue(AaniIDType.MOBILE_NUMBER.validate("1234567890123"))
    }

    @Test
    fun `mobile number accepts typical 9-digit number`() {
        assertTrue(AaniIDType.MOBILE_NUMBER.validate("501234567"))
    }

    @Test
    fun `mobile number rejects fewer than 5 digits`() {
        assertFalse(AaniIDType.MOBILE_NUMBER.validate("1234"))
    }

    @Test
    fun `mobile number rejects more than 13 digits`() {
        assertFalse(AaniIDType.MOBILE_NUMBER.validate("12345678901234"))
    }

    @Test
    fun `mobile number rejects input containing letters`() {
        assertFalse(AaniIDType.MOBILE_NUMBER.validate("0501a34567"))
    }

    @Test
    fun `mobile number rejects input containing spaces`() {
        assertFalse(AaniIDType.MOBILE_NUMBER.validate("050 123 456"))
    }

    @Test
    fun `mobile number rejects empty string`() {
        assertFalse(AaniIDType.MOBILE_NUMBER.validate(""))
    }

    // endregion

    // region EMIRATES_ID — regex: ^784-[0-9]{4}-[0-9]{7}-[0-9]$

    @Test
    fun `emirates id accepts correctly formatted id`() {
        assertTrue(AaniIDType.EMIRATES_ID.validate("784-1234-1234567-1"))
    }

    @Test
    fun `emirates id rejects wrong country prefix`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("785-1234-1234567-1"))
    }

    @Test
    fun `emirates id rejects too many digits in year group`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("784-12345-1234567-1"))
    }

    @Test
    fun `emirates id rejects too few digits in year group`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("784-123-1234567-1"))
    }

    @Test
    fun `emirates id rejects too many digits in sequence group`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("784-1234-12345678-1"))
    }

    @Test
    fun `emirates id rejects too few digits in sequence group`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("784-1234-123456-1"))
    }

    @Test
    fun `emirates id rejects missing check digit`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("784-1234-1234567"))
    }

    @Test
    fun `emirates id rejects missing hyphens`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate("78412341234567 1"))
    }

    @Test
    fun `emirates id rejects empty string`() {
        assertFalse(AaniIDType.EMIRATES_ID.validate(""))
    }

    // endregion

    // region PASSPORT_ID — regex: ^[0-9CFGHJKLMNPRTVWXYZ]{9}$
    // Valid characters: digits and uppercase C F G H J K L M N P R T V W X Y Z

    @Test
    fun `passport id accepts 9 characters from valid set`() {
        assertTrue(AaniIDType.PASSPORT_ID.validate("C12345678"))
    }

    @Test
    fun `passport id accepts all digits`() {
        assertTrue(AaniIDType.PASSPORT_ID.validate("123456789"))
    }

    @Test
    fun `passport id accepts all valid uppercase letters`() {
        assertTrue(AaniIDType.PASSPORT_ID.validate("CFGHJKLMN"))
    }

    @Test
    fun `passport id rejects letters not in valid set`() {
        // A, B, D, E, I, O, Q, S, U are excluded
        assertFalse(AaniIDType.PASSPORT_ID.validate("A12345678"))
        assertFalse(AaniIDType.PASSPORT_ID.validate("B12345678"))
        assertFalse(AaniIDType.PASSPORT_ID.validate("I12345678"))
        assertFalse(AaniIDType.PASSPORT_ID.validate("O12345678"))
        assertFalse(AaniIDType.PASSPORT_ID.validate("S12345678"))
    }

    @Test
    fun `passport id rejects lowercase`() {
        assertFalse(AaniIDType.PASSPORT_ID.validate("c12345678"))
    }

    @Test
    fun `passport id rejects fewer than 9 characters`() {
        assertFalse(AaniIDType.PASSPORT_ID.validate("C1234567"))
    }

    @Test
    fun `passport id rejects more than 9 characters`() {
        assertFalse(AaniIDType.PASSPORT_ID.validate("C123456789"))
    }

    @Test
    fun `passport id rejects empty string`() {
        assertFalse(AaniIDType.PASSPORT_ID.validate(""))
    }

    // endregion

    // region EMAIL_ID — regex: ^[a-zA-Z][a-zA-Z0-9._]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$

    @Test
    fun `email accepts valid address`() {
        assertTrue(AaniIDType.EMAIL_ID.validate("user@example.com"))
    }

    @Test
    fun `email accepts address with subdomain`() {
        assertTrue(AaniIDType.EMAIL_ID.validate("user@mail.example.co.uk"))
    }

    @Test
    fun `email accepts address with dots and digits in local part`() {
        assertTrue(AaniIDType.EMAIL_ID.validate("first.last123@domain.com"))
    }

    @Test
    fun `email rejects address starting with digit`() {
        assertFalse(AaniIDType.EMAIL_ID.validate("1user@example.com"))
    }

    @Test
    fun `email rejects address starting with underscore`() {
        assertFalse(AaniIDType.EMAIL_ID.validate("_user@example.com"))
    }

    @Test
    fun `email rejects address with no at sign`() {
        assertFalse(AaniIDType.EMAIL_ID.validate("userexample.com"))
    }

    @Test
    fun `email rejects address with no domain after at sign`() {
        assertFalse(AaniIDType.EMAIL_ID.validate("user@"))
    }

    @Test
    fun `email rejects address with single character TLD`() {
        assertFalse(AaniIDType.EMAIL_ID.validate("user@domain.c"))
    }

    @Test
    fun `email rejects empty string`() {
        assertFalse(AaniIDType.EMAIL_ID.validate(""))
    }

    // endregion

    // region QR_CODE — regex: .* (accepts everything, no user input required)

    @Test
    fun `qr code accepts any non-empty string`() {
        assertTrue(AaniIDType.QR_CODE.validate("https://aani.ae/pay?ref=ABC123"))
    }

    @Test
    fun `qr code accepts empty string`() {
        assertTrue(AaniIDType.QR_CODE.validate(""))
    }

    @Test
    fun `qr code does not require input`() {
        assertFalse(AaniIDType.QR_CODE.requiresInput)
    }

    @Test
    fun `all other types require input`() {
        val typesRequiringInput = AaniIDType.entries.filter { it != AaniIDType.QR_CODE }
        typesRequiringInput.forEach { assertTrue("${it.name} should require input", it.requiresInput) }
    }

    // endregion
}
