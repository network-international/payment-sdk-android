package payment.sdk.android.core

object Utils {

    fun String.getQueryParameter(param: String): String? {
        return parseUrlParameters(this)[param]
    }

    private fun parseUrlParameters(urlString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        val regex = Regex("[?&]([^=]+)=([^&]+)")
        val matchResults = regex.findAll(urlString)

        for (matchResult in matchResults) {
            val (key, value) = matchResult.destructured
            params[key] = value
        }

        return params
    }
}