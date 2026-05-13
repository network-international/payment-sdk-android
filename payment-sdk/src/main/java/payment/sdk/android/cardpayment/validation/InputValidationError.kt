package payment.sdk.android.cardpayment.validation

enum class InputValidationError {
    INVALID_CARD_NUMBER,
    INVALID_CARD_NUMBER_LENGTH,
    INVALID_EXPIRE_DATE,
    INVALID_CVV,
    INVALID_CARD_HOLDER
}