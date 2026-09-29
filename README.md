# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## V1.4.0 通用隐私第二阶段

V1.4.0 在 V1.3.4 的“菜单化 + 任意 App 选择器”基础上，把通用保护扩展为 6 类独立能力。每一类都可以单独开关，并分别选择任意已安装 App。

### 通用保护菜单

首页现在提供：

- 定位保护
- 截图隐私
- 剪贴板保护
- 设备标识保护
- 文件/相册保护
- 应用列表保护

每个菜单都会显示当前开关状态和已选择 App 数量。

### 剪贴板保护

对所选 App 隐藏：

- `hasPrimaryClip`
- `hasText`
- `getPrimaryClip`
- `getPrimaryClipDescription`
- `getText`

WebView 核心进程继续自动跳过，避免旧版全局剪贴板 Hook 曾经遇到的底层崩溃风险。

### 设备标识保护

对所选 App 保护常见设备标识：

- Device ID
- IMEI
- MEID
- IMSI / Subscriber ID
- SIM Serial
- Android ID
- Build Serial

当前采用稳定保护值或空值返回。部分登录、支付、风控或设备绑定功能可能依赖这些标识，因此默认关闭并建议按需启用。

### 文件/相册保护

第一阶段只处理常规 `java.io.File` 目录扫描，对以下公共目录返回空列表：

- DCIM
- Pictures
- Download
- Movies
- Screenshots

不会拦截 Android 系统文件选择器 / SAF，也不会修改真实文件。

### 应用列表保护

第一阶段限制批量枚举：

- `getInstalledApplications`
- `getInstalledPackages`

结果仅保留目标 App 自身和 Android 核心包。单个包查询和 Intent 查询暂不拦截，降低对分享、跳转、支付等正常功能的影响。

### 独立能力开关

V1.4.0 不再依赖一个隐藏的“通用隐私父开关”。6 个能力的总开关完全独立。

升级时：

- V1.3.x 的定位/截图会按升级前的实际生效状态迁移。
- 新增的设备标识、文件/相册、应用列表默认关闭。
- 旧版全局剪贴板入口退出“应用增强”。如果用户曾显式开启旧剪贴板开关，V1.4.0 会把已登记目标 App 迁移到新的按 App 剪贴板保护；隐式默认状态不会主动扩散到所有应用。

### App 选择器

所有 6 类保护继续共用同一个任意 App 选择器，支持：

- 搜索 App 名称或包名
- 默认隐藏系统应用
- 可手动显示系统应用
- 全选当前筛选结果
- 清空当前能力的全部 App
- 显示每个 App 最近 LSPosed 注入记录

目标 App 仍需在 LSPosed 的 S Tool Scope 中勾选，才能真正生效。

### 应用增强兼容

已有专属模块继续保留：

- 中国联通的定位/截图规则在对应通用保护启用时自动让位。
- 美团文件隐私、设备标识隐私在对应通用保护启用时自动让位。
- 拼多多文件隐私在对应通用保护启用时自动让位。

避免同一 API 被重复 Hook。

### 配置桥

ConfigProvider 协议升级到 v6，继续发布动态包名配置、注入记录和 Hook 结果。V1.3.2 起的延迟实例化、单 Hook 故障隔离以及 V1.3.3 的包名分发继续保留。

## 构建

项目要求：

- JDK 17
- Gradle 8.2
- Android SDK 34

本地调试：

```bash
gradle clean assembleDebug
```

## Release 签名

仓库不保存任何签名文件或密码。

GitHub Debug/Release 稳定签名使用：

- `SIGNING_KEY_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

配置完成后 Debug 与 Release APK 使用同一把固定密钥，可持续覆盖升级。

## 安全说明

不要把 JKS、keystore 密码、验证码、短信正文或其他敏感信息提交到公开仓库或发布日志。

> 历史版本曾提交过签名材料。旧密钥应继续视为已泄露，不再用于后续发布。

## 版本

当前开发版本：V1.4.0
