package payment.sdk.android.demo.home


class HomeActivityContract {

    interface View {
        fun basketSizeChanged(size: Int)
    }

    interface Presenter {
        fun init()

        fun cleanup()
    }
}
