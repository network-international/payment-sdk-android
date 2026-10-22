package payment.sdk.android.core.interactor

import android.content.Context
import android.content.res.Configuration

/**
 * The payer device fields Trident screens payments with, sent at the top level of every card and
 * wallet payment body. Every field is optional on the gateway, so a value that cannot be read is
 * left out rather than holding up the payment.
 *
 * `browserName`, `deviceName` and `fingerprint` are not sent: the SDK is not a browser, and the
 * other two have no agreed value yet.
 */
object PayerDeviceInfo {

    const val FIELD_DEVICE_ID = "deviceId"
    const val FIELD_DEVICE_OS = "deviceOs"
    const val FIELD_DEVICE_TYPE = "deviceType"

    /** Trident wants the platform name only, with no version. */
    const val DEVICE_OS_ANDROID = "Android"

    const val DEVICE_TYPE_MOBILE = "Mobile"
    const val DEVICE_TYPE_TABLET = "Tablet"

    fun fields(context: Context): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        runCatching { DeviceIdProvider.getDeviceId(context) }.getOrNull()
            ?.let { fields[FIELD_DEVICE_ID] = it }
        fields[FIELD_DEVICE_OS] = DEVICE_OS_ANDROID
        deviceType(context)?.let { fields[FIELD_DEVICE_TYPE] = it }
        return fields
    }

    /** Android's own tablet threshold: a smallest width of 600dp or more. */
    internal fun deviceType(context: Context): String? {
        val configuration = runCatching { context.resources.configuration }.getOrNull() ?: return null
        return deviceType(configuration.smallestScreenWidthDp)
    }

    internal fun deviceType(smallestScreenWidthDp: Int): String? = when {
        smallestScreenWidthDp == Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED -> null
        smallestScreenWidthDp >= 600 -> DEVICE_TYPE_TABLET
        else -> DEVICE_TYPE_MOBILE
    }
}
