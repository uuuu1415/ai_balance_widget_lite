# Contributing

感谢你参与 BalanceWidget。提交代码前请先创建 Issue，说明问题、设备环境和复现步骤；新功能请先讨论使用场景和兼容性影响。

## 开发环境

- Android Studio 最近稳定版
- JDK 23
- Android SDK 35 或更高版本
- 最低 Android API 26

## 提交前检查

```powershell
./gradlew assembleDebug
./gradlew test
```

提交内容应保持职责清晰、避免无关重构，并为复杂逻辑补充解释设计原因的注释。不要提交 API Key、`local.properties`、签名文件或构建产物。

## Provider 适配器

新增平台时优先扩展统一的 `Account.effectiveRequest()` 和字段映射模型，不要在 Widget 或 Activity 中解析平台专属 JSON。需要新增认证方式或请求方法时，请同时补充文档和测试。
