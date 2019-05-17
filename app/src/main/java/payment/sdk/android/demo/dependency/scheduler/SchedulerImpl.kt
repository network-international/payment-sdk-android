package payment.sdk.android.demo.dependency.scheduler

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class SchedulerImpl @Inject constructor() : Scheduler {

    override fun io(): io.reactivex.Scheduler {
        return Schedulers.io()
    }

    override fun main(): io.reactivex.Scheduler {
        return AndroidSchedulers.mainThread()
    }
}
