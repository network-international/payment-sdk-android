package payment.sdk.android.cardpayment.threedsecure

import org.json.JSONException
import org.json.JSONObject
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoRequest

data class ThreeDSecureRequest(
        val acsUrl: String,
        val acsPaReq: String,
        val acsMd: String,
        val gatewayUrl: String,
        val threeDSTwo: ThreeDSecureTwoRequest?,
        val threeDSTwoAuthenticationURL: String?,
        val threeDSTwoChallengeResponseURL: String?
) {
    companion object {
        fun buildFromOrderResponse(responseJson: JSONObject): ThreeDSecureRequest {
            // $._links.cnp:3ds
            val gatewayUrl = try {
                responseJson.getJSONObject("_links")
                    .getJSONObject("cnp:3ds").getString("href")
            } catch(e: JSONException) {
                ""
            }

            val threeDSTwoAuthenticationURL = try {
                responseJson.getJSONObject("_links")
                    .getJSONObject("cnp:3ds2-authentication").getString("href")
            } catch(e: JSONException) {
                null
            }

            val threeDSTwoChallengeResponseURL = try {
                responseJson.getJSONObject("_links")
                    .getJSONObject("cnp:3ds2-challenge-response").getString("href")
            } catch(e: JSONException) {
                null
            }

            val aclInfoJson = try {
                responseJson.getJSONObject("3ds")
            } catch (e: JSONException) {
                null
            }

            if(aclInfoJson != null) {
                val acsUrl = aclInfoJson.getString("acsUrl")
                val acsPaReq = aclInfoJson.getString("acsPaReq")
                val acsMd = aclInfoJson.getString("acsMd")
                return ThreeDSecureRequest(acsUrl, acsPaReq, acsMd, gatewayUrl,
                    null, threeDSTwoAuthenticationURL, threeDSTwoChallengeResponseURL)
            }

            val threeDSTwo  = ThreeDSecureTwoRequest.buildFromOrderResponse(responseJson)
            return ThreeDSecureRequest("", "", "", gatewayUrl,
                threeDSTwo, threeDSTwoAuthenticationURL, threeDSTwoChallengeResponseURL)
        }
    }
}
