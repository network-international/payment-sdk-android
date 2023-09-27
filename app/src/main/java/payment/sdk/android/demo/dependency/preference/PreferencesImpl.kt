package payment.sdk.android.demo.dependency.preference

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import payment.sdk.android.core.SavedCard
import javax.inject.Inject

class PreferencesImpl @Inject constructor(private val context: Context) : Preferences {

    override fun put(key: String, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    override fun getString(key: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
    }

    override fun getSavedCard(): SavedCard? {
        val savedCardJson =
            PreferenceManager.getDefaultSharedPreferences(context).getString(SAVE_CARD_KEY, null)
        return try {
            Gson().fromJson(savedCardJson, SavedCard::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    override fun saveCard(savedCard: SavedCard) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(SAVE_CARD_KEY, Gson().toJson(savedCard))
            .apply()
    }

    companion object {
        const val SAVE_CARD_KEY = "saved_card_key"
    }
}
