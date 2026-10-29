package payment.sdk.android.payments.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.colorResource
import payment.sdk.android.sdk.R

/**
 * Follows the system light/dark setting. Surfaces and text come from the SDK colour
 * resources (values / values-night), so text stays readable on whichever surface is shown.
 */
@Composable
fun SDKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val label = colorResource(id = R.color.payment_sdk_default_label_color)
    val colors = (if (darkTheme) darkColors() else lightColors()).copy(
        background = colorResource(id = R.color.payment_sdk_screen_background_color),
        onBackground = label,
        surface = colorResource(id = R.color.payment_sdk_surface_color),
        onSurface = label,
    )

    MaterialTheme(colors = colors) {
        CompositionLocalProvider(LocalContentColor provides label, content = content)
    }
}
