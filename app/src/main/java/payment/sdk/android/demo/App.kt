package payment.sdk.android.demo

import android.app.Application
import payment.sdk.android.demo.dependency.AppComponent
import payment.sdk.android.demo.dependency.BaseComponent
import payment.sdk.android.demo.dependency.DaggerAppComponent
import payment.sdk.android.demo.dependency.configuration.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.facebook.stetho.Stetho
import payment.sdk.android.SDKConfig
import javax.inject.Inject

class App : Application() {

    @Inject
    lateinit var configuration: Configuration

    lateinit var baseComponent: BaseComponent

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        SDKConfig.shouldShowOrderAmount(true)

        Stetho.initializeWithDefaults(this)

        baseComponent = DaggerAppComponent
                .builder()
                .application(this)
                .build()
        (baseComponent as AppComponent).inject(this)
    }
}