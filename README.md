# S Tool

S Tool 是一个基于 LSPosed / libxposed 的 Android 功能增强与隐私工具集合。

## V1.4.3 高德 R3

V1.4.3 重写高德处理链路，不再把 View 文本扫描当主方案。

### 三层高德架构

1. **业务语义断源**
   - `BootBizDataPreloaderImpl.canShowSplash()` 强制返回 false，走高德自己的无开屏广告分支。
   - 屏蔽 `SplashScreenServiceImpl` 的实时开屏拉取、遮罩显示与展示状态。
   - 屏蔽高德 Banner 管理/解析、后台运营消息、开屏联动数据和搜索页模板广告入口。
   - 对 `DBanner` 保留视图层兜底。

2. **AJX 列表绑定层**
   - 监听高德 AJX 文本控件的真实 setText / setAttribute。
   - 命中“探索本地”“扫街榜”“订周末”等目标后，向上定位真实 AJX 列表 item。
   - 自动挂载该列表 adapter 的 `onBindViewHolder`，以后每个 item 绑定完成即判断并隐藏。
   - 避免依赖全页持续扫描。

3. **底部导航结构识别**
   - 识别靠近屏幕底部、包含多个已知 tab 的真实导航行。
   - 隐藏“探索 / AI对话 / 路线”后重新分配剩余 tab 宽度。
   - 不再仅依赖单个 TextView 的父级猜测。

旧版 R2 的 View 指纹和文字扫描继续保留作为 fallback，避免高德版本漂移后完全失效。

### 设计参考

Gaode R3 的架构参考了开源项目 **ldxm666/MapAdKiller** 对高德 16.23 / 17.00 的公开逆向分析与验证结论。S Tool 未直接复制其 GPL-3.0-or-later 源码，而是基于公开的类/方法事实和处理思路重新实现，以保持 S Tool 自身代码结构独立。

## V1.4.2 验证码 V2 · 高德 R2 · 游戏助手 R2

V1.4.2 继续收紧 V1.4.1 modern libxposed 迁移后的稳定性，并重点修复三个已经确认的实际问题。

### 验证码自动复制 V2

验证码改为双通道：

1. **NotificationListenerService 主通道**
   - 不再依赖 Google 信息内部实现。
   - 可处理普通短信通知、RCS 通知以及其它 App 的验证码通知。
   - 第一次使用需要在系统中授予 S Tool “通知使用权”。

2. **Google 信息 Hook 备用通道**
   - 原 SMS_RECEIVED / SMS_DELIVER / ContentObserver 逻辑继续保留。
   - 即使通知监听暂不可用，仍有原链路兜底。

两条通道共用去重逻辑，2 分钟内相同验证码不会重复复制。

验证码识别新增：
- 中文：验证码、校验码、动态码、安全码等。
- 英文：verification code、security code、passcode、OTP、one-time password 等。
- 4–8 位数字。
- 形如 123-456 的分组数字。
- 在明确验证码关键词附近出现的数字字母混合码。

日志继续只记录脱敏信息，不记录验证码正文。

### 高德地图 R2

旧版单纯依赖 TextView 文字扫描的方式保留为 fallback，同时新增：

- Activity 名称
- View 真实类名
- resource-id / resourceName
- 父级 View 链
- 底部区域 / 右侧区域候选识别
- resource-id / 类名辅助匹配

命中关键 UI 时会输出 `R2 fingerprint` 日志。根据当前高德版本的真实 fingerprint，可以继续把“探索本地 / AI 对话 / 路线 / 扫街榜 / 订周末”等规则收紧到具体组件，而不是长期依赖文字猜测。

### OPPO / ColorOS 游戏助手 R2

修复一个确定存在的执行时机问题：

旧版：
```
SMainHook -> Application.attach -> OplusGameHook
                             -> 再 Hook Application.attach
```

第二层 attach Hook 很可能错过当前 attach，因此 MMKV Hook 根本没有安装。

V1.4.2 改为：
```
SMainHook -> OplusGameHook -> 直接尝试 MMKV
                         -> 未就绪时 Application.onCreate 重试
```

同时支持：
- `com.oplus.games`
- `com.coloros.gamespaceui`
- `com.coloros.gamespace`

并对包含 game / automation / assistant / black 的 MMKV key 输出诊断 key 名，不记录配置值，方便适配新版 ColorOS / OxygenOS。

### 应用增强 Scope 状态

应用增强卡片现在显示真实 LSPosed Scope 状态：

- `Scope ✓`
- `待 Scope（点此申请）`
- `Scope 服务未连接`

缺少 Scope 时可直接点击状态文字，调用 LSPosed 原生 Scope Request，不再手动进入管理器勾选。

### modern 时机清理

除游戏助手外，Airvoy 也移除了 late `Application.attach` 二次 Hook，改为直接跟踪目标 App 的 Activity.onResume / onDestroy，避免同类时机问题。

## 构建

- JDK 17
- Gradle 8.2
- Android SDK 36
- targetSdk 34
- libxposed API 101

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

## 安全说明

验证码正文、短信正文、设备标识和其它敏感内容不应写入日志或提交到公开仓库。

## 版本

当前开发版本：V1.4.2
