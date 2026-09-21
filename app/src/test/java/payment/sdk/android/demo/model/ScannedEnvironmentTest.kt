package payment.sdk.android.demo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The QR payload is the contract between the outlet-cred-puller, the older standalone
 * env-qr-generator, and the iOS / Flutter demos. A code that scans on one demo must
 * scan on all of them, so the accepted shapes are pinned here.
 */
class ScannedEnvironmentTest {

    @Test
    fun `ten fields carry the order action and type too`() {
        val e = ScannedEnvironment.parse("KSA UAT|ni|o|k|m|KSA|SAR|UAT|PURCHASE|INSTALLMENT")!!
        assertEquals("PURCHASE", e.orderAction)
        assertEquals("INSTALLMENT", e.orderType)
        assertEquals(Region.KSA, e.region)
        assertEquals(EnvironmentType.UAT, e.type)
    }

    @Test
    fun `an omitted order type means SINGLE, matching the enum`() {
        assertEquals("SINGLE", ScannedEnvironment.parse("n|ni|o|k|m|UAE|AED|DEV|SALE|")!!.orderType)
        assertEquals("SINGLE", ScannedEnvironment.parse("n|ni|o|k|m|UAE|AED|DEV|SALE|SINGLE")!!.orderType)
    }

    @Test
    fun `order action and type are case-normalised, like the other fields`() {
        // The puller emits uppercase, but a hand-written or older code may not, and a
        // lowercase value that silently fell back to the default would be invisible.
        val e = ScannedEnvironment.parse("n|ni|o|k|m|ksa|qar|uat|purchase|recurring")!!
        assertEquals("PURCHASE", e.orderAction)
        assertEquals("RECURRING", e.orderType)
        assertEquals(Region.KSA, e.region)
        assertEquals("QAR", e.currency)
        assertEquals(EnvironmentType.UAT, e.type)
    }

    @Test
    fun `an unknown order action or type falls back rather than storing junk`() {
        val e = ScannedEnvironment.parse("n|ni|o|k|m|UAE|AED|DEV|REFUND|WEEKLY")!!
        assertEquals("SALE", e.orderAction)
        assertEquals("SINGLE", e.orderType)
    }

    @Test
    fun `toEnvironment carries the order settings onto the saved outlet`() {
        val env = ScannedEnvironment.parse("N|ni|o|k|m|KSA|QAR|PROD|AUTH|RECURRING")!!.toEnvironment()
        assertEquals("AUTH", env.orderAction)
        assertEquals("RECURRING", env.orderType)
        assertEquals("QAR", env.currency)
    }

    @Test
    fun `eight fields carry region, currency and type`() {
        val e = ScannedEnvironment.parse("KSA UAT|ni|outlet-1|key-1|merchant.com.x|KSA|SAR|UAT")!!
        assertEquals("KSA UAT", e.nickname)
        assertEquals("ni", e.realm)
        assertEquals("outlet-1", e.outletReference)
        assertEquals("key-1", e.apiKey)
        assertEquals(Region.KSA, e.region)
        assertEquals("SAR", e.currency)
        assertEquals(EnvironmentType.UAT, e.type)
        assertEquals("SALE", e.orderAction)
        assertEquals("SINGLE", e.orderType)
    }

    @Test
    fun `five fields - the pre-region contract - default the rest`() {
        val e = ScannedEnvironment.parse("KSA UAT|ni|outlet-1|key-1|merchant.com.x")!!
        assertEquals(Region.UAE, e.region)
        assertEquals("AED", e.currency)
        assertEquals(EnvironmentType.DEV, e.type)
        assertEquals("SALE", e.orderAction)
        assertEquals("SINGLE", e.orderType)
    }

    @Test
    fun `empty trailing fields fall back to defaults and are case-normalised`() {
        val e = ScannedEnvironment.parse("n|ni|o|k|||qar|")!!
        assertEquals(Region.UAE, e.region)
        assertEquals("QAR", e.currency)
        assertEquals(EnvironmentType.DEV, e.type)
    }

    @Test
    fun `unknown region or type falls back rather than crashing`() {
        val e = ScannedEnvironment.parse("n|ni|o|k||MARS|AED|STAGING")!!
        assertEquals(Region.UAE, e.region)
        assertEquals(EnvironmentType.DEV, e.type)
    }

    @Test
    fun `four fields - nickname without an Apple Pay id`() {
        val e = ScannedEnvironment.parse("KSA UAT|ni|outlet-1|key-1")!!
        assertEquals("KSA UAT", e.nickname)
        assertEquals("key-1", e.apiKey)
    }

    @Test
    fun `three fields - realm leads, no nickname`() {
        val e = ScannedEnvironment.parse("ni|outlet-1|key-1")!!
        assertEquals("", e.nickname)
        assertEquals("ni", e.realm)
        assertEquals("key-1", e.apiKey)
    }

    @Test
    fun `other shapes are rejected rather than mis-mapped`() {
        assertNull(ScannedEnvironment.parse("ni|outlet-1"))
        assertNull(ScannedEnvironment.parse("https://example.com/x"))
    }

    @Test
    fun `bulk payload is one environment per line, blank lines ignored`() {
        val b = ScannedEnvironment.parseAll("A|ni|o1|k1|\n\nB|ni|o2|k2||KSA|SAR|UAT\r\nC|ni|o3|k3\n")
        assertEquals(listOf("o1", "o2", "o3"), b.environments.map { it.outletReference })
        assertEquals(Region.KSA, b.environments[1].region)
        assertEquals("C", b.environments[2].nickname)
        assertEquals(0, b.rejected)
    }

    @Test
    fun `a bad line is counted and skipped, the rest still import`() {
        val b = ScannedEnvironment.parseAll("A|ni|o1|k1|\nnot an env\nB|ni|o2|k2|")
        assertEquals(2, b.environments.size)
        assertEquals(1, b.rejected)
    }

    @Test
    fun `toEnvironment names the outlet by nickname, falling back to realm`() {
        assertEquals("KSA UAT", ScannedEnvironment.parse("KSA UAT|ni|o|k|")!!.toEnvironment().name)
        assertEquals("ni", ScannedEnvironment.parse("ni|o|k")!!.toEnvironment().name)
    }
}
