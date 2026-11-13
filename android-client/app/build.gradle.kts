import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.scasp.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.scasp.mobile"
        // minSdk 24 (Android 7.0): covers almost every active Android device, gives runtime permissions (API 23+),
        // network security config (API 24+) and works with WorkManager/AndroidX. See android-client/README.md.
        minSdk = 24
        targetSdk = 35
        versionCode = 10
        versionName = "10.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Emulator alias for the host PC running Payara. Can be changed on the login screen at run time.
        buildConfigField("String", "DEFAULT_BASE_URL", "\"http://10.0.2.2:8080/scasp\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true   // needed by Robolectric
        unitTests.isReturnDefaultValues = false
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.org.json)          // real org.json for plain JVM tests (Android's copy is a stub there)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
