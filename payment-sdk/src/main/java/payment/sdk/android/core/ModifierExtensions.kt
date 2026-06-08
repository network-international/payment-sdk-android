package payment.sdk.android.core

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

/**
 * Sets both [testTag] (for Compose UI tests) and [contentDescription] (so Appium can locate the
 * element via the `accessibility id` strategy on Android, matching iOS behaviour).
 */
fun Modifier.testId(id: String): Modifier = this.semantics(mergeDescendants = true) {
    testTag = id
    contentDescription = id
}
