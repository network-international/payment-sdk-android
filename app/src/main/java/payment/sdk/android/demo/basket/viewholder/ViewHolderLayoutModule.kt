package payment.sdk.android.demo.basket.viewholder

import payment.sdk.android.demo.dependency.scope.ViewHolderScope
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.Module
import dagger.Provides

@Module
class ViewHolderLayoutModule(@param:LayoutRes @field:LayoutRes private val layoutId: Int) {

    @Provides
    @ViewHolderScope
    fun provideItemView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }
}