# ============================================================
# Android 商业级混淆规则
# 适用于：Kotlin + KSP + 自定义路由 + Retrofit + Gson + MMKV + WorkManager
# ============================================================

# ============================================================
# 1. 基础保留规则（所有 Android/Kotlin 项目必须）
# ============================================================

# 保留所有注解（运行时反射依赖）
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# 保留 Kotlin 元数据与反射
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlin.Metadata { *; }

# 保留 Parcelable 实现类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 R 文件资源引用
-keep class **.R$* { *; }
-keepclassmembers class **.R$* {
    <fields>;
}

# ============================================================
# 2. 网络层（Retrofit + OkHttp + Gson）
# ============================================================

# Retrofit 动态代理与接口
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Gson 序列化（所有数据模型必须保留，否则序列化失败）
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 项目数据模型（按实际包名保留）
-keep class com.ch.core.network.token.** { *; }

# Gson @SerializedName 注解保留
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================
# 3. 路由层（自定义 KSP 路由框架）
# ============================================================

# KSP 生成的路由注册类（RouterHelper 通过反射调用）
-keep class com.ch.middleware.router.generated.** { *; }

# 路由注解保留
-keep @interface com.ch.middleware.router.annotation.** { *; }

# 被 @Route 注解的类（Activity/Fragment）
-keep @com.ch.middleware.router.annotation.Route class * { *; }
-keepclasseswithmembers class * {
    @com.ch.middleware.router.annotation.Route *;
}

# 被 @ServiceProvider 注解的服务实现类
-keep @com.ch.middleware.router.annotation.ServiceProvider class * { *; }
-keepclasseswithmembers class * {
    @com.ch.middleware.router.annotation.ServiceProvider *;
}

# 被 @RouteInterceptorDef 注解的拦截器
-keep @com.ch.middleware.router.annotation.RouteInterceptorDef class * { *; }

# 路由接口和基类
-keep class com.ch.middleware.router.** { *; }

# 跨模块服务接口（ServiceManager 依赖）
-keep class com.ch.core.base.service.** { *; }

# ============================================================
# 4. 存储层（MMKV）
# ============================================================

# MMKV
-keep class com.tencent.mmkv.** { *; }
-keep class * implements com.tencent.mmkv.IMKVInstance { *; }

# ============================================================
# 5. 后台任务（WorkManager）
# ============================================================

# Worker 实现类
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# WorkManager 相关
-keep class androidx.work.** { *; }

# ============================================================
# 6. 业务模块
# ============================================================

# Application 类（清单文件引用，不能混淆）
-keep class com.ch.sample.BaseApplication { *; }

# 所有 Activity（清单文件引用）
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# 所有 Fragment
-keep public class * extends androidx.fragment.app.Fragment

# ============================================================
# 7. 日志剥离（Release 包移除 Debug 日志）
# ============================================================

# 移除 android.util.Log（如果使用了）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# 自定义 Logger（如果 Release 不打印，确保 Logger 内部判断了 BuildConfig.DEBUG）
# 由于 Logger 内部已判断 BuildConfig.DEBUG，R8 会自动裁剪未调用的方法

# ============================================================
# 8. 调试与异常（保留行号用于还原堆栈）
# ============================================================

-keepattributes LineNumberTable
-keepattributes SourceFile

# ============================================================
# 9. 第三方依赖兜底
# ============================================================

# Google API Client（被 Tink 引用）
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**

# JavaScript 接口（WebView 交互）
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 反射调用保留（防止 Class.forName 等反射找不到类）
-keep class * {
    public <init>(...);
}
-keepclassmembers class * {
    <init>(...);
}

# AndroidX 杂项
-keep class androidx.** { *; }
-dontwarn androidx.**
