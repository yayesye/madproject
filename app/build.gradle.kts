plugins {


        id("com.android.application")

        // Add the Google services Gradle plugin
        id("com.google.gms.google-services")


}

android {
    namespace = "com.example.authentication"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.authentication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)




        // Firebase BoM and Auth
        implementation(platform("com.google.firebase:firebase-bom:34.14.0"))
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-auth")

    // Paste these raw strings directly to ignore the libs.versions.toml catalog
    implementation("com.google.firebase:firebase-firestore:24.10.0")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.google.firebase:firebase-database:20.2.0")
    implementation("com.google.android.material:material:1.9.0")


        // Use this specific, stable version for the Google Sign-In buttons
        implementation("com.google.android.gms:play-services-auth:20.7.0")

        // Standard AndroidX libraries
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.9.0")
    }

