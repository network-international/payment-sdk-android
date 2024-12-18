package payment.sdk.android.googlepay

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.coroutines.tasks.await
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.interactor.GooglePayConfigInteractor

internal class GooglePayConfigFactory(
    private val paymentsClient: PaymentsClient,
    private val googlePayJsonConfig: GooglePayJsonConfig,
    private val googlePayConfigInteractor: GooglePayConfigInteractor,
) {
    suspend fun checkGooglePayConfig(
        googlePayConfigUrl: String?,
        accessToken: String,
        amount: Double,
        currencyCode: String,
        googlePayAcceptUrl: String
    ): GooglePayUiConfig? {
        return checkForGooglePay(googlePayConfigUrl, accessToken)
            ?.let { googlePayConfigResponse ->
                createGooglePayRequest(
                    googlePayConfigResponse = googlePayConfigResponse,
                    amount = amount,
                    currencyCode = currencyCode,
                    googlePayAcceptUrl = googlePayAcceptUrl
                )
            }
    }

    private suspend fun checkForGooglePay(
        url: String?, accessToken: String
    ) = url?.let { googlePayConfigInteractor.getConfig(url = it, accessToken = accessToken) }

    private suspend fun createGooglePayRequest(
        googlePayConfigResponse: GooglePayConfigResponse,
        googlePayAcceptUrl: String,
        amount: Double,
        currencyCode: String
    ): GooglePayUiConfig? {
        try {
            val paymentDataRequestJson = googlePayJsonConfig.create(
                googlePayConfigResponse = googlePayConfigResponse,
                amount = amount,
                currencyCode = currencyCode
            )
            val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
            val allowedPaymentMethods = googlePayJsonConfig.getAllowedPaymentMethods(
                allowedAuthMethods = googlePayConfigResponse.allowedAuthMethods,
                allowedCardNetworks = googlePayConfigResponse.allowedPaymentMethods,
                merchantGatewayId = googlePayConfigResponse.merchantGatewayId
            )
            return GooglePayUiConfig(
                canUseGooglePay = fetchCanUseGooglePay(
                    googlePayJsonConfig.isReadyToPayRequest(
                        allowedPaymentMethods
                    )
                ),
                allowedPaymentMethods = allowedPaymentMethods.toString(),
                task = paymentsClient.loadPaymentData(request),
                googlePayAcceptUrl = googlePayAcceptUrl
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