# Payment SDK for Android

![Banner](assets/banner.jpg)

[![Build Status](https://travis-ci.com/network-international/payment-sdk-android.svg?branch=master)](https://travis-ci.com/network-international/payment-sdk-android)
[![](https://jitpack.io/v/network-international/payment-sdk-android.svg)](https://jitpack.io/#network-international/payment-sdk-android)

The Payment SDK for Android provides a pre-built checkout experience for accepting payments in your Android app. It supports card payments, Google Pay, Samsung Pay, Click to Pay, saved cards, Visa Installments, Aani Pay, and partial authorization — all with 3D Secure support.

## Requirements

| Requirement | Version |
|-------------|---------|
| **Min SDK** | 21 (Android 5.0 Lollipop) |
| **Target SDK** | 34 (Android 14) |
| **Compile SDK** | 34 |
| **Java** | 17 |
| **Kotlin** | 1.9.22+ |
| **Gradle** | 8.10.2+ |
| **Android Gradle Plugin** | 8.7.3+ |

#### Supported Languages

English and Arabic.

---

## Installation

### 1. Configure Gradle

**gradle-wrapper.properties:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
```

**Root build.gradle:**
```groovy
buildscript {
    ext.kotlin_version = '1.9.22'

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.7.3'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**gradle.properties:**
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
android.nonFinalResIds=true
android.enableJetifier=false
android.defaults.buildfeatures.buildconfig=true
kotlin.code.style=official
```

### 2. Add Dependencies

```groovy
android {
    compileSdk 34

    defaultConfig {
        minSdk 21
        targetSdk 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.8'
    }
}

dependencies {
    // Required - Core module
    implementation 'com.github.network-international.payment-sdk-android:payment-sdk-core:5.0.0'

    // Card payment (includes Jetpack Compose UI)
    implementation 'com.github.network-international.payment-sdk-android:payment-sdk:5.0.0'

    // Samsung Pay integration (optional)
    implementation 'com.github.network-international.payment-sdk-android:payment-sdk-samsungpay:5.0.0'
}
```

> Replace `5.0.0` with the latest version from [JitPack](https://jitpack.io/#network-international/payment-sdk-android).

---

## Modules

| Module | Description |
|--------|-------------|
| `payment-sdk-core` | Common interfaces, data models, and API layer. Required by all other modules. |
| `payment-sdk` | Card payment UI (Jetpack Compose), 3D Secure, Google Pay, Saved Cards, Click to Pay, Visa Installments, Aani Pay, Partial Auth. |
| `payment-sdk-samsungpay` | Samsung Pay integration. |

---

## Quick Start — Card Payment

The SDK uses the Activity Result API. Initialize `UnifiedPaymentPageLauncher` as a property in your Activity, then launch it with the order URLs from your backend.

```kotlin
import payment.sdk.android.payments.UnifiedPaymentPageLauncher
import payment.sdk.android.payments.UnifiedPaymentPageRequest
import payment.sdk.android.payments.UnifiedPaymentPageResult

class CheckoutActivity : ComponentActivity() {

    private val paymentsLauncher = UnifiedPaymentPageLauncher(this) { result ->
        handleResult(result)
    }

    private fun startPayment(authUrl: String, payPageUrl: String) {
        paymentsLauncher.launch(
            UnifiedPaymentPageRequest.builder()
                .gatewayAuthorizationUrl(authUrl)
                .payPageUrl(payPageUrl)
                .build()
        )
    }

    private fun handleResult(result: UnifiedPaymentPageResult) {
        when (result) {
            is UnifiedPaymentPageResult.Success -> // SALE or PURCHASE successful
            is UnifiedPaymentPageResult.Authorised -> // AUTH successful
            is UnifiedPaymentPageResult.Captured -> // Payment captured
            is UnifiedPaymentPageResult.PostAuthReview -> // Pending fraud review
            is UnifiedPaymentPageResult.PartiallyAuthorised -> // Partial auth accepted
            is UnifiedPaymentPageResult.PartialAuthDeclined -> // Partial auth declined by user
            is UnifiedPaymentPageResult.PartialAuthDeclineFailed -> // Reversal failed
            is UnifiedPaymentPageResult.Failed -> // Payment failed: result.error
            is UnifiedPaymentPageResult.Cancelled -> // User cancelled
            is UnifiedPaymentPageResult.SamsungPayRequested -> // Handle Samsung Pay
        }
    }
}
```

### Jetpack Compose

Use `rememberUnifiedPaymentPageLauncher` inside a composable:

```kotlin
@Composable
fun CheckoutScreen() {
    val launcher = rememberUnifiedPaymentPageLauncher { result ->
        // handle result
    }

    Button(onClick = {
        launcher.launch(
            UnifiedPaymentPageRequest.builder()
                .gatewayAuthorizationUrl(authUrl)
                .payPageUrl(payPageUrl)
                .build()
        )
    }) {
        Text("Pay Now")
    }
}
```

---

## Google Pay

Pass a `GooglePayConfig` when building the request:

```kotlin
import payment.sdk.android.googlepay.GooglePayConfig

val googlePayConfig = GooglePayConfig(
    environment = GooglePayConfig.Environment.Test,  // .Production for live
    merchantGatewayId = "your-gateway-id"
)

paymentsLauncher.launch(
    UnifiedPaymentPageRequest.builder()
        .gatewayAuthorizationUrl(authUrl)
        .payPageUrl(payPageUrl)
        .setGooglePayConfig(googlePayConfig)
        .build()
)
```

The Google Pay button appears automatically when the config is provided and the device supports it.

---

## Saved Card Payment

Use `SavedCardPaymentLauncher` for returning customers with saved cards:

```kotlin
import payment.sdk.android.savedCard.SavedCardPaymentLauncher
import payment.sdk.android.savedCard.SavedCardPaymentRequest

class CheckoutActivity : ComponentActivity() {

    private val savedCardLauncher = SavedCardPaymentLauncher(this) { result ->
        handleResult(result)  // same UnifiedPaymentPageResult
    }

    private fun paySavedCard(authUrl: String, payPageUrl: String, cvv: String? = null) {
        savedCardLauncher.launch(
            SavedCardPaymentRequest.builder()
                .gatewayAuthorizationUrl(authUrl)
                .payPageUrl(payPageUrl)
                .setCvv(cvv)  // optional
                .build()
        )
    }
}
```

Also available as a composable: `rememberSavedCardPaymentLauncher`.

---

## Click to Pay

Click to Pay (Visa SRC) lets returning consumers pay with saved cards without re-entering card details.

### 1. Configure

Obtain DPA credentials from Visa during onboarding:

```kotlin
import payment.sdk.android.core.interactor.ClickToPayConfig

val clickToPayConfig = ClickToPayConfig(
    dpaId = "your-dpa-id",
    dpaClientId = "your-client-id",   // optional, for multi-merchant setups
    cardBrands = listOf("visa", "mastercard"),
    dpaName = "Your Merchant Name",
    isSandbox = true                   // false for production
)
```

### 2. Launch (integrated)

Pass the config when building the payment request — the Click to Pay option appears automatically:

```kotlin
paymentsLauncher.launch(
    UnifiedPaymentPageRequest.builder()
        .gatewayAuthorizationUrl(authUrl)
        .payPageUrl(payPageUrl)
        .setClickToPayConfig(clickToPayConfig)
        .build()
)
```

### 3. Launch (standalone)

You can also use `ClickToPayLauncher` independently:

```kotlin
import payment.sdk.android.clicktopay.ClickToPayLauncher

private val clickToPayLauncher = ClickToPayLauncher(this) { result ->
    when (result) {
        ClickToPayLauncher.Result.Success -> // Payment succeeded
        ClickToPayLauncher.Result.Authorised -> // Payment authorised
        ClickToPayLauncher.Result.Captured -> // Payment captured
        ClickToPayLauncher.Result.PostAuthReview -> // Pending review
        is ClickToPayLauncher.Result.Failed -> // result.error
        ClickToPayLauncher.Result.Canceled -> // User cancelled
        is ClickToPayLauncher.Result.Requires3DS -> // 3DS required
    }
}
```

---

## Samsung Pay

See our [Samsung Pay Integration Guide](https://github.com/network-international/payment-sdk-android/wiki/Samsung-Pay) and the [FAQ & Troubleshooting](https://github.com/network-international/payment-sdk-android/wiki/Samsung-Pay#faq--troubleshooting) section.

---

## Visa Installments

Use `VisaInstallmentsLauncher` to present installment plan selection:

```kotlin
import payment.sdk.android.visaInstalments.VisaInstallmentsLauncher

private val visaInstallmentsLauncher = VisaInstallmentsLauncher(this) { result ->
    when (result) {
        is VisaInstallmentsLauncher.Result.Success -> // result.installmentPlan
        is VisaInstallmentsLauncher.Result.Cancelled -> // User cancelled
    }
}
```

---

## SDK Configuration

### Show/Hide Order Amount

Control whether the amount is displayed on the pay button (default: `false`):

```kotlin
SDKConfig.shouldShowOrderAmount(true)
```

### Cancel Alert Dialog

Prompt an alert when users try to close the payment page:

```kotlin
SDKConfig.shouldShowCancelAlert(true)
```

### Merchant Logo

Display your logo at the top of the payment screen:

```kotlin
SDKConfig.setMerchantLogo(R.drawable.your_logo)
```

---

## Customizing Colors

The SDK provides two ways to customize colors: **XML resource overrides** (build-time) and **runtime overrides** via `SDKConfig.setColor()`.

### Available Color Resources

| Resource Name | Default | Description |
|---------------|---------|-------------|
| `payment_sdk_pay_button_background_color` | `#4885ED` | Pay button background |
| `payment_sdk_pay_button_text_color` | `#FFFFFF` | Pay button text |
| `payment_sdk_button_disabled_background_color` | `#D1D1D6` | Disabled button background |
| `payment_sdk_button_disabled_text_color` | `#8E8E93` | Disabled button text |
| `payment_sdk_toolbar_color` | `#000000` | Toolbar background |
| `payment_sdk_toolbar_text_color` | `#FFFFFF` | Toolbar text |
| `payment_sdk_toolbar_icon_color` | `#FFFFFF` | Toolbar icons (back arrow, close) |

### Option 1: XML Override (build-time)

Add color resources in your app's `res/values/colors.xml`. These override the SDK defaults at build time:

```xml
<resources>
    <color name="payment_sdk_pay_button_background_color">#FF5722</color>
    <color name="payment_sdk_pay_button_text_color">#FFFFFF</color>
    <color name="payment_sdk_button_disabled_background_color">#BDBDBD</color>
    <color name="payment_sdk_button_disabled_text_color">#757575</color>
    <color name="payment_sdk_toolbar_color">#FF5722</color>
    <color name="payment_sdk_toolbar_text_color">#FFFFFF</color>
</resources>
```

### Option 2: Runtime Override

Use `SDKConfig.setColor()` to override colors at runtime. This takes precedence over XML values:

```kotlin
import payment.sdk.android.SDKConfig
import payment.sdk.android.sdk.R

// Call before launching any payment flow (e.g., in Application.onCreate or Activity.onCreate)
SDKConfig
    .setColor(R.color.payment_sdk_pay_button_background_color, Color.parseColor("#FF5722"))
    .setColor(R.color.payment_sdk_pay_button_text_color, Color.WHITE)
    .setColor(R.color.payment_sdk_button_disabled_background_color, Color.parseColor("#BDBDBD"))
    .setColor(R.color.payment_sdk_button_disabled_text_color, Color.parseColor("#757575"))
    .setColor(R.color.payment_sdk_toolbar_color, Color.parseColor("#FF5722"))
    .setColor(R.color.payment_sdk_toolbar_text_color, Color.WHITE)
```

Runtime overrides apply to all SDK screens including card payment, saved card, Click to Pay WebView buttons, Visa Installments, and Aani Pay.

---

## Legacy API — PaymentClient

For backward compatibility, `PaymentClient` is still available using `onActivityResult`:

```kotlin
PaymentClient(activity).launchCardPayment(request, REQUEST_CODE)

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                val paymentData = CardPaymentData.getFromIntent(data!!)
                when (paymentData.code) {
                    CardPaymentData.STATUS_PAYMENT_CAPTURED -> // Success (SALE)
                    CardPaymentData.STATUS_PAYMENT_AUTHORIZED -> // Success (AUTH)
                    CardPaymentData.STATUS_PAYMENT_FAILED -> // Failed
                    CardPaymentData.STATUS_GENERIC_ERROR -> // Error
                }
            }
            Activity.RESULT_CANCELED -> // User cancelled
        }
    }
}
```

---

## 3D Secure

3D Secure (both 1.0 and 2.0) is handled automatically within the card payment flow. No additional integration is required — the SDK manages the challenge and frictionless flows internally.

For manual 3DS execution with the legacy API:

```kotlin
paymentClient.executeThreeDS(paymentResponse, REQUEST_CODE)
```

---

## Troubleshooting

### Gradle Version Compatibility

| Gradle | AGP | Java |
|--------|-----|------|
| 8.10.2 | 8.7.3 | 17–21 |
| 8.6 | 8.3.x | 17–21 |

### Kotlin & Compose Compiler

| Kotlin | Compose Compiler |
|--------|------------------|
| 1.9.22 | 1.5.8 |
| 1.9.23 | 1.5.10 |
| 2.0.0+ | Use Compose Compiler Gradle Plugin |

### jcenter() Deprecation

Remove `jcenter()` from your repositories:

```groovy
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

### Payment fails after card submission

Ensure your merchant account has EMV 3DS 2.0 enabled. Contact support to enable it.

### Duplicate class error

If you see `Duplicate class com.nimbusds.jose.jwk.KeyOperation`, identify and exclude the conflicting dependency in your app.

---

## Support

For integration support, contact: **ecom-reintegration@network.global**
