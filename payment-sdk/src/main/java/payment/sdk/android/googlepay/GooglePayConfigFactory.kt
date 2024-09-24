package payment.sdk.android.googlepay

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.coroutines.tasks.await
import payment.sdk.android.cardPayments.GooglePayConfig
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.interactor.GooglePayConfigInteractor

internal class GooglePayConfigFactory(
    private val paymentsClient: PaymentsClient,
    private val googlePayJsonConfig: GooglePayJsonConfig,
    private val googlePayConfigInteractor: GooglePayConfigInteractor,
    private val allowedWallets: List<String>
) {
    suspend fun checkGooglePayConfig(
        googlePayConfigUrl: String?,
        accessToken: String,
    ): GooglePayConfig? {
        return allowedWallets
            .takeIf { it.contains(KEY_GOOGLE_PAY) }
            ?.let { checkForGooglePay(googlePayConfigUrl, accessToken) }
            ?.let { googlePayConfigResponse ->
                createGooglePayRequest(googlePayConfigResponse = googlePayConfigResponse)
            }
    }

    private suspend fun checkForGooglePay(
        url: String?, accessToken: String
    ) = url?.let { googlePayConfigInteractor.getConfig(url = it, accessToken = accessToken) }

    private suspend fun createGooglePayRequest(
        googlePayConfigResponse: GooglePayConfigResponse
    ): GooglePayConfig? {
        try {
            val paymentDataRequestJson = googlePayJsonConfig.create(
                googlePayConfigResponse = googlePayConfigResponse
            )
            val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
            val allowedPaymentMethods = googlePayJsonConfig.getAllowedPaymentMethods(
                allowedAuthMethods = googlePayConfigResponse.allowedAuthMethods,
                allowedCardNetworks = googlePayConfigResponse.allowedPaymentMethods
            )
            return GooglePayConfig(
                canUseGooglePay = fetchCanUseGooglePay(
                    googlePayJsonConfig.isReadyToPayRequest(
                        allowedPaymentMethods = allowedPaymentMethods
                    )
                ),
                allowedPaymentMethods = allowedPaymentMethods.toString(),
                task = paymentsClient.loadPaymentData(request)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private suspend fun fetchCanUseGooglePay(
        isReadyToPayRequest: String
    ): Boolean {
        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequest)
        return paymentsClient.isReadyToPay(request).await()
    }

    companion object {
        private const val KEY_GOOGLE_PAY = "GOOGLE_PAY"
    }
}