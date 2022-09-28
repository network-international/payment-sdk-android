package payment.sdk.android.demo.dependency

import payment.sdk.android.demo.dependency.repository.ProductDao
import payment.sdk.android.demo.dependency.repository.ProductDatabase
import payment.sdk.android.demo.dependency.repository.ProductRepository
import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        fun provideProductDatabase(app: Application): ProductDatabase =
                Room.databaseBuilder(
                        app.applicationContext, ProductDatabase::class.java, "product_db"
                ).fallbackToDestructiveMigration().build()

        @JvmStatic
        @Provides
        @Singleton
        fun provideProductDao(database: ProductDatabase): ProductDao =
                database.productDao()

        @JvmStatic
        @Provides
        @Singleton
        fun provideProductRepository(repository: ProductRepository): ProductRepository {
            return repository
        }
    }
}
