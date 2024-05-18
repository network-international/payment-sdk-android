package payment.sdk.android.demo.http

import payment.sdk.android.demo.Result
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.core.Order

class CreateOrderApiInteractor(private val apiService: ApiService) {
    suspend fun createOrder(environment: Environment, orderRequest: OrderRequest): Result<Order> {
        val accessToken = apiService.getAccessToken(
            url = environment.getIdentityUrl(),
            apiKey = environment.apiKey,
            realm = environment.realm
        )

        if (accessToken == null) {
            return Result.Error(message = "Failed to get access token")
        }

        val order = apiService.createOrder(
            url = environment.getGatewayUrl(),
            accessToken = accessToken,
            orderRequest = orderRequest
        )

        if (order == null) {
            return Result.Error(message = "Failed to create order")
        }
        return Result.Success(data = order)
    }
}