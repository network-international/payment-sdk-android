package payment.sdk.android.demo.products.data

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApiService {

    @GET(value = "products/products_{language}.json")
    fun getProducts(@Path("language") language: String): Single<ProductsResponseDto>
}
