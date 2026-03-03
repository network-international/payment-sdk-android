package payment.sdk.android.core.interactor

import android.util.Log
import com.google.gson.Gson
import payment.sdk.android.core.Order
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class GetOrderApiInteractor(private val httpClient: HttpClient) {

    suspend fun getOrder(url: String, accessToken: String): Order? {

        val response = httpClient.get(
            url = url,
            headers = mapOf(TransactionServiceHttpAdapter.HEADER_AUTHORIZATION to "Bearer $accessToken"),
            body = Body.Empty()
        )

        return when (response) {
            is SDKHttpResponse.Failed -> {
                Log.e(TAG, "GetOrder failed: ${response.error.message}")
                null
            }
            is SDKHttpResponse.Success -> {
                Log.d(TAG, "GetOrder response: ${response.body}")
                val order = Gson().fromJson(response.body, Order::class.java)
                Log.d(TAG, "Parsed order - paymentMethods.wallet: ${order?.paymentMethods?.wallet?.toList()}")
                Log.d(TAG, "Parsed order - paymentMethods.card: ${order?.paymentMethods?.card}")
                Log.d(TAG, "Parsed order - paymentMethods.apm: ${order?.paymentMethods?.apm?.toList()}")
                order
            }
        }
    }

    companion object {
        private const val TAG = "GetOrderApiInteractor"
    }
}