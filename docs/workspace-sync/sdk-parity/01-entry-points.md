# 01 — Entry Points

Everything a merchant app touches: how a payment is started, how it is configured, and how
the result comes back. This is the surface that a breaking change would break.

---

## 1. The two models

| | Android | iOS |
|---|---|---|
| Shape | One launcher class **per feature** | One **singleton** with a method per feature |
| Instance | `UnifiedPaymentPageLauncher(activity, callback)` | `NISdk.sharedInstance` |
| Result delivery | `ActivityResultContract` → sealed `Result` | `@objc` delegate protocol → `Int`-backed enum |
| Presentation | `Activity` via `Intent` | `UIViewController` presented modally over parent |
| Argument passing | `@Parcelize` `Config` / `Request` in an `Intent` extra | Swift function parameters |
| Compose/SwiftUI helper | `rememberXLauncher { }` composables | none |
| Nullability of callback | Non-null, always fires | Delegate methods mostly `@objc optional` |

The consequence: **on Android a merchant can start any flow directly; on iOS most flows are
only reachable from inside the unified payment page.** See §5.

---

## 2. Android — public entry points

### 2.1 Current API: per-feature launchers

Every launcher follows the same three-part shape.

```kotlin
// 1. Construct — registers an ActivityResultContract
private val launcher = UnifiedPaymentPageLauncher(this) { result ->
    when (result) {
        is UnifiedPaymentPageResult.Success -> …
        is UnifiedPaymentPageResult.Failed  -> …
        // …
    }
}

// 2. Build a request
val request = UnifiedPaymentPageRequest.builder()
    .gatewayAuthorizationUrl(authUrl)
    .payPageUrl(payPageUrl)
    .build()

// 3. Launch
launcher.launch(request)
```

Each also ships a Compose variant, e.g. `rememberUnifiedPaymentPageLauncher(callback)`
(`payments/PaymentsLauncher.kt:81`).

| Launcher | Input type | Result type | Source |
|---|---|---|---|
| `UnifiedPaymentPageLauncher` | `UnifiedPaymentPageRequest` (builder) | `UnifiedPaymentPageResult` | `payments/PaymentsLauncher.kt:34` |
| `SavedCardPaymentLauncher` | `SavedCardPaymentRequest` (builder) | `UnifiedPaymentPageResult` | `savedCard/SavedCardPaymentLauncher.kt` |
| `AaniPayLauncher` | `AaniPayLauncher.Config` (constructor) | `AaniPayLauncher.Result` | `aaniPay/AaniPayLauncher.kt` |
| `ClickToPayLauncher` | `ClickToPayLauncher.Config` (data class) | `ClickToPayLauncher.Result` | `clicktopay/ClickToPayLauncher.kt` |
| `QPayLauncher` | `QPayLauncher.Config` (data class) | `QPayLauncher.Result` | `qpay/QPayLauncher.kt` |
| `BenefitLauncher` | `BenefitLauncher.Config` (data class) | `BenefitLauncher.Result` | `benefit/BenefitLauncher.kt` |
| `VisaInstallmentsLauncher` | `VisaInstallmentsLauncher.Config` | `VisaInstallmentsLauncher.Result` | `visaInstalments/VisaInstallmentsLauncher.kt` |
| `GooglePayLauncher` | `GooglePayLauncher.Config` | `GooglePayLauncher.Result` | `googlepay/GooglePayLauncher.kt` |
| `SamsungPayLauncher` | `Order` + `SamsungPayConfig` | `SamsungPayLauncher.Result` | `samsungpay/SamsungPayLauncher.kt` |

⚠️ **Config construction is inconsistent across launchers.** `UnifiedPaymentPageRequest`
and `SavedCardPaymentRequest` use a `Builder` with `check()` validation; `AaniPayLauncher.Config`
uses a plain constructor; `ClickToPay`/`QPay`/`Benefit` use `data class` constructors. Three
idioms for the same job in one SDK.

⚠️ **`SavedCardPaymentRequest.Builder.build()` validation is a no-op.** At
`savedCard/SavedCardPaymentRequest.kt`:

```kotlin
check(this::_authorizationUrl.isInitialized || _authorizationUrl.isEmpty()) { … }
```

If `_authorizationUrl` is uninitialized, the left operand is `false` and evaluating the
right operand throws `UninitializedPropertyAccessException` rather than the intended
`IllegalStateException` with the friendly message. `UnifiedPaymentPageRequest.Builder`
gets this right (`check(this::_authorizationUrl.isInitialized)` alone).

⚠️ **Naming is inconsistent across the file tree.** The classes are named
`UnifiedPaymentPage*` but live in files named `Payments*.kt`
(`PaymentsLauncher.kt` declares `UnifiedPaymentPageLauncher`, `PaymentsRequest.kt`
declares `UnifiedPaymentPageRequest`, `PaymentsResult.kt` declares
`UnifiedPaymentPageResult`). A rename was applied to types but not to files.

⚠️ **`ResultCallback` is defined twice with different meanings.**
`clicktopay/ClickToPayLauncher.kt` declares a top-level `fun interface ResultCallback`
returning `ClickToPayLauncher.Result`; `qpay/QPayLauncher.kt` declares
`typealias ResultCallback = QPayResultCallback`. Both are top-level in their own packages,
so importing both into one file collides.

### 2.2 Legacy API: `PaymentClient`

`PaymentClient(activity, serviceId)` — `PaymentClient.kt:26`. Predates the launcher model.

| Method | Status | Notes |
|---|---|---|
| `launchCardPayment(request, requestCode)` | `@Deprecated` | → `UnifiedPaymentPageLauncher` |
| `launchSavedCardPayment(order, code)` | `@Deprecated` | → `SavedCardPaymentLauncher` |
| `launchSavedCardPayment(order, cvv, code)` | `@Deprecated` | → `SavedCardPaymentLauncher` |
| `launchSamsungPay(order, merchantName, response)` | `@Deprecated` | → `SamsungPayLauncher` |
| `isSamsungPayAvailable(statusListener)` | **active** | Leaks `com.samsung.…StatusListener` |
| `getSupportedPaymentMethods(listener)` | **active** | Only ever returns `CARD_PAYMENT` (+ `SAMSUNG_PAY`) |
| `executeThreeDS(paymentResponse, requestCode)` | **active** | Standalone 3DS for merchant-initiated payments |

`SamsungPayLauncher` is the public standalone entry and completes the payment.
`PaymentClient.launchSamsungPay` delegates to it. The unified page completes Samsung Pay
in-page when `samsungPayConfig` is set; `SamsungPayRequested` remains only as a fallback
when that config is missing.

⚠️ `getSupportedPaymentMethods` uses `GlobalScope.launch` (`PaymentClient.kt:36`), which
outlives the Activity and is not cancelled on destroy.

### 2.3 Configuration: `SDKConfig`

A mutable Kotlin `object` (process-global singleton), `SDKConfig.kt`:

```kotlin
SDKConfig.shouldShowOrderAmount(true)
         .shouldShowCancelAlert(true)
         .setMerchantLogo(R.drawable.logo)
         .setColor(R.color.payment_sdk_pay_button_background, 0xFF00AA88.toInt())
         .setLanguage("ar")
```

| Property | Type | Default |
|---|---|---|
| `showOrderAmount` | `Boolean` | **`false`** |
| `showCancelAlert` | `Boolean` | `false` |
| `merchantLogoResId` | `@DrawableRes Int` | `0` |
| `colorOverrides` | `Map<@ColorRes Int, @ColorInt Int>` | empty |
| `language` | `String?` (en/ar/fr) | device language, fallback `en` |
| `sdkVersion` | `String` | `"5.0.0"` (read-only) |

---

## 3. iOS — public entry points

All on `NISdk.sharedInstance` (`NISdk.swift:15`).

### 3.1 Launch methods

| Method | Delegate(s) | Notes |
|---|---|---|
| `showCardPaymentViewWith(cardPaymentDelegate:overParent:for:)` | `CardPaymentDelegate` | `@objc` convenience overload |
| `showCardPaymentViewWith(cardPaymentDelegate:applePayDelegate:overParent:for:with:clickToPayConfig:aaniBackLink:orderItems:savedCards:)` | `CardPaymentDelegate`, `ApplePayDelegate?` | **The real unified-page entry point** |
| `launchSavedCardPayment(cardPaymentDelegate:overParent:for:with cvv:)` | `CardPaymentDelegate` | |
| `launchSavedCardPayment(cardPaymentDelegate:overParent:for:)` | `CardPaymentDelegate` | No-CVV overload |
| `launchAaniPay(aaniPaymentDelegate:overParent:orderResponse:backLink:)` | `AaniPaymentDelegate` | `@available(iOS 14.0, *)` |
| `initiateApplePayWith(applePayDelegate:cardPaymentDelegate:overParent:for:with:)` | both | |
| `launchClickToPay(clickToPayDelegate:overParent:for:with config:)` | `ClickToPayDelegate` | Validates `dpaId`/`merchantId` up front |
| `launchQPay(qpayDelegate:overParent:orderResponse:)` | `QPayPaymentDelegate` | Rejects non-`QAR` currency |
| `executeThreeDSTwo(cardPaymentDelegate:overParent:for paymentResponse:)` | `CardPaymentDelegate` | Standalone 3DS2 |

Every method builds a view controller, wraps it in a `UINavigationController`, sets
`modalPresentationStyle = .overCurrentContext` and `isModalInPresentation = true`, then
presents on the main queue. That ~10-line block is **duplicated 8 times** in `NISdk.swift`
with small variations (background `.clear` vs `.white`, nav controller vs. bare VC in
`initiateApplePayWith`).

### 3.2 Configuration

Properties on the singleton rather than a separate config object:

| Property / method | Type | Default |
|---|---|---|
| `shouldShowOrderAmount` | `Bool` | **`true`** |
| `shouldShowCancelAlert` | `Bool` | `false` |
| `merchantLogo` | `UIImage?` | `nil` |
| `isLoggingEnabled` | `Bool` (proxies `NILogger.shared`) | `false` |
| `version` | `String` | `"7.0.0"` |
| `setSDKLanguage(language:)` | en/ar/fr | device language, fallback `en` |
| `setSDKColors(sdkColors:)` | `NISdkColors` (24 named colors) | built-in palette |
| `deviceSupportsApplePay()` | `Bool` | — |

⚠️ **`shouldShowOrderAmount` defaults to `true` on iOS and `false` on Android.** Same
merchant, same order, different screens out of the box.

⚠️ **Colour theming models differ fundamentally.** iOS exposes 24 semantically named
properties on `NISdkColors` (`cardPreviewColor`, `payButtonDisabledTitleColor`, …).
Android exposes an untyped `Map<@ColorRes Int, @ColorInt Int>` keyed by the SDK's own
resource IDs — a merchant must know the internal resource name to override anything, and
there is no compile-time list of what is overridable.

⚠️ **Android has no logging switch.** iOS has `NILogger` with `isEnabled = false` by
default, header masking, and body truncation. Android calls `android.util.Log.d`
unconditionally in production code (`PaymentsActivity.kt:97,100,111,113,128,383,492,495`,
`savedCard/view/CardView.kt:140,144` — the latter logs cardholder name).

---

## 4. Result vocabularies

This is the sharpest divergence in the public API.

### 4.1 Main payment result

| Concept | Android `UnifiedPaymentPageResult` | iOS `PaymentStatus` |
|---|---|---|
| Auth-only success | `Authorised` | — (collapsed into `PaymentSuccess`) |
| Capture/purchase success | `Success` | `PaymentSuccess` |
| Post-auth fraud review | `PostAuthReview` | `PaymentPostAuthReview` |
| Partial auth accepted | `PartiallyAuthorised` | `PartiallyAuthorised` |
| Partial auth declined | `PartialAuthDeclined` | `PartialAuthDeclined` |
| Partial auth decline failed | `PartialAuthDeclineFailed` | `PartialAuthDeclineFailed` |
| Failure | `Failed(error: String)` | `PaymentFailed` (+ optional `NIPaymentError`) |
| User cancelled | `Cancelled` | `PaymentCancelled` |
| Malformed request | — | `InValidRequest` |
| Hand off to Samsung Pay | `SamsungPayRequested` | — (N/A) |

⚠️ **Android cannot report an invalid request** at the unified-page level; it collapses
into `Failed(error)`. iOS cannot distinguish **authorised** from **captured**.

⚠️ iOS `PaymentStatus.rawVal` is inconsistent: most cases use PascalCase
(`"PaymentSuccess"`), but the three partial-auth cases use SCREAMING_SNAKE
(`"PARTIAL_AUTH_DECLINED"`). `init?(rawVal:)` round-trips correctly, but any consumer
pattern-matching on the string sees two conventions.

⚠️ Both platforms' `init?(rawVal:)` / result parsing **swallow unknown values into a
default** rather than returning `nil` — iOS `PaymentStatus` defaults to `.PaymentCancelled`,
`ThreeDSStatus` defaults to `.ThreeDSSuccess` (⚠️ defaulting an unknown 3DS outcome to
*success* is the wrong direction), and Android's contracts default to
`Failed("Error while processing result.")`.

### 4.2 Per-feature result types

| Feature | Android | iOS |
|---|---|---|
| Aani | `Success` / `Failed(String)` / `Canceled` | `success` / `failed` / `cancelled` / `invalidRequest` / `dismissedToPaymentPage` |
| QPay | `Success` / `Failed(String)` / `Canceled` / `InvalidRequest` | `success` / `failed` / `cancelled` / `invalidRequest` |
| Click to Pay | `Success` / `Authorised` / `Captured` / `PostAuthReview` / `Failed` / `Canceled` / `Requires3DS` / `Requires3DSTwo` | `success` / `failed` / `cancelled` / `postAuthReview` |
| Benefit | `Success` / `PostAuthReview` / `Failed` / `Canceled` / `CanceledOnProvider` / `InvalidRequest` | *(internal)* `.cancelledOnProvider` and peers on `BenefitViewController` |
| Visa Instalments | `Success(InstallmentPlan)` / `Cancelled` | *(internal only)* |

⚠️ **Spelling is not even internally consistent.** Android uses `Canceled` (one `l`) in
Aani/QPay/ClickToPay/Benefit but `Cancelled` (two) in `UnifiedPaymentPageResult` and
`VisaInstallmentsLauncher.Result`. iOS uses `cancelled` throughout.

⚠️ **Android's `ClickToPayLauncher.Result` exposes 3DS plumbing to the merchant.**
`Requires3DS` and `Requires3DSTwo` carry 14 raw gateway fields (`acsUrl`, `paymentCookie`,
`threeDSMethodNotificationURL`, …) out through the public API, making the merchant
responsible for continuing the 3DS flow. iOS keeps 3DS entirely internal. This is the
largest single public-API asymmetry in the SDKs.

### 4.3 Error detail

| | Android | iOS |
|---|---|---|
| Type | `String` inside `Failed` | `NIPaymentError` class |
| Categories | none | `network` / `configuration` / `declined` / `timeout` / `provider` / `unknown` |
| Originating method | not carried | `paymentMethod: String?` |
| Delivery | in the result | `paymentDidComplete(with:error:)` — **`@objc optional`** |

⚠️ iOS's richer error is delivered through an *optional* delegate method. A merchant
implementing only the required `paymentDidComplete(with:)` never receives it. Android has
no equivalent structure at all.

---

## 5. Feature reachability

Which flows a merchant can start **directly**:

| Feature | Android | iOS |
|---|---|---|
| Unified payment page | ✅ `UnifiedPaymentPageLauncher` | ✅ `showCardPaymentViewWith(…)` |
| Saved card | ✅ `SavedCardPaymentLauncher` | ✅ `launchSavedCardPayment(…)` |
| Aani | ✅ `AaniPayLauncher` | ✅ `launchAaniPay(…)` |
| QPay | ✅ `QPayLauncher` | ✅ `launchQPay(…)` |
| Click to Pay | ✅ `ClickToPayLauncher` | ✅ `launchClickToPay(…)` |
| Apple Pay | — | ✅ `initiateApplePayWith(…)` |
| Google Pay | ✅ `GooglePayLauncher` | — |
| Samsung Pay | ✅ `SamsungPayLauncher` (`PaymentClient.launchSamsungPay` deprecated) | — |
| **Benefit** | ✅ `BenefitLauncher` | ❌ **unified page only** |
| **Visa Instalments** | ✅ `VisaInstallmentsLauncher` | ❌ **unified page only** |
| Standalone 3DS | ✅ `PaymentClient.executeThreeDS` | ✅ `executeThreeDSTwo` (3DS2 only) |

⚠️ Benefit and Visa Instalments are public on Android, internal on iOS. On iOS they are
reached only through `PaymentViewController.initiateBenefitFromUnifiedPage()`
(`PaymentViewController.swift:438`) and inline `VisaInstallmentViewController` transitions
(`:604`, `:695`).

⚠️ `PaymentClient.executeThreeDS` handles **both** 3DS1 and 3DS2 (branching on whether
`ThreeDSecureTwoConfig` is fully populated). iOS's `executeThreeDSTwo` is 3DS2 only, with
no standalone 3DS1 entry point.

---

## 6. iOS internal dispatch: `PaymentViewController`

Worth documenting here because it has no Android counterpart. `NISdk` does not launch
features directly — it always builds a `PaymentViewController` carrying a `PaymentMedium`,
and that controller switches on the medium in `initiatePaymentForm()`
(`PaymentViewController.swift:174`):

```swift
switch paymentMedium {
case .Card:       // renders UnifiedPaymentPageViewController (:177), whose
                  // callbacks route to initiateApplePayFromUnifiedPage()   (:334)
                  //                     initiateAaniFromUnifiedPage()      (:377)
                  //                     initiateQPayFromUnifiedPage()      (:403)
                  //                     initiateBenefitFromUnifiedPage()   (:438)
                  //                     initiateClickToPayFromUnifiedPage()
case .ApplePay:   // (:277)
case .ThreeDSTwo: // (:299)
case .SavedCard:  // (:302)
}
```

`PaymentMedium` has only four cases (`ApplePay`, `Card`, `ThreeDSTwo`, `SavedCard`), so
Aani, QPay, Benefit and Click to Pay are **not** mediums — they are branches reached from
inside the `.Card` case. The enum no longer describes the flows it gates.

⚠️ `PaymentMedium.init?(rawVal:)` has no case for `"SavedCard"` — the string round-trip
silently returns `.Card`.

Android's equivalent responsibility is split across `UnifiedPaymentPageActivity` (effect
handling) and `PaymentsViewModel` (`UnifiedPaymentPageVMEffects.LaunchClickToPay`,
`InitiateThreeDS`, …), with each sub-flow having its own launcher rather than being a
branch in a switch.

---

## 7. Divergence summary

Recorded in [`20-divergences.md`](20-divergences.md).

| # | Divergence | Tag |
|---|---|---|
| E1 | Per-feature launchers vs. one singleton | accidental |
| E2 | Android has no `InvalidRequest`; iOS has no `Authorised` vs `Success` split | accidental |
| E3 | iOS has `NIPaymentError` categories; Android has a bare `String` | accidental |
| E4 | `shouldShowOrderAmount` defaults `false` (Android) vs `true` (iOS) | accidental |
| E5 | Benefit + Visa Instalments public on Android, internal on iOS | accidental |
| E6 | Android leaks 3DS plumbing through `ClickToPayLauncher.Result` | accidental |
| E7 | `Canceled` / `Cancelled` spelling inconsistent within Android | accidental |
| E8 | Named colour properties (iOS) vs. untyped resource-ID map (Android) | accidental |
| E9 | Android has no logging toggle; logs unconditionally incl. cardholder name | accidental |
| E10 | Samsung Pay has no launcher; only reachable via deprecated `PaymentClient` | **fixed** — `SamsungPayLauncher` |
| E11 | `PaymentMedium` no longer describes actual flows; missing `SavedCard` round-trip | accidental |
| E12 | Three different config-construction idioms across Android launchers | accidental |
| E13 | `SavedCardPaymentRequest.Builder` validation throws the wrong exception | bug |
| E14 | iOS `ThreeDSStatus` defaults unknown values to `.ThreeDSSuccess` | bug |
| E15 | `ResultCallback` declared twice with different meanings on Android | accidental |
| E16 | `Payments*.kt` filenames vs. `UnifiedPaymentPage*` type names | accidental |
| E17 | `getSupportedPaymentMethods` uses `GlobalScope` | bug |
| E18 | iOS presentation boilerplate duplicated 8× in `NISdk.swift` | accidental |
