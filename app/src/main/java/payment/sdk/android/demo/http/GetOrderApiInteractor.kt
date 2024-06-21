package payment.sdk.android.demo.http

import payment.sdk.android.demo.Result
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.core.Order

class GetOrderApiInteractor(
    private val apiService: ApiService
) {
    suspend fun getOrder(environment: Environment, orderReference: String?): Result<Order> {
        if (orderReference == null) {
            return Result.Error(message = "Order reference is null")
        }
        val accessToken = apiService.getAccessToken(
            url = environment.getIdentityUrl(),
            apiKey = environment.apiKey,
            realm = environment.realm
        )
        if (accessToken == null) {
            return Result.Error(message = "Failed to get access token")
        }
        val order = apiService.getOrder(
            url = environment.getGatewayUrl(),
            orderReference = orderReference,
            accessToken = accessToken
        )
        if (order == null) {
            return Result.Error(message = "Order not found")
        }
        return Result.Success(order)
    }
}