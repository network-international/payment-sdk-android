# Click to Pay OTP Input Fix Attempts

## Problem
When entering a digit in an OTP field, it doesn't automatically jump to the next field.
The "Verify and Continue" button doesn't get enabled when all fields are filled.

## Attempt 1 - Add keyup fallback + compositionend
- Added `keyup` event listener as fallback for unreliable `input` events in Android WebView
- Added `compositionend` for IME keyboards
- Changed `maxlength` from 1 to 2 and `type` from "text" to "tel"
- **Result: FAILED** - Two digits appeared in the same field because `maxlength="2"` allowed both chars before JS could process them.

## Attempt 2 - preventDefault in keydown
- Reverted `maxlength` back to 1
- Made `keydown` the primary handler with `e.preventDefault()` to block browser's default character insertion
- Manually set `box.value = e.key` and called `boxes[index + 1].focus()`
- Kept `input` event as fallback for keyboards sending `Unidentified` key
- **Result: FAILED** - Single digit displays correctly now, but focus does NOT jump to next field. Android WebView ignores `.focus()` calls during synchronous keyboard event handlers.

## Attempt 3 - Delayed focus with setTimeout
- Keep `keydown` + `preventDefault()` approach (works for single digit)
- Wrap `boxes[index + 1].focus()` in `setTimeout(..., 0)` to defer focus change until after the current event loop completes
- Android WebView processes focus changes reliably when deferred out of the keyboard event handler
- Also blur current field before focusing next to force keyboard detachment
- **Result: FAILED** - Still not jumping. Android WebView fundamentally refuses to move focus between separate `<input>` elements programmatically while the soft keyboard is active.

## Attempt 4 - Single hidden input (no focus switching needed)
- Completely replaced the 6 `<input>` elements with 6 `<div>` display boxes
- Added one real `<input id="otpHiddenInput" type="tel" maxlength="6">` positioned as a transparent overlay on top of the boxes
- All typing goes into the single hidden input — no focus switching at all
- On `input` event: strip non-digits, cap at 6 chars, distribute digits to the visual `<div>` boxes via `textContent`
- Active box gets `.active` class (blue border) based on current cursor position
- `clearOtpBoxes()` and `getOtpFromBoxes()` read from the hidden input
- Visual boxes have `pointer-events: none` so taps pass through to the hidden input
- **Result: TESTING** (couldn't test — stuck at "Initializing Click to Pay..." loading screen)

## Fix: Test mode stuck at loading
- `loadHtml()` was navigating to a real HTTPS URL and intercepting via `shouldInterceptRequest`
- In test mode, network/SSL issues could prevent the page from loading
- Fixed: in test mode, load HTML directly via `file:///android_asset/click_to_pay.html`
- Also skip `fetchVctpConfig()` in test mode since it's not needed
- **Result: PARTIALLY WORKED** — HTML loaded, `showTestOtpPage()` was called, but two JS errors crashed the script:
  1. `getElementById('skipEmailBtn').onclick` — element doesn't exist, throws null reference
  2. `otpHiddenInput.value` — variable never initialized because error #1 killed the script
- **Fix:** Added null guard on `skipEmailBtn` reference
