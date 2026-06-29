# ============================================================
# Multidex Keep Proguard 配置
# ============================================================
#
# 说明：
# 当方法数超过 65535 时，Android 会将代码拆分到多个 Dex 文件中。
# 主 Dex（classes.dex）包含启动阶段必需的类。
# 此文件确保启动关键类被保留在主 Dex 中，
# 避免在启动时因加载 Secondary Dex 而导致启动延迟。
#
# 使用方式：
# 在 app/build.gradle.kts 中配置：
# ```kotlin
# defaultConfig {
#     multiDexEnabled = true
#     multiDexKeepProguard = file("multidex-config.pro")
# }
# ```
#
# 注意：
# - 仅保留启动路径上的类，不要将所有类都加入
# - 过多的主 Dex 类会增加 classes.dex 的大小
# - 配合 Application.attachBaseContext() 中的异步预加载使用

# ============================================================
# Application 及启动入口
# ============================================================
-keep class com.ch.sample.BaseApplication { *; }
-keep class com.ch.sample.splash.SplashActivity { *; }
-keep class com.ch.sample.MainActivity { *; }

# ============================================================
# 启动任务调度器
# ============================================================
-keep class com.ch.service.startup.dag.** { *; }
-keep class com.ch.service.startup.monitor.** { *; }
-keep class com.ch.service.startup.idle.** { *; }
-keep class com.ch.service.startup.initializer.** { *; }

# ============================================================
# AndroidX Startup 框架
# ============================================================
-keep class androidx.startup.** { *; }

# ============================================================
# Core 模块初始化类
# ============================================================
-keep class com.ch.core.network.client.NetworkClient { *; }
-keep class com.ch.core.storage.mmkv.MMKVHelper { *; }

# ============================================================
# DI 框架（如果使用 Hilt/Dagger）
# ============================================================
# -keep class com.ch.sample.di.** { *; }
# -keep class dagger.** { *; }
# -keep class javax.inject.** { *; }
