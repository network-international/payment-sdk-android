package payment.sdk.android.demo.dependency.repository

import payment.sdk.android.demo.products.data.Price
import androidx.room.TypeConverter
import com.google.gson.GsonBuilder

object PriceListTypeConverter {

    private val gson = GsonBuilder().create()

    @TypeConverter
    @JvmStatic
    fun toString(values: List<Price>): String {
        return gson.toJson(values)
    }

    @TypeConverter
    @JvmStatic
    fun toPriceList(json: String): List<Price> {
        return gson.fromJson(json, Array<Price>::class.java).toList()
    }
}