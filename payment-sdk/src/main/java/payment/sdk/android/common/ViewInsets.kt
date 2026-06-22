package payment.sdk.android.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

/**
 * Pads a legacy (View-based) screen by the system-bar / display-cutout / IME insets so
 * its content is not drawn underneath the navigation bar, status bar or keyboard when the
 * host application runs edge-to-edge (the default for apps targeting Android 15 / API 35).
 *
 * Each edge can be toggled independently: container layouts that already consume the top
 * inset (e.g. a [com.google.android.material.appbar.AppBarLayout] with `fitsSystemWindows`)
 * should leave [top] disabled to avoid double padding.
 *
 * The listener is backward compatible: on platforms / configurations where no insets are
 * reported the applied padding is simply zero, so behaviour on older Android versions and
 * on hosts that have not gone edge-to-edge is unchanged.
 */
fun View.applySystemWindowInsetsAsPadding(
    left: Boolean = true,
    top: Boolean = false,
    right: Boolean = true,
    bottom: Boolean = true,
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = initialLeft + if (left) bars.left else 0,
            top = initialTop + if (top) bars.top else 0,
            right = initialRight + if (right) bars.right else 0,
            bottom = initialBottom + if (bottom) max(bars.bottom, ime.bottom) else 0,
        )
        insets
    }
    // Ensure the listener runs even if the view is already attached.
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    }
}
