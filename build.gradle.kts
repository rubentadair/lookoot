plugins {
    id("com.android.application") version "8.5.1" apply false
    // This line configures the Android application plugin, which is necessary for building Android apps.
    // The `version "8.5.1"` specifies the version of the plugin.
    // `apply false` means that the plugin is not applied automatically; it needs to be applied in the module's build.gradle file.

    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    // This line configures the Kotlin plugin for Android, which is necessary for writing Android apps in Kotlin.
    // The `version "1.9.0"` specifies the version of Kotlin that you want to use.
    // `apply false` means that this plugin will be applied in the module's build.gradle file.

    id("com.google.gms.google-services") version "4.4.0" apply false
    // This line adds the Google Services plugin, which is necessary for integrating Google services (like Firebase) into your app.
    // The `version "4.4.0"` specifies the version of the Google Services plugin.
    // `apply false` means this plugin will be applied in the module's build.gradle file.

    alias(libs.plugins.google.firebase.crashlytics) apply false
    // This line adds the Firebase Crashlytics plugin, which is used for crash reporting in Firebase.
    // `apply false` indicates that this plugin will be applied in the module's build.gradle file.
    kotlin("plugin.serialization") version "1.6.21"

}
