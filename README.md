# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## V1.3.3 按 App 通用保护与诊断优化

V1.3.3 在 V1.3.2 已修复的稳定注入入口上继续收紧架构：通用隐私可以按 App 分别控制，应用增强 Hook 按包名分发，注入记录和 Hook 失败状态也更容易判断。

### 通用隐私按 App 控制

“通用隐私保护”仍保留总开关和两个子功能：

- 通用定位保护
- 通用截图隐私

展开后，每个已登记目标 App 都可以分别选择“定位”和“截图”。未选择的 App 不启用对应通用保护。

从 V1.3.2 升级时，如果旧版“通用隐私保护”已经开启，V1.3.3 会把现有已登记目标 App 迁移为已选择，以保持旧版行为；如果旧版总开关关闭，则按 App 选择默认关闭。

### Hook 按包名分发

应用增强 Hook 不再在每个目标进程里全部实例化。现在只有包名匹配的 Hook 才会加载，例如：

- TikTok 进程只加载 TikTokHook
- 中国联通进程只加载联通相关 Hook
- 高德进程只加载 GaodeHook
- 车300 两个已登记包名都路由到 Che300Hook

通用隐私和剪贴板保护属于全局型能力，仍在 LSPosed Scope 中按配置检查。

### 注入与失败诊断

管理页现在区分：

- “尚无注入记录”：更新后还没有实际启动并记录该 App，不等于故障
- “已注入 HH:mm”：已真实进入 SMainHook / Application.attach 链路
- “⚠ Hook失败”：该 App 对应 Hook 最近一次初始化或顶层执行发生异常

顶部显示改为“已记录注入 N 个 App · 最近 App 时间”，不再用 N/总数表示健康度。

运行状态卡新增“清除注入记录”，方便清空后重新启动目标 App 做验证。

### 配置桥

功能开关继续通过只读 ConfigProvider 跨进程读取。ConfigProvider 协议升级到 v4，并增加经过调用 UID 校验的 Hook 结果回报。

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

配置完成后 Debug 与 Release APK 使用同一把固定密钥，可以持续覆盖升级。

## 安全说明

不要把 JKS、keystore 密码、验证码、短信正文或其他敏感信息提交到公开仓库或发布日志。

> 历史版本曾提交过签名材料。旧密钥应继续视为已泄露，不再用于后续发布。

## 版本

当前开发版本：V1.3.3
