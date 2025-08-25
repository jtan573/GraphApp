// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
    id("androidx.room") version "2.7.1" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
}

buildscript {
     val objectboxVersion by extra("4.3.0") // For KTS build scripts

    repositories {
        mavenCentral()
    }

    dependencies {
        // Android Gradle Plugin 8.0 or later supported
        classpath("com.android.tools.build:gradle:8.0.2")
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }

    // Apply to every configuration in the app module
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.hamcrest:hamcrest-core:1.1")).using(module("junit:junit:4.13.2"))
        }
    }

}