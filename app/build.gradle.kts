plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { namespace = "com.example.balancewidget"; compileSdk = 35; buildToolsVersion = "36.0.0"
    defaultConfig { applicationId = "com.example.balancewidget"; minSdk = 26; targetSdk = 35; versionCode = 4; versionName = "1.2.1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_23; targetCompatibility = JavaVersion.VERSION_23 }
    kotlinOptions { jvmTarget = "23" }
    testOptions { unitTests.isReturnDefaultValues = true }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
