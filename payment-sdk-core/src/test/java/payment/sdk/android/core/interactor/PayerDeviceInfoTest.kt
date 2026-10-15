package payment.sdk.android.core.interactor

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayerDeviceInfoTest {

    @Test
    fun `phones are Mobile and 600dp and wider are Tablet`() {
        assertEquals(PayerDeviceInfo.DEVICE_TYPE_MOBILE, PayerDeviceInfo.deviceType(411))
        assertEquals(PayerDeviceInfo.DEVICE_TYPE_TABLET, PayerDeviceInfo.deviceType(600))
        assertEquals(PayerDeviceInfo.DEVICE_TYPE_TABLET, PayerDeviceInfo.deviceType(800))
    }

    @Test
    fun `deviceType is left out when the screen width is undefined`() {
        assertNull(PayerDeviceInfo.deviceType(Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED))
    }
}
