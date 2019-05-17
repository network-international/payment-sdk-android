package payment.sdk.android.demo.dependency.resource

interface AssetResources {

    fun getAsset(file: String): String

    fun getAssetAsByteBuffer(file: String): ByteArray
}
