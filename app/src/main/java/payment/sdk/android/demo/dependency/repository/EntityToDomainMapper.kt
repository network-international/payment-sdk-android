package payment.sdk.android.demo.dependency.repository

import payment.sdk.android.demo.basket.data.BasketProductDomain
import javax.inject.Inject

class EntityToDomainMapper @Inject constructor() {

    fun map(entity: ProductEntity): BasketProductDomain =
            BasketProductDomain(
                    entity.id,
                    entity.name,
                    entity.description,
                    entity.imageUrl,
                    entity.prices,
                    entity.amount
            )

}