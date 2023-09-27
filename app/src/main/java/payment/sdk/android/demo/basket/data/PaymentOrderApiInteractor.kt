package payment.sdk.android.demo.basket.data

import payment.sdk.android.demo.dependency.configuration.Configuration
import payment.sdk.android.core.CardMapping
import io.reactivex.Single
import payment.sdk.android.core.CardType
import payment.sdk.android.core.Order
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.SavedCard
import java.math.BigDecimal
import javax.inject.Inject

class PaymentOrderApiInteractor @Inject constructor(
        private val merchantApiService: MerchantApiService,
        private val configuration: Configuration) {

    /**
     * BigDecimal amount should be translated into payment gateway integer value using
     *      @code {mapToGatewayAmount(BigDecimal)}
     *
     * Before calling SALE API (or AUTH API), we have to move decimal point to the right
     * This maybe different depending on currency
     *
     *      USD -> $1.00 translates to 100
     *      AED -> AED1.00 translates to 100
     *      GBP -> £1.00 translates to 100
     */
    fun createPaymentOrder(action: String, amount: BigDecimal, currency: String): Single<CreatePaymentOrderResponseDomain> =
            Single.just(configuration.locale.language)
                    .flatMap { language ->
                        val request = CreatePaymentOrderRequestDto(
                                action = action,
                                amount = PaymentOrderAmountDto(mapToGatewayAmount(amount), currency),
                                language = language
                        )
                        return@flatMap merchantApiService.createPaymentOrder(request).map { dto ->
                            CreatePaymentOrderResponseDomain(
                                    orderReference = dto.reference,
                                    paymentAuthorizationUrl = dto.paymentLinks.paymentAuthorization.href,
                                    code = dto.paymentLinks.payment.href.split("=").get(1),
                                    supportedCards = CardMapping.mapSupportedCards(dto.paymentMethods.card))
                        }
                    }

    fun createSavedCardOrder(action: String, amount: BigDecimal, currency: String): Single<PaymentResponse> =
        Single.just(configuration.locale.language)
            .flatMap { language ->
                val  request = CreatePaymentOrderRequestDto(
                    action = action,
                    amount = PaymentOrderAmountDto(mapToGatewayAmount(amount), currency),
                    language = language
                )
                return@flatMap merchantApiService.createSavedCardOrder(request).map{ PaymentResponse ->
                    PaymentResponse
                }
            }

    fun createOrder(action: String, amount: BigDecimal, currency: String, savedCard: SavedCard? = null): Single<Order> =
            Single.just(configuration.locale.language)
                    .flatMap { language ->
                        val request = CreatePaymentOrderRequestDto(
                            action = action,
                            amount = PaymentOrderAmountDto(mapToGatewayAmount(amount), currency),
                            language = language,
                            savedCard = savedCard
                        )
                        return@flatMap merchantApiService.createOrder(request)
                    }

    fun getOrder(orderId: String): Single<Order> {
        return Single.just(configuration.locale.language)
            .flatMap { language ->
                return@flatMap merchantApiService.getOrder(orderId = orderId).map {
                        order -> order
                }
            }
    }

    private fun mapToGatewayAmount(amount: BigDecimal) =
            amount.movePointRight(2).toInt()

    fun updateShippingAddress(postCode: String): Single<BigDecimal> =
            merchantApiService.updateShippingAddress(ShippingFeeRequestDto(postCode)).map { it.fee }


    fun updateCard(cardType: CardType): Single<BigDecimal> =
            merchantApiService.updateCard(CardSurchargeRequestDto(cardType)).map { it.fee }


    fun updateDeliveryMethod(selection: String): Single<FeeResponseDto> =
            merchantApiService.updateDeliveryMethod(DeliveryMethodRequestDto(selection))


}

