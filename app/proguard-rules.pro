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

# SLF4J Rules
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# iText Rules
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.example.recordapp.**$$serializer { *; }
-keepclassmembers class com.example.recordapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.recordapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# ML Kit - OCR
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Compose UI
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Resources
-keep class **.R
-keep class **.R$* {
    public static <fields>;
}

# Prevent resource stripping
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**