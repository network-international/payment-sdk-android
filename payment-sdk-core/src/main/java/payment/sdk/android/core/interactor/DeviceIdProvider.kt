package payment.sdk.android.core.interactor

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.UUID
import androidx.core.content.edit

/**
 * The payer's device identifier, sent to the gateway as `X-Payer-Fingerprint` and as `deviceId` in
 * payment bodies for Trident fraud screening.
 *
 * It is `Settings.Secure.ANDROID_ID`, the mechanism Trident asked for: it needs no permission and is
 * stable per app signing key, user and device. Android can still return nothing for it, so a random
 * ID stored on first use stands in rather than leaving the payment without one.
 */
object DeviceIdProvider {

    private const val PREF_NAME = "ni_sdk_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getDeviceId(context: Context): String = androidId(context) ?: storedFallbackId(context)

    @SuppressLint("HardwareIds")
    private fun androidId(context: Context): String? =
        runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun storedFallbackId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        var id = prefs.getString(KEY_DEVICE_ID, null)

        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit { putString(KEY_DEVICE_ID, id) }
        }

        return id
    }
}
