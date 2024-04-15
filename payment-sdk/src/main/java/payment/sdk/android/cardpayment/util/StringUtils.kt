package payment.sdk.android.cardpayment.util

import java.util.regex.Pattern

fun String.extractUrls(): List<Triple<String, Int, Int>> {
    val containedUrls: MutableList<Triple<String, Int, Int>> = ArrayList()
    val urlRegex =
        "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"
    val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
    val urlMatcher = pattern.matcher(this)
    while (urlMatcher.find()) {
        val start = urlMatcher.start(0)
        val end = urlMatcher.end(0)
        containedUrls.add(
            Triple(
                this.substring(
                    start,
                    end
                ),
                start,
                end
            )
        )
    }
    return containedUrls
}