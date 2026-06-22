package payment.sdk.android.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
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
 * The screen body does not need this: the surrounding `Scaffold` already reports the
 * system-bar insets through its `contentPadding`, which each screen applies.
 */
@Composable
fun Modifier.topAppBarInsets(backgroundColor: Color): Modifier =
    this
        .background(backgroundColor)
        .windowInsetsPadding(
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        )
