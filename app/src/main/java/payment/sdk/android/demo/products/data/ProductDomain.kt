package payment.sdk.android.demo.products.data

import org.parceler.Parcel
import org.parceler.ParcelConstructor
import java.math.BigDecimal
import java.util.*

@Parcel(Parcel.Serialization.BEAN)
data class ProductDomain @ParcelConstructor constructor(
        val id: String,
        val name: String,
        val description: String,
        val prices: List<Price>,
        val imageUrl: String
)

@Parcel(Parcel.Serialization.BEAN)
class Price @ParcelConstructor constructor(
        val currency: Currency,
        val price: BigDecimal,
        val tax: BigDecimal)


