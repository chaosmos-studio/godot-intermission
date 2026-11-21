import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "studio.chaosmos.intermission"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // The game's own build supplies Godot and LevelPlay (the addon's export plugin adds LevelPlay), so neither is
    // bundled here.
    compileOnly(libs.godot)
    compileOnly(libs.lifecycle.common)
    compileOnly(libs.levelplay)
    testImplementation(libs.junit)
}
