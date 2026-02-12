package payment.sdk.android.payments

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import payment.sdk.android.sdk.R

@Composable
fun resolveTermsConfig(
    orderType: String,
    isSubscriptionOrder: Boolean
): TermsConfig {

    return when {
        orderType == "RECURRING" && isSubscriptionOrder -> TermsConfig(
            termsText = stringResource(R.string.recurring_consent_terms),
            linkText = stringResource(R.string.consent_link_text)
        )

        orderType == "INSTALLMENT" && isSubscriptionOrder -> TermsConfig(
            termsText = stringResource(R.string.installment_consent_terms),
            linkText = stringResource(R.string.consent_link_text)
        )

        orderType == "UNSCHEDULED" -> TermsConfig(
            termsText = stringResource(R.string.unscheduled_consent_terms),
            linkText = stringResource(R.string.consent_link_text)
        )

        else -> TermsConfig(
            termsText = stringResource(R.string.default_terms),
            linkText = stringResource(R.string.default_consent_link_text)
        )
    }
}

data class TermsConfig(
    val termsText: String,
    val linkText: String,
)
