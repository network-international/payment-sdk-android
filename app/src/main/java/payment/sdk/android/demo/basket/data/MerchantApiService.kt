package payment.sdk.android.demo.basket.data

import io.reactivex.Single
import payment.sdk.android.core.Order
import payment.sdk.android.core.PaymentResponse
import retrofit2.http.*

interface MerchantApiService {

    @POST(value = "api/update_shipping_address")
    fun updateShippingAddress(@Body shippingFeeRequest: ShippingFeeRequestDto): Single<FeeResponseDto>

    @POST(value = "api/update_card")
    fun updateCard(@Body cardSurchargeRequest: CardSurchargeRequestDto): Single<FeeResponseDto>

    @POST(value = "api/update_delivery_method")
    fun updateDeliveryMethod(@Body deliveryMethodRequest: DeliveryMethodRequestDto): Single<FeeResponseDto>

    @POST(value = "api/createOrder")
    fun createPaymentOrder(@Body createPaymentOrderRequest: CreatePaymentOrderRequestDto): Single<CreateOrderResponseDto>

    @POST(value = "api/savedCard")
    fun createSavedCardOrder(@Body createSavedCardOrder: CreatePaymentOrderRequestDto): Single<PaymentResponse>

    @POST(value = "api/createOrder")
    fun createOrder(@Body createPaymentOrderRequest: CreatePaymentOrderRequestDto): Single<Order>

    @GET(value = "order/{orderId}")
    fun getOrder(@Path("orderId") orderId: String): Single<Order>
}
