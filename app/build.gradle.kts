plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.football"
    // 修复1：AGP 9.0+ 编译SDK写法简化（你的写法语法有误）
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.football"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NDK 架构配置（保留，仅编译主流架构）
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a") // 优化：简化写法
        }
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

    // 视图绑定（保留，UI操作必需）
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // 基础依赖（保留）
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 网络请求（OkHttp 5.x 适配Java 11）
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.google.code.gson:gson:2.13.2")

    // CameraX 相机核心（版本稳定，适配API 26+）
    val cameraxVersion = "1.3.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")

    // TensorFlow Lite dependencies removed: AGP 9 fails manifest merge due to
    // duplicate namespace between tensorflow-lite and tensorflow-lite-api.
    // Re-add with AGP-compatible versions when ML code is introduced.

    // MediaPipe Tasks Vision（仅添加这一套，避免手动引入 TensorFlow Lite 造成冲突）
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // LiveData + ViewModel（实时UI刷新）
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // 修复3：MPAndroidChart 依赖（版本兼容，需配合settings.gradle.kts配置仓库）
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ExoPlayer：用于更稳定的视频播放，替代 VideoView
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
}