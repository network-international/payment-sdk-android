package payment.sdk.android.demo.dependency.resource

import android.content.Context
import android.support.annotation.StringRes
import javax.inject.Inject

class StringResourcesImpl @Inject constructor(private val context: Context) : StringResources {

    override fun getString(@StringRes resourceId: Int): String =
            context.getString(resourceId)

    override fun getString(resourceId: Int, vararg formatArgs: Any): String =
            context.getString(resourceId, *formatArgs)
}