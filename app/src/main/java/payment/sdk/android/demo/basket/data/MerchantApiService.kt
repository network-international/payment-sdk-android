package payment.sdk.android.demo.basket.data

import io.reactivex.Single
import retrofit2.http.*

interface MerchantApiService {

    @POST(value = "api/update_shipping_address")
    fun updateShippingAddress(@Body shippingFeeRequest: ShippingFeeRequestDto): Single<FeeResponseDto>

    @POST(value = "api/update_card")
    fun updateCard(@Body cardSurchargeRequest: CardSurchargeRequestDto): Single<FeeResponseDto>

    @POST(value = "api/update_delivery_method")
    fun updateDeliveryMethod(@Body deliveryMethodRequest: DeliveryMethodRequestDto): Single<FeeResponseDto>

    @POST(value = "api/create_payment_order")
    fun createPaymentOrder(@Body createPaymentOrderRequest: CreatePaymentOrderRequestDto): Single<CreatePaymentOrderResponseDto>


}
