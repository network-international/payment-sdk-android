# Click to Pay - Origin Issue Fix

## Problem

When loading `click_to_pay.html` from `file://` protocol in Android WebView:

```kotlin
// DON'T DO THIS - causes origin issues
webView.loadUrl("file:///android_asset/click_to_pay.html")
```

The WebView reports `window.origin` as `"null"`, which breaks cross-origin communication with Visa SDK iframes (`sandbox.auth.visa.com`, etc.). The `Vsb` object never initializes because the SDK can't complete its handshake.

## Solution

Use `loadDataWithBaseURL()` with an HTTPS base URL:

```kotlin
// DO THIS - sets proper HTTPS origin
val htmlContent = assets.open("click_to_pay.html").bufferedReader().use { it.readText() }
webView.loadDataWithBaseURL(
    "https://secure.checkout.visa.com/",  // This becomes window.origin
    htmlContent,
    "text/html",
    "UTF-8",
    null
)
```

This sets `window.origin` to `https://secure.checkout.visa.com` instead of `null`, enabling proper cross-origin `postMessage` communication with Visa SDK iframes.

## Required WebView Settings

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    setSupportMultipleWindows(true)
    javaScriptCanOpenWindowsAutomatically = true
    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
}

// Accept third-party cookies for SDK authentication
CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
```

## References

- [Android WebView.loadDataWithBaseURL](https://developer.android.com/reference/android/webkit/WebView#loadDataWithBaseURL)
- [Visa Developer - Click to Pay Integration](https://developer.visa.com/capabilities/visa-secure-remote-commerce/uctp-integration-overview)
