package payment.sdk.android.payments.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColors(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
)

private val LightColors = lightColors(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
)

@Composable
fun SDKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!darkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}