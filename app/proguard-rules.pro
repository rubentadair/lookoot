# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# General Android and Kotlin Rules
# Keep all annotations, especially important for Kotlin
-keepattributes *Annotation*

# Retain class members needed by Butterknife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**

# Retain class members for Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Firebase
# Keep classes used by Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# If using Firebase Database
-keep class com.google.firebase.database.** { *; }
-dontwarn com.google.firebase.database.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

# For Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# If using Gson with Retrofit
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# If using OkHttp with Retrofit
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Uncomment the following if using WebView with JS
# -keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
# }

# Jetpack Compose
# Keep all classes related to Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Rules for Material3
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Add any additional library-specific rules here...

# Note: Adjust this file as needed to include or exclude specific library rules or classes
