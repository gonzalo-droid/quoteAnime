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
# ── Project rules ────────────────────────────────────────────────────────────

# Compile-only annotation from Play Services, referenced by play-core-ktx but never
# shipped in any artifact. R8 generated this rule itself (missing_rules.txt).
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# Crashlytics stack traces are useless once line numbers are stripped, and the
# original file name is what maps a frame back to a source file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
