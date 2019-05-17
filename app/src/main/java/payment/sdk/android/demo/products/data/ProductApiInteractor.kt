package payment.sdk.android.demo.products.data

import payment.sdk.android.demo.dependency.configuration.Configuration
import io.reactivex.Single
import javax.inject.Inject

class ProductApiInteractor @Inject constructor(
        private val apiService: ProductApiService,
        private val configuration: Configuration,
        private val dtoToDomainMapper: ProductDtoToDomainMapper
) {

    fun getProducts(): Single<List<ProductDomain>> {
        return Single.fromCallable {
            Pair(configuration.locale, configuration.currency)
        }.flatMap { (locale, currency) ->
            val language = locale.language.toLowerCase()
            apiService.getProducts(language).map { responseDto ->
                responseDto.products.map { dto ->
                    dtoToDomainMapper.map(dto, currency)
                }
            }
        }
    }
}

