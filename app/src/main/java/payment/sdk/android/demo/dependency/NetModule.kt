package payment.sdk.android.demo.dependency

import android.app.Application
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
object NetModule {

    @JvmStatic
    @Provides
    @Singleton
    fun provideHttpCache(app: Application) =
            Cache(app.cacheDir, (10 * 1024 * 1024).toLong())

    @JvmStatic
    @Provides
    @Singleton
    fun provideOkHttpClient(cache: Cache): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        return OkHttpClient.Builder().apply {
            addInterceptor(loggingInterceptor)
            addInterceptor(StethoInterceptor())
            connectTimeout(60, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            cache(cache)
        }.build()
    }
}