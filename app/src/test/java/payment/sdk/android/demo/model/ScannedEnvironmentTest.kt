package payment.sdk.android.demo.model

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
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

    // --- encode: the inverse of parse, shared with the iOS and Flutter demos ---

    private fun env(
        nickname: String? = "KSA Main",
        outlet: String = "outlet-1",
        type: EnvironmentType = EnvironmentType.UAT,
        region: Region = Region.KSA,
        currency: String? = "SAR",
        orderAction: String? = "PURCHASE",
        orderType: String? = "INSTALLMENT",
    ) = Environment(
        type = type, name = "n", nickname = nickname, apiKey = "key-1",
        outletReference = outlet, realm = "ni", region = region,
        currency = currency, orderAction = orderAction, orderType = orderType,
    )

    private val context: Context = mockk<Context>().also {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(any(), any()) } answers { secondArg() }
        every { it.getSharedPreferences(any(), any()) } returns prefs
    }

    @Test
    fun `encode writes all eleven fields in the shared order`() {
        assertEquals(
            "KSA Main|ni|outlet-1|key-1||KSA|SAR|UAT|PURCHASE|INSTALLMENT|",
            ScannedEnvironment.encode(env(), context),
        )
    }

    @Test
    fun `encode strips a pipe from free text rather than shifting later fields`() {
        val parts = ScannedEnvironment.encode(env(nickname = "a|b"), context).split("|")
        assertEquals(11, parts.size)
        assertEquals("key-1", parts[3])
    }

    @Test
    fun `parse of encode preserves every field`() {
        val original = env()
        val back = ScannedEnvironment.parse(ScannedEnvironment.encode(original, context))!!
        assertEquals(original.nickname, back.nickname)
        assertEquals(original.realm, back.realm)
        assertEquals(original.outletReference, back.outletReference)
        assertEquals(original.apiKey, back.apiKey)
        assertEquals(original.region, back.region)
        assertEquals(original.currency, back.currency)
        assertEquals(original.type, back.type)
        assertEquals(original.orderAction, back.orderAction)
        assertEquals(original.orderType, back.orderType)
    }

    @Test
    fun `encodeAll then parseAll returns every environment in order`() {
        val envs = listOf(env(outlet = "o1"), env(outlet = "o2", region = Region.UAE), env(outlet = "o3"))
        val batch = ScannedEnvironment.parseAll(ScannedEnvironment.encodeAll(envs, context))
        assertEquals(0, batch.rejected)
        assertEquals(listOf("o1", "o2", "o3"), batch.environments.map { it.outletReference })
        assertEquals(Region.UAE, batch.environments[1].region)
    }

    // --- chunking: the same rule the puller and the other demos use ---

    @Test
    fun `eleven fields carry the payment methods`() {
        val e = ScannedEnvironment.parse(
            "N|ni|o|k|m|UAE|AED|UAT|SALE|SINGLE|VISA,APPLE_PAY,DIRECT_APPLE_PAY")!!
        assertEquals(listOf("VISA", "APPLE_PAY", "DIRECT_APPLE_PAY"), e.paymentMethods)
    }

    @Test
    fun `methods are trimmed, upper-cased and blanks dropped`() {
        val e = ScannedEnvironment.parse("N|ni|o|k|m|UAE|AED|UAT|SALE|SINGLE| visa , qpay ,,")!!
        assertEquals(listOf("VISA", "QPAY"), e.paymentMethods)
    }

    @Test
    fun `a ten-field line from an older generator still parses`() {
        val e = ScannedEnvironment.parse("N|ni|o|k|m|UAE|AED|UAT|SALE|SINGLE")!!
        assertEquals(emptyList<String>(), e.paymentMethods)
        assertEquals("SALE", e.orderAction)
    }

    @Test
    fun `encode writes the methods back as field eleven`() {
        val line = ScannedEnvironment.encode(
            env().copy(paymentMethods = listOf("VISA", "QPAY")), context)
        val parts = line.split("|")
        assertEquals(11, parts.size)
        assertEquals("VISA,QPAY", parts[10])
    }

    @Test
    fun `a small batch stays in one code`() {
        assertEquals(1, ScannedEnvironment.chunkLines(List(3) { "x".repeat(100) }, 2000).size)
    }

    @Test
    fun `a large batch splits without losing, reordering or splitting a line`() {
        val lines = List(10) { "$it" + "x".repeat(260) }
        val chunks = ScannedEnvironment.chunkLines(lines, 2000)
        assert(chunks.size > 1) { "expected more than one code" }
        assertEquals(lines, chunks.flatten())
        chunks.forEach { assert(it.joinToString("\n").toByteArray().size <= 2000) }
    }

    @Test
    fun `a single oversize line still gets a code of its own`() {
        val chunks = ScannedEnvironment.chunkLines(listOf("y".repeat(3000), "short"), 2000)
        assertEquals(listOf(1, 1), chunks.map { it.size })
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
