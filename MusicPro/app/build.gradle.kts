plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.musicpro"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.musicpro"
        minSdk = 36
        targetSdk = 36
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.airbnb.android:lottie:6.4.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // 核心 ExoPlayer 库
    implementation("androidx.media3:media3-exoplayer:1.3.1") // 请使用最新稳定版
    // MediaSession 服务端和客户端交互库
    implementation("androidx.media3:media3-session:1.3.1")
    // ExoPlayer 核心 UI 组件 (后面写界面会用到)
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("com.google.android.material:material:1.11.0") // 版本号可能略有不同，用你现有的即可
    implementation("androidx.documentfile:documentfile:1.0.1")
}