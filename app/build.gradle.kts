import org.gradle.internal.declarativedsl.parsing.main
import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelightPlugin)
    id("com.google.devtools.ksp")
    id("kotlin-kapt") // Only for Kotlin projects
    id("com.google.dagger.hilt.android")
    id("io.objectbox") // Apply last
}

android {
    namespace = "com.example.graphapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.graphapp"
        minSdk = 24
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
        compose = true
        viewBinding = true
    }

    packaging {
        resources {
            // Option A: keep one copy of NOTICE.md
            pickFirsts += "META-INF/NOTICE.md"
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.rxjava2)
    implementation(libs.androidx.room.rxjava3)
    implementation(libs.androidx.room.guava)
    testImplementation(libs.androidx.room.testing)
    implementation(libs.androidx.room.paging)
    implementation(libs.gson)
    implementation(libs.androidx.webkit)
    implementation(libs.ktor.client.android)
    implementation(libs.android.driver)
    implementation(libs.coroutines.extensions)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.onnxruntime.android)
    implementation(libs.onnxruntime.extensions.android)
    implementation(libs.sentence.embeddings)
    implementation(libs.objectbox.android)
    implementation(libs.kotlin.reflect)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.guava)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.stanford.corenlp) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }


}