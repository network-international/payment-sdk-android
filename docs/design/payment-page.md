# Unified Payment Page — Design Token Reference

Source of truth: `payment-page-figma-flutter.dart` (Flutter plugin export), verified against
`payment-page-figma-swiftui.swift` where the Flutter export had colour artifacts.

---

## Token Files

| File | Purpose |
|---|---|
| `payment-sdk/src/main/java/payment/sdk/android/payments/theme/PaymentPageTokens.kt` | Color, spacing, radius, size, typography tokens |
| `payment-sdk/src/main/res/values/colors.xml` | SDK-overridable colour resources (`pg_*`) |
| `payment-sdk/src/main/java/payment/sdk/android/payments/theme/ResponsiveScale.kt` | Width-proportional scaling helpers |

---

## Colour Tokens (`PgColors`)

| Token | Hex | Usage |
|---|---|---|
| `accentPrimary` | `#0069B1` | Radio selected, Slice tab selected bg, banner logo text |
| `surfacePage` | `#FFFFFF` | Page background, input field background |
| `surfaceRow` | `#F5F9FC` | Payment option row fill, Slice tab unselected bg |
| `surfaceSliceDetail` | `#FFF8EA` | Slice installment detail card background |
| `textPrimary` | `#070707` | Headings, row labels, input text |
| `textSecondary` | `#4A4A4A` | Order item names, Slice detail row labels |
| `textMuted` | `#8F8F8F` | Placeholders, sub-labels, disclaimer text |
| `textEligibility` | `#EEAA16` | Slice eligibility row text + spinner |
| `textOnTabSelected` | `#FFFFFF` | Text on selected pill tab |
| `borderRow` | `#E6F0F7` | Payment option row border, section dividers |
| `borderSection` | `#C2DBEC` | Section-level card border, Slice banner border |
| `borderInput` | `#DADADA` | Unfocused input border |
| `borderInputFocused` | `#91BFDD` | Focused input border |
| `borderTabUnselected` | `#C2DBEC` | Unselected pill tab border |
| `dividerSlice` | `#FFF2D8` | Divider inside Slice detail card |
| `badgeDarkBg` | `#5C3F00` | "Zero interest" / "Zero fees" badge background |
| `badgeDarkText` | `#FFFFFF` | Badge label colour |
| `spinnerPrimary` | `#EEAA16` | `CircularProgressIndicator` tint |
| `spinnerTrack` | `#FFD781` | `CircularProgressIndicator` track colour |

SDK-overridable colours (pay button, disabled state) live in `R.color.payment_sdk_*` and are
read via `sdkColor()` — they are not in `PgColors`.

---

## Spacing Tokens (`Spacing`)

| Token | Value | Usage |
|---|---|---|
| `pageH` | 20dp | Horizontal page margin |
| `sectionGap` | 24dp | Vertical gap between major sections |
| `rowGap` | 12dp | Gap between rows within a section |
| `rowPaddingH` | 16dp | Horizontal padding inside a row card |
| `rowPaddingV` | 12dp | Vertical padding inside a row card |
| `fieldLabelGap` | 4dp | Gap between field label text and the input box |
| `fieldRowGap` | 12dp | Horizontal gap between side-by-side fields |
| `fieldsStackGap` | 12dp | Vertical gap between stacked field groups |
| `headingToContent` | 16dp | Gap from a section heading to first row |
| `tabPaddingH` | 24dp | Horizontal padding inside a pill tab |
| `tabPaddingV` | 8dp | Vertical padding inside a pill tab |
| `appBarPadding` | 16dp | Horizontal padding for the pay button / app bar area |

---

## Radius Tokens (`Radius`)

| Token | Value | Usage |
|---|---|---|
| `pill` | 20dp | Slice installment pill tabs |
| `row` | 8dp | Payment option row cards, Slice banner, detail card |
| `input` | 8dp | `PgTextField` input box corners |
| `badge` | 16dp | Slice "Zero interest" / "Zero fees" badges |
| `button` | 8dp | Pay button shape |

---

## Size Tokens (`PgSize`)

| Token | Value | Usage |
|---|---|---|
| `radioOuter` | 18dp | Outer diameter of `PaymentRadioButton` |
| `radioInner` | 9dp | Inner fill diameter of `PaymentRadioButton` |
| `tabHeight` | 36dp | Fixed height of pill tabs (min-height guard) |
| `inputMinHeight` | 56dp | Minimum height of `PgTextField` input box |
| `buttonHeight` | 48dp | Pay button height |
| `brandLogoStripHeight` | 36dp | Accepted-cards logo strip height |
| `providerLogoHeight` | 24dp | Provider logo (Samsung Pay, AANI, CtP) |
| `optionLogoSize` | 32dp | Card brand logo in `SavedCardRow` |
| `merchantLogoWidth` | 100dp | Merchant logo max width |

---

## Typography Tokens (`PgType`)

All sizes are in `.sp` (respects system font scale). Line heights are in `.sp`.

| Token | Size / Weight | Usage |
|---|---|---|
| `headingSection` | 16sp Medium | Section headings ("Use Credit or Debit Card", "Or select…") |
| `bodyRowTitle` | 14sp Medium | Row labels, Slice banner title, installment amount |
| `bodyRowSubtitle` | 12sp Regular · 0.12sp tracking | Sub-labels, Slice detail rows, saved-card expiry |
| `labelField` | 14sp Medium | Input field label (above the box) |
| `bodyInput` | 16sp Regular · 0.16sp tracking | Text typed inside an input |
| `bodyPlaceholder` | 16sp Regular · 0.16sp tracking | Placeholder text inside an input |
| `amountSummary` | 18sp Medium | Order summary amount (large) |
| `amountRow` | 14sp Medium | Order item amount column |
| `captionSlicePeriod` | 12sp Regular · 0.12sp tracking | Slice badge labels |
| `pillTabSelected` | 12sp Bold | Selected Slice pill tab |
| `pillTabUnselected` | 12sp Medium | Unselected Slice pill tab |
| `buttonPrimary` | 16sp Medium · 0.38sp tracking | Pay button label |
| `captionDisclaimer` | 13sp Regular · 0.13sp tracking | Terms agreement text |

---

## Responsive Scaling

`ResponsiveScale.kt` exposes two helpers for layouts that must scale with device width:

```kotlin
@Composable fun scaleW(value: Dp): Dp    // clamps scale to [0.85×, 1.15×]
@Composable fun scaleFont(value: TextUnit): TextUnit  // clamps scale to [0.90×, 1.10×]
```

Base device width: **393dp** (Pixel 7a). These helpers are opt-in; most components rely on
`fillMaxWidth()` + `Arrangement.spacedBy()` which adapts automatically.

---

## Component Map

| Composable | File | Notes |
|---|---|---|
| `UnifiedPaymentPageScreen` | `PaymentsScreen.kt` | Top-level entry. RTL via `LocalLayoutDirection`. |
| `OrderSummarySection` | `PaymentsScreen.kt` | Collapsible. `PgType.amountSummary` for amount. |
| `SavedCardsSection` | `PaymentsScreen.kt` | CVV recapture uses `PgTextField`. |
| `CardPaymentSection` | `CardPaymentSection.kt` | Expand/collapse via `AnimatedVisibility`. |
| `CardNumberTextField` | `CardNumberTextField.kt` | Trailing icon = card brand when detected, else none. |
| `ExpiryDateTextField` | `ExpiryDateTextField.kt` | Uses `TextFieldValue` overload for cursor control. |
| `PgTextField` | `PgTextField.kt` | Custom bordered input; label above, focus border swap. |
| `SliceInstallmentSection` | `SliceInstallmentSection.kt` | Pill tabs + yellow detail card. |
| `SliceEligibilityRow` | `SliceInstallmentSection.kt` | Spinner + text below card number field. |
| `OtherPaymentOptionsSection` | `OtherPaymentOptionsSection.kt` | Each row is a `surface.row` card. |
| `PaymentRadioButton` | `PaymentRadioButton.kt` | Canvas-drawn; colors from `PgColors`. |
| `BottomPayBar` | `PaymentsScreen.kt` | Sticky footer. Button height `PgSize.buttonHeight`. |
| `TermsAgreementText` | `PaymentsScreen.kt` | `PgType.captionDisclaimer`, `TextAlign.Start`. |

---

## What Was Removed

- **`PaymentFooterView`** call — "Powered by Network International / Terms & Privacy" footer
  block was not in the Figma spec and has been removed from `PaymentsScreen.kt`.
  The `PaymentFooterView.kt` file is retained but no longer called.
- **`SDKOutlinedTextFieldColors` / Material `OutlinedTextField`** — replaced by `PgTextField`
  throughout the payment page.

---

## RTL Notes

RTL is applied at the `UnifiedPaymentPageScreen` level via:
```kotlin
CompositionLocalProvider(
    LocalLayoutDirection provides
        if (SDKConfig.getLanguage() == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
)
```

All padding uses `start`/`end` (not `left`/`right`). All alignments use `Alignment.Start` /
`Alignment.End` which are layout-direction-aware. No Compose component in the payment page
has an RTL override or hardcoded `left`/`right` directional value.
