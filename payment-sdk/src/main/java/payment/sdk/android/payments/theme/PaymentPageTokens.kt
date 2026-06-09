package payment.sdk.android.payments.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colors ────────────────────────────────────────────────────────────────────
// SDK-overridable colors (pay button, radio) live in R.color.payment_sdk_* and
// are read via sdkColor(). Everything below is a static design token.

object PgColors {
    val accentPrimary = Color(0xFF0069B1)

    val surfacePage = Color.White
    val surfaceRow = Color(0xFFF5F9FC)
    val surfaceSliceDetail = Color(0xFFFFF8EA)

    val textPrimary = Color(0xFF070707)
    val textSecondary = Color(0xFF4A4A4A)
    val textMuted = Color(0xFF8F8F8F)
    val textEligibility = Color(0xFFEEAA16)
    val textOnTabSelected = Color.White

    val borderRow = Color(0xFFE6F0F7)
    val borderSection = Color(0xFFC2DBEC)
    val borderInput = Color(0xFFDADADA)
    val borderInputFocused = Color(0xFF91BFDD)
    val borderTabUnselected = Color(0xFFC2DBEC)

    val borderInputError = Color(0xFFD0021B)
    val textError = Color(0xFFD0021B)

    val dividerSlice = Color(0xFFFFF2D8)

    val badgeDarkBg = Color(0xFF2FBF71)
    val badgeDarkText = Color.White

    val spinnerPrimary = Color(0xFFEEAA16)
    val spinnerTrack = Color(0xFFFFD781)
}

// ── Spacing ───────────────────────────────────────────────────────────────────

object Spacing {
    val pageH = 20.dp
    val sectionGap = 24.dp
    val rowGap = 12.dp
    val rowPaddingH = 16.dp
    val rowPaddingV = 12.dp
    val fieldLabelGap = 4.dp
    val fieldRowGap = 12.dp
    val fieldsStackGap = 12.dp
    val headingToContent = 16.dp
    val tabPaddingH = 24.dp
    val tabPaddingV = 8.dp
    val appBarPadding = 16.dp
}

// ── Radii ─────────────────────────────────────────────────────────────────────

object Radius {
    val pill = 20.dp
    val row = 8.dp
    val input = 8.dp
    val badge = 16.dp
    val button = 8.dp
}

// ── Component sizes ───────────────────────────────────────────────────────────

object PgSize {
    val radioOuter = 18.dp
    val radioInner = 9.dp
    val tabHeight = 36.dp
    val inputMinHeight = 56.dp
    val buttonHeight = 48.dp
    val brandLogoStripHeight = 36.dp
    val providerLogoHeight = 24.dp
    val optionLogoSize = 32.dp
    val merchantLogoWidth = 100.dp
}

// ── Typography ────────────────────────────────────────────────────────────────
// All styles use the system font; Inter is loaded at app level if present.
// Layout values use .dp; typography values use .sp (accessibility font scaling).

object PgType {
    val headingSection = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 32.sp,
    )
    val bodyRowTitle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    )
    val bodyRowSubtitle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.12.sp,
    )
    val labelField = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    )
    val bodyInput = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.16.sp,
    )
    val bodyPlaceholder = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.16.sp,
    )
    val amountSummary = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 36.sp,
    )
    val amountRow = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    )
    val captionSlicePeriod = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.12.sp,
    )
    // Selected tab is Bold; unselected is Medium.
    val pillTabSelected = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp,
    )
    val pillTabUnselected = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
    )
    val buttonPrimary = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 25.6.sp,
        letterSpacing = 0.38.sp,
    )
    val captionDisclaimer = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.13.sp,
    )
}
