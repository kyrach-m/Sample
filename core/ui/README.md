# Core UI 组件库

> `core:ui` 模块提供基于 Jetpack Compose 的 UI 组件库，所有组件均遵循 Material 3 设计规范，支持浅色/深色主题自动切换。

## 目录结构

```
core/ui/src/main/java/com/ch/core/ui/
├── component/                    # Compose UI 组件
│   ├── Badge.kt                  # 徽章组件
│   ├── BottomSheet.kt            # 底部面板
│   ├── Button.kt                 # 按钮组件
│   ├── Card.kt                   # 卡片组件
│   ├── Dialog.kt                 # 对话框组件
│   ├── InputField.kt             # 输入框组件
│   ├── ListItem.kt               # 列表项组件
│   ├── LoadingMask.kt            # 加载遮罩
│   └── StateView.kt              # 状态视图
└── theme/                        # 主题系统
    ├── Color.kt                  # 颜色定义
    ├── Theme.kt                  # 主题入口
    ├── ThemeManager.kt           # 主题管理器
    ├── ThemeSwitchDialog.kt      # 主题切换对话框
    └── Type.kt                   # 字体排版
```

---

## 一、基础组件（Basic）

页面构建的基础 UI 元素，适用于表单、列表、信息展示等常见场景。

### 1. GlobalButton — 按钮

**文件：** `component/Button.kt`

**使用场景：** 表单提交、操作触发、页面导航等。

**特性：**
- 4 种样式变体：`PRIMARY` / `SECONDARY` / `TEXT` / `DANGER`
- 3 种尺寸：`LARGE`(52dp) / `MEDIUM`(44dp) / `SMALL`(36dp)
- 内置 loading 状态（显示圆形进度条替代文字）
- 支持前导图标

**用法示例：**

```kotlin
// 主要按钮
GlobalButton(
    text = "提交",
    onClick = { /* 提交逻辑 */ }
)

// 危险按钮 + loading
GlobalButton(
    text = "删除",
    onClick = { /* 删除逻辑 */ },
    variant = ButtonVariant.DANGER,
    loading = isDeleting
)

// 文字按钮 + 小尺寸
GlobalButton(
    text = "查看更多",
    onClick = { },
    variant = ButtonVariant.TEXT,
    size = ButtonSize.SMALL
)

// 带图标的按钮
GlobalButton(
    text = "登录",
    onClick = { },
    icon = { Icon(Icons.Default.Login, null) }
)
```

---

### 2. GlobalCard — 卡片

**文件：** `component/Card.kt`

**使用场景：** 信息分组展示、内容区块容器。

**特性：**
- 圆角 16dp，无阴影（elevation = 0dp）
- 支持可选标题
- 自定义内容区域

**用法示例：**

```kotlin
// 带标题的卡片
GlobalCard(title = "个人信息") {
    Text("姓名：张三")
    Spacer(Modifier.height(8.dp))
    Text("年龄：28")
}

// 无标题卡片
GlobalCard {
    // 自定义内容
    LazyColumn { /* ... */ }
}
```

---

### 3. GlobalBadge — 徽章

**文件：** `component/Badge.kt`

**使用场景：** 未读消息计数、状态标记、角标提示。

**特性：**
- 数字计数显示，超过 `maxCount` 显示 `maxCount+`
- `count <= 0` 时自动隐藏
- 圆形背景，自适应宽度

**用法示例：**

```kotlin
// 显示未读消息数
GlobalBadge(count = 5)

// 自定义颜色和上限
GlobalBadge(
    count = 150,
    maxCount = 999,
    containerColor = MaterialTheme.colorScheme.error
)

// 搭配图标使用
Box {
    Icon(Icons.Default.Notifications, null)
    GlobalBadge(
        count = unreadCount,
        modifier = Modifier.align(Alignment.TopEnd)
    )
}
```

---

### 4. GlobalInputField — 输入框

**文件：** `component/InputField.kt`

**使用场景：** 登录表单、搜索框、信息录入等文本输入场景。

**特性：**
- 支持标签（label）和占位符（placeholder）
- 错误状态高亮 + 错误提示文字
- 支持前导/尾随图标
- 支持密码视觉转换（`VisualTransformation`）
- 自定义键盘选项

**用法示例：**

```kotlin
// 基础输入框
GlobalInputField(
    value = username,
    onValueChange = { username = it },
    label = "用户名",
    placeholder = "请输入用户名"
)

// 带错误提示的输入框
GlobalInputField(
    value = password,
    onValueChange = { password = it },
    label = "密码",
    isError = hasError,
    errorMessage = "密码不能为空",
    visualTransformation = PasswordVisualTransformation(),
    leadingIcon = { Icon(Icons.Default.Lock, null) }
)
```

---

### 5. GlobalListItem — 列表项

**文件：** `component/ListItem.kt`

**使用场景：** 设置页面列表、菜单项、信息展示行。

**特性：**
- 主标题 + 辅助文字
- 前导图标 + 尾部组件
- 可选点击事件（`onClick = null` 时不可点击）

**用法示例：**

```kotlin
// 基础列表项
GlobalListItem(
    headline = "通知设置",
    supportingText = "管理推送通知偏好"
)

// 带图标和箭头的列表项
GlobalListItem(
    headline = "个人资料",
    leadingIcon = { Icon(Icons.Default.Person, null) },
    trailing = { Icon(Icons.Default.ArrowForward, null) },
    onClick = { navigateToProfile() }
)
```

---

## 二、反馈组件（Feedback）

用于向用户展示操作结果、加载状态或系统反馈。

### 6. GlobalDialog — 对话框

**文件：** `component/Dialog.kt`

**使用场景：** 确认操作、警告提示、简单信息弹窗。

**特性：**
- 标题 + 消息内容
- 自定义确认/取消按钮文字
- Material 3 AlertDialog 风格

**用法示例：**

```kotlin
var showDialog by remember { mutableStateOf(false) }

GlobalDialog(
    show = showDialog,
    title = "提示",
    message = "确定要删除这条记录吗？",
    confirmText = "确定",
    cancelText = "取消",
    onConfirm = {
        // 执行删除
        showDialog = false
    },
    onDismiss = { showDialog = false }
)
```

---

### 7. GlobalBottomSheet — 底部面板

**文件：** `component/BottomSheet.kt`

**使用场景：** 分享面板、筛选器、操作选择菜单等从底部滑出的面板。

**特性：**
- 基于 Material 3 `ModalBottomSheet`
- 显示/隐藏状态控制
- 自定义拖拽手柄样式
- 支持完全展开模式

**用法示例：**

```kotlin
var showSheet by remember { mutableStateOf(false) }

GlobalBottomSheet(
    show = showSheet,
    onDismiss = { showSheet = false }
) {
    Text(
        text = "选择操作",
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.titleMedium
    )
    // 自定义内容
    GlobalListItem(headline = "分享到微信", onClick = { })
    GlobalListItem(headline = "复制链接", onClick = { })
}
```

---

### 8. GlobalLoadingMask — 加载遮罩

**文件：** `component/LoadingMask.kt`

**使用场景：** 数据提交、页面初始化等需要阻止用户操作的加载场景。

**特性：**
- 半透明黑色背景遮罩（alpha = 0.5）
- 中央圆形进度指示器
- 可选加载提示文字
- 加载时拦截所有用户操作

**用法示例：**

```kotlin
GlobalLoadingMask(
    isLoading = isSubmitting,
    loadingText = "提交中..."
) {
    // 页面内容
    Column {
        GlobalInputField(value = name, onValueChange = { name = it })
        GlobalButton(text = "提交", onClick = { submit() })
    }
}
```

---

### 9. GlobalStateView — 状态视图

**文件：** `component/StateView.kt`

**使用场景：** 页面级状态管理，自动处理加载中、空数据、错误重试、成功内容四种状态。

**特性：**
- 4 种状态：`LOADING` / `SUCCESS` / `ERROR` / `EMPTY`
- 加载状态：圆形进度条 + 提示文字
- 错误状态：错误信息 + 可选重试按钮
- 空状态：空数据提示
- 成功状态：显示自定义内容

**用法示例：**

```kotlin
// 搭配 ViewModel 使用
GlobalStateView(
    state = uiState,
    onRetry = { viewModel.refresh() },
    modifier = Modifier.fillMaxSize()
) {
    // SUCCESS 状态时的内容
    LazyColumn {
        items(dataList) { item ->
            GlobalListItem(headline = item.title)
        }
    }
}

// 自定义提示文字
GlobalStateView(
    state = UiState.EMPTY,
    emptyText = "暂无订单记录",
    onRetry = null
) { }
```

**关联类型：**

```kotlin
enum class UiState {
    LOADING,   // 加载中
    SUCCESS,   // 加载成功
    ERROR,     // 加载失败
    EMPTY      // 空数据
}
```

---

## 三、主题系统（Theme）

统一管理应用的颜色、字体、主题切换。

### 10. AppTheme — 主题入口

**文件：** `theme/Theme.kt`

**使用场景：** 应用根组件包裹，提供统一的颜色、排版样式。

**用法示例：**

```kotlin
// 在 Activity 或根 Composable 中使用
AppTheme {
    // 应用内容
    Scaffold { /* ... */ }
}

// 访问扩展颜色
val colors = MaterialTheme.extendedColors
Text(text = "成功", color = colors.success)
```

---

### 11. ThemeManager — 主题管理器

**文件：** `theme/ThemeManager.kt`

**使用场景：** 管理应用的浅色/深色/跟随系统主题模式，设置自动持久化。

**用法示例：**

```kotlin
// 获取当前主题模式
val mode = ThemeManager.getThemeMode()  // SYSTEM / LIGHT / DARK

// 设置深色模式
ThemeManager.setThemeMode(ThemeManager.ThemeMode.DARK)

// 判断是否为深色主题
val isDark = ThemeManager.isDarkTheme()

// 切换主题（浅色↔深色）
val newMode = ThemeManager.toggleTheme()

// 在 Activity.onCreate 中应用主题（在 super.onCreate 之前调用）
ThemeManager.applyTheme(this)
```

---

### 12. ThemeSwitchDialog — 主题切换对话框

**文件：** `theme/ThemeSwitchDialog.kt`

**使用场景：** 设置页面中的主题切换功能，提供浅色/深色/跟随系统三种选项。

**用法示例：**

```kotlin
var showDialog by remember { mutableStateOf(false) }

ThemeSwitchDialog(
    show = showDialog,
    onDismiss = { showDialog = false },
    onThemeChanged = { newMode ->
        // 主题切换完成回调（可选）
        Log.d("Theme", "切换到: $newMode")
    }
)
```

---

### 13. 颜色系统

**文件：** `theme/Color.kt`

提供 Material 3 标准颜色方案（浅色 `LightColorScheme` + 深色 `DarkColorScheme`），以及扩展语义颜色：

| 扩展颜色 | 用途 | 浅色值 | 深色值 |
|---------|------|--------|--------|
| `success` | 成功状态 | `#4CAF50` | `#81C784` |
| `warning` | 警告状态 | `#FF9800` | `#FFB74D` |
| `error` | 错误状态 | `#B3261E` | `#F2B8B5` |
| `info` | 信息状态 | `#2196F3` | `#64B5F6` |

---

### 14. 字体排版

**文件：** `theme/Type.kt`

基于 Material 3 排版系统，提供完整的 15 级文字样式：

| 层级 | Large | Medium | Small |
|------|-------|--------|-------|
| Display | 57sp | 45sp | 36sp |
| Headline | 32sp | 28sp | 24sp |
| Title | 22sp (Bold) | 16sp (Medium) | 14sp (Medium) |
| Body | 16sp | 14sp | 12sp |
| Label | 14sp (Medium) | 12sp (Medium) | 11sp (Medium) |

---

## 新增组件指南

新增组件时请遵循以下规范：

1. **文件位置：** 按使用场景放入 `component/` 对应文件，或新建文件
2. **命名规范：** 组件函数以 `Global` 前缀命名（如 `GlobalXxx`）
3. **注释要求：** 必须包含 KDoc 注释，说明使用场景、功能特性、参数含义
4. **使用示例：** 注释中必须包含 `用法示例` 代码块
5. **文档同步：** 新增组件后，同步更新本 README 文档对应章节
