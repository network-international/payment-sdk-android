package payment.sdk.android.webview

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

/**
 * Keeps a payment screen's content inside the safe area.
 *
 * From targetSdk 35 the platform draws every activity edge to edge with no opt-out. The APM screens
 * build a bare container in code rather than inflating a themed layout, so — unlike the 3DS screens,
 * which inherit `fitsSystemWindows` from `activity_3d_secure.xml` — nothing else pads them. Without
 * this a provider's hosted page renders under the status bar and camera cutout, and its own footer
 * button sits under the navigation bar, which on Tamara's plan screen is the button the payer has
 * to press to continue.
 *
 * The IME is folded into the bottom inset because an edge-to-edge window is not resized for the
 * keyboard: the providers ask for an SMS code on their own page, and the field would otherwise open
 * underneath it.
 *
 * [container] is expected to paint its own background, since it is what shows through the padding.
 *
 * Call this **after** `setContentView`: the insets controller reaches through the window's decor
 * view, which does not exist before then, and asking for it early throws.
 */
fun Activity.applyPaymentWindowInsets(container: View) {
    ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = maxOf(bars.bottom, ime.bottom)
        )
        insets
    }
    // The system bars now sit over the page's own background, which on every provider is white.
    // Guarded rather than assumed: without a decor view this throws, and a crash on the way into a
    // hosted checkout would cost the payment, while light status-bar icons only cost legibility.
    if (window.peekDecorView() != null) {
        WindowInsetsControllerCompat(window, container).isAppearanceLightStatusBars = true
    }
}
