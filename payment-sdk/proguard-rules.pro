# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all SDK Parcelable classes and their fields — required because @Parcelize
# generates code that references class/field names by reflection at runtime.
# R8 obfuscation breaks enum ordinal() lookups and field access inside Parcel
# read/write methods when these classes are renamed.
-keep class payment.sdk.android.** implements android.os.Parcelable { *; }
-keepclassmembers class payment.sdk.android.** implements android.os.Parcelable { *; }

# Keep Kotlin enums inside the SDK (enum ordinal()/name() used by Parcelize)
-keepclassmembers enum payment.sdk.android.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    *;
}
