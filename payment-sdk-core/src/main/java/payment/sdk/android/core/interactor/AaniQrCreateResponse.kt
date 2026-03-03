package payment.sdk.android.core.interactor

import payment.sdk.android.core.AaniPayResponse

sealed class AaniQrCreateResponse {
    data class Success(val aaniPayResponse: AaniPayResponse) : AaniQrCreateResponse()
    data class Error(val error: Exception) : AaniQrCreateResponse()
}
