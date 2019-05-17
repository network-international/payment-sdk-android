package payment.sdk.android.demo.dependency


import payment.sdk.android.demo.dependency.preference.Preferences
import payment.sdk.android.demo.dependency.preference.PreferencesImpl
import payment.sdk.android.demo.dependency.formatter.FormatterImpl
import payment.sdk.android.demo.dependency.formatter.Formatter
import payment.sdk.android.demo.dependency.configuration.Configuration
import payment.sdk.android.demo.dependency.configuration.ConfigurationImpl
import payment.sdk.android.demo.dependency.resource.AssetResources
import payment.sdk.android.demo.dependency.resource.AssetResourcesImpl
import payment.sdk.android.demo.dependency.resource.StringResources
import payment.sdk.android.demo.dependency.resource.StringResourcesImpl
import payment.sdk.android.demo.dependency.scheduler.Scheduler
import payment.sdk.android.demo.dependency.scheduler.SchedulerImpl
import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindScheduler(impl: SchedulerImpl): Scheduler

    @Binds
    @Singleton
    abstract fun bindApplication(impl: Application): Context

    @Binds
    @Singleton
    abstract fun bindStringResource(impl: StringResourcesImpl): StringResources

    @Binds
    @Singleton
    abstract fun bindFormatter(impl: FormatterImpl): Formatter

    @Binds
    @Singleton
    abstract fun bindPreferences(impl: PreferencesImpl): Preferences

    @Binds
    @Singleton
    abstract fun bindConfiguration(impl: ConfigurationImpl): Configuration

    @Binds
    @Singleton
    abstract fun bindAssets(impl: AssetResourcesImpl): AssetResources


}
