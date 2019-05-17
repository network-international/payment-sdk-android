package payment.sdk.android.demo.dependency.preference

import android.content.Context
import android.support.v7.preference.PreferenceManager
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

}
