# S Tool

S Tool 是一个基于 Xposed / LSPosed 的 Android 功能增强与隐私工具集合。

## V1.2.2 配置与签名稳定性

V1.2.2 解决跨进程配置和 APK 签名稳定性问题，同时保留 V1.2.1 的分层子功能控制：

- 查看跨进程配置是否可用
- 查看已启用 Hook 数量
- 查看目标 App 安装状态和版本
- 每个 Hook 独立总开关
- 中国联通、高德、淘宝、美团、拼多多支持展开子功能逐项控制
- 总开关关闭时保留子功能原设置
- 全部开启 / 全部关闭 / 恢复默认
- 修改时间诊断
- 关闭某项后，重启对应目标 App 即生效

功能开关现在以只读 `ConfigProvider` 作为主跨进程通道。模块 UI 只把设置写在自己的私有 SharedPreferences 中；目标 App 里的 Hook 通过 Binder 读取配置，不再依赖 `MODE_WORLD_READABLE`。legacy `XSharedPreferences` 仅作为旧环境兼容兜底。

为避免配置桥临时不可用导致现有功能突然失效，Hook 读取不到任何配置通道时默认保持开启。

## 架构

V1.1 起使用单一 Xposed 入口 `SMainHook`。V1.2 加入 `HookConfig`；V1.2.1 支持父子级配置；V1.2.2 将配置主通道迁移到 `ConfigProvider`，避免新 Android/LSPosed 环境下 legacy New XSharedPreferences 失效。

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

GitHub Debug/Release 稳定签名需要在仓库 Secrets 中配置：

- `SIGNING_KEY_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

配置 Secrets 后，GitHub Actions 的 Debug APK 与 Release APK 都使用同一把固定密钥。创建 `v*` Tag 后还会生成 SHA256 并创建 GitHub Release。首次切换到这把新密钥时，旧 APK 若签名不同，需要卸载一次；之后可直接覆盖更新。

## 安全说明

不要把 JKS、keystore 密码、验证码、短信正文或其他敏感信息提交到公开仓库或写入发布版日志。

> 历史版本曾提交过签名材料。仅从当前分支删除文件不能清除 Git 历史中的旧对象，应视旧密钥为已泄露并更换新签名密钥。

## 版本

当前开发版本：V1.2.2
