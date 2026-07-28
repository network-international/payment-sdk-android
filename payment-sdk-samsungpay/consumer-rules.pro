# Keep rules propagated to any app that consumes payment-sdk-samsungpay.
#
# The Samsung Pay SDK marshals its model classes (CustomSheetPaymentInfo, CardInfo, CustomSheet,
# PartnerInfo, etc.) as Parcelables across a Binder boundary to the Samsung Pay service. If the
# consuming app minifies (R8/ProGuard) without these keeps, those classes get renamed/stripped and
# the IPC reply fails to unmarshal — "BadParcelableException: ClassNotFoundException" surfacing as
# REMOTE_EXCEPTION / -103 ERROR_INITIATION_FAIL when calling startInAppPayWithCustomSheet.
-dontwarn com.samsung.android.sdk.samsungpay.**
-keep class com.samsung.android.sdk.** { *; }
-keep interface com.samsung.android.sdk.** { *; }

# Keep this module's classes that participate in the Samsung Pay flow (listeners/mappers/responses).
-keep class payment.sdk.android.samsungpay.** { *; }
