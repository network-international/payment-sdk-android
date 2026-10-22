package payment.sdk.android.demo.model

import android.content.Context

/**
 * One environment decoded from a QR payload line.
 *
 * Line format, pipe-delimited, shared with the iOS and Flutter demos and the
 * outlet-cred-puller:
 *
 *     nickname|realm|outletReference|apiKey|applePayMerchantId|region|currency|type|orderAction|orderType|paymentMethods
 *
 * `paymentMethods` is comma-separated and informational — what the gateway offered on a
 * probe order, shown on the environment tile so an outlet's capabilities are visible
 * without opening it.
 *
 * The first five fields are the original contract; the rest were appended as region,
 * currency, order action and order type each moved onto the outlet, so a five- or
 * eight-field code from an older generator still parses, defaulting whatever it omits.
 * The three- and four-field legacy
 * shapes (no nickname / no Apple Pay id) stay accepted because older codes are in
 * circulation. Apple Pay id is parsed for position only — this demo has no use for it.
 */
data class ScannedEnvironment(
    val nickname: String,
    val realm: String,
    val outletReference: String,
    val apiKey: String,
    val region: Region = Region.UAE,
    val currency: String = "AED",
    /** DEV / UAT / PROD. Present so a bulk import needs no per-outlet prompt. */
    val type: EnvironmentType = EnvironmentType.DEV,
    /** SALE / PURCHASE / AUTH; an omitted field means the default, SALE. */
    val orderAction: String = "SALE",
    /** RECURRING / UNSCHEDULED / INSTALLMENT; SINGLE is the default. */
    val orderType: String = "SINGLE",
    /** Gateway payment-method names. Empty for a code predating the field. */
    val paymentMethods: List<String> = emptyList(),
) {
    fun toEnvironment(): Environment = Environment(
        type = type,
        name = if (nickname.isBlank()) realm else nickname,
        nickname = nickname.ifBlank { null },
        apiKey = apiKey,
        outletReference = outletReference,
        realm = realm,
        region = region,
        currency = currency,
        orderAction = orderAction,
        orderType = orderType,
        paymentMethods = paymentMethods,
    )

    /** Everything parsed from a payload, plus how many lines were not an environment. */
    data class Batch(val environments: List<ScannedEnvironment>, val rejected: Int)

    companion object {
        /** Parses one pipe-delimited line. Null on an unrecognised shape. */
        fun parse(line: String): ScannedEnvironment? {
            val parts = line.trim().split("|")
            fun at(i: Int, fallback: String = ""): String =
                parts.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

            return when {
                parts.size >= 5 -> ScannedEnvironment(
                    nickname = at(0),
                    realm = at(1),
                    outletReference = at(2),
                    apiKey = at(3),
                    region = Region.entries.firstOrNull { it.code == at(5, "UAE").uppercase() }
                        ?: Region.UAE,
                    currency = at(6, "AED").uppercase(),
                    type = EnvironmentType.entries.firstOrNull { it.value == at(7, "DEV").uppercase() }
                        ?: EnvironmentType.DEV,
                    orderAction = OrderAction.entries.firstOrNull { it.code == at(8, "SALE").uppercase() }
                        ?.code ?: OrderAction.SALE.code,
                    // Other platforms store SINGLE as the empty string; Android's enum has an
                    // explicit SINGLE, so an omitted field maps to it rather than to "".
                    orderType = OrderType.entries.firstOrNull { it.code == at(9, "SINGLE").uppercase() }
                        ?.code ?: OrderType.SINGLE.code,
                    paymentMethods = at(10).split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() },
                )
                parts.size == 4 -> ScannedEnvironment(
                    nickname = at(0), realm = at(1), outletReference = at(2), apiKey = at(3),
                )
                parts.size == 3 -> ScannedEnvironment(
                    nickname = "", realm = at(0), outletReference = at(1), apiKey = at(2),
                )
                else -> null
            }
        }

        /**
         * Encodes one environment as a payload line — the inverse of [parse].
         *
         * Always writes all ten fields so a line never has a variable shape, and strips
         * pipes and newlines from the free-text fields: the format has no escaping, so a
         * pipe in a nickname would shift every later field and put the region where the
         * API key belongs.
         */
        fun encode(e: Environment, context: Context): String = listOf(
            e.nickname.orEmpty(),
            e.realm,
            e.outletReference,
            e.apiKey,
            "",                     // applePayMerchantId: iOS-only, not stored here
            e.region.code,
            e.currencyCode(context),
            e.type.value,
            e.orderActionCode(context),
            e.orderTypeCode(context),
            e.paymentMethods.orEmpty().joinToString(","),
        ).joinToString("|") { it.replace(Regex("[|\r\n]"), " ").trim() }

        /** Encodes every environment, one per line — what Copy and the QR both use. */
        fun encodeAll(envs: List<Environment>, context: Context): String =
            envs.joinToString("\n") { encode(it, context) }

        /**
         * Splits [lines] into groups that each fit within [limitBytes] when joined.
         *
         * Whole lines only: an environment split across two codes would parse as two
         * broken ones. A single line over the limit still gets a group of its own —
         * dropping it would silently lose an outlet. The limit matches the
         * outlet-cred-puller's, so a batch splits identically whichever end makes it.
         */
        fun chunkLines(lines: List<String>, limitBytes: Int): List<List<String>> {
            val chunks = mutableListOf<List<String>>()
            var current = mutableListOf<String>()
            var size = 0
            lines.forEach { line ->
                val add = line.toByteArray().size + if (current.isEmpty()) 0 else 1
                if (current.isNotEmpty() && size + add > limitBytes) {
                    chunks += current
                    current = mutableListOf()
                    size = 0
                }
                current += line
                size += add
            }
            if (current.isNotEmpty()) chunks += current
            return chunks
        }

        /**
         * Parses a whole payload: one environment per line, blank lines ignored. A bulk
         * code with one bad line still imports the rest and reports what it skipped.
         */
        fun parseAll(payload: String): Batch {
            val envs = mutableListOf<ScannedEnvironment>()
            var rejected = 0
            payload.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                parse(line)?.let { envs += it } ?: rejected++
            }
            return Batch(envs, rejected)
        }
    }
}
