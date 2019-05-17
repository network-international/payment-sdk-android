package payment.sdk.android.demo.basket.data

import java.math.BigDecimal

data class FeeResponseDto(
        val fee: BigDecimal,
        val optionalFeeText: String?
)