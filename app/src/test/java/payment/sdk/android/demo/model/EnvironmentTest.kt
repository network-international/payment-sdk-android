package payment.sdk.android.demo.model

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentTest {

    /**
     * A context whose preferences hold the pre-migration app-wide values, so the fallback
     * path can be exercised without an Android runtime.
     */
    private fun contextWith(vararg values: Pair<String, String>): Context {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(any(), any()) } answers {
            values.toMap()[firstArg<String>()] ?: secondArg<String?>()
        }
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        return context
    }

    @Test
    fun `an outlet carrying its own values ignores the legacy globals`() {
        val context = contextWith("currency" to "SAR", "order_action" to "AUTH", "order_type" to "RECURRING")
        val env = Environment(
            EnvironmentType.UAT, name = "n", apiKey = "k", outletReference = "o", realm = "r",
            region = Region.KSA, currency = "BHD", orderAction = "PURCHASE", orderType = "INSTALLMENT",
        )
        assertEquals("BHD", env.currencyCode(context))
        assertEquals("PURCHASE", env.orderActionCode(context))
        assertEquals("INSTALLMENT", env.orderTypeCode(context))
    }

    @Test
    fun `an outlet saved before the move inherits the old app-wide pickers`() {
        val context = contextWith("currency" to "SAR", "order_action" to "AUTH", "order_type" to "RECURRING")
        val env = Environment(
            EnvironmentType.UAT, name = "n", apiKey = "k", outletReference = "o", realm = "r",
            region = Region.UAE,
        )
        assertEquals("SAR", env.currencyCode(context))
        assertEquals("AUTH", env.orderActionCode(context))
        assertEquals("RECURRING", env.orderTypeCode(context))
    }

    @Test
    fun `with nothing stored at all the documented defaults apply`() {
        val context = contextWith()
        val env = Environment(
            EnvironmentType.UAT, name = "n", apiKey = "k", outletReference = "o", realm = "r",
            region = Region.UAE,
        )
        assertEquals("AED", env.currencyCode(context))
        assertEquals("SALE", env.orderActionCode(context))
        assertEquals("SINGLE", env.orderTypeCode(context))
    }

    @Test
    fun `a KSA outlet is routed to the KSA deployment`() {
        val ksa = Environment(
            EnvironmentType.UAT, name = "n", apiKey = "k", outletReference = "o", realm = "r",
            region = Region.KSA,
        )
        assert(ksa.getGatewayUrl().contains(".ksa.")) { "expected a KSA host, got ${ksa.getGatewayUrl()}" }
        assert(ksa.getIdentityUrl().contains(".ksa.")) { "expected a KSA identity host" }
    }

    @Test
    fun `getGatewayUrl and getIdentityUrl return correct URLs for all environment types`() {
        val environments = listOf(
            Environment(EnvironmentType.DEV, name = "Dev Environment", apiKey = "api_key", outletReference = "outlet_ref", realm = "realm", region = Region.UAE),
            Environment(EnvironmentType.UAT, name = "UAT Environment", apiKey = "api_key", outletReference = "outlet_ref", realm = "realm", region = Region.UAE),
            Environment(EnvironmentType.PROD, name = "Prod Environment", apiKey = "api_key", outletReference = "outlet_ref", realm = "realm", region = Region.UAE)
        )

        environments.forEach { environment ->
            val gatewayUrl = when (environment.type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/transactions/outlets/outlet_ref/orders"
                EnvironmentType.UAT -> "https://api-gateway.sandbox.ngenius-payments.com/transactions/outlets/outlet_ref/orders"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/transactions/outlets/outlet_ref/orders"
            }

            val identityUrl = when (environment.type) {
                EnvironmentType.DEV -> "https://api-gateway-dev.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.UAT -> "https://api-gateway.sandbox.ngenius-payments.com/identity/auth/access-token"
                EnvironmentType.PROD -> "https://api-gateway.ngenius-payments.com/identity/auth/access-token"
            }

            assertEquals(gatewayUrl, environment.getGatewayUrl())
            assertEquals(identityUrl, environment.getIdentityUrl())
        }
    }
}
