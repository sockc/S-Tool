# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## 当前结构

V1.1 开始使用单一 Xposed 入口 `SMainHook`，各功能仍保持独立模块，便于隔离异常和后续维护。

当前模块包括高德、联通、QQ、淘宝、闲鱼、短信验证码、剪贴板保护、OPPO 游戏助手等功能。具体兼容性取决于目标 App 版本。

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

GitHub Release 需要在仓库 Secrets 中配置：

- `SIGNING_KEY_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

创建 `v*` Tag 后，GitHub Actions 会构建签名 Release APK、生成 SHA256，并创建 GitHub Release。

## 安全说明

不要把 JKS、keystore 密码、验证码、短信正文或其他敏感信息提交到公开仓库或写入发布版日志。

> 注意：历史版本中曾提交过签名材料。仅从当前分支删除文件不能清除 Git 历史中的旧对象，应视旧密钥为已泄露并更换新签名密钥。

## 版本

当前开发版本：V1.1.0
