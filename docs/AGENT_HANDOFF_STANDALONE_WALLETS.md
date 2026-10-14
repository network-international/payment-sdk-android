# Agent handoff — standalone wallet launchers (Android)

Branch: `feature/standalone-wallet-launchers`  
Sister branch (iOS): `network-international/payment-sdk-ios` → `feature/standalone-wallet-launchers`

This branch lets merchants trigger **Google Pay** and **Samsung Pay** without opening the unified payment page. Enable/disable is “what you pass,” not an `SDKConfig` flag.

Read this file before changing wallet code. Integrator docs are in [README.md](../README.md) (Wallets / Google Pay / Samsung Pay).

## What is done

| Item | Status |
|---|---|
| `GooglePayLauncher` + Activity / ViewModel / Contract | Done |
| `GooglePayAvailability.isReady` | Done |
| Payment-state mapping (W1) in launcher **and** UPP `acceptGooglePay` | Done |
| `SamsungPayLauncher` wrapping `SamsungPayClient` | Done |
| `PaymentClient.launchSamsungPay` deprecated, delegates to launcher | Done |
| Demo: `PaymentType.GOOGLE_PAY`, standalone Samsung Pay, UPP omits wallet configs | Done |
| Unit tests for ViewModel, mapper, Samsung result mapping, UPP accept states | Done |
| README enable / disable / standalone / hybrid | Done |

## Enable / disable (do not add a second switch)

- **UPP on:** `setGooglePayConfig` / `setSamsungPayConfig`
- **UPP off:** omit those setters — the row does not appear
- **Standalone:** `GooglePayLauncher` / `SamsungPayLauncher` from the merchant’s own button
- **Hybrid:** standalone wallets + UPP **without** wallet configs

Device-ready is not enough. The order must list `GOOGLE_PAY` or `SAMSUNG_PAY` in `paymentMethods.wallet`.

## Key files

```
payment-sdk/src/main/java/payment/sdk/android/googlepay/
  GooglePayLauncher.kt
  GooglePayLauncherContract.kt
  GooglePayActivity.kt
  GooglePayViewModel.kt
  GooglePayAvailability.kt
  WalletPaymentStateMapper.kt          # shared UPP + standalone mapping
payment-sdk/src/main/java/payment/sdk/android/samsungpay/SamsungPayLauncher.kt
payment-sdk/src/main/java/payment/sdk/android/payments/PaymentsViewModel.kt   # acceptGooglePay
payment-sdk/src/main/java/payment/sdk/android/PaymentClient.kt                # deprecated delegate
app/src/main/java/payment/sdk/android/demo/MainActivity.kt
```

Reuse `GooglePayConfigFactory`, `GooglePayJsonConfig`, `GooglePayConfigInteractor`, `GooglePayAcceptInteractor`. Keep the factory `internal`.

## How to continue

1. Sync with iOS sister branch if you change the enable/disable model or result vocabulary.
2. Run: `./gradlew :payment-sdk:testDebugUnitTest --tests "payment.sdk.android.cardpayment.googlePay.*" --tests "payment.sdk.android.samsungpay.SamsungPayLauncherTest" --tests "payment.sdk.android.cardpayment.payments.UnifiedPaymentPageViewModelTest"`
3. Manual: sandbox Google Pay (`Environment.Test` + real `merchantGatewayId`); Samsung Pay on a ready device with a valid service ID.
4. Do **not** add `SDKConfig.enableGooglePay` or UPP `setGooglePayEnabled`. Omitting config is the disable switch.

## Suggested follow-ups (not in this branch)

- Confirm Google Pay accept JSON always includes `state`; mapper polls the order if the body has no state.
- `SamsungPayRequested` is still a UPP fallback when `samsungPayConfig` is missing — leave it or delete after merchants migrate.
- Flutter / RN wrappers were out of scope.
- Workspace copies of parity/rulebook updates live in [workspace-sync/](workspace-sync/). Those files belong in the `Current/docs` tree (not a git repo). Merge them back there if you maintain that tree.

## Out of scope (do not expand unless asked)

- Flutter / React Native wallet bridges
- Auto-triggering a wallet after UPP opens
- Unrelated wallet bugs (Samsung log level, empty Apple Pay summary items)
