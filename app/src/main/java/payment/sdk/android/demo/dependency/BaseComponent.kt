package payment.sdk.android.demo.dependency


import payment.sdk.android.demo.dependency.preference.Preferences
import payment.sdk.android.demo.dependency.repository.ProductDao
import payment.sdk.android.demo.dependency.formatter.Formatter
import payment.sdk.android.demo.dependency.configuration.Configuration
import payment.sdk.android.demo.dependency.resource.AssetResources
import payment.sdk.android.demo.dependency.resource.StringResources
import payment.sdk.android.demo.dependency.scheduler.Scheduler
import android.app.Application
import android.content.Context


import dagger.BindsInstance
import okhttp3.OkHttpClient

interface BaseComponent {

    fun applicationContext(): Context

    fun scheduler(): Scheduler

    fun stringResources(): StringResources

    fun productDao(): ProductDao

    fun okHttpClient(): OkHttpClient

    fun formatter(): Formatter

    fun preferences(): Preferences

    fun configuration(): Configuration

    fun assetResources(): AssetResources

    interface Builder<C : BaseComponent, B : Builder<C, B>> {

        @BindsInstance
        fun application(application: Application): B

        fun build(): C
    }
}
