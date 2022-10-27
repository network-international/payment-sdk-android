package payment.sdk.android.cardpayment.threedsecuretwo

import com.google.gson.Gson
import org.json.JSONObject
import java.lang.Exception
import java.net.URI
import java.util.*

data class ThreeDSecureTwoRequest(
    val directoryServerID: String?,
    val threeDSMessageVersion: String?,
    val threeDSMethodURL: String?,
    val threeDSServerTransID: String?,
    val threeDSMethodData: String?,
    val threeDSMethodNotificationURL: String?
) {
    companion object {
        private fun constructThreeDSNotificationURL(responseJson: JSONObject): String {
            val links = responseJson.getJSONObject("_links")
            val selfLink = links?.getJSONObject("self")?.getString("href")
            val selfUri = URI(selfLink)
            val outletRef = responseJson.getString("outletId")
            val orderRef = responseJson.getString("reference")
            return "https://${selfUri.host}/api/outlets/${outletRef}/orders/${orderRef}" +
            "/payments/{paymentRef}/3ds2/method/notification"
        }

        private fun constructThreeDSMethodData(notificationUrl: String, threeDSServerTransID: String): String {
            val data = hashMapOf<String, String>()
            data["threeDSMethodNotificationURL"] = notificationUrl
            data["threeDSServerTransID"] = threeDSServerTransID
            val threeDSMethodData = Gson().toJson(data)
            return android.util.Base64.encodeToString(
                threeDSMethodData.toByteArray(), android.util.Base64.NO_PADDING
            )
        }

        fun buildFromOrderResponse(responseJson: JSONObject): ThreeDSecureTwoRequest {
            var directoryServerID: String? = null
            var threeDSMessageVersion: String? = null
            var threeDSMethodURL: String? = null
            var threeDSServerTransID: String? = null
            var threeDSMethodData: String? = null
            var threeDSMethodNotificationURL: String? = null
            try {
                val threeDSTwoConfig = responseJson.getJSONObject("3ds2")
                directoryServerID = threeDSTwoConfig.getString("directoryServerID")
                threeDSMessageVersion = threeDSTwoConfig.getString("messageVersion")
                threeDSMethodURL = threeDSTwoConfig.getString("threeDSMethodURL")
                threeDSServerTransID = threeDSTwoConfig.getString("threeDSServerTransID")
                threeDSMethodNotificationURL = constructThreeDSNotificationURL(responseJson)
                threeDSMethodData = constructThreeDSMethodData(threeDSMethodNotificationURL, threeDSServerTransID)
            } catch (e: Exception) { }
            return ThreeDSecureTwoRequest(
                directoryServerID,
                threeDSMessageVersion,
                threeDSMethodURL,
                threeDSServerTransID,
                threeDSMethodData,
                threeDSMethodNotificationURL
            )
        }
    }
}
