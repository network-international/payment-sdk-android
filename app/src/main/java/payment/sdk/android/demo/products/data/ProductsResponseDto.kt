package payment.sdk.android.demo.products.data

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*

data class ProductsResponseDto(
        @SerializedName("products") val products: List<ProductDto>
)

data class ProductDto(
        @SerializedName("id") val id: String,
        @SerializedName("info") val info: ProductInfoDto,
        @SerializedName("prices") val prices: List<PriceDto>
)

data class ProductInfoDto(
        @SerializedName("locale") val locale: String,
        @SerializedName("productDescription") val description: String,
        @SerializedName("name") val name: String,
        @SerializedName("images") val images: List<ProductImageDto>
)

data class ProductImageDto(
        @SerializedName("url") val url: String,
        @SerializedName("size") val size: ProductImageSizeDto
)

data class ProductImageSizeDto(
        @SerializedName("width") val width: Int,
        @SerializedName("height") val height: Int,
        @SerializedName("dpi") val url: Int
)

data class PriceDto(
        @SerializedName("total") val total: BigDecimal,
        @SerializedName("tax") val tax: BigDecimal,
        @SerializedName("currency") val currency: Currency
)