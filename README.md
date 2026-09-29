# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## V1.2.1 管理面板

V1.2.1 在管理面板基础上加入分层子功能控制：

- 查看跨进程配置是否可用
- 查看已启用 Hook 数量
- 查看目标 App 安装状态和版本
- 每个 Hook 独立总开关
- 中国联通、高德、淘宝、美团、拼多多支持展开子功能逐项控制
- 总开关关闭时保留子功能原设置
- 全部开启 / 全部关闭 / 恢复默认
- 修改时间诊断
- 关闭某项后，重启对应目标 App 即生效

功能开关使用 LSPosed 的 XSharedPreferences 机制。项目的 `xposedminversion` 为 93，并显式声明 `xposedsharedprefs`。如果管理页显示“跨进程配置不可用”，请先在 LSPosed 中启用 S Tool，再重新打开 S Tool。

为避免配置文件暂时不可读导致现有功能突然失效，Hook 读取不到配置时默认保持开启。

## 架构

V1.1 起使用单一 Xposed 入口 `SMainHook`。V1.2 加入 `HookConfig` 管理模块总开关；V1.2.1 进一步支持父子级配置，目标 Hook 只注册已开启的具体子功能，各模块仍保持相互隔离。

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

> 历史版本曾提交过签名材料。仅从当前分支删除文件不能清除 Git 历史中的旧对象，应视旧密钥为已泄露并更换新签名密钥。

## 版本

当前开发版本：V1.2.1
