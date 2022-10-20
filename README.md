:warning: Whilst we make some improvements to our mobile experiences, we’ve temporarily removed the EMV 3DS SDK. Please contact ecom-reintegration@network.global to obtain the files you need.

# Payment SDK for Android

![Banner](assets/banner.jpg)

[![Build Status](https://travis-ci.com/network-international/payment-sdk-android.svg?branch=master)](https://travis-ci.com/network-international/payment-sdk-android)
[![](https://jitpack.io/v/network-international/payment-sdk-android.svg)](https://jitpack.io/#network-international/payment-sdk-android)

## Specifications
#### Android API Level:

Android SDK and the sample merchant app support a minimum API level 19 (Android
Kitkat).
For more details, please refer to Android API level requirements [online](https://developer.android.com/guide/topics/manifest/uses-sdk-element#min)

#### Languages
Android SDK supports English and Arabic.

## Installation
```groovy
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  // Required
  implementation 'com.github.network-international.payment-sdk-android:payment-sdk-core:1.0.0'

  // For card payment
  implementation 'com.github.network-international.payment-sdk-android:payment-sdk:1.0.0'

  //For samsung payment
  implementation 'com.github.network-international.payment-sdk-android:payment-sdk-samsungpay:1.0.0'
}
```
### `payment-sdk-core`
This module contains the common interfaces and classes used by the other SDK modules.

### `payment-sdk`
This SDK contains the Android Pay Page source code for card payment. The module handles getting card details from the User, and submitting payment requests to the Payment Gateway. If 3D Secure is required with the existing payment flow, this will be handled by the page page as well. A successful or failed payment response will be returned to merchant app in an Intent bundle.

### `payment-sdk-samsungpay`
This SDK contains the Samsung Pay source code for samsung in-app payment journey.


## Card integration
SDK provides the PaymentClient class to launch various payment methods and get payment in the merchant app. PaymentClient requires an activity instance parameter to be instantiated. Please see the sample app for more details about using PaymentClient. It requires an activity instance rather than a Context since card payment result could only return the result to a calling activity.

The following code shows how to construct PaymentClient by passing an Activity
reference:

```kotlin
class PaymentClient(private val context: Activity)
```

### Card API
SDK provides a very simple API to be able to get payment using debit or credit cards.

```kotlin
PaymentClient.launchCardPayment(request: CardPaymentRequest, requestCode: Int)
```

The card payment API internally launches another activity to get card details from the user, and control payment/3D Secure flows between the payment gateway and the merchant app. Once payment flow completes, onActivityResult is called on merchant’s activity (which is passed in constructor) and CardPaymentData type is returned in the data Intent. requestCode is used to filter out activity result requests to find out where activity response comes from. Please see HomeActivity.onActivityResult for more details in the sample merchant app or refer to the following code snippet to learn how to handle card payment response in your merchant app.

```kotlin
override fun onActivityResult(
  requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == CARD_PAYMENT_REQUEST_CODE) {
      when (resultCode) {
        Activity.RESULT_OK -> onCardPaymentResponse(CardPaymentData.getFromIntent(data!!))
        Activity.RESULT_CANCELED -> onCardPaymentCancelled()
      }
    }
  }
}
```

You may notice that requestCode parameter is the same value as we already passed on to launchCardPayment method. If the User presses the Back button and cancels the card payment flow, RESULT_CANCELED is returned as an activity resultcode.


### Result Code in CardPaymentData
View the following code snippet to see how merchant app handles result code in `CardPaymentData`
```kotlin
override fun onCardPaymentResponse(data: CardPaymentData) {
  when (data.code) {
    CardPaymentData.STATUS_PAYMENT_AUTHORIZED,
    CardPaymentData.STATUS_PAYMENT_CAPTURED -> {
      view.showOrderSuccessful()
    }
    CardPaymentData.STATUS_PAYMENT_FAILED -> {
      view.showError("Payment failed")
    }
    CardPaymentData.STATUS_GENERIC_ERROR -> {
      view.showError("Generic error(${data.reason})")
    }
    else -> IllegalArgumentException("Unknown payment response (${data.reason})")
  }
}
```

Result Codes
Every possible result code is checked, and an appropriate action is taken:
- STATUS_PAYMENT_CAPTURED shows order creation with “SALE” action parameter is successful.
- STATUS_PAYMENT_AUTHORIZED shows order creation with “AUTH” action parameter is successful.
- STATUS_PAYMENT_FAILED shows payment is not successful on payment gateway.
- STATUS_GENERIC_ERROR: shows possible issues on the client side, for instance, network is not accessible or an unexpected error occurs.

## Samsung pay integration
Just like card, Samsung pay integration is also very simple.

Build samsung pay request:
```kotlin
SamsungPayRequest.builder()
  .merchantId(merchantId = String)
  .merchantName(merchantName = String)
  .orderNumber(orderNumber = String)
  .supportedCards(supportedCards = Set<CardType>)
  .addressInPaymentSheet(addressInPaymentSheet = SamsungPayRequest.AddressInPaymentSheet)
  .addAmountBoxControl(amountBoxControl = AmountBoxControl)
  .build()
```
And pass the above request as:
```kotlin
PaymentClient.launchSamsungPay(request: SamsungPayRequest)
```

## Attempt threeDSTwo on a payment
```kotlin
paymentClient.executeThreeDS(paymentResponse: PaymentResponse, requestCode: Int)

override fun onActivityResult(
    requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == CARD_PAYMENT_REQUEST_CODE) {
        when (resultCode) {
            Activity.RESULT_OK -> onCardPaymentResponse(CardPaymentData.getFromIntent(data!!))
            Activity.RESULT_CANCELED -> onCardPaymentCancelled()
        }
    }
}
```

Use the above method to execute threeDS frictionless or challenge for a paymentResponse. 
Once the threeDS operation is completed, the `onActivityResult` method is called with the requestedCode and a CardPaymentResponse object is passed as the data element in the third argument.
Use the same flow as handling a card response to assert the state of the payment for the order as shown in the previous section.

## Debugging build issues in your app

#### Payment failure after card information is submitted
Ensure your merchant account has EMV 3DS 2.0 enabled. Get in touch with our support to enable.

#### Missing required architecture x86_64 in file...
You need to compile and execute the project on a real device. The SDK is not compatible to be run on a simulator

#### Duplicate class issue
If you see the following error
`Duplicate class com.nimbusds.jose.jwk.KeyOperation found in modules jetified-ni-three-ds-two-android-sdk-1.0-runtime`
You need to identify another dependency in your app that has the same `com.nimbusds.jose` library and remove the duplicate copy

For more details please refer to sample app integration [`SamsungPayPresenter.kt`](https://github.com/network-international/payment-sdk-android/blob/master/app/src/main/java/payment/sdk/android/demo/basket/SamsungPayPresenter.kt#L39)
