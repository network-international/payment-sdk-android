package payment.sdk.android.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Makes a Material (M2) [androidx.compose.material.TopAppBar] edge-to-edge aware.
 *
 * From Android 15 (API 35) apps that target SDK 35 are displayed edge-to-edge by
 * default, so the system status bar / display cutout no longer reserves space.
 * M2 components are not inset-aware on their own, so without this the app bar would
 * be drawn underneath the status bar.
 *
 * This paints [backgroundColor] across the full top region (including the status bar
 * and any horizontal display cutout) and then insets the bar content below the system
 * bars, keeping the toolbar look intact while preventing overlap.
 *
 * The screen body still needs [screenContentInsets]: the M2 `Scaffold` reports only the
 * top/bottom app-bar heights through its `contentPadding`, NOT the window insets, so the
 * body would otherwise be drawn under the side navigation bar / cutout in landscape.
 */
@Composable
fun Modifier.topAppBarInsets(backgroundColor: Color): Modifier =
    this
        .background(backgroundColor)
        .windowInsetsPadding(
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        )

/**
 * Insets a screen body away from the system bars, display cutout and IME on the sides and
 * bottom. The top edge is intentionally omitted because the [topAppBarInsets] app bar
 * already covers the status bar above the body.
 *
 * Needed because the M2 [androidx.compose.material.Scaffold] does not propagate window
 * insets through its `contentPadding` (unlike the M3 Scaffold). In landscape the system
 * navigation bar moves to a side, so without this the content (card preview, input fields)
 * is drawn underneath it. The bottom edge uses [WindowInsets.safeDrawing], which resolves
 * to the larger of the navigation bar and the IME, so a single modifier handles both the
 * nav bar and the soft keyboard without double padding.
 */
@Composable
fun Modifier.screenContentInsets(): Modifier =
    this.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    )
