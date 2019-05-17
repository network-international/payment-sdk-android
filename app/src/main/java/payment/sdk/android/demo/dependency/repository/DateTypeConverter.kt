package payment.sdk.android.demo.dependency.repository

import android.arch.persistence.room.TypeConverter
import java.util.*

object DateTypeConverter {

    @TypeConverter
    @JvmStatic
    fun toDate(value: Long?): Date? =
            value?.let {
                Date(it)
            }

    @TypeConverter
    @JvmStatic
    fun toLong(value: Date?): Long? =
            value?.let {
                value.time
            }

}