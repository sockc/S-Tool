# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## V1.3.1 注入入口修复与诊断

V1.3.1 修复目标 App 进程中过早读取配置的入口设计，并加入真实 LSPosed 注入状态回报。V1.3 的通用隐私引擎继续保留。

### 通用定位保护

- 默认关闭，避免升级后影响已有 App。
- 只作用于 LSPosed 已为 S Tool 勾选的目标 App。
- 自动跳过 Android 核心进程、SystemUI、系统设置和 S Tool 自身。
- 第一阶段覆盖 Android 原生 `LocationManager`：
  - `getLastKnownLocation`
  - `getCurrentLocation`
  - `requestLocationUpdates`
  - `requestSingleUpdate`
- 当前模式是“阻断定位结果/请求”；Google Fused Location、厂商定位 SDK 后续再扩展。

### 通用截图隐私

- 默认跟随“通用隐私保护”总开关，升级后总开关默认关闭。
- 阻止 Android 14+ `registerScreenCaptureCallback` 注册。
- 阻止常见 MediaStore 图片/截图目录的 `ContentObserver` 监听。
- 中国联通专属截图规则改为复用同一个 `ScreenshotPrivacyEngine`。
- 当通用截图隐私已启用时，中国联通不会重复注册同类 Hook。

### 管理界面

首页现在分为：

- 通用保护
- 应用增强

“恢复默认”会恢复每个功能自己的默认状态，而不是简单把所有开关全部打开。通用隐私保护默认关闭，现有应用增强默认保持开启。

## 注入与配置桥

`SMainHook.handleLoadPackage()` 现在只建立 `Application.attach(Context)` 入口；等目标 App 获得自己的真实 Context 后，再读取配置并安装具体 Hook。这样避免在 Application 尚未 attach 时通过 system context 读取跨进程配置。

每个目标 App 成功进入 `Application.attach` 后，会向 S Tool 的 ConfigProvider 回报一次注入时间。管理页会显示“真实注入”数量，并在已安装目标旁显示“已注入 HH:mm”或“未检测到注入”。

功能开关继续使用只读 `ConfigProvider` 作为主跨进程配置通道；legacy `XSharedPreferences` 作为兼容兜底。

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

GitHub Debug/Release 稳定签名使用以下 Repository Secrets：

- `SIGNING_KEY_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

配置完成后 Debug 与 Release APK 使用同一把固定密钥，可持续覆盖升级。

## 安全说明

不要把 JKS、keystore 密码、验证码、短信正文或其他敏感信息提交到公开仓库或发布版日志。

> 历史版本曾提交过签名材料。旧密钥应继续视为已泄露，不再用于后续发布。

## 版本

当前开发版本：V1.3.1
