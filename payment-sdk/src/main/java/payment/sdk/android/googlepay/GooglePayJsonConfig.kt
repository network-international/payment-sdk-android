package payment.sdk.android.googlepay

import org.json.JSONArray
import org.json.JSONObject
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.MerchantInfo
import java.math.BigDecimal
import java.math.RoundingMode

internal class GooglePayJsonConfig() {
    private val baseRequest = JSONObject()
        .put("apiVersion", 2)
        .put("apiVersionMinor", 0)

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
            .put("merchantId", merchantInfo.reference)
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
        allowedCardNetworks: List<String>,
        merchantGatewayId: String?,
    ): JSONArray = JSONArray().put(
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters",
                JSONObject()
                    .put("allowedAuthMethods", JSONArray(allowedAuthMethods))
                    .put("allowedCardNetworks", JSONArray(allowedCardNetworks))
            ).put(
                "tokenizationSpecification",
                JSONObject()
                    .put("type", "PAYMENT_GATEWAY")
                    .put(
                        "parameters",
                        JSONObject()
                            .put("gatewayMerchantId", merchantGatewayId)
                            .put("gateway", PAYMENT_GATEWAY_TOKENIZATION_NAME)
                    )
            )
    )

    fun isReadyToPayRequest(
        allowedPaymentMethods: JSONArray
    ): String {
        return baseRequest
            .put("allowedPaymentMethods", allowedPaymentMethods)
            .toString()
    }

    fun baseCardPaymentMethod(
        allowedCardNetworks: List<String>,
        allowedAuthMethods: List<String>
    ): JSONObject = JSONObject()
        .put("type", "CARD")
        .put(
            "parameters",
            JSONObject()
                .put("allowedAuthMethods", JSONArray(allowedAuthMethods))
                .put("allowedCardNetworks", JSONArray(allowedCardNetworks))
        )

    /**
     * Creates the full payment request in JSON format.
     *
     * @param googlePayConfigResponse The response configuration.
     * @return String representation of the payment request.
     */
    fun create(
        googlePayConfigResponse: GooglePayConfigResponse,
        amount: Double,
        currencyCode: String,
    ): String = getPaymentDataRequest(
        transactionInfo = getTransactionInfo(amount, currencyCode),
        allowedPaymentMethods = getAllowedPaymentMethods(
            allowedCardNetworks = googlePayConfigResponse.allowedPaymentMethods,
            allowedAuthMethods = googlePayConfigResponse.allowedAuthMethods,
            merchantGatewayId = googlePayConfigResponse.merchantGatewayId
        ),
        merchantInfo = createMerchantConfig(googlePayConfigResponse.merchantInfo)
    ).toString()

    companion object {
        private const val PAYMENT_GATEWAY_TOKENIZATION_NAME = "networkintl"
    }
}

/**
 * Extension function to convert cents to a string format.
 */
fun Double.centsToString(): String = BigDecimal(this)
    .divide(BigDecimal(100))
    .setScale(2, RoundingMode.HALF_EVEN)
    .toString()