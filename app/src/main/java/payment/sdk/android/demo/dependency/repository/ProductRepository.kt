package payment.sdk.android.demo.dependency.repository

import payment.sdk.android.demo.basket.data.BasketProductDomain
import payment.sdk.android.demo.products.data.ProductDomain
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject


class ProductRepository @Inject constructor(
        private val productDao: ProductDao,
        private val entityToDomainMapper: EntityToDomainMapper
) {

    fun getBasketProducts(): Flowable<List<BasketProductDomain>> =
            productDao.getProducts().map { entities ->
                entities.map { entity ->
                    entityToDomainMapper.map(entity)
                }
            }

    fun insertProduct(product: ProductDomain): Completable =
            productDao.findProductBy(product.id).flatMapCompletable { entities ->
                return@flatMapCompletable if (entities.isEmpty()) {
                    Completable.fromAction {
                        productDao.insert(ProductEntity(
                                id = product.id,
                                name = product.name,
                                description = product.description,
                                imageUrl = product.imageUrl,
                                prices = product.prices,
                                amount = 1
                        ))
                    }
                } else {
                    val entity = entities[0]
                    Completable.fromAction {
                        productDao.update(entity.copy(amount = entity.amount + 1))
                    }
                }
            }

    fun deleteProduct(id: String): Completable {
        return Completable.fromCallable {
            productDao.delete(id)
        }
    }

    fun removeProduct(id: String): Completable {
        return productDao.findProductBy(id).flatMapCompletable { entities ->
            return@flatMapCompletable if (entities.isEmpty()) {
                Completable.complete()
            } else {
                val entity = entities[0]
                if (entity.amount > 1) {
                    Completable.fromCallable { productDao.update(entity.copy(amount = entity.amount - 1)) }
                } else {
                    deleteProduct(id)
                }
            }
        }
    }

    fun removeAll(): Completable {
        return Completable.fromCallable {
            productDao.deleteAll()
        }
    }

    fun hasProduct(id: String): Single<Boolean> {
        return productDao.findProductBy(id).flatMap { entities ->
            return@flatMap Single.just(entities.isNotEmpty())
        }
    }
}