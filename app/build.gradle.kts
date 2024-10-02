plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.adair.lookoot" // Specifies the namespace (package name) for the app.
    compileSdk = 34 // Target SDK version that the app will be compiled against.

    defaultConfig {
        applicationId = "com.adair.lookoot" // Unique application ID for the app.
        minSdk = 26 // Minimum SDK version that the app supports.
        targetSdk = 34 // Target SDK version that the app is optimized for.
        versionCode = 1 // Internal version number, increments with each release.
        versionName = "1.0" // User-facing version name.

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // Specifies the runner used for testing.
        vectorDrawables {
            useSupportLibrary = true // Enables support for vector drawables on older Android versions.
        }
    }

    buildTypes {
        debug {
            firebaseCrashlytics {
                mappingFileUploadEnabled = false // Disables uploading mapping files for debug builds.
            }
        }
        release {
            isMinifyEnabled = false // Disables code shrinking and obfuscation for release builds.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Specifies ProGuard configuration files for release builds.
            firebaseCrashlytics {
                mappingFileUploadEnabled = true // Enables uploading mapping files for Crashlytics in release builds.
            }
        }
    }

    buildFeatures {
        viewBinding = true // Enables view binding, allowing you to directly access views without using `findViewById`.
        compose = true // Enables Jetpack Compose, the modern Android UI toolkit.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Sets the source compatibility to Java 8.
        targetCompatibility = JavaVersion.VERSION_1_8 // Sets the target compatibility to Java 8.
    }

    kotlinOptions {
        jvmTarget = "1.8" // Sets the Kotlin JVM target to Java 8.
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Specifies the version of the Kotlin compiler extension for Compose.
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}" // Excludes specific files to avoid conflicts during packaging.
        }
    }
}

    dependencies {
        implementation(libs.androidx.core.ktx.v1120)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose.v181)
        implementation(libs.coil.compose)

        // Firebase - no explicit versions due to BoM
        implementation(platform(libs.firebase.bom.v3320))
        implementation(libs.firebase.analytics.ktx)
        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.firestore.ktx)
        implementation(libs.firebase.crashlytics.ktx)
        implementation(libs.firebase.database.ktx)
        implementation(libs.firebase.storage.ktx)
        implementation(libs.google.firebase.messaging.ktx)

        // Compose
        implementation(platform(libs.androidx.compose.bom.v20240800))
        implementation(libs.ui)
        implementation(libs.ui.graphics)
        implementation(libs.ui.tooling.preview)
        implementation(libs.material3)
        implementation(libs.androidx.runtime.livedata)
        implementation(libs.lifecycle.runtime.ktx.v262)
        implementation(libs.androidx.lifecycle.viewmodel.compose.v261)
        implementation(libs.lifecycle.viewmodel.compose)

        // Navigation
        implementation(libs.play.services.maps)
        implementation(libs.maps.compose)
        implementation(libs.android.maps.utils)
        implementation(libs.play.services.location)
        implementation(libs.maps.compose.widgets)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.compose.material3.material3)
        implementation(libs.androidx.material.icons.extended)
        implementation(libs.androidx.appcompat.v161)
        implementation(libs.androidx.room.ktx)
        implementation(libs.firebase.messaging)
        implementation(libs.firebase.crashlytics)
        implementation(libs.places)
        implementation(libs.firebase.storage)
        implementation(libs.firebase.auth.v2101)
        implementation(libs.play.services.safetynet)

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit.v115)
        androidTestImplementation(libs.androidx.espresso.core.v351)
        androidTestImplementation(platform(libs.compose.bom.v20231001))
        androidTestImplementation(libs.ui.test.junit4)
        debugImplementation(libs.ui.tooling)
        debugImplementation(libs.ui.test.manifest)

        implementation(libs.androidx.material3.v101)
        implementation(libs.androidx.foundation.layout)
        implementation(libs.androidx.compose.ui.ui)
        implementation(libs.androidx.compose.ui.ui.tooling)
        implementation(libs.com.google.firebase.firebase.firestore.ktx)
        implementation(libs.play.services.maps.v1701)
        implementation(libs.maps.compose.v200)
        implementation(libs.places.v240)
        implementation(libs.firebase.firestore.ktx.v2400)
        implementation(libs.places.v260)
        implementation(libs.android.maps.utils.v220)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.maps.ktx)
        implementation(libs.androidx.material3.v112)
        implementation(libs.android.maps.utils.v223)
        implementation(libs.maps.compose.v503)
        implementation("com.google.maps.android:maps-compose:2.11.4")
        implementation("com.google.android.gms:play-services-maps:18.1.0")
        implementation("com.google.maps.android:android-maps-utils:2.3.0")
        implementation("com.google.firebase:firebase-appcheck-playintegrity")
        implementation("com.google.android.play:integrity:1.4.0")
        implementation("com.google.firebase:firebase-auth:22.0.0")
        implementation("com.google.firebase:firebase-firestore:24.0.0")
        implementation("com.google.firebase:firebase-storage:20.0.0")
        implementation("com.google.firebase:firebase-appcheck:16.0.0")

    }

