# 20 — Divergences & Unification Backlog

Every place the Android and iOS SDKs disagree, plus defects found along the way.
Consolidated from all 18 docs in this set.

**Coverage:** complete — architecture, entry points, networking, domain models, all 12
feature flows, theming/localization, both class indexes.
**Items recorded:** 137, across ~40k LOC.

---

## How items are tagged

| Tag | Meaning | Action |
|---|---|---|
| **bug** | Wrong on one or both platforms regardless of the other | Fix independently |
| **accidental** | Nobody decided this; the platforms drifted | Unification candidate |
| **intentional** | Forced by the platform or a vendor constraint | Document, don't "fix" |
| **verify** | Suspected, not confirmed | Needs a test or a run |

Priority reflects **cost of leaving it alone**, not effort.

---

## The core finding

There is **no shared-code path** — Kotlin and Swift, no KMP, and `:payment-sdk-core` is
Android-coupled. So "unify the logic" cannot mean "share the logic."

But the survey changed the picture in an important way: **the business rules are already
unified; only their representation isn't.** Aani, Benefit, Slice, QPay and the design
tokens are byte-for-byte equivalent — same regexes, same polling intervals, same terminal
state sets, same hex colours, occasionally the same variable names and comments. Someone
has already been porting logic across by hand.

What has *not* been kept in sync is structure — and every P0 below traces to the same
structural fact:

> **iOS resolves payment state in one place. Android resolves it in six.**

`PaymentViewController.handlePaymentResponse` is the single funnel for every iOS payment.
Android has `UnifiedPaymentPageViewModel.initiateCardPayment`,
`UnifiedPaymentPageViewModel.makeSavedCardPayment`, `SavedPaymentViewModel`,
`PartialAuthViewModel`, `QPayActivity` and `BenefitActivity` — six independent state
machines over the same gateway states. Four of them handle `VERIFIED`; two don't. Five
handle partial auth; one doesn't. Two map `AUTHORISED` correctly; two don't.

**Consolidating Android's payment-state resolution into one function would eliminate five
of the eight P0 bugs by construction.** That is the single highest-value change in this
backlog.

---

## P0 — Fix now

### Correctness

| # | Platform | Issue | Location |
|---|---|---|---|
| **U1** | Android | `AUTHORISED` reported to merchants as **`PartiallyAuthorised`** — a full authorization looks like a reduced one. The post-3DS path in the same file maps it correctly. | `PaymentsActivity.kt:548`, `SavedCardPaymentActivity.kt:219` |
| **U2** | Android | `VERIFIED` unhandled in the card and saved-card paths → success reported as `Failed("Unknown payment state")`. Handled correctly in QPay, Benefit and partial auth. | `PaymentsViewModel.kt:559,517` |
| **U3/S1** | Android | In-page saved-card path drops `AWAITING_PARTIAL_AUTH_APPROVAL` → a partially approved payment is reported as failed while the gateway holds an authorization. | `PaymentsViewModel.kt:517-545` |
| **W1** | Android | Google Pay always emits `Captured`, never reading the payment state — an `AUTH` order or a `POST_AUTH_REVIEW` outcome is reported as captured. | **fixed** — `WalletPaymentStateMapper` |
| **PA1** | iOS | Partial-auth result mapping drops `POST_AUTH_REVIEW` → falls into the default branch. | `PartialAuthViewController.swift:89` |
| **U4** | Android | Missing auth code returns with **no effect emitted** — UI stays on `Loading(AUTH)` forever, no result, no callback. | `PaymentsViewModel.kt:194` |
| **AA1** | Android | Aani alias polling loop exits on any unrecognised state without updating `_state` — UI hangs in `Pooling`. | `AaniPayViewModel.kt:208` |
| **E14** | iOS | `ThreeDSStatus.init?(rawVal:)` defaults **unknown values to `.ThreeDSSuccess`** — an unrecognised 3DS outcome reads as a passed challenge. | `CardPaymentDelegate.swift` |
| **T1** | Android | Sends `"192.168.0.1"` as the cardholder IP to the ACS when the payer IP is unavailable, weakening issuer risk scoring. | `BrowserData.kt:31` |
| **T5** | Android | `PaymentClient.executeThreeDS` swallows auth errors with `println(it)` — the merchant's `startActivityForResult` never fires. | `PaymentClient.kt:211` |
| **N3** | iOS | `authorizePayment` reports every failure as `completion([:])`; bad URL, network error and rejected code are indistinguishable. | `TransactionServiceAdapter.swift:47` |

### Cards and money

| # | Platform | Issue |
|---|---|---|
| **C1** | **both** | A card expiring in the **current month** is rejected. Both compare against the 1st of the expiry month; cards are valid through the last day. Every affected cardholder is refused, all month. |
| **C2** | **both** | Expiry is parsed against the device's **default calendar** (`Calendar.getInstance()` / unconfigured `DateFormatter`). On a non-Gregorian calendar — plausible in Gulf markets — dates misparse. |
| **V1** | Android | Visa Instalment **terms and conditions** are selected by **device** locale (`Locale.getDefault().isO3Language`); iOS uses the SDK language. Legally-material text in the wrong language. |
| **D1** | Android | Gson writes `null`/leaves `lateinit` uninitialized in non-null Kotlin properties (`Order.Embedded.payment`, `MatchedPlan`, `CostInfo`) — deferred crashes far from the decode site. |

### Privacy, security and localization

| # | Platform | Issue |
|---|---|---|
| **U17/E9** | **both** | **89 unconditional log statements each.** Android's `Log.d` includes the **cardholder name** (`savedCard/view/CardView.kt:140,144`). iOS's `NILogger` is opt-in and masks headers, but view controllers use raw `print` (45 in `PaymentViewController` alone), bypassing it. Neither platform has a working logging switch. |
| **TH3/A7** | iOS | `setSDKLanguage` sets `UIView.appearance().semanticContentAttribute` — mutating the **entire host app's** layout direction, never reverted. One Arabic payment leaves the merchant's app RTL. |
| **TH1** | Android | `Locale.getDefault()` used instead of the SDK language in **four** places: layout direction, VIS terms, result-screen date, amount formatting. One small self-contained fix. |
| **TH2/P7** | Android | Arabic is missing **17 keys**, including all 9 Click to Pay strings; `ClickToPayState` also hardcodes English (`"Initializing Click to Pay..."`). Arabic Click to Pay is entirely in English. |
| **Q2** | Android | `resetWebSession()` clears WebView cookies and local storage for the **whole host app**, not just the SDK (QPay and Benefit). |

---

## P1 — Contract alignment

Breaking API changes. Needs a coordinated major version and a merchant migration guide.

### P1.1 — Result vocabulary

| # | Issue | Target |
|---|---|---|
| **E2** | Android has no `InvalidRequest`; iOS cannot distinguish `Authorised` from `Success` | One shared set: `Authorised`, `Captured`, `PostAuthReview`, `PartiallyAuthorised`, `PartialAuthDeclined`, `PartialAuthDeclineFailed`, `Failed(error)`, `Cancelled`, `InvalidRequest` |
| **E7** | Android spells it `Canceled` in 4 launchers and `Cancelled` in 2 | Standardise on `Cancelled` |
| **AA7** | Android's Aani lacks `invalidRequest` and `dismissedToPaymentPage` (the latter keeps the payment page open on iOS) | Add both |
| **Q1** | QPay reports `POST_AUTH_REVIEW` as success on both platforms; no post-auth-review case exists in either result enum | Add it |
| — | iOS `PaymentStatus.rawVal` mixes PascalCase and SCREAMING_SNAKE | One convention |
| — | Both swallow unknown result values into a plausible default | Fail loudly instead |

### P1.2 — Error model

| # | Issue | Target |
|---|---|---|
| **E3** | iOS has `NIPaymentError` (6 categories + originating method); Android has a bare `String` | Port the shape to Android, keep categories identical |
| **U6** | iOS calls **both** `paymentDidComplete` overloads — merchants implementing both get two callbacks per payment | Make the error-carrying callback primary |
| **T4** | Android reports 3DS *configuration* errors as `STATUS_PAYMENT_FAILED` | Use a configuration category |
| **W3** | iOS reports a missing Apple Pay link as `.PaymentFailed` + `.AuthFailed` rather than `.InValidRequest` | **fixed** — `.InValidRequest` |

### P1.3 — Public surface

| # | Issue | Target |
|---|---|---|
| **P1/E6** | Android's `ClickToPayLauncher.Result` exposes **14 raw 3DS2 fields incl. the payment cookie**, making the merchant drive the 3DS protocol; iOS completes 3DS internally | **The single most valuable P1.** Complete 3DS inside the SDK on Android; delete both result cases |
| **E5** | Benefit and Visa Instalments are public on Android, internal-only on iOS | Decide once per feature, apply to both |
| **E10/W2** | Samsung Pay has no launcher and is the only method the SDK does not complete — `SamsungPayRequested` hands the flow back to the merchant | **fixed** — `SamsungPayLauncher` |
| **P2** | iOS `ClickToPayConfig` supports `kid`/`publicKey` encryption; Android does not | Feature gap — add to Android |
| **P3** | Android ships `testOtpMode` (skip SDK init, show OTP page) in its **public** config | Remove or make internal |

### P1.4 — Configuration and defaults

| # | Issue |
|---|---|
| **E4/TH8** | `shouldShowOrderAmount` defaults `false` (Android) / `true` (iOS). Audit every default the same way |
| **E8/TH7** | iOS exposes 24 named colour properties; Android an untyped `Map<@ColorRes Int, @ColorInt Int>` requiring knowledge of internal resource names |
| **A6** | Versions unrelated (5.0.0 / 7.0.0), hardcoded in two Android places, neither derived from the build |
| **TH12** | Design tokens (19 colours, 7 spacings) kept identical **by hand** with nothing enforcing it | Generate from one source |

---

## P2 — Structural

Apply opportunistically, when the surrounding code is being touched.

| # | Platform | Issue |
|---|---|---|
| **U10** | Android | **Payment state resolved in six places.** Root cause of U1, U2, U3, W1, S1. Consolidate into one funnel like iOS's `handlePaymentResponse` |
| **N2** | iOS | Service returns raw `Data`; every view controller decodes and classifies. Direct cause of `UI Components/` being 71% of the SDK |
| **N6** | Android | Ten structurally identical `Success`/`Error` sealed classes → one generic `ApiResult<T>` |
| **A4/U16** | iOS | No `uiState` / one-shot `effects` separation; `PaymentViewController.State` describes screens, not payment progress, with ~30 loose properties beside it |
| **A3** | iOS | UIKit with SwiftUI in 17 files and one view model — already converging; keep new features declarative |
| **A5** | iOS | Main-thread hopping is a per-call-site convention across ~40 sites; hop once in the adapter |
| **N4** | Android | 3DS and partial-auth networking live in `:payment-sdk`, not core — core isn't reusable for them |
| **N5/P10** | iOS | Click to Pay networking lives under `UI Components/` |
| **A1/A2** | iOS | One target, folders grouped by kind; a single feature spans 3–4 directories |
| **A8/W5** | Android | `:payment-sdk-samsungpay` exposed via `api`; `PaymentClient` has Samsung types in its public signature |
| **C9** | Android | A complete deprecated card implementation (45 files, 4,255 LOC) still ships |
| **U13/V10** | Android | Legacy full-screen Visa plans path kept alive by `skipVisaPlansCheck` |
| **C3** | iOS | Scheme detection needs the **complete** PAN (anchored regexes); Android detects from ~6 digits. Delays the card logo, the Amex CVV switch and eligibility checks |
| **D7** | Android | Link accessors return `String?` and callers `.orEmpty()`; iOS validates once in `toXArgs()` and can report `.invalidRequest` cleanly |
| **U11/T13** | Android | Post-3DS truth is a string in an Intent extra; iOS re-fetches the order and tolerates multiple attempts |
| **TH5** | Android | Spacing/fonts scaled by screen width (0.85–1.15×); iOS uses raw values — identical tokens, different layouts |
| **P5** | iOS | Click to Pay has 9 explicit states on Android and none on iOS |
| **P6** | iOS | Native Click to Pay email screen + GIF/logo injection; Android is WebView-only — visibly different UX |
| **U9/S7** | both | iOS caps saved cards at the **last 3** and falls back to `order.savedCard`; Android does neither |
| **D5** | Android | iOS decodes 9 order fields Android ignores (order summary, billing address, merchant details, …) |
| **A10** | Android | Gson declared 2.8.9 / 2.8.6 across modules |
| **N9** | Android | TLS pinned to exactly 1.2 — excludes 1.3. Revisit when `minSdk` rises |
| **N10** | iOS | `HTTPClient` imports UIKit and calls `UIDevice()` per request |

---

## P3 — Cosmetic

| # | Platform | Issue |
|---|---|---|
| **E16** | Android | `Payments*.kt` filenames declare `UnifiedPaymentPage*` types (5 files) |
| **E12** | Android | Three config-construction idioms across launchers (`Builder`, constructor, `data class`) |
| **E15** | Android | `ResultCallback` declared three times with different meanings |
| **E18** | iOS | Present-modally boilerplate duplicated 8× in `NISdk.swift` |
| **U18/W8** | Android | Log tag `"NI-SDK-GPay-Debug"` on all 16 `PaymentsViewModel` statements; 13 are inside `acceptGooglePay` |
| **U7** | Android | `Failed("Unknown payment state: $uiState")` interpolates the `StateFlow`, not the state |
| **U15** | Android | Three distinct success effects collapse into two results |
| **U19/PA5** | iOS | Dead `#available` guards (13.0/14.0 vs. deployment target 14.0), one with no `else` |
| **U20** | Android | `requireNotNull { … return }` used for control flow, 3× |
| **N7** | both | `Accept` header applied inconsistently — in *different* places on each platform |
| **N8** | Android | Click to Pay sends `application/json` instead of the vendor content type |
| **N12/N13** | Android | `Body.JsonStr`/`StringBody` redundant; `HttpClient` carries two live API styles |
| **N15/W7** | Android | `acceptSamsungPay` on the shared `TransactionService` |
| **N16** | iOS | Slice eligibility uses overloads, `getVisaPlans` uses optionals — internally inconsistent |
| **N17/V9** | both | Typos in type names: `AaniPayApiInterator`, `VisaEligibilityRequets` |
| **AA3** | iOS | Emirates ID placeholder shows 3 digits where the regex needs 4 |
| **AA11** | both | Mobile/passport sample values differ |
| **AA8** | both | Alias polls at 6s, QR at 5s — replicated faithfully with no reason |
| **T9** | iOS | `BrowserInfo`'s fluent API is decorative; call sites discard the return |
| **T10** | both | Payer IP fetched twice per payment via two different URL helpers |
| **V5** | iOS | Pay-in-full plan uses `UUID().uuidString` instead of a constant |
| **V8** | iOS | 184-entry ISO 639 map for 3 supported languages |
| **Q4** | iOS | Carries an Android-specific comment about POST navigations |
| **P11** | Android | Origin fix documented in an in-package markdown file (now folded into doc 12) |
| **W9** | Android | `SamsungPayClient` logs a normal not-ready status at **error** level |
| **D11** | both | `links`/`embedded` vs `orderLinks`/`embeddedData` |
| **TH10** | Android | Two `SDKTheme.kt` files |
| — | iOS | `RawValue` typealias at file scope shadows the stdlib associated type (blocks the Release build) |

---

## Latent bugs (unreachable today)

| # | Platform | Issue |
|---|---|---|
| **E13** | Android | `SavedCardPaymentRequest.Builder.build()` validation inverted — throws `UninitializedPropertyAccessException` instead of the intended message |
| **E17** | Android | `getSupportedPaymentMethods` uses `GlobalScope` |
| **N11** | Android | `Body.Form(emptyMap())` throws `StringIndexOutOfBoundsException` |
| **N14** | iOS | `Set-Cookie` read as one comma-joined header; cookie expiry values contain commas |
| **E11** | iOS | `PaymentMedium.init?(rawVal:)` has no `"SavedCard"` case |
| **C5** | Android | `Luhn` accepts non-numeric characters |
| **C11/C12** | Android | `formatExpireDateForApi` has no length guard; `isValidExpire` can throw `NumberFormatException` |
| **T8** | Android | `BrowserData` sets fixed fields twice; caller mutations are discarded |
| **T12** | Android | `threeDSMethodNotificationURL!!` force-unwrapped |
| **U5** | iOS | `onThreeDSCompletion` force-unwraps order link + access token on one line |
| **S11** | iOS | Unreachable pre-iOS-13 fallback would charge the full amount instead of instalments |
| **V2** | Android | Sends `acceptedTAndCVersion = 0` when no terms match the locale |
| **W4** | iOS | Unimplemented `ApplePayDelegate` methods answer with empty `paymentSummaryItems` |
| **P4** | both | Android handles bridge event `onClose`, iOS handles `onSwitchId`; each silently drops the other |

---

## Needs verification

| # | Question |
|---|---|
| **D3** | Minor-unit conversion (`amount / 10^n`) appears only in Android's Click to Pay path. Does iOS apply it? A 100× amount discrepancy is the failure mode |
| **D4** | Android's `PaymentLinks` has no `payment:partial-auth-decline` key — how does decline resolve its URL? |
| **S9** | Does iOS's inline saved-card CVV field switch to 4 digits for Amex? `Cvv.length` is driven by `.didChangePan`, which never fires for a saved card |
| **AA12** | Android declares ZXing for Aani QR; iOS uses no QR library. One of the two is doing unnecessary work |
| **Q3** | Android pins the QPay page viewport to stop auto-zoom; iOS does not. Does QCB's page auto-zoom under `WKWebView`? |
| **C-Amex** | Do Android's `SpacingPatterns` and iOS's PAN grouping produce the same 4-6-5 Amex format? |

---

## Intentional — document, do not change

| # | Divergence | Why |
|---|---|---|
| — | Apple Pay / Google Pay / Samsung Pay platform exclusivity | Nothing to unify |
| — | `ActivityResultContract` vs. modal presentation | Idiomatic per platform |
| — | `@Parcelize` Intent extras vs. Swift parameters | Android process-death requires parcelling |
| — | Click to Pay origin workaround shape (`loadDataWithBaseURL` vs. `document.write`) | Genuinely different platform APIs, same solution |
| **N9** | TLS 1.2 pinning | Required by `minSdk 21` — but revisit, it excludes 1.3 |

### ⚠️ Correction to an earlier entry

**A9 was wrong.** [`00-architecture.md §6`](00-architecture.md) originally recorded iOS's
dual dependency managers (CocoaPods + Carthage) as an intentional vendor constraint for the
3DS2 SDK. It is not: `ni-three-ds-two-ios-sdk` is declared in `Cartfile`/`Cartfile.resolved`
and **imported nowhere** — `ThreeDSTwoViewController` implements 3DS2 with `WKWebView`,
exactly as Android does. The Cartfile, `carthage.sh` and the concern about CocoaPods-only
consumers missing the dependency are all dead weight and can be deleted. Reclassified
**accidental / dead code** as **T2**.

---

## Suggested sequence

1. **P0 bugs, per platform, no coordination needed.** Start with the three that affect
   real money or real privacy: **U1** (authorised reported as partially authorised),
   **U17/E9** (cardholder name in release logs), **C1** (cards expiring this month
   rejected — affects both platforms).
2. **U10 — consolidate Android's payment-state resolution into one funnel.** This closes
   U1, U2, U3, W1 and S1 structurally rather than one at a time, and stops them recurring.
3. **TH1 — replace `Locale.getDefault()` with `SDKConfig.getLanguage()`** in the four
   Android sites. One small change, closes V1, U14, D2 and the direction bug.
4. **N6 and N2** — the two structural cleanups that pay for themselves regardless of
   whether unification proceeds.
5. **Answer the six verification questions** — D3 in particular, since a 100× amount error
   would be severe.
6. **P1 as one coordinated major version** on both platforms, led by **P1/E6** (Click to
   Pay 3DS) and **E2** (result vocabulary), with a shared migration guide.
7. **P2/P3** opportunistically. Generate the design tokens (**TH12**) from one source
   before they drift.

Steps 1–5 are safe, independently valuable, and need no cross-platform coordination.
Step 6 is the decision point.
