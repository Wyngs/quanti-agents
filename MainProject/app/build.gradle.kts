plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.quantiagents.app"
    compileSdk = 35

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
    
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.0")


    // For QR Code.... -> https://github.com/journeyapps/zxing-android-embedded
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    // Include the CameraX libraries
    implementation ("androidx.camera:camera-core:1.5.1")
    implementation ("androidx.camera:camera-camera2:1.5.1")
    implementation ("androidx.camera:camera-lifecycle:1.5.1")
    implementation ("androidx.camera:camera-view:1.5.1")
    implementation ("androidx.camera:camera-extensions:1.5.1")

    // ML Kit Barcode Scanning for CameraX
    implementation ("com.google.mlkit:barcode-scanning:17.3.0")

    // ShortcutBadger for app icon badges
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")

}

afterEvaluate {
    val variant = android.applicationVariants.find { it.name == "debug" }

    if (variant != null) {
        tasks.register<Javadoc>("generateJavadoc") {
            // Compile first to generate R.class
            dependsOn(variant.javaCompileProvider)

            // Source files (Converted to FileTree to satisfy type requirements)
            source = files(android.sourceSets.getByName("main").java.srcDirs).asFileTree

            // Build Classpath
            val pathList = ArrayList<File>()
            // Android Framework (android.jar)
            pathList.addAll(android.bootClasspath)
            // External Libraries (Glide, Firebase, etc)
            pathList.addAll(variant.javaCompileProvider.get().classpath.files)
            // Project Classes (R.class)
            pathList.add(variant.javaCompileProvider.get().destinationDirectory.get().asFile)

            // Apply Classpath
            classpath = files(pathList)

            // Javadoc Options
            options {
                this as StandardJavadocDocletOptions
                // Explicitly passing classpath to the tool ensures dependencies are found
                this.classpath = pathList

                addStringOption("Xdoclint:none", "-quiet")
                links("https://docs.oracle.com/javase/8/docs/api/")
                links("https://d.android.com/reference/")
            }

            isFailOnError = false
        }
    }
}