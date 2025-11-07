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
        testInstrumentationRunner = "com.quantiagents.app.QaTestRunner"
    }

    // Avoid duplicate mockito-extensions resources during packaging
    packaging {
        resources {
            excludes += "mockito-extensions/org.mockito.plugins.MockMaker"
            excludes += "mockito-extensions/org.mockito.plugins.MemberAccessor"
        }
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

    // (Optional) keeps androidTest linked against the same APK variant
    testOptions {
        animationsDisabled = true
    }
}

/* ---------- GLOBAL EXCLUDES / ALIGNMENT ---------- */
// 1) Kill protobuf-lite everywhere (Firestore uses javalite)
configurations.configureEach {
    exclude(group = "com.google.protobuf", module = "protobuf-lite")
}

// 2) Make sure no androidx.test artifacts leak into runtime;
// if something accidentally pulls a newer/older test-core, force our 1.5.0 only for androidTest.
configurations.matching { it.name.contains("AndroidTest", ignoreCase = true) }.all {
    resolutionStrategy {
        force(
            "androidx.test:core:1.5.0",
            "androidx.test:rules:1.5.0",
            "androidx.test.ext:junit:1.1.5",
            "androidx.test.espresso:espresso-core:3.5.1",
            "androidx.test.espresso:espresso-contrib:3.5.1"
        )
    }
}

dependencies {
    // --- App/runtime (NO androidx.test libs here) ---
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Firebase via BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    // Ensure we have javalite (and we excluded lite above)
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")

    // --- Instrumentation tests ONLY ---
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    // Mockito for instrumentation
    androidTestImplementation("org.mockito:mockito-android:5.12.0")

    // Fragment testing (debug only)
    debugImplementation("androidx.fragment:fragment-testing:1.8.2")
}
