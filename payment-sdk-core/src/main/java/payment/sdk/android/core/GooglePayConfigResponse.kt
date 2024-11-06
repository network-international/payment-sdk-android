package payment.sdk.android.core

import androidx.annotation.Keep

@Keep
data class GooglePayConfigResponse(
    val allowedAuthMethods: List<String>,
    val allowedPaymentMethods: List<String>,
    val environment: String,
    val gatewayName: String,
    val merchantInfo: MerchantInfo
)

@Keep
data class MerchantInfo(
    val name: String,
    val reference: String
)