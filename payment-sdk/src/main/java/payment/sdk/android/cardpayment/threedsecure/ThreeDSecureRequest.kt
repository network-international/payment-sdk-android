package payment.sdk.android.cardpayment.threedsecure

import org.json.JSONObject

data class ThreeDSecureRequest(
        val acsUrl: String,
        val acsPaReq: String,
        val acsMd: String,
        val gatewayUrl: String
) {
    companion object {
        fun buildFromOrderResponse(responseJson: JSONObject): ThreeDSecureRequest {
            // $._links.cnp:3ds
            val gatewayUrl = responseJson.getJSONObject("_links")
                    .getJSONObject("cnp:3ds").getString("href")
            // $.3ds
            val aclInfoJson = responseJson.getJSONObject("3ds")
            val acsUrl = aclInfoJson.getString("acsUrl")
            val acsPaReq = aclInfoJson.getString("acsPaReq")
            val acsMd = aclInfoJson.getString("acsMd")

            return ThreeDSecureRequest(acsUrl, acsPaReq, acsMd, gatewayUrl)
        }
    }
}
