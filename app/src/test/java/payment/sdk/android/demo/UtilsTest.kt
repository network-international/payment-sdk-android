package payment.sdk.android.demo

import junit.framework.TestCase.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun `formatCurrency formats numbers correctly`() {
        val testCases = listOf(
            123.45 to "123.45",
            12_345.67 to "12.35K",
            1_234_567.89 to "1.23M",
            1_234_567_890.12 to "1.23B",
            1_234_567_890_123.45 to "1.23T"
        )

        testCases.forEach { (number, expected) ->
            val formatted = number.formatCurrency()
            assertEquals(expected, formatted)
        }
    }

    @Test
    fun `toggle adds item to empty list`() {
        val list = mutableListOf<Int>()
        list.toggle(1)
        assertEquals(listOf(1), list)
    }

    @Test
    fun `toggle adds item to list without item`() {
        val list = mutableListOf(2, 3)
        list.toggle(1)
        assertEquals(listOf(2, 3, 1), list)
    }

    @Test
    fun `toggle removes item from list with item`() {
        val list = mutableListOf(1, 2, 3)
        list.toggle(2)
        assertEquals(listOf(1, 3), list)
    }
}