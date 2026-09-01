# 16 — Class Index: Android

Every type in the three SDK modules, grouped by package. The demo app (`:app`) and tests
are excluded.

**Totals:** 236 declared types across 189 source files, plus ~90 `@Composable` functions.

Legend: `C` class · `D` data class · `S` sealed class · `O` object · `I` interface ·
`E` enum · `F` fun interface · `T` typealias · `A` abstract class

---

## `:payment-sdk-core` — 50 files

### `core/` — gateway models

| Type | Kind | File | Role |
|---|---|---|---|
| `Order` | C | `Order.kt` | The order response; nests `Amount`, `Links`, `Embedded`, `Payment`, `PaymentLinks`. 20 `Order.getXUrl()` extension functions live here |
| `PaymentResponse` | C | `PaymentResponse.kt` | A payment attempt; carries `state`, 3DS1/3DS2 config, links |
| `OrderAmount` | C | `OrderAmount.kt` | Amount + currency with `formattedCurrencyString2Decimal(isLTR)` |
| `SavedCard` | D | `SavedCard.kt` | Tokenized card; all fields non-null |
| `CardType` | E | `CardType.kt` | Visa, Mada, MasterCard, AmericanExpress, Discover, JCB, DinersClubInternational |
| `CardMapping` | C | `CardMapping.kt` | Gateway scheme strings → `CardType` |
| `TransactionService` | I | `TransactionService.kt` | 3-method legacy interface (auth, authorizePayment, acceptSamsungPay) |
| `TransactionServiceHttpAdapter` | C | `TransactionServiceHttpAdapter.kt` | Callback-style implementation; header name constants |
| `Utils` | O | `Utils.kt` | `getQueryParameter` |
| `AaniPayRequest`, `MobileNumber` | D | `AaniPayRequest.kt` | Aani alias payment request |
| `AaniPayResponse`, `Links`, `Aani` | D | `AaniPayResponse.kt` | Aani response + deep link |
| `BenefitInitResponse` | D | `BenefitInitResponse.kt` | Hosted Benefit page URL |
| `QPayInitResponse` | D | `QPayInitResponse.kt` | QPay redirect URI |
| `StringyDeserializer` | C | `QPayInitResponse.kt` | Gson adapter coercing non-string JSON to `String` |
| `GooglePayConfigResponse`, `MerchantInfo` | D | `GooglePayConfigResponse.kt` | Gateway Google Pay config |
| `SliceAmount`, `SliceOffer`, `SliceEligibilityResponse` | D | `SliceOffer.kt` | Slice eligibility |
| `SliceRequest` | D | `SliceRequest.kt` | Selected Slice offer sent with payment |
| `VisaPlans`, `MatchedPlan`, `CostInfo`, `TermsAndCondition`, `LastInstallment` | D | `VisaPlans.kt` | VIS plans |
| `ThreeDSAuthResponse`, `PaymentMethod`, `AuthResponse` | D | `ThreeDSAuthResponse.kt` | 3DS2 authentications response |

### `core/api/` — HTTP

| Type | Kind | Role |
|---|---|---|
| `HttpClient` | I | Dual API: callback `get/post/put` + `suspend get/post/put/delete` |
| `SDKHttpResponse` | S | `Success(headers, body)` / `Failed(error)` |
| `CoroutinesGatewayHttpClient` | C | `HttpURLConnection` implementation; builds the `User-Agent` |
| `Body` | C | Encoding hierarchy: `Json`, `Form`, `JsonStr`, `StringBody`, `Empty` |
| `TLSSocketFactoryDelegate` | C | Forces `TLSv1.2` for `minSdk 21` |

### `core/dependency/`

| Type | Kind | Role |
|---|---|---|
| `StringResources` | I | String lookup abstraction |
| `StringResourcesImpl` | C | Android resource-backed implementation |

### `core/interactor/` — one class per gateway call

| Type | Kind | Role |
|---|---|---|
| `AuthApiInteractor` | C | Auth code → cookies + order URL |
| `AuthResponse` | S | `Success(cookies, orderUrl)` with `getAccessToken()`/`getPaymentCookie()` / `Error` |
| `AuthResponseBody`, `Links` | D | Auth response JSON |
| `GetOrderApiInteractor` | C | Fetch order |
| `GetPayerIpInteractor` | C | Derives the IP URL from the pay-page host |
| `CardPaymentInteractor` | C | `PUT` card payment |
| `MakeCardPaymentRequest` | D | Card payment payload (incl. `sliceRequest`, `visaRequest`) |
| `CardPaymentResponse` | S | `Success(PaymentResponse)` / `Error` |
| `SavedCardPaymentApiInteractor` | C | `PUT` saved-card payment |
| `SavedCardPaymentApiRequest` | D | Saved-card payload |
| `SavedCardResponse` | S | `Success` / `Error` |
| `AaniPayApiInterator` *(sic)* | C | Aani alias payment |
| `AaniPayApiResponse` | S | `Success` / `Error` |
| `AaniPoolingApiInteractor` | C | Alias status polling (6s) |
| `AaniQrApiInteractor` | C | QR create / poll (5s) / cancel (`DELETE`) |
| `AaniQrCreateResponse` | S | `Success` / `Error` |
| `QPayApiInteractor` | C | QPay init |
| `QPayApiResponse` | S | `Success` / `Error` |
| `BenefitApiInteractor` | C | Benefit init |
| `BenefitApiResponse` | S | `Success` / `Error` |
| `GooglePayConfigInteractor` | C | Gateway Google Pay config |
| `GooglePayAcceptInteractor` | C | Post the Google Pay token |
| `SliceEligibilityInteractor` | C | Slice eligibility |
| `SliceEligibilityResult` | S | `Success` / `Error` |
| `VisaInstallmentPlanInteractor` | C | `POST $selfUrl/vis/eligibility-check` |
| `VisaPlansResponse` | S | `Success` / `Error` |
| `VisaRequest` | D | `planSelectionIndicator`, `acceptedTAndCVersion`, `vPlanId` |
| `ClickToPayApiInteractor` | C | Submit CtP payment; fetch order |
| `ClickToPayPaymentResult` | S | Incl. `Requires3DS` / `Requires3DSTwo` |
| `ClickToPayConfig` | D | DPA credentials, brands, sandbox, `testOtpMode`, `merchantId` |
| `ClickToPayConfigInteractor` | C | Paypage `/vctp/config` |
| `ClickToPayGatewayConfig`, `MerchantConfig`, `AcquirerBin` | D | Config payloads |
| `ClickToPayMerchantConfigInteractor` | C | `GET /config/merchants/{id}/configs/vctp` |
| `ConsumerIdentity`, `ClickToPayCard`, `DigitalCardData`, `ClickToPayCheckoutResponse`, `ValidationChannel` | D | CtP SDK payloads |
| `ClickToPayActionCode` | E | CtP action codes |
| `ClickToPaySdkResponse` | S | `CardsAvailable` / `IdentityValidationRequired` / `CheckoutSuccess` / `Error` |
| `DeviceIdProvider` | O | Device fingerprint for `X-Payer-Fingerprint` |

⚠️ Ten interactors declare a structurally identical `Success`/`Error` sealed class — item
**N6**.

---

## `:payment-sdk` — 135 files

### Root

| Type | Kind | Role |
|---|---|---|
| `PaymentClient` | C | Legacy entry: Samsung Pay, standalone 3DS, deprecated card/saved-card launchers |
| `SDKConfig` | O | Process-global config: language, colours, merchant logo, toggles |

### `payments/` — unified payment page

| Type | Kind | Role |
|---|---|---|
| `UnifiedPaymentPageLauncher` | C | Public entry (in `PaymentsLauncher.kt`) |
| `UnifiedPaymentPageResultCallback` | F | Result callback |
| `UnifiedPaymentPageLauncherContract` | C | `ActivityResultContract` |
| `UnifiedPaymentPageRequest` | C | Builder-based request (in `PaymentsRequest.kt`) |
| `UnifiedPaymentPageResult` | S | 9 outcomes incl. `SamsungPayRequested` |
| `UnifiedPaymentPageActivity` | C | Coordinator; effect handling, 3DS results, result screen |
| `UnifiedPaymentPageViewModel` | C | 789 LOC state machine |
| `UnifiedPaymentPageVMUiState` | S | `Init`, `Loading`, `Authorized`, `ShowVisaPlans`, `InitiatePartialAuth`, `ShowPaymentResult` |
| `UnifiedPaymentPageVMEffects` | S | 3DS, Click to Pay, terminal outcomes |
| `SliceCheckState` | S | `Idle`, `Checking`, `Available`, `BannerOnly`, `Unavailable` |
| `VisCheckState` | S | `Idle`, `Checking`, `Available`, `Unavailable` |
| `GooglePayUiConfig` | D | Resolved Google Pay request + accept URL |
| `SamsungPayConfig` | D | Merchant Samsung Pay config |
| `OrderItem` | D | Line item for the order summary |
| `PaymentResultArgs` | D | Result-screen payload |
| `CreditCardVisualTransformation` | C | PAN grouping |
| `PgColors`, `Spacing`, `Radius`, `PgSize`, `PgType` | O | Design tokens |

**Composables:** `UnifiedPaymentPageScreen`, `PaymentSectionsContent`, `BottomPayBar`,
`OrderSummarySection`, `SavedCardRow`, `InlineCvvField`, `TermsAgreementText`,
`CardPaymentSection`, `CardNumberTextField`, `ExpiryDateTextField`, `PgTextField`,
`ClickToPaySection`, `OtherPaymentOptionsSection`, `PaymentOptionRow`, `QPayExpressButton`,
`WalletButtonsSection`, `SliceInstallmentSection` (+ `SliceBanner`, `SliceBadge`,
`SliceTab`, `SliceDetailCard`, `SliceDetailRow`, `SliceEligibilityRow`),
`VisaInstallmentSection`, `PaymentResultScreen`, `MerchantHeader`, `DetailLine`,
`PaymentFooterView`, `PaymentRadioButton`, `PaymentSectionSeparator`, `AedAmountText`,
`SDKTheme`, `scaleW`, `scaleFont`.
`PaymentOption` (E) enumerates the rows.

### `cardpayment/` — legacy card flow + shared card logic

| Type | Kind | Role |
|---|---|---|
| `CardPaymentActivity`, `CardPaymentPresenter`, `CardPaymentContract`, `CardPaymentView` | C/I | **Deprecated** MVP card flow |
| `CardPaymentRequest`, `CardPaymentData` | C | Legacy request/result |
| `CardPaymentApiInteractor`, `PaymentApiInteractor` | C/I | Legacy API layer |
| `CardDetector` | C | BIN-range scheme detection |
| `CardModel`, `BinRange`, `BinLength`, `Cvv` | D | BIN tables |
| `CardFace`, `MatchCertainty` | E | |
| `PaymentCard` | D | Detected card |
| `CardValidator` | O | Composite field validation |
| `SpacingPatterns` | C | Per-scheme PAN grouping |
| `Luhn` | O | Checksum (does not reject non-digits) |
| `InputValidationError` | E | Field error codes |
| `EmiratesIdVisualTransformation`, `UppercaseVisualTransformation` | C | Compose input formatters |
| `DateFormatter` | C | `MMYY` → `YYYY-MM` |
| `LoadingMessage` | E | Spinner captions (in `CircularProgressDialog.kt`) |
| `ExpireDateEditText` | C | Legacy expiry field + `isValidExpire` |
| `NumericMaskedEditText`, `NumericMaskInputFilter`, `FloatingHintView`, `HorizontalViewFlipper`, `PreviewTextView`, `CharDrawableSpan`, `CharResources`, `CardHolderInputDelegate`, `TextWatcherAdapter` | C/A | Legacy View widgets |

**3DS:**

| Type | Kind | Role |
|---|---|---|
| `ThreeDSecureWebViewActivity`, `ThreeDSecureWebView`, `ThreeDSecureWebViewClient`, `ThreeDSecureWebChromeClient` | C | 3DS1 |
| `ThreeDSecureRequest` | D | 3DS1 request |
| `ThreeDSecureTwoWebViewActivity`, `ThreeDSecureTwoWebView`, `ThreeDSecureTwoWebViewClient`, `ThreeDSecureTwoWebChromeClient` | C | 3DS2 |
| `ThreeDSecureFactory` | C | Builds DTOs with 9 named `requireNotNull` validations |
| `ThreeDSecureDto`, `ThreeDSecureTwoDto` | D | 3DS payloads |
| `ThreeDSecureTwoConfig`, `ThreeDSecureTwoRequest` | D | 3DS2 config |
| `BrowserData` | D | 3DS2 browser fingerprint |
| `PartialAuthIntent` | D | Partial-auth handoff |

### Feature packages

| Package | Types |
|---|---|
| `savedCard/` | `SavedCardPaymentLauncher`, `SavedCardPaymentLauncherContract`, `SavedCardPaymentRequest`, `SavedCardPaymentActivity`, `SavedPaymentViewModel`, `SavedCardPaymentState` (S), `SavedCardPaymentsVMEffects` (S) · composables `SavedCardPaymentView`, `CreditCardView`, `CreditCardBack`, `SavedCardViewBottomBar` |
| `aaniPay/` | `AaniPayLauncher`, `ResultCallback` (F), `AaniPayLauncherContract`, `AaniPayActivity`, `AaniPayViewModel`, `AaniIDType` (E), `AaniPayVMState` (S), `QrStatusType` (E) · 9 screen composables |
| `clicktopay/` | `ClickToPayLauncher`, `ResultCallback` (F), `ClickToPayLauncherContract`, `ClickToPayActivity`, `ClickToPayViewModel`, `ClickToPayJsBridge`, `ClickToPayJsCallback` (I), `ClickToPayState` (S), `ClickToPayEffect` (S) |
| `qpay/` | `QPayLauncher`, `QPayResultCallback` (F), `ResultCallback` (T), `QPayLauncherContract`, `QPayActivity`, `QPayFormBuilder` (O) |
| `benefit/` | `BenefitLauncher`, `BenefitResultCallback` (F), `BenefitLauncherContract`, `BenefitActivity` |
| `visaInstalments/` | `VisaInstallmentsLauncher`, `VisaInstallmentsResultCallback` (F), `VisaInstallmentsLauncherContract`, `VisaInstallmentsActivity`, `InstallmentPlan` (D), `PlanFrequency` (E) · 5 composables |
| `partialAuth/` | `PartialAuthActivity`, `PartialAuthViewModel`, `PartialAuthActivityArgs` · composable `PartialAuthView` |
| `googlepay/` | `GooglePayLauncher`, `GooglePayAvailability`, `GooglePayActivity`, `GooglePayViewModel`, `GooglePayConfig` (D), `GooglePayConfigFactory`, `GooglePayJsonConfig`, `ButtonTheme` (E), `ButtonType` (E) · composable `GooglePayButton` |
| `core/`, `util/` | `ModifierExtensions.kt`, `StringUtils.kt` — extension functions only |

---

## `:payment-sdk-samsungpay` — 4 files

Public standalone entry lives in `:payment-sdk` as `SamsungPayLauncher`.

| Type | Kind | Role |
|---|---|---|
| `SamsungPayClient` | C | Availability check + payment sheet launch |
| `SamsungPayResponse` | I | Merchant callback |
| `SamsungPayTransactionListener` | C | Samsung SDK listener bridge |
| `SamsungPayCardMapper` | C | `CardType` → Samsung brand |

---

## Naming observations

- **File/type mismatch:** `PaymentsLauncher.kt` → `UnifiedPaymentPageLauncher`,
  `PaymentsRequest.kt` → `UnifiedPaymentPageRequest`, `PaymentsResult.kt` →
  `UnifiedPaymentPageResult`, `PaymentsActivity.kt` → `UnifiedPaymentPageActivity`,
  `PaymentsViewModel.kt` → `UnifiedPaymentPageViewModel`, `PaymentsVMUiState.kt` → four
  types. Item **E16**.
- **`ResultCallback` declared three times:** `fun interface` in `aaniPay` and `clicktopay`,
  `typealias` in `qpay`. Item **E15**.
- **Two `SDKTheme.kt`**, two `Links` data classes, two `AuthResponse` types (one sealed in
  `interactor/`, one data class in `ThreeDSAuthResponse.kt`), two `Cvv` types
  (`cardpayment/card/CardModel.kt` and iOS's `Cvv`).
- **Typos:** `AaniPayApiInterator`, package `visaInstalments` (one `l`) vs. class
  `VisaInstallments` (two).
- `LoadingMessage` (an enum used across the whole SDK) lives in
  `cardpayment/widget/CircularProgressDialog.kt`.

Cross-platform mapping is in [`17-class-index-ios.md`](17-class-index-ios.md); behavioural
differences are in the feature docs.
