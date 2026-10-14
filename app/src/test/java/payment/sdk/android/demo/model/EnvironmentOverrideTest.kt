package payment.sdk.android.demo.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentOverrideTest {

    private val outlet = Environment(
        EnvironmentType.UAT, id = "env-1", name = "n", apiKey = "k", outletReference = "o",
        realm = "r", region = Region.UAE, currency = "QAR", orderAction = "PURCHASE",
        orderType = "SINGLE",
    )

    /** Preferences holding [outlet] as the selected environment, plus [values]. */
    private fun contextWith(vararg values: Pair<String, String>): Context {
        val stored = mapOf(
            "saved_environments" to Gson().toJson(listOf(outlet)),
            "saved_env_id" to "env-1",
        ) + values
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(any(), any()) } answers {
            stored[firstArg<String>()] ?: secondArg<String?>()
        }
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        return context
    }

    @Test
    fun `off - orders use the selected environment`() {
        val context = contextWith("override_currency" to "SAR", "override_order_action" to "AUTH")
        assertEquals("QAR", Environment.selectedCurrency(context))
        assertEquals("PURCHASE", Environment.selectedOrderAction(context))
        assertEquals("SINGLE", Environment.selectedOrderType(context))
    }

    @Test
    fun `on - every order getter returns the override`() {
        val context = contextWith(
            "override_enabled" to "true",
            "override_region" to "KSA",
            "override_currency" to "SAR",
            "override_order_action" to "AUTH",
            "override_order_type" to "UNSCHEDULED",
        )
        assertTrue(EnvironmentOverride.isEnabled(context))
        assertEquals("KSA", EnvironmentOverride.region(context))
        assertEquals("SAR", Environment.selectedCurrency(context))
        assertEquals("AUTH", Environment.selectedOrderAction(context))
        assertEquals("UNSCHEDULED", Environment.selectedOrderType(context))
    }

    @Test
    fun `the override never moves the gateway or the outlet`() {
        val context = contextWith("override_enabled" to "true", "override_region" to "KSA")
        val selected = Environment.getSelectedEnvironment(context)!!
        assertEquals(Region.UAE, selected.region)
        assertTrue(selected.getGatewayUrl().startsWith("https://api-gateway.sandbox.ngenius-payments.com/"))
    }
}
