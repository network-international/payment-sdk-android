package payment.sdk.android.demo.dependency.repository

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class ProductDao {

    @Query("SELECT * FROM Product WHERE id = :id")
    abstract fun getProduct(id: String): Single<ProductEntity>

    @Query("SELECT * FROM Product WHERE id = :id LIMIT 1")
    abstract fun findProductBy(id: String): Single<List<ProductEntity>>

    @Query("SELECT * FROM Product ORDER BY date DESC")
    abstract fun getProducts(): Flowable<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(product: ProductEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(product: ProductEntity)

    @Query("DELETE FROM Product WHERE id = :id")
    abstract fun delete(id: String)

    @Query("DELETE FROM Product")
    abstract fun deleteAll()
}
