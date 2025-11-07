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
}

dependencies {
    // Use ONE Mockito artifact for instrumentation tests to avoid conflicts
    // Keep mockito-android; REMOVE mockito-inline
    androidTestImplementation("org.mockito:mockito-android:5.12.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Note: you already have espresso in androidTest; remove the implementation line if not needed
    implementation("androidx.test.espresso:espresso-core:3.7.0")

    debugImplementation("androidx.fragment:fragment-testing:1.8.9")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:core:1.5.0")

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
}
