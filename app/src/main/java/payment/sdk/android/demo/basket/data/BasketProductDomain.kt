package payment.sdk.android.demo.basket.data

import payment.sdk.android.demo.products.data.Price

data class BasketProductDomain(
        val id: String,
        val name: String,
        val description: String,
        val imageUrl: String,
        val prices: List<Price>,
        val amount: Int
)
