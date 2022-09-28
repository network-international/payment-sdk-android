package payment.sdk.android.demo.dependency.repository

import payment.sdk.android.demo.products.data.Price
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Date

@Entity(tableName = "Product")
data class ProductEntity(
        @PrimaryKey
        val id: String,
        @ColumnInfo(name = "name")
        val name: String,
        @ColumnInfo(name = "description")
        val description: String,
        @ColumnInfo(name = "imageUrl")
        val imageUrl: String,
        @ColumnInfo(name = "prices")
        val prices: List<Price>,
        @ColumnInfo(name = "amount")
        val amount: Int,
        @ColumnInfo(name = "date")
        val date: Date = Calendar.getInstance().time
)

