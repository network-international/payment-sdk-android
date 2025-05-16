package payment.sdk.android.cardpayment.threedsecuretwo

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import payment.sdk.android.util.Base64
import payment.sdk.android.core.PaymentResponse
import java.net.URI

data class ThreeDSecureTwoRequest(
    val directoryServerID: String?,
    val threeDSMessageVersion: String?,
    val threeDSMethodURL: String?,
    val threeDSServerTransID: String?,
    val threeDSMethodData: String?,
    val threeDSMethodNotificationURL: String
) {
    companion object {
        private fun constructThreeDSNotificationURL(
            outletRef: String,
            orderRef: String,
            paymentRef: String,
            authenticationUrl: String
        ): String {
            return getNotificationUrl(authenticationUrl,"/api/outlets/${outletRef}/orders/${orderRef}" +
                    "/payments/${paymentRef}/3ds2/method/notification")
        }

        private fun getNotificationUrl(stringVal: String, path: String): String {
            if (stringVal.contains("-uat", true) ||
                stringVal.contains("sandbox", true)
            ) {
                return "https://api-gateway.sandbox.ngenius-payments.com$path"
            }
            if (stringVal.contains("-dev", true)) {
                return "https://api-gateway-dev.ngenius-payments.com$path"
            }
            return "https://api-gateway.ngenius-payments.com$path"
        }

        private fun constructThreeDSNotificationURL(responseJson: JSONObject): String {
            try {
                val outletRef = responseJson.getString("outletId")
                val orderRef = responseJson.getString("orderReference")
                val paymentRef = responseJson.getString("reference")
                val authUrl = responseJson.getJSONObject("_links")
                    .getJSONObject("cnp:3ds2-authentication").getString("href")
                return constructThreeDSNotificationURL(
                    outletRef,
                    orderRef,
                    paymentRef,
                    authUrl
                )
            } catch (_: Exception) {
                return ""
            }
        }

        private fun constructThreeDSMethodData(
            notificationUrl: String,
            threeDSServerTransID: String?
        ): String? {
            if(threeDSServerTransID == null) {
                return null
            }
            val data = hashMapOf<String, String>()
            data["threeDSMethodNotificationURL"] = notificationUrl
            data["threeDSServerTransID"] = threeDSServerTransID
            val threeDSMethodData = Gson().toJson(data)
            return Base64.getEncoder().encodeToString(threeDSMethodData.toByteArray())
        }

        fun buildFromOrderResponse(responseJson: JSONObject): ThreeDSecureTwoRequest {
            var directoryServerID: String? = null
            var threeDSMessageVersion: String? = null
            var threeDSMethodURL: String? = null
            var threeDSServerTransID: String? = null
            var threeDSMethodData: String? = null
            val threeDSMethodNotificationURL: String = constructThreeDSNotificationURL(responseJson)
            try {
                val threeDSTwoConfig = responseJson.getJSONObject("3ds2")
                directoryServerID = threeDSTwoConfig.getString("directoryServerID")
                threeDSMessageVersion = threeDSTwoConfig.getString("messageVersion")
                threeDSMethodURL = threeDSTwoConfig.getString("threeDSMethodURL")
                threeDSServerTransID = threeDSTwoConfig.getString("threeDSServerTransID")
                threeDSMethodData =
                    constructThreeDSMethodData(threeDSMethodNotificationURL, threeDSServerTransID)
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

        fun buildFromPaymentResponse(paymentResponse: PaymentResponse): ThreeDSecureTwoRequest {
            var directoryServerID: String? = null
            var threeDSMessageVersion: String? = null
            var threeDSMethodURL: String? = null
            var threeDSServerTransID: String? = null
            var threeDSMethodData: String? = null
            val threeDSMethodNotificationURL: String = constructThreeDSNotificationURL(
                outletRef = paymentResponse.outletId ?: "",
                orderRef = paymentResponse.orderReference ?: "",
                paymentRef = paymentResponse.reference ?: "",
                authenticationUrl = (((paymentResponse.links?.threeDSAuthenticationsUrl ?: "") as PaymentResponse.Href).href).toString()
            )
            try {
                val threeDSTwoConfig = paymentResponse.threeDSTwo
                directoryServerID = threeDSTwoConfig?.directoryServerID
                threeDSMessageVersion = threeDSTwoConfig?.messageVersion
                threeDSMethodURL = threeDSTwoConfig?.threeDSMethodURL
                threeDSServerTransID = threeDSTwoConfig?.threeDSServerTransID
                threeDSMethodData =
                    constructThreeDSMethodData(threeDSMethodNotificationURL, threeDSServerTransID)
            } catch (e: Exception) {
                Log.e("ThreeDSecureTwoRequest", "Unable to de-serialise certain fields")
            }
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
