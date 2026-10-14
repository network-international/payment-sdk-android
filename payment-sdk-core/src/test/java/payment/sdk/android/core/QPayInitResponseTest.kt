package payment.sdk.android.core

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QCB returns the same field as a JSON number or a JSON string depending on the endpoint, so the
 * decoder has to accept both and always hand back a string for the redirect form POST.
 */
class QPayInitResponseTest {

    private val gson = Gson()

    private fun parse(json: String) = gson.fromJson(json, QPayInitResponse::class.java)

    @Test
    fun `decodes the gateway's PascalCase keys`() {
        val response = parse(
            """
            {
              "redirectUri": "https://qcb.example.com/pay",
              "Amount": "500",
              "CurrencyCode": "634",
              "PUN": "PUN123",
              "MerchantModuleSessionID": "sess-1",
              "PaymentDescription": "Order 1",
              "NationalID": "nid",
              "MerchantID": "merch-1",
              "BankID": "bank-1",
              "Lang": "en",
              "Action": "0",
              "SecureHash": "hash",
              "TransactionRequestDate": "01012026",
              "ExtraFields_f14": "extra",
              "Quantity": "1"
            }
            """.trimIndent()
        )

        assertEquals("https://qcb.example.com/pay", response.redirectUri)
        assertEquals("500", response.amount)
        assertEquals("PUN123", response.pun)
        assertEquals("sess-1", response.merchantModuleSessionID)
        assertEquals("extra", response.extraFieldsF14)
    }

    @Test
    fun `numeric values are coerced to strings`() {
        val response = parse("""{"Amount": 500, "Quantity": 1, "CurrencyCode": 634}""")

        assertEquals("500", response.amount)
        assertEquals("1", response.quantity)
        assertEquals("634", response.currencyCode)
    }

    @Test
    fun `absent fields decode to null`() {
        val response = parse("""{"redirectUri": "https://qcb.example.com/pay"}""")

        assertNull(response.amount)
        assertNull(response.secureHash)
        assertNull(response.cancelled)
    }

    @Test
    fun `cancelled flag is decoded`() {
        assertEquals(true, parse("""{"cancelled": true}""").cancelled)
        assertEquals(false, parse("""{"cancelled": false}""").cancelled)
    }

    @Test
    fun `ordered form fields keep the order QCB expects`() {
        val names = parse("""{"Amount": "500"}""").orderedFormFields().map { it.first }

        assertEquals(
            listOf(
                "Amount", "CurrencyCode", "PUN", "MerchantModuleSessionID", "PaymentDescription",
                "NationalID", "MerchantID", "BankID", "Lang", "Action", "SecureHash",
                "TransactionRequestDate", "ExtraFields_f14", "Quantity"
            ),
            names
        )
    }

    @Test
    fun `missing values become empty strings rather than dropped fields`() {
        val fields = parse("""{"Amount": "500"}""").orderedFormFields().toMap()

        assertEquals("500", fields["Amount"])
        assertEquals("", fields["SecureHash"])
        assertEquals(14, fields.size)
    }

    @Test
    fun `an empty payload still yields the full field set`() {
        val response = parse("{}")

        assertNull(response.redirectUri)
        assertEquals(14, response.orderedFormFields().size)
        assertTrue(response.orderedFormFields().all { it.second == "" })
    }
}
