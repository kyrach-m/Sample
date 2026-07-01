plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.ch.core.base"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.lifecycle.viewmodel)
    implementation(libs.androidx.compose.activity)
    implementation(libs.androidx.compose.navigation)

    // Window 窗口适配（折叠屏/大屏适配）
    implementation(libs.androidx.window)

    // Logger 门面（api 暴露给子模块，子模块无需再单独依赖）
    api(project(":core:common"))

    // UI 组件库（ThemeManager、StatusBarUtil、GlobalToolbar、GlobalBottomNav）
    api(project(":core:ui"))
}
