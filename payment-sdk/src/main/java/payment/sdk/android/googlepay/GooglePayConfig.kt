package payment.sdk.android.googlepay

import android.os.Parcelable
import com.google.android.gms.wallet.WalletConstants
import kotlinx.parcelize.Parcelize

@Parcelize
data class GooglePayConfig(
    val environment: Environment,
    var isEmailRequired: Boolean = false,
    var billingAddressConfig: BillingAddressConfig = BillingAddressConfig()
) : Parcelable {
    @Parcelize
    data class BillingAddressConfig @JvmOverloads constructor(
        internal val isRequired: Boolean = false,
        internal val format: Format = Format.Min,
        internal val isPhoneNumberRequired: Boolean = false
    ) : Parcelable

    @Parcelize
    enum class Environment(internal val value: Int) : Parcelable {
        Production(WalletConstants.ENVIRONMENT_PRODUCTION),
        Test(WalletConstants.ENVIRONMENT_TEST)
    }

    @Parcelize
    enum class Format : Parcelable {
        Min,
        Full
    }
}

internal fun GooglePayConfig?.env() =
    this?.environment?.value ?: GooglePayConfig.Environment.Test.value