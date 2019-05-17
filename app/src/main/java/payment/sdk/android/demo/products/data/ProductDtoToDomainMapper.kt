package payment.sdk.android.demo.products.data

import payment.sdk.android.BuildConfig
import java.util.*
import javax.inject.Inject

class ProductDtoToDomainMapper @Inject constructor() {

    fun map(dto: ProductDto, currency: Currency): ProductDomain =
            ProductDomain(
                    dto.id,
                    dto.info.name,
                    dto.info.description,
                    dto.prices.asSequence().filter { it.currency == currency }.map { priceDto ->
                        Price(priceDto.currency,
                                price = priceDto.total.movePointLeft(2),
                                tax = priceDto.tax.movePointLeft(2)
                        )
                    }.toList(),
                    imageUrl = BuildConfig.MERCHANT_SERVER_URL + dto.info.images.first().url

            )
}
