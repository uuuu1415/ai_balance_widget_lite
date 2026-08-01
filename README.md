# BalanceWidget

一个 Android 原生主屏幕小组件，用于查看 DeepSeek、Sub2API/OneAPI 类中转站和自定义 AI API 账户余额。

本项目使用 Vibe Coding 方式开发，主要使用 `GPT-5.6 Terra` 协助完成架构设计、代码编写、重构和文档整理。所有代码仍由项目维护者负责审核、测试和发布。

项目面向个人设备使用：API Key 仅保存在本机，不经过 BalanceWidget 后端，也不会上传到第三方服务。第一版未对本地 API Key 加密，请在共享设备上谨慎使用。

## 功能

- 单账户余额 Widget
- 多账户余额汇总 Widget
- DeepSeek 官方余额接口预设
- Sub2API/OneAPI 类中转站和通用自定义接口
- 自定义 Base URL、API 路径和 JSON 字段路径
- 点号路径与数组下标，例如 `usage.today.actual_cost`、`data[0].balance`
- 手动刷新和系统周期刷新
- 网络失败时保留最近一次缓存
- Material 3 应用界面
- 浅色、深色和动态主题
- 调色盘、HEX 自定义主题色与动态颜色开关
- 任意分钟数的后台刷新间隔
- GitHub Releases 更新检查、更新说明和 APK 直接下载
- 手机、横屏和平板窗口布局适配

## 技术栈

- Kotlin
- Android Views
- Material Components for Android
- Kotlin Coroutines
- `SharedPreferences` 本地存储
- Android `AppWidgetProvider`

## 构建

使用 Android Studio 打开项目根目录，或使用 Gradle Wrapper：

```powershell
./gradlew assembleDebug
```

环境要求：

- JDK 23
- Android SDK Platform 35 或更高版本
- Android Build Tools 36.0.0 或兼容版本
- 最低 Android API 26

Debug APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`。

## 配置账户

在应用中填写：

1. 账户名称。
2. Provider 类型：`deepseek`、`relay` 或 `custom`。`relay` 用于 Sub2API、OneAPI 及类似中转站。
3. HTTPS Base URL。
4. GET 余额 API 路径。
5. Bearer API Key。
6. 余额、币种、可用状态和消费字段的 JSON 路径。

DeepSeek 类型会自动请求 `/user/balance`，并默认读取：

```text
balance_infos[0].total_balance
balance_infos[0].currency
is_available
```

如果接口返回多个币种，可以调整数组下标选择目标币种。

Sub2API、OneAPI 及其他中转站通常提供余额或用量接口，但接口路径、认证方式和 JSON 字段不完全一致。请优先查阅对应中转站的官方文档，再将 URL、API 路径和 JSON 字段映射填入应用。应用不会假定某个中转站的余额接口一定兼容。

## 架构

```text
MainActivity
    |
    +-- AccountStore       本地账户配置
    +-- SnapshotStore      最近余额缓存
    +-- BalanceClient       HTTPS 请求与统一结果
            |
            +-- Account.effectiveRequest()
                    +-- DeepSeek 预设
                    +-- 自定义 JSON 路径

SingleBalanceWidget / MultiBalanceWidget
    +-- WidgetRefresh       后台刷新和 PendingIntent
    +-- SnapshotStore       读取缓存并渲染 RemoteViews
```

Provider 专属解析应保持在数据层，Widget 和界面只消费 `BalanceSnapshot`。新增 Provider 时优先扩展配置映射，不要把平台字段写进 UI。

## 设置

设置页提供以下选项：

- 系统动态颜色开关、调色盘和自定义 HEX 主题色。
- Widget 后台刷新间隔：可输入任意非负整数分钟数，输入 `0` 表示仅手动刷新。
- GitHub Releases 更新检查、更新说明和 APK 直接下载。
- 应用版本、作者、Vibe Coding 声明和 MIT 许可证说明。

Android 会依据网络和省电策略延后后台刷新，无法保证精确到分钟。Widget 的刷新按钮始终会立即发起查询。

## 隐私与安全

- 应用没有后端服务，不会上传账户配置或 API Key。
- API Key 当前以本地未加密形式保存，这是已知限制。
- 仅允许 HTTPS Base URL。
- 不要提交 `local.properties`、API Key、签名文件或构建产物。
- 不要在 Issue、日志或截图中公开完整 API Key。

更多信息见 [SECURITY.md](SECURITY.md)、[CONTRIBUTING.md](CONTRIBUTING.md) 和 [LICENSE](LICENSE)。

## 开源许可

本项目使用 [MIT License](LICENSE) 发布。项目依赖的第三方库和服务仍受其各自的许可证、服务条款和隐私政策约束。

## 项目地址

<https://github.com/uuuu1415/balancewidget-android-uuuu1415>
