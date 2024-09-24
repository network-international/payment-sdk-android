package payment.sdk.android.cardpayment

import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk

object TestUtils {

    /**
     * Mocks the simplest behaviour of a task so .await() can return task or throw exception
     * See more on [com.google.android.gms.tasks.Task<T>.await] and inside of that on awaitImpl
     */
    fun <T> mockTask(exception: Exception? = null, value: T): Task<T> {
        val task: Task<T> = mockk(relaxed = true)
        every { task.isComplete } returns true
        every { task.exception } returns exception
        every { task.isCanceled } returns false
        every { task.result } returns value
        return task
    }
}