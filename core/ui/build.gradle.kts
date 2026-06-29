plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ch.core.ui"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)

    // Window 窗口适配（折叠屏/大屏适配）
    implementation(libs.androidx.window)
}
