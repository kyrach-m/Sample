plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ch.core.network"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG", "true")
        }
        release {
            buildConfigField("boolean", "DEBUG", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // 网络核心依赖（使用 api 暴露给业务模块，业务模块定义 Retrofit 接口）
    api(libs.retrofit)
    api(libs.retrofit.converter.gson)
    api(libs.okhttp)

    // 日志拦截器（仅 core:network 内部使用）
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // MMKV（SecurityInterceptor 需要存储 deviceId）
    implementation(libs.mmkv)

    // Logger 门面
    implementation(project(":core:common"))

    // KVStorage（TokenManager 需要存储 Token，api 暴露给业务层复用）
    api(project(":core:storage"))
}
