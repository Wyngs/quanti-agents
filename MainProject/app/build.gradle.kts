plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.quantiagents.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quantiagents.app"
        minSdk = 26
        targetSdk = 34
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

    // (Optional) If your unit tests need access to resources
    // testOptions { unitTests.isIncludeAndroidResources = true }
}

// Enable JUnit 5 for all JVM unit tests
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // --- App deps ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase (via BoM)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    // --- Local unit tests (JVM) ---
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")         // mock finals
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0") // optional integration

    // --- Instrumented tests (device/emulator) ---
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:core:1.5.0")

    // Optional: fragment testing (debug only)
    debugImplementation("androidx.fragment:fragment-testing:1.7.1")
}
