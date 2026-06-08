package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Response from N-Genius `/qpay` endpoint — contains the form fields the QCB hosted gateway expects.
 *
 * QCB returns numeric fields (`Amount: 500`, `Quantity: 1`) as JSON numbers, but they all need to
 * be POSTed as strings. Each scalar field uses [StringyDeserializer] so we accept either form.
 */
@Keep
data class QPayInitResponse(
    val redirectUri: String? = null,

    val cancelled: Boolean? = null,

    @SerializedName("Amount")
    @JsonAdapter(StringyDeserializer::class)
    val amount: String? = null,

    @SerializedName("CurrencyCode")
    @JsonAdapter(StringyDeserializer::class)
    val currencyCode: String? = null,

    @SerializedName("PUN")
    @JsonAdapter(StringyDeserializer::class)
    val pun: String? = null,

    @SerializedName("MerchantModuleSessionID")
    @JsonAdapter(StringyDeserializer::class)
    val merchantModuleSessionID: String? = null,

    @SerializedName("PaymentDescription")
    @JsonAdapter(StringyDeserializer::class)
    val paymentDescription: String? = null,

    @SerializedName("NationalID")
    @JsonAdapter(StringyDeserializer::class)
    val nationalID: String? = null,

    @SerializedName("MerchantID")
    @JsonAdapter(StringyDeserializer::class)
    val merchantID: String? = null,

    @SerializedName("BankID")
    @JsonAdapter(StringyDeserializer::class)
    val bankID: String? = null,

    @SerializedName("Lang")
    @JsonAdapter(StringyDeserializer::class)
    val lang: String? = null,

    @SerializedName("Action")
    @JsonAdapter(StringyDeserializer::class)
    val action: String? = null,

    @SerializedName("SecureHash")
    @JsonAdapter(StringyDeserializer::class)
    val secureHash: String? = null,

    @SerializedName("TransactionRequestDate")
    @JsonAdapter(StringyDeserializer::class)
    val transactionRequestDate: String? = null,

    @SerializedName("ExtraFields_f14")
    @JsonAdapter(StringyDeserializer::class)
    val extraFieldsF14: String? = null,

    @SerializedName("Quantity")
    @JsonAdapter(StringyDeserializer::class)
    val quantity: String? = null
) {
    /**
     * Ordered hidden-input set the QCB gateway expects on the redirect POST.
     * Mirrors PayPageV2's `qpayRedirectForm.ts` field set.
     */
    fun orderedFormFields(): List<Pair<String, String>> = listOf(
        "Amount" to (amount ?: ""),
        "CurrencyCode" to (currencyCode ?: ""),
        "PUN" to (pun ?: ""),
        "MerchantModuleSessionID" to (merchantModuleSessionID ?: ""),
        "PaymentDescription" to (paymentDescription ?: ""),
        "NationalID" to (nationalID ?: ""),
        "MerchantID" to (merchantID ?: ""),
        "BankID" to (bankID ?: ""),
        "Lang" to (lang ?: ""),
        "Action" to (action ?: ""),
        "SecureHash" to (secureHash ?: ""),
        "TransactionRequestDate" to (transactionRequestDate ?: ""),
        "ExtraFields_f14" to (extraFieldsF14 ?: ""),
        "Quantity" to (quantity ?: "")
    )
}

/** Accepts JSON string OR number and returns the value as a String. */
internal class StringyDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) return null
        if (json.isJsonPrimitive) {
            val prim = json.asJsonPrimitive
            return when {
                prim.isString -> prim.asString
                prim.isNumber -> prim.asString // gson treats numbers as strings via asString
                prim.isBoolean -> prim.asString
                else -> null
            }
        }
        return null
    }
}
