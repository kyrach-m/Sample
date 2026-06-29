plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ch.service.crash"
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
    implementation(libs.androidx.lifecycle.process)

    // Logger 门面
    implementation(project(":core:common"))
}
