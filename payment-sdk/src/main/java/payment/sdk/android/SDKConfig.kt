package payment.sdk.android

object SDKConfig {
    internal var showOrderAmount: Boolean = false
    internal var showCancelAlert: Boolean = false

    fun shouldShowOrderAmount(show: Boolean): SDKConfig {
        this.showOrderAmount = show
        return this
    }

    fun shouldShowCancelAlert(show: Boolean): SDKConfig {
        this.showCancelAlert = show
        return this
    }
}