# 08 — Wallets

Apple Pay, Google Pay and Samsung Pay. These are the one area where divergence is
**mostly legitimate** — each wallet exists on one platform only. What is *not* legitimate
is how differently each SDK integrates its own wallet.

---

## 1. Coverage

| Wallet | Android | iOS |
|---|---|---|
| Apple Pay | — | ✅ `ApplePayController` + `ApplePayDelegate` |
| Google Pay | ✅ `googlepay/` (4 files, 365 LOC) | — |
| Samsung Pay | ✅ `:payment-sdk-samsungpay` (4 files, 291 LOC) | — |

Platform exclusivity: **[intentional]**. Everything below concerns how each is wired in.

| Order field | Gates | Read by |
|---|---|---|
| `paymentMethods.wallet` contains `GOOGLE_PAY` | Google Pay row | Android |
| `paymentMethods.wallet` contains `SAMSUNG_PAY` | Samsung Pay row | Android |
| `embeddedData.payment[0].paymentLinks.applePayLink` | Apple Pay row | iOS |

---

## 2. Apple Pay (iOS)

| Piece | Type |
|---|---|
| Controller | `ApplePayController: PKPaymentAuthorizationViewControllerDelegate` |
| Merchant hooks | `ApplePayDelegate` — 3 `@objc optional` methods |
| Request | `PKPaymentRequest` supplied by the merchant |
| Network filtering | `order.paymentMethods?.card?.filter { $0.isApplePayNetwork }.map { $0.pkNetworkType }` |
| Accept call | `TransactionServiceAdapter.postApplePayResponse(for:with:using:payerIp:)` |
| Entry points | `NISdk.initiateApplePayWith(…)` **and** the unified page's Apple Pay row |

The merchant hooks pass through Apple's live-update callbacks:

```swift
@objc optional func didSelectPaymentMethod(paymentMethod:) -> PKPaymentRequestPaymentMethodUpdate
@objc optional func didSelectShippingMethod(shippingMethod:) -> PKPaymentRequestShippingMethodUpdate
@objc optional func didSelectShippingContact(shippingContact:) -> PKPaymentRequestShippingContactUpdate
```

⚠️ When a delegate method is not implemented, the controller answers with an **empty**
`paymentSummaryItems: []`. Apple treats an empty summary as a request to clear the sheet's
line items, so a merchant who implements `ApplePayDelegate` but omits one method may see
the total disappear from the sheet mid-flow. Answering with the *existing* items would be
safer.

⚠️ `initiateApplePayWith` is the only `NISdk` entry point that presents the view controller
**without** a `UINavigationController` wrapper — an inconsistency in the boilerplate noted
as E18 in [`01-entry-points.md`](01-entry-points.md).

⚠️ `PaymentViewController` aborts when Apple Pay is requested but the order has no
`applePayLink`, reporting `.PaymentFailed` + `.AuthFailed` — a *configuration* problem
reported as a failed payment and a failed authorization. `.InValidRequest` exists and
would be correct.

---

## 3. Google Pay (Android)

| Piece | Type |
|---|---|
| Config factory | `GooglePayConfigFactory` (Wallet `PaymentsClient` + JSON builder + config interactor) |
| JSON builder | `GooglePayJsonConfig` |
| Config fetch | `GooglePayConfigInteractor.getConfig` |
| Accept call | `GooglePayAcceptInteractor.accept(url, accessToken, token)` |
| Button | `GooglePayButton.kt` |
| Merchant config | `GooglePayConfig(merchantGatewayId, env)` on `UnifiedPaymentPageRequest` |
| Result launcher | `registerForActivityResult(GetPaymentDataResult())` in `UnifiedPaymentPageActivity` **and** standalone `GooglePayLauncher` / `GooglePayActivity` |

Flow: fetch gateway config → build a `PaymentDataRequest` JSON → `PaymentsClient` →
`GetPaymentDataResult` → `acceptGooglePay(paymentDataJson)` → map payment `state`
(`AUTHORISED` / `CAPTURED` / `PURCHASED` / `POST_AUTH_REVIEW`).

Standalone merchants use `GooglePayAvailability.isReady` + `GooglePayLauncher` with the
same authorize → sheet → accept path. Omit `setGooglePayConfig` on the unified page to
hide the in-page row.

⚠️ `acceptGooglePay` is the most heavily instrumented method in the SDK — **13 of the 16**
log statements in `PaymentsViewModel` are inside it, under the tag
`"NI-SDK-GPay-Debug"`. Debugging scaffolding left in a shipping release.

⚠️ `GooglePayConfigFactory.createGooglePayRequest` wraps its body in `try { … }` — verify
the `catch` does not swallow configuration errors into a silent `null`, which would make
the Google Pay row vanish with no diagnostic.

---

## 4. Samsung Pay (Android)

The odd one out on every axis.

| Piece | Type |
|---|---|
| Module | `:payment-sdk-samsungpay`, exposed via **`api`** (leaks to consumers) |
| Client | `SamsungPayClient` |
| Mapper | `SamsungPayCardMapper` |
| Listener | `SamsungPayTransactionListener` |
| Accept call | `TransactionService.acceptSamsungPay` — on the **shared** core interface |
| Entry | `SamsungPayLauncher` (standalone); `PaymentClient.launchSamsungPay` is a deprecated delegate |
| Availability | `PaymentClient.isSamsungPayAvailable(statusListener)` — leaks `com.samsung.…StatusListener` |
| From the unified page | Completes in-page when `samsungPayConfig` is set; `SamsungPayRequested` is only a fallback |

`SamsungPayLauncher` is the public standalone entry. It wraps `SamsungPayClient` and
reports Success / Failed / Cancelled. Omit `setSamsungPayConfig` on the unified page to
hide the in-page row.

⚠️ Two availability APIs exist with different shapes: `suspend fun isSamsungPayAvailable(): Boolean`
and `fun isSamsungPayAvailable(statusListener: StatusListener)`. The second exposes a
Samsung type through the SDK's public API.

⚠️ `SamsungPayClient.isSamsungPayAvailable` logs at **error** level for the ordinary case
of Samsung Pay simply not being ready:

```kotlin
if (status != SamsungPay.SPAY_READY) {
    Log.e("SamsungPayClient", "Samsung Pay is not available/ready. It's current status is code $status")
}
```

⚠️ `acceptSamsungPay` sits on the shared `TransactionService` interface rather than in the
Samsung module — item **N15** in [`20-divergences.md`](20-divergences.md).

⚠️ `PaymentClient.getSupportedPaymentMethods` reports only `CARD_PAYMENT` and
`SAMSUNG_PAY` — it has never been extended to the other methods the SDK now supports, and
it runs on `GlobalScope`.

Historical context on Samsung Pay's reliability is in the project memory note
`samsung-pay-findings`; this doc covers only the current code shape.

---

## 5. Structural comparison

| Aspect | Apple Pay (iOS) | Google Pay (Android) | Samsung Pay (Android) |
|---|---|---|---|
| Merchant supplies | `PKPaymentRequest` | `merchantGatewayId` + env | merchant name |
| Merchant callbacks | 3 optional delegate methods | none | `SamsungPayResponse` |
| SDK completes payment | ✅ | ✅ | ✅ (`SamsungPayLauncher` / in-page when config is set) |
| In the main module | ✅ | ✅ | ❌ separate module |
| Reachable from unified page | ✅ | ✅ | ✅ when `samsungPayConfig` is set |
| Public standalone entry | ✅ | ✅ `GooglePayLauncher` | ✅ `SamsungPayLauncher` |
| Accept endpoint owner | `TransactionService` | dedicated interactor | `TransactionService` |

**No two of the three are integrated the same way**, even the two on the same platform.
Google Pay uses a dedicated interactor; Samsung Pay hangs off the shared service interface;
Apple Pay uses the service too but with its own `payerIp` parameter.

---

## 6. Divergence summary

| # | Divergence | Tag |
|---|---|---|
| **W1** | Google Pay always reports `Captured`, ignoring the payment state | **fixed** — `WalletPaymentStateMapper` maps accept/order state in UPP and `GooglePayLauncher` |
| **W2** | Samsung Pay is the only method the SDK does not complete — merchant must integrate twice | **fixed** — `SamsungPayLauncher` completes the payment; UPP completes it when `samsungPayConfig` is set |
| **W3** | Apple Pay config errors reported as `.PaymentFailed` + `.AuthFailed` instead of `.InValidRequest` | **fixed** — missing `applePayLink` on the standalone path reports `.InValidRequest` |
| **W4** | Unimplemented `ApplePayDelegate` methods answer with empty `paymentSummaryItems` | bug |
| **W5** | `:payment-sdk-samsungpay` exposed via `api`; Samsung types in `PaymentClient`'s signature | accidental — see A8 |
| **W6** | Two `isSamsungPayAvailable` overloads, one leaking a Samsung type | accidental |
| **W7** | `acceptSamsungPay` on the shared `TransactionService` | accidental — see N15 |
| **W8** | 13 `"NI-SDK-GPay-Debug"` log statements inside `acceptGooglePay` | cosmetic — see U18 |
| **W9** | `SamsungPayClient` logs a normal not-ready status at error level | cosmetic |
| **W10** | `getSupportedPaymentMethods` never extended past card + Samsung Pay | accidental |
| **W11** | Three wallets, three different integration shapes | accidental |
| — | Platform exclusivity of each wallet | ✅ **intentional** |
