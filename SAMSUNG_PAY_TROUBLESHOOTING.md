# Samsung Pay (In-App) Troubleshooting — payment-sdk-android

A field log of debugging Samsung Pay in-app payments in this SDK + demo app, with
root causes, fixes, and a reusable error-code reference. Written against
**Samsung Pay SDK `samsungpay_2.22.00.jar`** and the NGenius KSA sandbox.

> TL;DR of the journey: a single user-visible symptom ("this app isn't supported by
> Samsung Pay") had **three different root causes** at different stages — wrong
> signing cert, R8 obfuscation of the SDK, and (likely) an expired Samsung debug key.
> Always read the SDK status/error **code** in logcat; the on-screen message is too
> generic to diagnose from.

---

## Key facts about this project

- **Samsung Pay SDK**: bundled as a local jar — `payment-sdk-samsungpay/libs/samsungpay_2.22.00.jar`
  (not a remote dependency). All its classes live under `com.samsung.android.sdk.**`.
- **Service ID** (hardcoded): `6b50b00a4a324030a0c671` — `app/.../MainViewModel.kt`
  (`PaymentClient(activity, "6b50b00a4a324030a0c671")`).
- **Backend**: NGenius **KSA sandbox**, realm `NIARABIA`
  (`https://api-gateway.sandbox.ksa.ngenius-payments.com`). Order response includes
  `"isSamsungPayV2": true` and a `payment:samsung_pay_v2` accept link.
- **App package**: `payment.sdk.android.demo`. The `demoDebug` flavor adds `.debug`
  (`applicationIdSuffix ".debug"`), so it becomes `payment.sdk.android.demo.debug`.
- **Signing**: `app/release.keystore` (referenced by `app/build.gradle`, **not committed**
  to this checkout). Default passwords `Test@1234`, alias `key0`, overridable via
  `SIGNING_KEYSTORE_PASSWORD` / `SIGNING_KEY_ALIAS` / `SIGNING_KEY_PASSWORD`.

### The Samsung Pay call path (native, this repo)
1. `PaymentClient.isSamsungPayAvailable()` → `SamsungPayClient.getSamsungPayStatus()` →
   expects `SamsungPay.SPAY_READY`.
2. `PaymentClient.launchSamsungPay()` → `SamsungPayClient.startSamsungPay()`:
   - authorizes payment with NGenius (gets `payment-token`),
   - builds `CustomSheetPaymentInfo` (merchantId = outletId, allowed card brands, amount),
   - calls `paymentManager.startInAppPayWithCustomSheet(...)`.
3. `SamsungPayTransactionListener`:
   - `onSuccess(...)` → POSTs the encrypted payload to the NGenius `samsung-pay/accept` link.
   - `onFailure(code, bundle)` → surfaces the SDK error.

---

## Symptom → Root cause → Fix

### 1. "This app isn't supported by Samsung Pay" (on a genuine Galaxy device)
**Cause:** App/signing did not match what Service ID `6b50b00a4a324030a0c671` is
registered with in the Samsung Pay Developers portal. Samsung binds a Service ID to an
**exact package name + signing-certificate SHA + test-device allowlist**.

Two concrete mismatches found:
- Running the **`demoDebug`** flavor → package `payment.sdk.android.demo.debug` (wrong package).
- The built APK was signed with the **generic Android debug cert** (`CN=Android Debug`),
  SHA-256 `ED:21:0D:20:CB:C2:37:DD:39:66:CD:4A:01:80:A8:61:68:4B:E8:EF:18:03:DA:31:09:92:B4:C3:14:9A:57:77`
  — not a unique registered cert.

**Fix:** Build the **`demoReleaseRelease`** variant (package `payment.sdk.android.demo`,
signed with the registered `release.keystore`). After this, `getSamsungPayStatus()`
returned `SPAY_READY` with all `[PASS]` checks.

> ⚠️ **Trap:** `demoReleaseDebug` does **not** fix this. The `debug` build type carries
> the implicit default debug signing config, and a **build type's signing config overrides
> the product flavor's** — so `demoReleaseDebug` is debug-signed again → "app isn't supported".
> Only the `release` build type is release-signed here.

#### How to read a cert's SHA
```bash
KT="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
# From the registered release keystore (compare this against the portal):
"$KT" -list -v -keystore app/release.keystore \
  -storepass "${SIGNING_KEYSTORE_PASSWORD:-Test@1234}" \
  -alias "${SIGNING_KEY_ALIAS:-key0}" | grep -iE "Owner|SHA1:|SHA256:"
# From a built APK (what Samsung actually validates):
"$KT" -printcert -jarfile path/to/app.apk | grep -iE "Owner|SHA1:|SHA256:"
```

---

### 2. "Samsung Pay authorization failed with code 103" → actually `-103` (`ERROR_INITIATION_FAIL`)
**Discovery:** There is **no positive `103`** error in the SDK — all SpaySdk/PaymentManager
error codes are **negative**. The logged value was `-103`.

`-103` here was a **symptom**. The real cause, seen only after instrumenting the listener
to log the `bundle`, was an obfuscation failure:
```
SPAYSDK:PartnerRequest  E  Unknown exception while executing request:
android.os.BadParcelableException: ClassNotFoundException when unmarshalling: Q2.b$c
    at Q2.d$a$a.e(...)  at Q2.k.u(...)  at O2.k.h(...)
SPAYSDK:PartnerRequest  E  startInAppPayCommon - error: REMOTE_EXCEPTION, -103
SamsungPayTxnListener   E  onFailure code=-103 reason=0 reasonMessage=null bundle=Bundle[{}]
```
`Q2.b$c`, `O2.k`, etc. are **R8-obfuscated** Samsung SDK classes. The minified release
build (`demoReleaseRelease`, `minifyEnabled true`) renamed/stripped
`com.samsung.android.sdk.**`, so the Parcelable returned by the Samsung Pay app could not
be unmarshalled in-process.

**Why the existing keep rules didn't help:** `payment-sdk-samsungpay/proguard-rules.pro`
already had `-keep class com.samsung.android.sdk.** { *; }`, but the module wired it via
**`proguardFiles`** on a `minifyEnabled false` library — so the rules did nothing for the
library *and were never passed to the consuming app's R8 pass.*

**Fixes applied:**
- `payment-sdk-samsungpay/build.gradle` → moved rules to
  **`consumerProguardFiles 'proguard-rules.pro'`** in `defaultConfig` so they propagate to
  every consuming app. (This is the real SDK fix — benefits all integrators.)
- `app/proguard-rules.pro` → added the keep rules directly as a local safety net:
  ```proguard
  -dontwarn com.samsung.android.sdk.samsungpay.**
  -keep class com.samsung.android.sdk.** { *; }
  -keep interface com.samsung.android.sdk.** { *; }
  ```
- `SamsungPayTransactionListener.onFailure` → now logs and surfaces
  `EXTRA_ERROR_REASON` / `EXTRA_ERROR_REASON_MESSAGE` instead of just the code.

**Lesson:** Library ProGuard/R8 rules that must protect a *consumer* app belong in
`consumerProguardFiles`, never `proguardFiles`. Any SDK whose classes cross a Binder/Parcel
boundary (Samsung Pay, Google Pay, IPC) must be `-keep`-protected against obfuscation.

---

### 3. "API key expired" (current open item — likely `-310`)
**Likely cause:** `SpaySdk.ERROR_EXPIRED_OR_INVALID_DEBUG_KEY = -310`. The service is in
**test/debug mode** (sandbox; device country `IQ`), and the **Debug API Key** registered in
the Samsung Pay Developers portal has expired. Debug keys and test-device registrations
have validity windows and must be renewed periodically.

**This is a portal-side fix, not code:**
1. Confirm the code: `adb logcat -s SamsungPayTxnListener SPAYSDK:PartnerRequest` → expect `-310`.
2. In the Samsung Pay Developers portal → the service → Test/Debug section →
   regenerate/extend the **Debug API Key** and re-confirm the **test-device allowlist**.
3. Wait for propagation, retest `demoReleaseRelease`.

> Unrelated red herring: the NGenius access token is `expires_in: 300` (5 min), refreshed
> per order — only an issue on a long mid-flow stall. The Samsung "API key expired" wording
> points at the Samsung debug key (`-310`), not the NGenius token.

---

## Build & test cheatsheet

```bash
cd payment-sdk-android

# The ONLY variant that is both release-signed (matches Samsung registration)
# AND minified (real production-like path). This is the correct test target.
./gradlew :app:installDemoReleaseRelease     # needs app/release.keystore

# Watch the meaningful Samsung Pay logs while testing:
adb logcat -s SamsungPayClient SamsungPayTxnListener SPAYSDK:PartnerRequest \
              SPAYSDK:SpayValidity SPAYSDK:PaymentManager
```

Signs of a healthy flow in logcat:
- `SPAYSDK:SpayValidity ... [PASS]` for every check, then status `SPAY_READY`.
- `NI-SDK-HTTP` shows order created (`201`) and `payment:samsung_pay_v2` link present.
- `startInAppPayWithCustomSheet` reaches the payment sheet (no `REMOTE_EXCEPTION`).

---

## Samsung Pay SDK error-code reference (v2.22.00)
All error codes are **negative**. If a UI/log shows a bare positive number, it's likely the
absolute value. Extracted from the bundled jar via `javap -p -constants`.

| Code | Constant | Meaning / typical fix |
|---|---|---|
| 0 | `ERROR_NONE` | success |
| -1 | `ERROR_SPAY_INTERNAL` | Samsung Pay internal error |
| -2 | `ERROR_INVALID_INPUT` | bad parameter |
| -3 | `ERROR_NOT_SUPPORTED` | device/region/app not supported |
| -6 | `ERROR_NOT_ALLOWED` | operation not allowed |
| -7 | `ERROR_USER_CANCELED` | user canceled |
| -10 | `ERROR_PARTNER_SDK_API_LEVEL` | partner SDK API level mismatch |
| -11 | `ERROR_PARTNER_SERVICE_TYPE` | wrong service type (must be INAPP_PAYMENT here) |
| -21 | `ERROR_NO_NETWORK` | no network |
| -99 | `ERROR_PARTNER_INFO_INVALID` | bad PartnerInfo / Service ID |
| **-103** | **`ERROR_INITIATION_FAIL`** | **could not initiate in-app pay (we hit this via R8 obfuscation → BadParcelableException)** |
| -104 | `ERROR_REGISTRATION_FAIL` | registration failed |
| -301 | `ERROR_SERVICE_ID_INVALID` | Service ID invalid |
| -303 | `ERROR_PARTNER_APP_SIGNATURE_MISMATCH` | **signing cert doesn't match registration** |
| -304 | `ERROR_PARTNER_APP_VERSION_NOT_SUPPORTED` | app version not supported |
| -305 | `ERROR_PARTNER_APP_BLOCKED` | partner app blocked |
| -306 | `ERROR_USER_NOT_REGISTERED_FOR_DEBUG` | **test device/account not on debug allowlist** |
| -307 | `ERROR_SERVICE_NOT_APPROVED_FOR_RELEASE` | service still in test, not approved for release |
| -308 | `ERROR_PARTNER_NOT_APPROVED` | partner not approved |
| **-310** | **`ERROR_EXPIRED_OR_INVALID_DEBUG_KEY`** | **debug API key expired/invalid — renew in portal (current item)** |
| -350 | `ERROR_DEVICE_NOT_SAMSUNG` | not a Samsung device |
| -351 | `ERROR_SPAY_PKG_NOT_FOUND` | Samsung Pay/Wallet app not installed |
| -356 | `ERROR_SPAY_SETUP_NOT_COMPLETED` | Wallet not set up / no card |
| -357 | `ERROR_SPAY_APP_NEED_TO_UPDATE` | update Samsung Wallet |

(There are more — shipping/billing address errors `-201..-205`, session/timeout
`-108..-116`, region `-300/-302`, integrity `-353/-360/-361`, `ERROR_SPAY_FMM_LOCK -604`,
`ERROR_SPAY_CONNECTED_WITH_EXTERNAL_DISPLAY -605`. Dump fresh with
`javap -p -constants com.samsung.android.sdk.samsungpay.v2.SpaySdk`.)

---

## Web Checkout SDK note (different code path, for reference)
A separate, unrelated Samsung issue exists in the **Web Checkout SDK**
(`https://img.mpay.samsung.com/gsmpi/sdk/samsungpay_web_sdk.js`), not the native SDK above.
Its device detection fallback —
`ua.indexOf("Android") > 0 && ua.indexOf("Mobile") > 0 || ua.indexOf("SM-") > 0` —
treats **any** Android phone (Pixel, Xiaomi) as Samsung-eligible, so the Samsung Pay button
shows on non-Samsung devices and may prompt "Install Samsung Wallet" instead of falling back
to web/QR. That JS is Samsung-hosted and cannot be patched by the integrator; the workaround
is to gate the button on a confirmed Samsung model
(`navigator.userAgentData.getHighEntropyValues(["model"])` → starts with `SM-`) and fall
back to QR otherwise. Tracked with Samsung (Praveen Kumar) as of Oct 2025; fallback behavior
still unconfirmed by Samsung.

---

## Open questions / follow-ups
- Confirm current error is `-310` via logcat, then renew the Samsung **Debug API Key** + test-device allowlist in the portal.
- Verify the **production** Network app consumes `payment-sdk-samsungpay` as a dependency (so it inherits the new `consumerProguardFiles` rules) and is built with R8/minify.
- Optionally make `demoReleaseDebug` use the release signing config so debug-type builds of the release flavor are also Samsung-Pay-testable.
- Audit other payment modules (Google Pay, etc.) for the same `proguardFiles` vs `consumerProguardFiles` mistake.
