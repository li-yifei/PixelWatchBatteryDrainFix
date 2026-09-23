plugins {
    id("com.android.application")
}

val signingNames = listOf("RELEASE_KEYSTORE", "RELEASE_KEY_ALIAS", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_PASSWORD")
val signingValues = signingNames.associateWith { providers.environmentVariable(it).orNull }
val hasSigning = signingValues.values.all { !it.isNullOrBlank() }
require(hasSigning || signingValues.values.all { it.isNullOrBlank() }) { "Provide all four RELEASE_* signing variables." }

android {
    namespace = "io.github.li_yifei.pixelwatchbatterydrainfix"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.li_yifei.pixelwatchbatterydrainfix"
        minSdk = 30
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }

    if (hasSigning) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(signingValues.getValue("RELEASE_KEYSTORE")!!)
            keyAlias = signingValues.getValue("RELEASE_KEY_ALIAS")
            storePassword = signingValues.getValue("RELEASE_STORE_PASSWORD")
            keyPassword = signingValues.getValue("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
}
