plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.swetw.multilaps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.swetw.multilaps"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "0.4.0"
    }

    // The upload key lives outside the repository. Its location and passwords come from Gradle
    // properties, normally ~/.gradle/gradle.properties; without them a release build is left
    // unsigned, which is all a machine that never uploads needs. The store path is taken as
    // written: give it in full, since "~" is not expanded.
    fun signingProperty(name: String): String? = providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
    fun requiredSigningProperty(name: String): String =
        signingProperty(name) ?: error("$name is not set, though MULTILAPS_UPLOAD_STORE_FILE is")

    val uploadStoreFile = signingProperty("MULTILAPS_UPLOAD_STORE_FILE")
    signingConfigs {
        if (uploadStoreFile != null) {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = requiredSigningProperty("MULTILAPS_UPLOAD_STORE_PASSWORD")
                keyAlias = requiredSigningProperty("MULTILAPS_UPLOAD_KEY_ALIAS")
                keyPassword = requiredSigningProperty("MULTILAPS_UPLOAD_KEY_PASSWORD")
            }
        }
    }
    if (uploadStoreFile == null && gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
        logger.warn("MultiLaps: no upload key is configured, so this release build will be unsigned.")
    }

    buildTypes {
        release {
            // R8 shrinks, optimises and renames the code, and unused resources go with it.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    compileOptions {
        // java.time arrived in Android 8; desugaring brings it to older phones.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Compose UI tests need the debug-only test manifest, so unit tests run against debug alone.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enableUnitTest = false }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.play.review)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
