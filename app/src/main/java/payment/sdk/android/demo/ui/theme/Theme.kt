package payment.sdk.android.demo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = NIBlueDark,
    onPrimary = Color(0xFF002D6E),
    primaryContainer = NIBlueDarkContainer,
    onPrimaryContainer = NIBlueLight,
    secondary = NISlateDark,
    onSecondary = Color(0xFF1B2D45),
    background = Color(0xFF1A1B1F),
    surface = Color(0xFF1A1B1F),
    error = NIError
)

private val LightColors = lightColorScheme(
    primary = NIBlue,
    onPrimary = Color.White,
    primaryContainer = NIBlueLight,
    onPrimaryContainer = Color(0xFF001A42),
    secondary = NISlate,
    onSecondary = Color.White,
    background = NISurface,
    surface = NISurface,
    error = NIError
)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(12.dp),
)

@Composable
fun NewMerchantAppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes,
        typography = Typography,
        content = content,
    )
}
