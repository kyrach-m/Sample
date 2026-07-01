plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ch.service.startup"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // AndroidX Startup
    implementation(libs.androidx.startup)

    // 依赖 core 模块
    implementation(project(":core:common"))

    // 依赖 logger 模块（上报启动耗时数据）
    implementation(project(":service:logger"))
}
