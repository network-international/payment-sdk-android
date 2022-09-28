package payment.sdk.android.cardpayment.threedsecuretwo

import org.json.JSONObject
import java.lang.Exception

data class ThreeDSecureTwoRequest(
    val directoryServerID: String?,
    val threeDSMessageVersion: String?
) {
    companion object{
        fun buildFromOrderResponse(responseJson: JSONObject): ThreeDSecureTwoRequest {
            var directoryServerID: String?
            var threeDSMessageVersion: String?
            try {
                val threeDSTwoConfig = responseJson.getJSONObject("3ds2")
                directoryServerID = threeDSTwoConfig.getString("directoryServerID")
                threeDSMessageVersion = threeDSTwoConfig.getString("messageVersion")
            } catch (e: Exception) {
                directoryServerID = null
                threeDSMessageVersion = null
            }
            return ThreeDSecureTwoRequest(directoryServerID, threeDSMessageVersion)
        }
    }
}
