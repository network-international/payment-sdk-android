package payment.sdk.android.demo.basket.data

import androidx.annotation.Keep
import payment.sdk.android.core.SavedCard

@Keep
data class CreatePaymentOrderRequestDto(
        private val action: String,
        private val amount: PaymentOrderAmountDto,
        private val language: String,
        private val description: String = "Furniture Store Android App",
        private val merchantAttributes: Map<String, String> = mapOf(),
        private val savedCard: SavedCard? = null
)

/**
 * Gateway amount is always integer.
 * BigDecimal amount value should be mapped Int depending on currency
 *
 * £1.00    -> 100
 * $1.00    -> 100
 * AED1.00  -> 100
 *
 *  This is not the same for all currencies. Mapping should be done before sending Gateway
 */
@Keep
data class PaymentOrderAmountDto(
        private val value: Int,
        private val currencyCode: String
)
