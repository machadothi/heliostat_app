import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.machadothi.templateapp"
    // Renders @PreviewTest composables to PNG on the JVM: ./gradlew updateDebugScreenshotTest
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    compileSdk = 35

    val localProperties = Properties()
    try {
        localProperties.load(FileInputStream("local.properties"))
    } catch (ignored: Exception) {
        // It means it couldn't find the local.properties file
    }

    defaultConfig {
        applicationId = "com.machadothi.templateapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "SERVER_URL",
            "\"${localProperties.getProperty("server.url")}\""
        )
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// The libraries that you can use
dependencies {

    // Implementation allows you to use the dependency
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // platform + bom is a bill of materials means that it contains a bunch of version
    // that compatible with each other so you don't have to define versions 1 by 1
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // testImplementation means that this dep will be only used in Tests (Unit Tests)
    testImplementation(libs.junit)
    // Serves the firmware's real recorded API responses to the app's HTTP layer
    testImplementation(libs.okhttp.mockwebserver)
    // same as above but in AndroidTest which ones are the UI Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // debugImplementation when you are building the app in DebugMode
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Dependency Injection
    implementation(libs.hilt.android)
    // ksp - Kotlin Symbol Processor
    ksp(libs.hilt.compiler)

    // Network communication library
    implementation(libs.retrofit)
    // Converter for DTOs
    implementation(libs.retrofit.converter.serializationx)
    // Logger for networks
    implementation(libs.okhttp.logging.interceptor)

    // Graph library
    implementation (libs.compose.charts)

    // Heliostat: remembers the board's address and the WiFi password it was given
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)
}