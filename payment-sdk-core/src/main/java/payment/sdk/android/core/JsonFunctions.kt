package payment.sdk.android.core

import org.json.JSONObject
import org.json.JSONArray

inline fun <reified T> JSONArray.toList() = List(length()) { i -> get(i) as T }

fun JSONArray.at(index: Int): JSONObject = getJSONObject(index)

fun JSONObject.int(key: String?): Int? = key?.let { getOrNull(it) { getInt(it) } }

fun JSONObject.double(key: String?): Double? = key?.let { getOrNull(it) { getDouble(it) } }

fun JSONObject.string(key: String?): String? = key?.let { getOrNull(it) { getString(it) } }

fun JSONObject.json(key: String?): JSONObject? = key?.let { getOrNull(it) { getJSONObject(it) } }

fun JSONObject.array(key: String?): JSONArray? = key?.let { getOrNull(it) { getJSONArray(it) } }

fun JSONObject.boolean(key: String?): Boolean? = key?.let { getOrNull(it) { getBoolean(it) } }

private inline fun <T> JSONObject.getOrNull(
        key: String,
        getter: JSONObject.() -> T
): T? = if (has(key)) getter(this) else null

