package payment.sdk.android.core.dependency

import android.content.Context
import androidx.annotation.StringRes

class StringResourcesImpl constructor(private val context: Context) : StringResources {

    override fun getString(@StringRes resourceId: Int): String =
            context.getString(resourceId)

    override fun getString(resourceId: Int, vararg formatArgs: Any): String =
            context.getString(resourceId, *formatArgs)
}