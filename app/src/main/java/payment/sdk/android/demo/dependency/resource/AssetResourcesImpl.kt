package payment.sdk.android.demo.dependency.resource

import android.content.Context
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class AssetResourcesImpl @Inject constructor(private val context: Context) : AssetResources {

    override fun getAsset(file: String): String =
            context.assets.open(file).bufferedReader().use { it.readText() }

    override fun getAssetAsByteBuffer(file: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        context.assets.open(file).use { it.copyTo(bytes) }
        return bytes.toByteArray()
    }
}