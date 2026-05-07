package payment.sdk.android.payments.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val BASE_WIDTH_DP = 393f
private const val SCALE_MIN = 0.85f
private const val SCALE_MAX = 1.15f
private const val FONT_SCALE_MIN = 0.90f
private const val FONT_SCALE_MAX = 1.10f

@Composable
private fun widthScale(): Float {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    return (screenWidthDp / BASE_WIDTH_DP).coerceIn(SCALE_MIN, SCALE_MAX)
}

/** Scale a spacing/layout value proportionally to device width vs 393dp base. */
@Composable
fun scaleW(value: Dp): Dp = (value.value * widthScale()).dp

/** Scale a font size proportionally, clamped tighter for accessibility. */
@Composable
fun scaleFont(value: TextUnit): TextUnit {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val scale = (screenWidthDp / BASE_WIDTH_DP).coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)
    return (value.value * scale).sp
}
