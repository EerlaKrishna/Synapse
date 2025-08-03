// val navVersion = "2.7.7" // Comment out for now

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Directly use the string
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}