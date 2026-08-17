# 03 — Payment & authorization

Rules for submitting card and wallet payments and interpreting authorization outcomes.

---

### RULE-PAY-001 — Treat payment state as a finite state machine

**Severity:** MUST  
**Status:** Active  
**Scope:** All integrations · Direct API and native SDK results

**Rule**  
Drive UI and fulfilment from the gateway **payment `state`**, not from HTTP 200 alone. Success for customer fulfilment = `{ AUTHORISED, PURCHASED, CAPTURED }`. Treat `FAILED`, `DECLINED`, `CANCELLED`, and `POST_AUTH_REVIEW` as non-success paths with distinct handling.

**Rationale**  
`AUTHORISED` is success for auth-only flows but still requires capture on two-stage merchants. `POST_AUTH_REVIEW` is terminal but not a decline — do not ship goods until review clears.

**Correct pattern**

```
STARTED → (submit payment) → AWAIT_3DS? → terminal state
if state ∈ {AUTHORISED, PURCHASED, CAPTURED} → success path
if POST_AUTH_REVIEW → pending / hold fulfilment
else → failure or retry UX
```

**Anti-pattern**

```
if (response.status == 200) { showOrderConfirmation(); }
```

**Reference**

- [SDK-API-CONTRACT.md §A.5](../../ngenius_flutter/SDK-API-CONTRACT.md#a5-payment-state-machine)
- Native `UnifiedPaymentPageResult` / `PaymentStatus` enums

---

### RULE-PAY-002 — Validate card input before any network call

**Severity:** MUST  
**Status:** Active  
**Scope:** Standalone SDK · card payments

**Rule**  
Run client-side validation (Luhn, PAN length per scheme, CVV length, expiry not in past) **before** `PUT payment:card`. Return `INVALID_CARD` locally without hitting the API.

**Rationale**  
Reduces declined noise, improves UX, and matches parity requirements between Flutter and React Native cores.

**Correct pattern**

```typescript
if (!isValidCard(card)) throw new NGeniusError('INVALID_CARD');
await putCard(paymentLink, normalizeExpiry(card));
```

**Anti-pattern**

```
POST every keystroke to the gateway to “check” the card.
```

**Reference**

- [SDK-API-CONTRACT.md §B.2](../../ngenius_flutter/SDK-API-CONTRACT.md#b2-card-validation-rules-must-be-identical)

---

### RULE-PAY-003 — Submit card fields in the documented shape

**Severity:** MUST  
**Status:** Active  
**Scope:** Standalone SDK · Direct API · card

**Rule**  
`PUT payment:card` with JSON: `cardholderName`, `pan` (digits only), `expiry` as `YYYY-MM`, `cvv` (digits only). Normalize user input (`MM/YY`, `MM/YYYY`) to `YYYY-MM` before submit.

**Rationale**  
Gateway rejects malformed expiry or padded PAN strings. Validation rules are shared across SDKs for parity.

**Correct pattern**

```json
{
  "cardholderName": "John Doe",
  "pan": "4111111111111111",
  "expiry": "2030-12",
  "cvv": "123"
}
```

**Anti-pattern**

```json
{ "pan": "4111 1111 1111 1111", "expiry": "12/30" }
```

**Reference**

- [SDK-API-CONTRACT.md §A.3](../../ngenius_flutter/SDK-API-CONTRACT.md#a3-card-submission)

---

### RULE-PAY-004 — Wallet token acquisition stays native; submission stays in PaymentController

**Severity:** MUST  
**Status:** Active  
**Scope:** Standalone SDK · Apple Pay / Google Pay / Samsung Pay

**Rule**  
Use the platform wallet bridge only to obtain the wallet payload (Apple `paymentData`, Google token string, Samsung credential map). Submit to the order's wallet link (`payment:apple_pay`, `payment:google_pay`, `payment:samsung_pay`) via the SDK — do not POST wallet tokens to your own backend for forwarding unless that is your explicit PCI/architecture choice.

On native SDKs the supported ways to submit a wallet payload **without** the unified payment page are:

- iOS: `NISdk.initiateApplePayWith(…)`
- Android: `GooglePayLauncher` and `SamsungPayLauncher`

Enable a wallet on the unified page by passing its config (`applePayRequest`, `setGooglePayConfig`, `setSamsungPayConfig`). Disable it by omitting that config. There is no separate SDK flag.

**Rationale**  
Keeps one state machine for card and wallets; wallet rel method and body differ (Google Pay uses `POST`, others `PUT`).

**Correct pattern**

```
token = await requestApplePayToken(config)
step = await paymentController.payWithWallet('payment:apple_pay', 'PUT', token)
advance(step) // same AWAIT_3DS / terminal handling as card
```

**Anti-pattern**

```
Send Apple paymentData to merchant API /api/wallet-pay without documenting PCI implications.
```

**Reference**

- [SDK-API-CONTRACT.md §A.6, §C](../../ngenius_flutter/SDK-API-CONTRACT.md#a6-wallet-submission)

---

### RULE-PAY-005 — Use wallet link rel keys from the live order

**Severity:** MUST  
**Status:** Active  
**Scope:** Standalone SDK · wallets

**Rule**  
Look up wallet actions by the **exact** `_links` rel string on the order (e.g. `payment:apple_pay` vs `apple-pay`). Treat rel as an opaque key; follow the `href` value.

**Rationale**  
Documentation inconsistently shows underscore vs hyphen variants. Only the order's links are authoritative.

**Correct pattern**

```dart
String? walletRel;
for (final key in links.keys) {
  if (key.contains('apple') && key.contains('pay')) walletRel = key;
}
final href = links[walletRel!]['href'];
```

**Anti-pattern**

```dart
links['payment:apple_pay'] // hard-coded without fallback
```

**Reference**

- [SDK-API-CONTRACT.md §A.2 casing caveat](../../ngenius_flutter/SDK-API-CONTRACT.md#a2-order-object--link-rels-the-sdk-acts-on)
- [SDK-API-CONTRACT.md §D.3 open items](../../ngenius_flutter/SDK-API-CONTRACT.md#d3-open-items-to-verify-against-a-live-sandbox)

---

### RULE-PAY-006 — Handle native SDK result variants explicitly

**Severity:** SHOULD  
**Status:** Active  
**Scope:** Native iOS/Android SDK

**Rule**  
Implement handlers for all result cases your SDK version exposes: success (`Success`, `Authorised`, `Captured`), `PostAuthReview`, partial authorization variants, `Failed`, `Cancelled`, standalone wallet results (`GooglePayLauncher.Result`, `SamsungPayLauncher.Result`), and the legacy `SamsungPayRequested` fallback on Android.

**Rationale**  
Defaulting unknown cases to success or generic failure hides fraud-review and partial-auth flows required by some merchants.

**Correct pattern**

```kotlin
when (result) {
  is UnifiedPaymentPageResult.Success,
  is UnifiedPaymentPageResult.Authorised,
  is UnifiedPaymentPageResult.Captured -> fulfil()
  is UnifiedPaymentPageResult.PostAuthReview -> holdOrder()
  is UnifiedPaymentPageResult.Failed -> showDecline(result.error)
  is UnifiedPaymentPageResult.Cancelled -> // no charge
  else -> logAndTreatAsUnknown(result)
}
```

**Anti-pattern**

```kotlin
when (result) {
  is UnifiedPaymentPageResult.Success -> fulfil()
  else -> showGenericError()
}
```

**Reference**

- `Android/payment-sdk-android/README.md` — Quick Start result handling
- `iOS/payment-sdk-ios/README.md` — `CardPaymentDelegate`
