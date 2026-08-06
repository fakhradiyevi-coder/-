plugins {
    id("com.android.application")
}

android {
    namespace = "kz.edu.kaznmu.heartsounds"
    compileSdk = 36

    defaultConfig {
        applicationId = "kz.edu.kaznmu.heartsounds"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        noCompress += listOf("mp3", "wav")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
