package payment.sdk.android

object SDKConfig {
    var showOrderAmount: Boolean = false

    fun shouldShowOrderAmount(show: Boolean): SDKConfig {
        this.showOrderAmount = show
        return this
    }

}