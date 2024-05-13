
plugins {

    alias(libs.plugins.androidApplication)

}

android {
    namespace = "com.example.trail6"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.trail6"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(project(":sdk"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Gson (if not already included with Retrofit)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Add OpenCV library dependency
    implementation(project(":sdk"))

    //   implementation("com.google.code.gson:gson:2.8.8")
    //   implementation("com.google.android.libraries:places:18.0.0")
    //  implementation("com.google.android.libraries.places:places:3.4.0")
    //   implementation("com.google.android.libraries.places:places-api:3.4.0")

    // Google Maps and Places
//    implementation("com.google.android.gms:play-services-maps:20.2.0")
    //   implementation("com.google.android.gms:play-services-places:20.2.0")
    //   implementation("com.google.android.gms:play-services-location:21.2.0")

}