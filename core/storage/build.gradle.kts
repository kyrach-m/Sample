plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ch.core.storage"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        // Room Schema 导出（KSP 配置）
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Core common（Logger 门面 + PreferenceUtil 统一 KV 存储）
    implementation(project(":core:common"))

    // MMKV - 高性能 KV 存储
    implementation(libs.mmkv)

    // Room - 本地数据库
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // SQLCipher - 数据库加密
    implementation(libs.sqlcipher)
    implementation(libs.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
