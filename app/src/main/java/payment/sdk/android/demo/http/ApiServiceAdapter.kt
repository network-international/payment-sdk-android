package payment.sdk.android.demo.http

import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.toMap
import com.google.gson.Gson
import com.google.gson.JsonParser
import payment.sdk.android.core.Order
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class ApiServiceAdapter(
    private val httpClient: HttpClient
) : ApiService {
    override suspend fun getAccessToken(url: String, apiKey: String, realm: String): String? {
        val response = httpClient.post(
            url = url,
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-identity.v1+json",
                HEADER_AUTH to "Basic $apiKey"
            ),
            body = Body.Json(
                mapOf(
                    "realmName" to realm
                )
            )
        )
        return when (response) {
            is SDKHttpResponse.Failed -> null
            is SDKHttpResponse.Success -> {
                JsonParser.parseString(response.body).asJsonObject.get("access_token")?.asString
            }
        }
    }

    override suspend fun getOrder(
        url: String,
        orderReference: String,
        accessToken: String
    ): Order? {
        val response = httpClient.get(
            url = "$url/$orderReference",
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_AUTH to "Bearer $accessToken"
            ),
            body = Body.Empty()
        )
        return when (response) {
            is SDKHttpResponse.Failed -> null
            is SDKHttpResponse.Success -> {
                return Gson().fromJson(response.body, Order::class.java)
            }
        }
    }

    override suspend fun createOrder(
        url: String,
        accessToken: String,
        orderRequest: OrderRequest
    ): Order? {
        var createOrderUrl = "https://api-gateway-dev.ngenius-payments.com/subscription/outlets/3a85b238-9d42-4fb4-9962-b4c489fb2765/orders/direct-order"
        var contentType = "application/vnd.ni-subscription.v1+json"
        if (orderRequest.type == "RECURRING" || orderRequest.type == "INSTALLMENT") {
            createOrderUrl = url.replace("transactions", "recurring-payment")
            contentType = "application/vnd.ni-subscription.v1+json"
        }
        val bodyMap = mutableMapOf(
            "planReference" to "3d3581a2-b366-4a06-bc70-82b62828ffec",
            "transactionType" to "RECURRING_PURCHASE",
            "tenure" to 2,
            "total" to mapOf(
                "currencyCode" to "AED",
                "value" to 1100
            ),
            "orderStartDate" to "2026-01-29T01:01:00Z",
            "firstName" to "Jayavelu",
            "lastName" to "M",
            "email" to "jayavelu.mohan@equalexperts.com",
            "paymentAttempts" to 3
//            "planReference" to "ae27797e-bfd3-454c-9651-5714caf522ae",
//            "transactionType" to "INSTALLMENT",
//            "tenure" to 5,
//            "total" to mapOf(
//                "currencyCode" to "AED",
//                "value" to 100000
//            ),
//            "orderStartDate" to "2026-01-27T01:01:00Z",
//            "invoiceExpiryDate" to "2026-01-29T01:01:00Z",
//            "firstName" to "Jayavelu",
//            "lastName" to "M",
//            "email" to "jayavelu.mohan@equalexperts.com",
//            "paymentAttempts" to 3,
//            "skipInvoiceCreatedEmailNotification" to false,
//            "notifyPayByLink" to true,
//            "paymentStructure" to "INTRODUCTORY",
//            "initialInstallmentAmount" to 2000,
//            "initialPeriodLength" to 1
        )
        val response = httpClient.post(
            url = createOrderUrl,
            headers = mapOf(
                HEADER_CONTENT_TYPE to contentType,
                HEADER_ACCEPT to contentType,
                HEADER_AUTH to "Bearer $accessToken"
            ),
            body = Body.Json(
                bodyMap
            )
        )
        return when (response) {
            is SDKHttpResponse.Failed -> null
            is SDKHttpResponse.Success -> {
                return Gson().fromJson(response.body, Order::class.java)
            }
        }
    }

    companion object {
        const val HEADER_AUTH = "Authorization"
        const val HEADER_ACCEPT = "Content-Type"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}