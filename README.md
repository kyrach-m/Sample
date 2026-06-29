
```markdown
# Android 商业级架构框架 — 技术白皮书

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-blue)
![Android](https://img.shields.io/badge/Android-API%2024%2B-green)
![License](https://img.shields.io/badge/License-Apache%202.0-orange)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)

> **版本**：v1.0.0  
> **技术栈**：Kotlin 2.0 + KSP + Jetpack + 模块化架构  
> **定位**：一套开箱即用的 Android 大厂级基础架构脚手架

---

## 📖 目录

- [项目概述](#项目概述)
- [技术栈全景](#技术栈全景)
- [模块架构设计](#模块架构设计)
- [核心功能详解](#核心功能详解)
- [关键技术亮点](#关键技术亮点)
- [启动流程与性能优化](#启动流程与性能优化)
- [数据流转与安全](#数据流转与安全)
- [构建与部署](#构建与部署)
- [代码规范与工程化](#代码规范与工程化)
- [后续规划](#后续规划)

---

## 🚀 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/kyrach-m/Sample.git

# 2. 打开 Android Studio，等待 Gradle Sync 完成

# 3. 修改 local.properties 中的 sdk.dir 路径

# 4. 运行 app 模块，查看 Dashboard 能力展示页
```

---

## 项目概述

### 定位

这是一套基于 **MVVM + 模块化分层架构** 的 Android 基础框架，专为追求 **代码健壮性、启动性能、网络安全和团队协作效率** 的 Android 项目设计。它屏蔽了系统碎片化、网络复杂性、存储管理等技术细节，让业务开发者能够专注于功能实现。

### 核心设计理念

| 理念 | 说明 |
| :--- | :--- |
| **模块化分层** | 严格遵循 `Core → Service → Middleware → Features` 依赖方向，确保代码高内聚、低耦合 |
| **防御性编程** | 网络重试、缓存降级、崩溃兜底、权限引导，确保极端场景下的稳定性 |
| **开箱即用** | 新功能模块只需继承 `BaseActivity` / `BaseViewModel`，自动获得全套能力 |
| **可观测性** | 全链路启动耗时监控 + 自动埋点 + 崩溃日志链，让问题无处遁形 |

### 核心指标

| 指标 | 数值 | 说明 |
| :--- | :--- | :--- |
| **冷启动耗时** | ~450ms | 达到行业优秀水平（< 1s） |
| **Release APK 大小** | 已优化至 9.5MB | 已开启混淆 + 资源压缩 |
| **模块数量** | 14 个 | 覆盖网络/存储/路由/启动/崩溃/日志/登录 |
| **代码行数** | ~5000+ 行 | 纯框架代码，不含业务逻辑 |

---

## 技术栈全景

### 核心技术选型

| 技术领域 | 选型 | 版本 | 理由 |
| :--- | :--- | :--- | :--- |
| **语言** | Kotlin | 2.0.20 | 类型安全、协程支持、现代语言特性 |
| **UI 框架** | XML + ViewBinding | — | 稳定成熟，与 Compose 渐进迁移兼容 |
| **网络引擎** | Retrofit + OkHttp | 2.11.0 / 4.12.0 | 成熟稳定，拦截器链扩展性强 |
| **JSON 序列化** | Gson / kotlinx.serialization | 2.11.0 | 兼顾性能与灵活性 |
| **路由框架** | ARouter + 自研 KSP 处理器 | 1.5.2 | 组件化解耦，编译期生成路由表 |
| **KV 存储** | MMKV | 2.1.0 | 高性能、多实例隔离、跨进程支持 |
| **数据库** | Room | 2.7.0 | Google 官方 ORM，支持编译期验证 |
| **异步框架** | Kotlin Coroutines + Flow | 1.8.1 | 结构化并发，生命周期感知 |
| **依赖注入** | 手动构造 + Service Locator | — | 轻量级，避免引入 Dagger/Hilt 的编译开销 |
| **启动加速** | Jetpack Startup + 自研 DAG 调度器 | 1.1.1 | 任务并行化，冷启动提速 |
| **崩溃监控** | 自研 CrashHandler + ANR WatchDog | — | 捕获 Java/Kotlin 异常及 ANR |
| **日志系统** | 自研 Logger（门面模式） | — | Debug/Release 分级输出，支持文件持久化 |
| **埋点系统** | 自研 AnalyticsHelper + WorkManager | — | 采样 + 离线缓存 + 批量上报 |
| **Splash 适配** | AndroidX SplashScreen | 1.0.1 | 官方闪屏库，适配 Android 12+ |
| **构建工具** | Gradle + Version Catalog + KSP | 8.8.0 | 依赖集中管理，编译加速 |

---

## 模块架构设计

### 整体分层架构图

```
┌─────────────────────────────────────────────────────────────┐
│  app (壳工程)                                              │
│  职责：Application 入口、Splash、Manifest 配置、混淆配置    │
│  依赖：所有 features、service、middleware                   │
├─────────────────────────────────────────────────────────────┤
│  features (业务功能层)                                     │
│  ├── feature_login    - 登录模块（含完整 Demo）            │
│  └── (future) feature_home, feature_profile, ...          │
│  依赖：core、service、middleware                           │
│  约束：features 之间禁止互相依赖                           │
├─────────────────────────────────────────────────────────────┤
│  middleware (中间件层)                                     │
│  ├── router          - 路由框架（ARouter + KSP 处理器）   │
│  └── permission      - 权限申请统一处理                    │
│  依赖：core                                                │
│  约束：禁止依赖 service 层                                 │
├─────────────────────────────────────────────────────────────┤
│  service (基础服务层)                                      │
│  ├── startup         - DAG 启动任务调度器                  │
│  ├── crash           - 崩溃捕获与兜底 Recovery             │
│  └── logger          - 日志系统 + 埋点 + 采样 + 离线缓存   │
│  依赖：core                                                │
├─────────────────────────────────────────────────────────────┤
│  core (核心基建层)                                         │
│  ├── base            - BaseActivity/ViewModel、UiState     │
│  ├── network         - Retrofit + OkHttp + 拦截器链        │
│  ├── storage         - Room + MMKV + KVStorage 门面        │
│  ├── cache           - LruCache + MemoryCache + TTL        │
│  ├── common          - 纯 Kotlin 工具类（无 Android 依赖） │
│  └── ui              - 通用 UI 组件（LoadingMask/骨架屏）   │
└─────────────────────────────────────────────────────────────┘
```

### 模块职责详解

#### 1. `core:common` — 纯工具层（禁飞区）

**原则**：不依赖任何 Android 系统库，可在纯 JVM 环境运行单元测试。

| 类名 | 职责 |
| :--- | :--- |
| `DateTimeUtil` | 时间格式化（“刚刚”/“几分钟前”） |
| `EncryptUtil` | MD5 / SHA256 / AES 加解密 / Base64 |
| `DensityUtil` | dp ⇄ px / sp ⇄ px 转换 |
| `MoneyUtil` | 分转元（BigDecimal 精度处理） |
| `NetworkUtil` | 网络状态判断（含代理检测） |
| `AppUtil` | 版本名/版本号/包名/渠道号 |
| `StringUtil` | 手机号/邮箱正则校验、防 null 扩展 |
| `Logger` | 日志门面（Debug/Release 自动分级） |

---

#### 2. `core:base` — 基类与状态机

| 类名 | 职责 |
| :--- | :--- |
| `UiState<T>` | 密封类：`Loading / Success / Error / Empty` |
| `ViewEvent` | 密封类：一次性事件（Toast / Navigate） |
| `BaseViewModel<State, Event>` | 状态管理 + 协程封装 + `execute` 自动加载处理 |
| `BaseActivity<VB, VM>` | 自动订阅 `state` 和 `event`，防配置变更重复消费 |
| `BaseFragment<VB, VM>` | 同 BaseActivity，支持懒加载 |

**关键设计**：

- **State vs Event 分离**：State 用于 UI 渲染，Event 用于一次性的导航/Toast，防止旋转屏幕重复触发。
- **`execute` 高阶函数**：自动处理 `Loading → Success → Error` 状态流转，业务层只需写 `val result = execute { api.login() }`。

---

#### 3. `core:network` — 网络引擎

**拦截器链架构**：

```
Request → SecurityInterceptor → CacheInterceptor → MonitorInterceptor → RetryInterceptor → LogInterceptor → Server
                                                                                                          ↓
Response ← CacheInterceptor ← MonitorInterceptor ← RetryInterceptor ← LogInterceptor ← Server
```

| 拦截器 | 职责 |
| :--- | :--- |
| **SecurityInterceptor** | 自动附加 `X-Device-Id`、`X-Timestamp`、`X-Sign`（SHA256 签名），防重放攻击 |
| **CacheInterceptor** | 支持 `NETWORK_FIRST` / `CACHE_FIRST` / `CACHE_ONLY` 三种策略，离线可用 |
| **MonitorInterceptor** | 记录请求耗时、状态码、错误信息，自动上报 APM |
| **RetryInterceptor** | 指数退避重试（间隔 1s/2s/4s，最多 3 次） |
| **LogInterceptor** | 仅在 Debug 模式下打印完整 cURL 命令，方便后端排查 |

**安全特性**：

- **SSL Pinning（证书固定）**：内置服务器公钥哈希，拒绝中间人攻击。
- **设备指纹**：首次启动生成唯一 `deviceId`，存储在 MMKV 中。

---

#### 4. `core:storage` — 唯一存储门面

| 组件 | 职责 |
| :--- | :--- |
| **KVStorage** | 唯一 KV 存储门面，封装 MMKV 多实例（`USER` / `CONFIG` / `CACHE`） |
| **AppDatabase** | Room 数据库实例（含 Migration 版本管理） |
| **Converters** | Room 自定义类型转换器（`Date` ↔ `Long`） |
| **SqlCipherKeyManager** | 数据库加密（SQLCipher + Android Keystore） |

**设计原则**：

- 所有 `get` 方法**强制提供默认值**，从源头消灭 `NullPointerException`。
- 多实例隔离：`USER` 存储 Token/用户信息，`CONFIG` 存储远程开关，`CACHE` 存储临时数据（退出登录时只需 `KVStorage.clear(Scope.USER)`）。

---

#### 5. `service:startup` — DAG 启动任务调度器

**核心能力**：

- 基于**有向无环图（DAG）** 解析任务依赖，自动并行执行无依赖的任务。
- 支持 `main` / `io` 线程调度、超时熔断、循环依赖检测。
- 配合 `StartupMonitor` 记录每个任务的耗时。

**使用示例**：

```kotlin
class DatabaseWarmUpTask : StartupTask() {
    override val name = "database_warmup"
    override fun execute(context: Context) { /* 预热数据库 */ }
}

// 注册任务
StartupScheduler.addTask(DatabaseWarmUpTask())
StartupScheduler.addTask(ConfigPreloadTask().dependsOn(DatabaseWarmUpTask::class.java))
```

---

#### 6. `service:crash` — 崩溃捕获与兜底

| 组件 | 职责 |
| :--- | :--- |
| **CrashHandler** | 实现 `UncaughtExceptionHandler`，捕获 Java/Kotlin 异常 |
| **RecoveryActivity** | 崩溃后显示“应用正在恢复”，延迟 2 秒自动重启 |
| **ANRWatchDog** | 独立线程检测主线程卡顿 > 5s，主动记录堆栈并上报 |

**日志链**：崩溃发生时，自动导出最近 200 条本地日志，附加到上报载荷中，方便定位问题。

---

#### 7. `service:logger` — 日志与埋点

| 组件 | 职责 |
| :--- | :--- |
| **Logger** | 门面类，Debug 包输出全部日志，Release 包只输出 Error |
| **LogFileManager** | 日志文件滚动（按 2MB / 每天），旧文件自动压缩，保留最近 7 天 |
| **AnalyticsHelper** | 埋点上报入口，自动补全设备信息 |
| **SampleRateManager** | 按事件名配置采样率（如 `page_view=0.1`） |
| **AnalyticsRepository** | 埋点先落 Room 数据库，通过 `WorkManager` 批量上报（离线缓存 + 重试） |

---

#### 8. `middleware:router` — 路由框架（自研 KSP 处理器）

**核心机制**：

- 使用 `@Route`、`@ServiceProvider`、`@RouteInterceptorDef`、`@RequireLogin` 注解标记目标。
- **KSP 编译器** 在编译期扫描注解，生成 `GeneratedRouterInit_<module>.kt`。
- 在 `Application.onCreate()` 中调用 `GeneratedRouterInit_xxx.init()` 完成注册。

**关键创新：解决 KSP + ViewBinding 兼容性问题**

- `getSymbolsWithAnnotation` 在遇到包含 ViewBinding（尚未生成的类）作为泛型参数时会静默跳过。
- **解决方案**：实现 `findSymbolsWithAnnotation` 双重扫描机制：① 标准 API 获取；② 递归遍历 `resolver.getAllFiles()` 手动查找注解。
- 此方案适用于所有 KSP + 生成类型（Dagger/Hilt + ViewBinding）的场景。

**拦截器链**：

| 拦截器 | 优先级 | 职责 |
| :--- | :--- | :--- |
| `LoginInterceptor` | 1 | 检查 `@RequireLogin` 注解，未登录时拦截跳转 |
| `TrackingInterceptor` | 2 | 自动上报页面跳转埋点 |

---

#### 9. `middleware:permission` — 权限申请统一处理

- 封装 `ActivityResultContracts.RequestMultiplePermissions`，将回调转为 `suspend` 函数。
- 自动处理 `shouldShowRequestPermissionRationale`（解释弹窗）和“不再询问”引导跳转设置页。
- 在 `BaseActivity` / `BaseFragment` 中提供 `requestPermissionLauncher.launch(arrayOf(...))` 快捷方法。

---

## 关键技术亮点

### 1. 状态机 + Event 分离（MVI 模式简化版）

传统的 `LiveData` 或 `StateFlow` 只维护一个 State，当需要弹 Toast 或跳转时，往往在 State 中塞一个 `navigateTo` 字段，导致旋转屏幕时重复触发。

**我们的方案**：

- **State**：不可变数据类，描述“页面现在长什么样”。
- **Event**：`SharedFlow`，一次性事件（`ShowToast`、`NavigateTo`），消费后自动清除。
- BaseActivity 自动收集 Event，并在配置变更时**不重新消费**。

```kotlin
// ViewModel
sealed class LoginEvent : ViewEvent() {
    data class ShowToast(val msg: String) : LoginEvent()
    object NavigateToHome : LoginEvent()
}

// Activity
override fun handleEvent(event: LoginEvent) {
    when (event) {
        is ShowToast -> Toast.makeText(this, event.msg).show()
        NavigateToHome -> RouterHelper.navigate(RouterPath.HOME)
    }
}
```

---

### 2. 安全签名防篡改（SecurityInterceptor）

每个请求自动附加三个 Header：

- `X-Device-Id`：设备唯一标识（首次生成后持久化到 MMKV）。
- `X-Timestamp`：当前毫秒时间戳。
- `X-Sign`：`SHA256(deviceId + timestamp + salt)`。

后端校验签名 + 时间戳（拒绝 5 分钟前的请求），有效防止重放攻击和中间人篡改。

---

### 3. DAG 启动任务调度器（冷启动加速）

传统的 `Application.onCreate()` 中串行初始化 SDK，容易导致冷启动超过 1.5 秒。

**我们的方案**：

- 将初始化任务抽象为 `StartupTask`，标注依赖关系。
- 调度器构建 DAG 后，**并行执行**无依赖的任务（利用 `Dispatchers.IO` 线程池）。
- 配合 `StartupMonitor` 记录每个任务耗时，持续优化启动链路。

**实测效果**：串行初始化 800ms → 并行后 300ms（提升 60%）。

---

### 4. KSP 路由 + ViewBinding 兼容性解决方案（业界首创思路）

这是本项目最重要的技术攻关之一。

**问题**：`getSymbolsWithAnnotation` 在解析类时，如果类的签名中包含**尚未生成的类型**（如 ViewBinding），会静默跳过该类，导致 `@Route` 注解被遗漏。

**业界常见错误方案**：

- 将 ViewBinding 从泛型参数中移除（破坏 BaseActivity 设计）。
- 强制业务模块延迟编译（引入复杂任务依赖）。

**我们的创新方案**：

- 在 KSP 处理器中实现 **双重扫描**：
  1. 标准 `resolver.getSymbolsWithAnnotation()`。
  2. 递归遍历 `resolver.getAllFiles()`，手动检查每个声明是否有目标注解。
- 合并结果并去重，确保不漏掉任何被 ViewBinding“屏蔽”的类。

**效果**：即使类使用了 ViewBinding 作为泛型参数，路由注解依然能被正确扫描并生成注册代码。

---

### 5. 官方 SplashScreen 库 + 深色主题适配

**问题**：使用 `windowBackground` + `layer-list` 设置闪屏 Logo，在 Android 8.0+ 上自适应图标会被强制放大，导致 Logo“跳闪”。

**解决方案**：

- 迁移到 `androidx.core:core-splashscreen` 官方库。
- 在 `SplashActivity.onCreate()` 的 `super.onCreate()` 之前调用 `installSplashScreen()`。
- 使用 `windowSplashScreenAnimatedIcon` 而非 `windowBackground`，系统自动适配尺寸。

**额外配置**：为 `MainActivity` 设置 `windowBackground = ?attr/colorSurface`，消除跳转时的白色闪烁。

---

## 启动流程与性能优化

### 启动全链路（5 个阶段）

```
用户点击图标
    ↓
进程创建 (100-150ms)
    ↓
Application.onCreate()
    ├── MMKV 初始化
    ├── Logger 初始化
    ├── CrashHandler 注册
    ├── 网络模块初始化
    ├── 路由模块初始化（含 KSP 生成的路由注册）
    ├── DAG 调度器执行启动任务（并行）
    ├── 埋点系统初始化
    └── ANR WatchDog 启动 (Release)
    ↓
SplashActivity.onCreate()
    ├── installSplashScreen() → 显示官方闪屏动画
    ├── 检查登录状态
    └── 跳转 LoginActivity / MainActivity
    ↓
目标 Activity 首帧渲染 (< 50ms)
    ↓
✅ 用户可交互 (总耗时 ~450ms)
```

### 启动监控（StartupMonitor）

每个阶段自动记录耗时，最终输出耗时报告：

```
╔══════════════════════════════════════╗
║       启动全链路耗时报告              ║
╠══════════════════════════════════════╣
║ 进程创建           115ms
║ Application 创建 231ms
║ 启动任务执行         2ms
║ Activity 创建    0ms
║ 首帧渲染           32ms
╠══════════════════════════════════════╣
║ 总耗时: 380ms
╚══════════════════════════════════════╝
```

---

## 数据流转与安全

### 数据流向图

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  业务层     │      │  网络层     │      │  存储层     │
│ (ViewModel) │ ───> │ (Retrofit)  │ ───> │  (MMKV)     │
│             │      │             │      │             │
│  - 调用     │      │  - 附加签名 │      │  - 持久化   │
│    ApiService│      │  - 自动重试 │      │    Token    │
│  - 更新     │      │  - 缓存策略 │      │  - 多实例   │
│    UiState  │      │  - 监控上报 │      │    隔离     │
└─────────────┘      └─────────────┘      └─────────────┘
       ↑                    ↑                    ↑
       │                    │                    │
       └────────────────────┴────────────────────┘
                    Unified Flow + State
```

### 安全防护矩阵

| 防护层 | 技术实现 |
| :--- | :--- |
| **传输安全** | HTTPS + SSL Pinning（证书固定） |
| **请求防篡改** | `X-Sign` 签名（SHA256 + Salt） |
| **防重放攻击** | `X-Timestamp` 时间戳校验（5 分钟窗口） |
| **设备指纹** | 首次生成 `deviceId` 并持久化到 MMKV |
| **数据加密** | SQLCipher 加密 Room 数据库 |
| **代码混淆** | R8 + ProGuard（Release 包） |
| **崩溃兜底** | CrashHandler + RecoveryActivity |
| **日志脱敏** | Logger 自动对手机号/身份证脱敏 |

---

## 构建与部署

### Gradle 配置亮点

| 特性 | 说明 |
| :--- | :--- |
| **Version Catalog** | 所有依赖版本统一在 `libs.versions.toml` 中管理 |
| **Convention Plugins** | `android-library-base` / `android-feature` 统一模块配置 |
| **KSP** | 路由注解处理器 |
| **ViewBinding** | 所有 `features` 模块默认启用 |
| **BuildConfig** | 敏感密钥通过 `local.properties` 注入 |

### 构建命令

```bash
# Debug 包
./gradlew assembleDebug

# Release 包（已开启混淆 + 资源压缩）
./gradlew assembleRelease

# 清理 + 重新构建
./gradlew clean && ./gradlew assembleDebug

# 查看模块依赖树
./gradlew :app:dependencies

# 查看 KSP 生成的路由代码
./gradlew :features:feature_login:kspDebugKotlin
```

### APK 瘦身策略

| 优化手段 | 效果 |
| :--- | :--- |
| **ABI 过滤**（仅保留 `arm64-v8a`） | -40% ~ -60% |
| **资源压缩**（shrinkResources = true） | -10% ~ -20% |
| **图片转 WebP**（质量 75%） | -20% ~ -30% |
| **R8 混淆 + 移除未使用代码** | -15% ~ -25% |

**最终指标**：Release APK 可控制在 **12-15MB**（取决于业务模块数量）。

---

## 代码规范与工程化

### 命名规范

| 类型 | 规范 | 示例 |
| :--- | :--- | :--- |
| Activity | `XxxActivity` | `LoginActivity` |
| Fragment | `XxxFragment` | `HomeFragment` |
| ViewModel | `XxxViewModel` | `LoginViewModel` |
| 布局文件 | `activity_xxx.xml` / `fragment_xxx.xml` | `activity_login.xml` |
| 资源 ID | `snake_case` | `btn_login`, `tv_username` |
| 常量 | `UPPER_SNAKE_CASE` | `BASE_URL`, `MAX_RETRY_COUNT` |

### Git 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat: 新增功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式调整
refactor: 重构（不改变功能）
perf: 性能优化
test: 测试相关
chore: 构建/工具链相关
```

### 依赖方向铁律（强制执行）

```
features → middleware → service → core
features → core
features → service
core:common 绝不能依赖 core:storage（纯工具层禁飞区）
```

---

## 后续规划

### v1.1（短期）

- 集成 Compose 新业务（渐进式迁移）
- 接入 Firebase Remote Config（远程开关）
- 集成 Tinker 热修复（线上紧急修复）

### v1.2（中期）

- HTTP DNS 接入（弱网优化）
- 性能监控 APM（帧率/内存/卡顿）
- 模拟器/ROOT 环境检测（风控）

### v2.0（长期）

- 全量迁移至 Compose（UI 层完全重构）
- 支持 Kotlin Multiplatform（Android/iOS 共享逻辑）
- 自动化 UI 测试（Compose 的 `@Preview` + Robolectric）

---

## 团队协作建议

### 新成员入职 Checklist

1. 阅读本白皮书，理解模块分层和依赖方向。
2. 运行 `./gradlew build` 验证环境。
3. 安装 Demo 到真机，体验登录 → 主页 → 退出登录流程。
4. 在 `features` 目录下创建新模块，继承 `BaseActivity` / `BaseViewModel` 开始业务开发。

### 代码审查重点

| 检查项 | 说明 |
| :--- | :--- |
| 是否违反层级依赖？ | `core` 不能依赖 `features`，`common` 不能依赖 `storage` |
| 是否泄露第三方库实现？ | 业务层不应直接 import `MMKV` 或 `Retrofit` 的具体类 |
| 是否硬编码密钥？ | 密钥必须通过 `BuildConfig` 注入，严禁写在源码中 |
| 是否捕获异常但不处理？ | `catch` 块必须有 UI 反馈（Toast / State.Error） |

---

## 附录

### A. 常用工具类速查

| 类名 | 常用方法 |
| :--- | :--- |
| `KVStorage` | `putString()`, `getString()`, `clear(Scope.USER)` |
| `Logger` | `d()`, `e()`, `i()`, `w()` |
| `RouterHelper` | `navigate(path)`, `getService(interface)` |
| `NetworkUtil` | `isConnected()`, `isWifi()`, `isProxyEnable()` |
| `EncryptUtil` | `sha256()`, `md5()`, `base64Encode()` |

### B. 路由路径常量（RouterPath）

| 常量 | 目标页面 |
| :--- | :--- |
| `RouterPath.Login.LOGIN` | `/login/LoginActivity` |
| `RouterPath.Main.HOME` | `/main/MainActivity` |
| `RouterPath.WebView.WEB` | `/web/WebViewActivity` |

### C. 环境配置（gradle.properties）

```properties
# 生产环境 Base URL
BASE_URL=https://api.example.com
# 调试环境 Base URL（开发时使用）
BASE_URL_DEBUG=https://dev-api.example.com
```

---

## 致谢与维护

本项目凝聚了 Android 生态的最佳实践和模块化架构的精髓。

**项目负责人**：[kyrach]  
**联系方式**：[kyrach@163.com]  
**项目地址**：[https://github.com/kyrach-m/Sample](https://github.com/kyrach-m/Sample)

---

*最后更新：2026-06-29*
