package payment.sdk.android.googlepay

import android.os.Parcelable
import com.google.android.gms.wallet.WalletConstants
import kotlinx.parcelize.Parcelize

/**
 * Represents the configuration for Google Pay integration.
 *
 * @property environment The environment to be used for Google Pay (e.g., Production or Test).
 * @property isEmailRequired Indicates whether the user's email is required.
 * @property billingAddressConfig Configuration for the billing address.
 */
@Parcelize
data class GooglePayConfig(
    val environment: Environment,
    var isEmailRequired: Boolean = false,
    var billingAddressConfig: BillingAddressConfig = BillingAddressConfig()
) : Parcelable {

    /**
     * Represents the configuration for the billing address.
     *
     * @property isRequired Indicates whether a billing address is required.
     * @property format The format of the billing address (e.g., Min or Full).
     * @property isPhoneNumberRequired Indicates whether a phone number is required in the billing address.
     */
    @Parcelize
    data class BillingAddressConfig @JvmOverloads constructor(
        internal val isRequired: Boolean = false,
        internal val format: Format = Format.Min,
        internal val isPhoneNumberRequired: Boolean = false
    ) : Parcelable

    /**
     * Enum representing the possible environments for Google Pay.
     *
     * @property value The internal value used to represent the environment in Google Pay.
     */
    @Parcelize
    enum class Environment(internal val value: Int) : Parcelable {
        /**
         * The production environment for Google Pay.
         */
        Production(WalletConstants.ENVIRONMENT_PRODUCTION),
        /**
         * The test environment for Google Pay.
         */
        Test(WalletConstants.ENVIRONMENT_TEST)
    }

    /**
     * Enum representing the formats of the billing address.
     */
    @Parcelize
    enum class Format : Parcelable {
        Min,
        Full
    }
}

internal fun GooglePayConfig?.env() =
    this?.environment?.value ?: GooglePayConfig.Environment.Test.value