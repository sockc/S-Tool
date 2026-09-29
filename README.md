# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## V1.3.4 通用保护菜单与任意 App 选择器

V1.3.4 将首页“通用保护”改成菜单式入口，不再把大量 App 开关直接堆在首页。

### 首页通用保护

首页只显示：

- 定位保护 >
- 截图隐私 >

每个菜单会显示当前状态和已选择 App 数量，例如：

- 已开启 · 已选择 6 个 App
- 已关闭 · 已选择 3 个 App

点击后进入独立 App 选择器。

### 任意 App 选择器

选择器直接读取手机已安装应用，不再局限于 S Tool 已有专属 Hook 的应用。

支持：

- 搜索应用名称或包名
- 默认仅显示用户应用
- 可手动显示系统应用
- 全选当前筛选结果
- 清空当前保护类型的全部选择
- 每个 App 显示最近注入状态
- 定位保护与截图隐私分别独立保存选择

S Tool 自身、Android 核心进程、SystemUI 和系统设置继续永久排除。

为了完整读取已安装应用，V1.3.4 声明 `QUERY_ALL_PACKAGES`。该能力适合当前侧载/自用场景；如果未来发布到 Google Play，需要另外评估商店对该权限的政策要求。

### 配置兼容

V1.3.3 已选择的 App 会继续保留。新选择的任意 App 以动态包名配置保存，例如：

```
universal_privacy.location.app.com.example.app=true
universal_privacy.screenshot.app.com.example.app=true
```

ConfigProvider 协议升级到 v5，会把这些动态 App 配置一起提供给目标进程 Hook。

### LSPosed Scope

在 S Tool 里选中某个 App 只代表“对该 App 启用保护配置”。目标 App 仍然必须在 LSPosed 的 S Tool 作用域中勾选，S Tool 才能真正注入。

### 稳定性

V1.3.2 起的 Hook 延迟实例化、单 Hook 故障隔离，以及 V1.3.3 的包名分发和注入诊断全部继续保留。

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

当前开发版本：V1.3.4
