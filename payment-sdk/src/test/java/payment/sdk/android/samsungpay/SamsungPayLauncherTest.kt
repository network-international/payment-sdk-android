package payment.sdk.android.samsungpay

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.Order
import payment.sdk.android.payments.SamsungPayConfig

class SamsungPayLauncherTest {

    private val config = SamsungPayConfig(serviceId = "service-id", merchantName = "WestZone")

    @Test
    fun `maps onSuccess to Success`() {
        val launcher = SamsungPayLauncher(mockk<Context>(relaxed = true)) { _, _, response ->
            response.onSuccess()
        }
        var result: SamsungPayLauncher.Result? = null

        launcher.launch(Order(), config) { result = it }

        assertEquals(SamsungPayLauncher.Result.Success, result)
    }

    @Test
    fun `maps onFailure to Failed`() {
        val launcher = SamsungPayLauncher(mockk<Context>(relaxed = true)) { _, _, response ->
            response.onFailure("sheet failed")
        }
        var result: SamsungPayLauncher.Result? = null

        launcher.launch(Order(), config) { result = it }

        assertTrue(result is SamsungPayLauncher.Result.Failed)
        assertEquals("sheet failed", (result as SamsungPayLauncher.Result.Failed).error)
    }

    @Test
    fun `maps onCancelled to Cancelled`() {
        val launcher = SamsungPayLauncher(mockk<Context>(relaxed = true)) { _, _, response ->
            response.onCancelled()
        }
        var result: SamsungPayLauncher.Result? = null

        launcher.launch(Order(), config) { result = it }

        assertEquals(SamsungPayLauncher.Result.Cancelled, result)
    }
}
