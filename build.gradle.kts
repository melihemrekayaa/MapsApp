
buildscript {
    repositories {
        google()
    }
    dependencies {

        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.9")
        classpath ("com.google.gms:google-services:4.4.2")

    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id ("com.google.dagger.hilt.android") version "2.49" apply false
    id ("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
    id("com.google.gms.google-services") version "4.4.2" apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
    id ("androidx.navigation.safeargs") version "2.8.9" apply false

}

