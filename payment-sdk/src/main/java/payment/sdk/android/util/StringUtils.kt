package payment.sdk.android.util

import java.util.regex.Pattern

internal fun String.extractUrlsAndText(): List<Pair<String, Boolean>> {
    val result: MutableList<Pair<String, Boolean>> = ArrayList()
    val urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"
    val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
    val urlMatcher = pattern.matcher(this)
    var lastMatchEnd = 0

    while (urlMatcher.find()) {
        val start = urlMatcher.start(0)
        val end = urlMatcher.end(0)

        // Add the text before the URL if there's any
        if (start > lastMatchEnd) {
            result.add(Pair(this.substring(lastMatchEnd, start), false)) // Non-URL text
        }

        // Add the URL
        result.add(Pair(this.substring(start, end), true)) // URL

        // Update last match end
        lastMatchEnd = end
    }

    // Add remaining text after the last URL
    if (lastMatchEnd < this.length) {
        result.add(Pair(this.substring(lastMatchEnd), false)) // Remaining non-URL text
    }

    return result
}