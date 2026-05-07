package payment.sdk.android.payments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration required to handle Samsung Pay inside the unified payment page.
 *
 * @property serviceId The Samsung Pay service ID registered in the Samsung developer portal.
 * @property merchantName The merchant name displayed on the Samsung Pay payment sheet.
 */
@Parcelize
data class SamsungPayConfig(
    val serviceId: String,
    val merchantName: String
) : Parcelable
