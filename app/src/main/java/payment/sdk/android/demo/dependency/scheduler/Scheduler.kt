package payment.sdk.android.demo.dependency.scheduler

interface Scheduler {

    fun io(): io.reactivex.Scheduler

    fun main(): io.reactivex.Scheduler
}