package payment.sdk.android.cardpayment.threedsecuretwo.webview

object ThreeDSTwoHTMLFrames {
    fun html (threeDSMethodURL: String,
                   threeDSServerTransID: String,
                   threeDSMethodNotificationURL: String,
                   threeDSMethodData: String): String {
        return "<!DOCTYPE html>" +
            "<html>" +
                "<head>" +
                "<title>3DS Authentication</title>" +
                "<style>" +
                ".hidden {" +
                "  display: none;" +
                "}" +
                ".threeDSIframe {" +
                    "width: 100%;" +
                    "border: 0;" +
                    "height: calc(100vh - 85px);" +
                "}" +
                "</style>" +
                "<script type='text/javascript'>" +
                "document.addEventListener(" +
                    "'DOMContentLoaded'," +
                    "function () {" +
                        "let timeoutId = null;" +
                        "const messageReceiver = (message) => {" +
                            "const messageData = get(message, 'data', '');" +
                            "if (messageData.startsWith('3DS2_FINGERPRINTING_COMPLETE')) {" +
                                "window.clearTimeout(timeoutId);" +
                                "alert('Fingerprint completed')" +
                                "/* const threeDSCompInd = messageData.split(':')[1]; */" +
                                "/* attemptThreeDsAuthentications(threeDSCompInd); */" +
                            "}" +
                        "};" +
                        "window.addEventListener('message', messageReceiver);" +
                        "timeoutId = window.setTimeout(() => {" +
                            "window.removeEventListener('message', messageReceiver);" +
                            "alert('Timed Out')" +
                            "/* attemptThreeDsAuthentications('N'); */" +
                        "}, 10000);" +
                        "const submitBtn = document.getElementById('ni-3ds2-fingerprint-submit');" +
                        "submitBtn.click();" +
                        "/* attemptThreeDsAuthentications('U'); */" +
                    "}," +
                    "false" +
                ");" +
                "</script>" +
        "</head>" +
            "<body>" +
                "<div>Hello</div>" +
            "<form action='${threeDSMethodURL}' class='hidden' method='post'>" +
            "   <input type='hidden' name='threeDSServerTransID' value='${threeDSServerTransID}' />" +
            "   <input type='hidden' name='threeDSMethodNotificationURL' value='${threeDSMethodNotificationURL}' />" +
            "   <input type='hidden' name='threeDSMethodData' value='${threeDSMethodData}' />" +
            "   <input type='submit' id='ni-3ds2-fingerprint-submit' />" +
            "</form>" +
            "</body>" +
            "</html>"}
}