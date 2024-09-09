package payment.sdk.android.googlepay

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.MerchantInfo
import java.math.BigDecimal
import java.math.RoundingMode

class GooglePayJsonConfig {
    /**
     * Create a Google Pay API base request object with properties used in all requests.
     *
     * @return Google Pay API base request object.
     * @throws JSONException
     */
    private val baseRequest = JSONObject()
        .put("apiVersion", 2)
        .put("apiVersionMinor", 0)

    // Create a tokenization specification for the payment gateway
    private val gatewayTokenizationSpecification: JSONObject =
        JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject(PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))

    /**
     * Provide Google Pay API with a payment amount, currency, and amount status.
     *
     * @param price The total price to be paid.
     * @param currencyCode The currency in which the payment will be made.
     * @return JSON object with transaction information.
     */
    private fun getTransactionInfo(amount: Double, currencyCode: String): JSONObject =
        JSONObject()
            .put("totalPrice", amount.centsToString())
            .put("totalPriceStatus", "FINAL")
            .put("totalPriceLabel", "Total")
            .put("currencyCode", currencyCode)

    /**
     * Creates the Google Pay payment request.
     *
     * @param transactionInfo Information about the transaction.
     * @param allowedPaymentMethods The allowed payment methods for the transaction.
     * @param merchantInfo Information about the merchant.
     * @return JSON object representing the payment data request.
     */
    private fun getPaymentDataRequest(
        transactionInfo: JSONObject,
        allowedPaymentMethods: JSONArray,
        merchantInfo: JSONObject
    ): JSONObject =
        baseRequest
            .put("allowedPaymentMethods", allowedPaymentMethods)
            .put("merchantInfo", merchantInfo)
            .put("transactionInfo", transactionInfo)

    /**
     * Creates the merchant configuration.
     *
     * @param merchantInfo Information about the merchant.
     * @return JSON object with the merchant configuration.
     */
    private fun createMerchantConfig(merchantInfo: MerchantInfo): JSONObject =
        JSONObject()
            .put("merchantId", "a9c12627-a429-49c1-b580-a938c86fd9c7")
            .put("merchantName", merchantInfo.name)

    /**
     * Generates the allowed payment methods.
     *
     * @param allowedAuthMethods List of allowed authentication methods.
     * @param allowedCardNetworks List of allowed card networks.
     * @return JSON array of allowed payment methods.
     */
    fun getAllowedPaymentMethods(
        allowedAuthMethods: List<String>,
        allowedCardNetworks: List<String>
    ): JSONArray = JSONArray().put(
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", JSONArray(allowedAuthMethods))
                    .put("allowedCardNetworks", JSONArray(allowedCardNetworks))
            )
    )

    fun baseCardPaymentMethod(
        allowedCardNetworks: List<String>,
        allowedAuthMethods: List<String>
    ): JSONArray = JSONArray().put(
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", JSONArray(allowedAuthMethods))
                    .put("allowedCardNetworks", JSONArray(allowedCardNetworks))
            ))


    /**
     * Creates the full payment request in JSON format.
     *
     * @param googlePayConfigResponse The response configuration.
     * @param price The price of the transaction.
     * @param currencyCode The currency of the transaction.
     * @return String representation of the payment request.
     */
    fun create(
        googlePayConfigResponse: GooglePayConfigResponse,
        amount: Double,
        currencyCode: String
    ): String = getPaymentDataRequest(
        transactionInfo = getTransactionInfo(amount, currencyCode),
        allowedPaymentMethods = getAllowedPaymentMethods(
            allowedCardNetworks = googlePayConfigResponse.allowedPaymentMethods,
            allowedAuthMethods = googlePayConfigResponse.allowedAuthMethods
        ),
        merchantInfo = createMerchantConfig(googlePayConfigResponse.merchantInfo)
    ).toString()

    companion object {
        private const val PAYMENT_GATEWAY_TOKENIZATION_NAME = "networkintl"
        private val PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS = mapOf(
            "gateway" to PAYMENT_GATEWAY_TOKENIZATION_NAME,
            "gatewayMerchantId" to "a9c12627-a429-49c1-b580-a938c86fd9c7"
        )
    }
}

/**
 * Extension function to convert cents to a string format.
 */
fun Double.centsToString(): String = BigDecimal(this)
    .divide(BigDecimal(100))
    .setScale(2, RoundingMode.HALF_EVEN)
    .toString()