package payment.sdk.android

object SDKConfig {
    internal var showOrderAmount: Boolean = false
    internal var showCancelAlert: Boolean = false
    private var sdkVersion: String = "3.1.7"

    fun shouldShowOrderAmount(show: Boolean): SDKConfig {
        this.showOrderAmount = show
        return this
    }

    fun shouldShowCancelAlert(show: Boolean): SDKConfig {
        this.showCancelAlert = show
        return this
    }

    fun getSDKVersion() =  this.sdkVersion
}