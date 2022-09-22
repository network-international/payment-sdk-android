package payment.sdk.android.demo.dependency.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ProductEntity::class], version = ProductDatabase.VERSION, exportSchema = false)
@TypeConverters(DateTypeConverter::class, PriceListTypeConverter::class)
abstract class ProductDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        const val VERSION = 1
    }
}