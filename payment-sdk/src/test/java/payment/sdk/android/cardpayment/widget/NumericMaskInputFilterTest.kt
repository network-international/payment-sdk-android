package payment.sdk.android.cardpayment.widget

import android.text.SpannableStringBuilder
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(JUnitParamsRunner::class)
class NumericMaskInputFilterTest {

    private lateinit var mask: String

    private lateinit var placeHolder: String

    @Mock
    private lateinit var mockMaskListener: NumericMaskInputFilter.MaskListener

    @Mock
    private lateinit var mockSpannableStringBuilder: SpannableStringBuilder


    private lateinit var numericMaskInputFilter: NumericMaskInputFilter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mask = "#### #### #### ####"
        placeHolder="0000 0000 0000 0000"
        numericMaskInputFilter = NumericMaskInputFilter(mask, placeHolder, mockMaskListener)
    }

    @Test
    fun shouldMaintainRightCursorPositionOnSingleDelete() {
        whenever(mockSpannableStringBuilder.toString()).thenReturn("1383 6985 445")

        numericMaskInputFilter.filter("", 0, 0,
            mockSpannableStringBuilder, 12, 13)

        verify(mockMaskListener).onNewText("1383698544", "1383 6985 44", 12)
    }

    @Test
    fun shouldMaintainRightCursorPositionOnDeletingTheTextAtCenter() {
        whenever(mockSpannableStringBuilder.toString()).thenReturn("1383 6985 445")

        numericMaskInputFilter.filter("", 0, 0,
            mockSpannableStringBuilder, 10, 11)

        verify(mockMaskListener).onNewText("1383698545", "1383 6985 45", 9)
    }

    @Test
    fun shouldMaintainRightCursorPositionOnDeletingMultipleText() {
        whenever(mockSpannableStringBuilder.toString()).thenReturn("1383 6985 445")

        numericMaskInputFilter.filter("1", 0, 1,
            mockSpannableStringBuilder, 3, 12)

        verify(mockMaskListener).onNewText("13815", "1381 5", 4)
    }

    @Test
    fun shouldMaintainRightCursorPositionOnReplacingMultipleText() {
        whenever(mockSpannableStringBuilder.toString()).thenReturn("1383 6985 445")

        numericMaskInputFilter.filter("", 0, 0,
            mockSpannableStringBuilder, 2, 9)

        verify(mockMaskListener).onNewText("13445", "1344 5", 2)
    }

    @Test
    fun shouldMaintainCursorPositionAtFirstPositionOnDeletingAllText() {
        whenever(mockSpannableStringBuilder.toString()).thenReturn("1")

        numericMaskInputFilter.filter("", 0, 0,
            mockSpannableStringBuilder, 0, 1)

        verify(mockMaskListener).onNewText("", "", 0)
    }
}