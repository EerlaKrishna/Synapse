plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("androidx.navigation.safeargs.kotlin")

}

android {
    namespace = "com.example.synapse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.synapse"
        minSdk = 29
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    // Import the Firebase BoM - THIS MANAGES VERSIONS FOR OTHER FIREBASE LIBRARIES
    implementation(platform(libs.firebase.bom)) // Ensure libs.firebase.bom points to the BOM artifact

    // Declare the Firebase library dependencies WITHOUT versions
    implementation(libs.firebase.auth)             // Or "com.google.firebase:firebase-auth-ktx" if not using version catalog
    implementation(libs.firebase.database.ktx)    // Or "com.google.firebase:firebase-database-ktx"


    // Other dependencies
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.ui.text.android)
    //implementation(libs.firebase.firestore.ktx)
    // implementation(libs.mediation.test.suite)
    // implementation(libs.identity.jvm) // Review if this is still needed or if it's related to a specific auth method

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.recyclerview:recyclerview:1.3.2") // Specify a version for non-Firebase libs

    // Glide (optional for profile images)
    implementation("com.github.bumptech.glide:glide:4.16.0") // Updated to a more recent version
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Keep Glide versions consistent

    implementation("androidx.fragment:fragment-ktx:1.5.7")


    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")


    implementation("com.google.android.material:material:1.12.0")
}