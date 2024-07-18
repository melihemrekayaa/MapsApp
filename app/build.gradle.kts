plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("com.google.gms.google-services")
    id ("kotlin-kapt")
    id ("dagger.hilt.android.plugin")
    id ("kotlin-parcelize")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

val MAPTILER_API_KEY = "7FSWmFQZTihljKDoGZiO"
val tomtomApiKey: String by project

android {

    namespace = "com.example.mapsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mapsapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPTILER_API_KEY", "\"${MAPTILER_API_KEY}\"")
    }

    packaging {
        jniLibs.pickFirsts.add("lib/**/libc++_shared.so")
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
    buildTypes.configureEach {
        buildConfigField("String", "TOMTOM_API_KEY", "\"$tomtomApiKey\"")
    }

    buildFeatures{
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

    kapt {
        correctErrorTypes = true
    }

    viewBinding{
        enable = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation ("com.google.firebase:firebase-auth-ktx:21.0.3")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.0.0")




    implementation ("com.google.firebase:firebase-auth-ktx:21.0.3")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.0.0")
    implementation ("com.google.firebase:firebase-analytics-ktx:19.0.0")

    implementation ("com.google.dagger:hilt-android:2.49")
    implementation(libs.play.services.maps)
    kapt ("com.google.dagger:hilt-compiler:2.49")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation ("androidx.navigation:navigation-ui-ktx:2.3.5")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
