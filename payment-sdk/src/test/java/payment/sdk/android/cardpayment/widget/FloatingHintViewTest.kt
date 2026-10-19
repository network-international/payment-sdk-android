package payment.sdk.android.cardpayment.widget

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import payment.sdk.android.sdk.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FloatingHintViewTest {

    private fun layOut(direction: Int): ViewGroup {
        // Like a host app declaring android:supportsRtl="true"; without it Android ignores RTL.
        RuntimeEnvironment.getApplication().applicationInfo.flags =
            RuntimeEnvironment.getApplication().applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            androidx.appcompat.R.style.Theme_AppCompat_Light
        )
        val root = LayoutInflater.from(context)
            .inflate(R.layout.view_bottom_sheet, null) as ViewGroup
        root.layoutDirection = direction
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 1080, 2000)
        return root
    }

    private fun alignHintTo(root: ViewGroup): Pair<FloatingHintView, View> {
        val hint = root.findViewById<FloatingHintView>(R.id.floating_hint_view)
        val field = root.findViewById<View>(R.id.edit_card_number)
        hint.text = "رقم بطاقة الدفع"
        hint.animateToAlignViewStart(field)
        return hint to field
    }

    @Test
    fun `RTL aligns the label's right edge with the field's right edge`() {
        val (hint, field) = alignHintTo(layOut(View.LAYOUT_DIRECTION_RTL))
        val label = hint.getChildAt(0)
        label.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        assertEquals(View.LAYOUT_DIRECTION_RTL, field.layoutDirection)
        assertEquals(field.x + field.width, hint.x + label.measuredWidth, 0.5f)
    }

    @Test
    fun `LTR keeps the label at the field's left edge`() {
        val (hint, field) = alignHintTo(layOut(View.LAYOUT_DIRECTION_LTR))

        assertEquals(field.x, hint.x, 0.5f)
    }
}
