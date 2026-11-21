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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# مكتبة إعلانات AdMob (احتياط ضد تقليص R8)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.**

# ===== قواعد الحماية لنسخة release (R8 مفعّل) =====
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.example.TropicalApp { *; }
-keep class com.example.notify.** { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Application { *; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
-keep class com.example.ads.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
