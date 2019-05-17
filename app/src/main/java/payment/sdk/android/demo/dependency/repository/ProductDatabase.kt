package payment.sdk.android.demo.dependency.repository

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

@Database(entities = [ProductEntity::class], version = ProductDatabase.VERSION, exportSchema = false)
@TypeConverters(DateTypeConverter::class, PriceListTypeConverter::class)
abstract class ProductDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        const val VERSION = 1
    }
}