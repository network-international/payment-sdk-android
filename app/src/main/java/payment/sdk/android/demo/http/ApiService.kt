package payment.sdk.android.demo.http

import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.core.Order

interface ApiService {

    suspend fun getAccessToken(url: String, apiKey: String, realm: String): String?

    suspend fun getOrder(url: String, orderReference: String, accessToken: String): Order?

    suspend fun createOrder(
        url: String,
        accessToken: String,
        orderRequest: OrderRequest
    ): Order?
}