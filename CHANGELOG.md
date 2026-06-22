# Changelog

All notable changes to the Network International Payment SDK for Android are documented in this file.

## [5.1.0] - 2026-06-22

Android 15 (API 35) and Android 16 large-screen compatibility.

### Added
- **Edge-to-edge support (Android 15 / API 35).** All payment screens now enable
  `enableEdgeToEdge()` and consume system-bar, display-cutout and IME insets, so content
  no longer overlaps the status bar, navigation bar or gesture areas when the host app
  targets SDK 35.
- Shared inset helpers for Compose (`Modifier.topAppBarInsets`) and View-based
  (`View.applySystemWindowInsetsAsPadding`) screens.

### Changed
- **Target / compile SDK raised from 34 to 35.** The SDK now builds against API level 35
  and is compatible with apps targeting SDK 35.
- **Orientation restrictions removed (Android 16 large-screen support).** The
  `screenOrientation` attribute has been removed from all SDK activities. Screens are now
  resizable and render correctly on foldables and tablets, where Android 16 (API 36) may
  ignore orientation and resizability restrictions.
- Build tooling updated to support API 35: Android Gradle Plugin `8.3.2` → `8.7.2`,
  Gradle wrapper `8.x` → `8.9`, and `androidx.activity:activity-compose` `1.7.2` → `1.9.3`.

### Fixed
- `AaniPayActivity.onNewIntent` now overrides the non-null `Intent` signature introduced
  in API 35.

### Upgrade notes
- No public API changes — this is a drop-in update for integrators.
- Payment screens were previously portrait-only; they can now appear in landscape on
  large-screen devices. Integrators are advised to verify the payment flows on an
  Android 15 device and on a foldable/tablet.

### Affected screens
PaymentsActivity, SavedCardPaymentActivity, VisaInstallmentsActivity, AaniPayActivity,
PartialAuthActivity, CardPaymentActivity, ThreeDSecureWebViewActivity,
ThreeDSecureTwoWebViewActivity.

[5.1.0]: https://github.com/network-international/payment-sdk-android/releases/tag/5.1.0
