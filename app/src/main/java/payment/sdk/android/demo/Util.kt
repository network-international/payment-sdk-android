package payment.sdk.android.demo

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

const val PREF_NAME = "merchant_app_preferences"

fun <T> MutableList<T>.toggle(item: T) {
    if (contains(item)) {
        remove(item)
    } else {
        add(item)
    }
}

fun Context.getPreferences(): SharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

fun Double.formatCurrency(): String {
    return if (this > 10_000) {
        val suffixes = arrayOf("", "K", "M", "B", "T")
        var numValue = this
        var index = 0
        while (numValue >= 1000 && index < suffixes.size - 1) {
            index++
            numValue /= 1000
        }
        "%.2f%s".format(Locale.ENGLISH, numValue, suffixes[index])
    } else {
        "%.2f".format(Locale.ENGLISH, this)
    }
}

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        configuration.screenWidthDp > 840
    } else {
        configuration.screenWidthDp > 600
    }
}

sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val message: String) : Result<T>()
}