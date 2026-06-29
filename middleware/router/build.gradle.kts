plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ch.middleware.router"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// KSP 配置：路由注解处理器
ksp {
    arg("router.moduleName", "middleware_router")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // 路由注解定义（纯 Kotlin 模块）
    api(project(":middleware:router-annotations"))

    // KSP 路由注解处理器
    ksp(project(":middleware:router-compiler"))

    // Coroutines（拦截器链异步回调支持）
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Logger（埋点拦截器需要上报事件）
    implementation(project(":service:logger"))

    // Logger 门面
    implementation(project(":core:common"))
}
