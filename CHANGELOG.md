# Changelog

All notable changes to the Network International Payment SDK for Android are documented in this file.

## [5.2.0] - 2026-06-30

Landscape / large-screen robustness and 3-D Secure reliability follow-up to 5.1.0.

### Added
- **Configurable 3-D Secure session timeout (new API).** `PaymentClient.threeDSSessionTimeoutMs`
  and `PaymentsRequest.Builder.setThreeDSSessionTimeout(timeoutMs)` set an overall wall-clock
  cap on a 3DS session. If the flow (fingerprint, authentication, ACS challenge render,
  customer challenge, challenge response) does not complete in time, the SDK returns a failure
  with reason `THREE_DS_TIMEOUT` so the merchant always gets a callback. Defaults to 5 minutes
  (well below the ~10 min server-side timeout); pass `0` to disable.
- **ACS challenge load watchdog.** If the ACS (Cardinal) challenge page does not render within
  the load window, the flow fails fast with `THREE_DS_ACS_LOAD_TIMEOUT` instead of leaving the
  customer waiting for the server-side timeout. ACS navigation errors during the load phase now
  surface `THREE_DS_ACS_LOAD_FAILED`. ACS URLs are masked (scheme + host only) in logs.
- **`Modifier.screenContentInsets()`** Compose helper that insets a screen body from the side
  navigation bar, display cutout and IME — needed because the Material 2 `Scaffold` does not
  propagate window insets through its `contentPadding`.
- **Samsung Pay troubleshooting guide** (`SAMSUNG_PAY_TROUBLESHOOTING.md`).

### Changed
- **3DS WebView activities survive rotation.** `ThreeDSecureWebViewActivity` and
  `ThreeDSecureTwoWebViewActivity` now declare `configChanges` for orientation/size, so rotating
  during the OTP challenge no longer recreates the WebView and discards a partially-entered OTP.
  Orientation remains unlocked (no `screenOrientation`), preserving foldable/tablet support.
- **3DS result delivery is now idempotent** (`hasFinished` guard) so a timeout can never override
  a result that has already been returned to the merchant.
- **Samsung Pay keep rules are shipped to consumers** via `consumerProguardFiles`, so a consuming
  app's R8/minify pass no longer obfuscates `com.samsung.android.sdk.**` (obfuscation broke
  Parcelable unmarshalling and failed with `REMOTE_EXCEPTION -103`).
- **Samsung Pay failures are more diagnosable** — the failure message now includes Samsung's
  `EXTRA_ERROR_REASON_MESSAGE` and the reason code is logged.

### Fixed
- **Loading dialogs overlapped the navigation bar in landscape.** The authorizing / submitting-
  payment / loading-order dialog (and the demo app's create-order dialog) stretched edge-to-edge
  and slid under the side system navigation bar. The card is now width-capped (max 400 dp) and
  centered.
- **3DS OTP could not be typed in landscape.** Three causes were addressed: (1) the 3DS WebViews
  disable fullscreen/extract IME mode (`IME_FLAG_NO_EXTRACT_UI | IME_FLAG_NO_FULLSCREEN`); (2) ACS
  pages that open the OTP field in a popup window had that popup WebView created with the
  application context instead of the Activity context, so its IME was not bound to the window and
  fell back to the fullscreen extract editor (text only committed on the keyboard's "Go" action);
  the popup now uses the Activity context; (3) the 3DS activities now set
  `windowSoftInputMode="adjustResize"` so the WebView shrinks when the keyboard shows and scrolls
  the OTP field into view instead of leaving it hidden behind the keyboard.
- **Screen bodies were clipped in landscape / with the keyboard open.** Saved-card, partial-auth
  and Aani Pay screens are now vertically scrollable, and payment screen bodies are inset from the
  side navigation bar and cutout, so fields and buttons stay reachable.

### Upgrade notes
- The new 3DS session timeout is opt-in via configuration; default behaviour is unchanged for
  integrators who do not set it (5-minute backstop applied).
- No breaking API changes — additive only.
- Integrators are advised to verify 3DS OTP entry in landscape and the loading dialogs on a
  foldable/tablet.

### Affected screens
PaymentsActivity, SavedCardPaymentActivity, VisaInstallmentsActivity, AaniPayActivity,
PartialAuthActivity, ThreeDSecureWebViewActivity, ThreeDSecureTwoWebViewActivity.

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

[5.2.0]: https://github.com/network-international/payment-sdk-android/releases/tag/5.2.0
[5.1.0]: https://github.com/network-international/payment-sdk-android/releases/tag/5.1.0
