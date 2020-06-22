package payment.sdk.android.demo.basket

import payment.sdk.android.BuildConfig
import payment.sdk.android.R
import payment.sdk.android.demo.basket.data.MerchantApiService
import payment.sdk.android.demo.basket.viewholder.BasketProductViewHolderFactory
import payment.sdk.android.demo.basket.viewholder.ViewHolderLayoutModule
import payment.sdk.android.demo.dependency.BaseComponent
import payment.sdk.android.demo.dependency.scope.FragmentViewScope
import payment.sdk.android.PaymentClient
import android.app.Activity
import dagger.Binds
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import payment.sdk.android.demo.basket.viewholder.DaggerBasketProductViewHolderFactory
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named

@Module
abstract class BasketFragmentModule {

    @Binds
    @FragmentViewScope
    abstract fun bindsBasketPresenter(impl: BasketFragmentPresenter):
            BasketFragmentContract.Presenter

    @Binds
    @FragmentViewScope
    abstract fun bindsViewHolderInteractions(impl: BasketProductViewHolderInteractions):
            BasketFragmentContract.Interactions

    @Module
    companion object {
        private const val MERCHANT_RETROFIT = "MERCHANT_RETROFIT"

        @JvmStatic
        @Provides
        @FragmentViewScope
        fun provideViewHolderFactoryBuilder(baseComponent: BaseComponent): BasketProductViewHolderFactory.Builder {
            return DaggerBasketProductViewHolderFactory
                    .builder()
                    .baseComponent(baseComponent)
                    .layoutModule(ViewHolderLayoutModule(R.layout.view_basket_product_item))
        }

        @JvmStatic
        @Provides
        @FragmentViewScope
        @Named(MERCHANT_RETROFIT)
        fun provideMerchantRetrofit(okHttpClient: OkHttpClient): Retrofit =
                Retrofit.Builder()
                        .baseUrl(BuildConfig.MERCHANT_SERVER_URL)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)
                        .build()

        @JvmStatic
        @Provides
        @FragmentViewScope
        fun provideMerchantApiService(@Named(MERCHANT_RETROFIT) retrofit: Retrofit): MerchantApiService {
            return retrofit.create(MerchantApiService::class.java)
        }

        @JvmStatic
        @Provides
        @FragmentViewScope
        fun providePaymentClient(context: Activity) = PaymentClient(context, "")
    }
}
