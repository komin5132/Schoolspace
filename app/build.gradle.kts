plugins {
    id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.schoolspace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.schoolspace"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        disable.add("MissingDefaultResource")
    }
}
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Szyfrowanie Jetpack Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Zależności Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.5.0")

    // głowne założenia Androida
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Testowanie
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}