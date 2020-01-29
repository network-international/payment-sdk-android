package payment.sdk.android.core

import org.json.JSONObject
import org.json.JSONArray

inline fun <reified T> JSONArray.toList() = List(length()) { i -> get(i) as T }

fun JSONArray.at(index: Int): JSONObject = getJSONObject(index)

fun JSONObject.int(key: String?): Int? = getOrNull(key) { getInt(key) }

fun JSONObject.double(key: String?): Double? = getOrNull(key) { getDouble(key) }

fun JSONObject.string(key: String?): String? = getOrNull(key) { getString(key) }

fun JSONObject.json(key: String?): JSONObject? = getOrNull(key) { getJSONObject(key) }

fun JSONObject.array(key: String?): JSONArray? = getOrNull(key) { getJSONArray(key) }

fun JSONObject.boolean(key: String?): Boolean? = getOrNull(key) { getBoolean(key) }

private inline fun <T> JSONObject.getOrNull(
        key: String?,
        getter: JSONObject.() -> T
) = if (key == null) null else {
    if (has(key)) getter(this) else null
}

