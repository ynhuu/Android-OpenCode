# OpenCode Android

OpenCode Android 是使用 Kotlin 开发的 Android 客户端，通过 Android System WebView 展示 OpenCode 前端，连接用户配置的 OpenCode 后端。

APK 不内置 Web 前端。首次启动时，应用从本仓库的 GitHub Releases 下载前端，并在安装前请求用户确认。前端与 APK 分别更新。

## 安装与使用

### 环境要求

- Android 8.0 及以上版本。
- 较新的 Android System WebView。
- 手机可访问的 OpenCode 后端。
- 首次安装前端时需要联网。

### 安装步骤

1. 从 [GitHub Releases](https://github.com/ynhuu/Android-OpenCode/releases) 下载 APK，按系统提示允许相应来源安装应用。
2. 首次启动需联网下载前端并确认安装；首次拒绝或下载失败时，可点击「尚未安装前端」页面重试。
3. 配置后端连接。后续前端可离线打开，但消息和终端等功能仍需要连接后端。

在前端中配置手机可访问的 OpenCode 后端地址。手机上的 `localhost` 指向手机本身，不能用来访问电脑上的后端。

应用启动时检查前端更新，普通后台返回不检查。检查失败仍可使用已安装前端。更新成功后会重新打开首页，**更新前请保存未发送内容**。前端更新不会升级 APK，APK 更新需另行安装。

覆盖安装需使用同一签名的新版本。卸载或清除应用数据会删除连接、主题和已下载的前端。使用 debug 签名的测试包仅供测试。

### iOS／iPadOS 使用方式

iPhone 和 iPad 无需安装 APK，可使用网站前端：

1. 在 **Safari** 中打开 [opencode.olonet.cn](http://opencode.olonet.cn)。
2. 添加手机可访问的 OpenCode 后端地址并连接；网站仅提供前端，不提供公共后端，手机上的 `localhost` 也不是电脑。
3. 如需独立窗口使用，点击 Safari 的「分享」→「添加到主屏幕」，再从主屏幕图标打开。若系统显示「作为 Web App 打开」选项，可启用。

**安全提示**：当前入口使用 HTTP，不保证离线缓存或其他需要安全上下文的功能；不要通过不可信网络传输敏感凭据或内容。

### 版本说明

实际发布版本以 [Release 页面](https://github.com/ynhuu/Android-OpenCode/releases) 为准，不一定与当前源码一致。

| 发布物 | 版本 | 版本代码 | 状态 |
| --- | --- | --- | --- |
| Android APK | 0.1.0 | 1 | 已发布；本次不变 |
| Android 前端 | 2.0.29-custom.4 | 20294 | GitHub Release |
| 网站前端（试验版） | 2.0.30-custom.5-test | 20295 | 网站试用，不是 GitHub Release |

Android APK 与前端分开更新。本次发布只更新前端，已安装 APK 的用户无需重装；应用启动后按提示安装前端更新。前端补丁基于官方 OpenCode 2.0.29，定制差异见[迁移指南](docs/frontend/前端修改与迁移指南.md)和[完整补丁](docs/frontend/前端修改与迁移指南.patch)。

主要移动端定制包括：四标签导航、会话列表触摸排序、终端辅助键与触摸滚动；隐藏键盘按钮会清除 Ctrl 状态并让输入框失焦，不向终端发送字符。终端选字按钮提供当前可见内容的静态文字层，便于使用浏览器原生选择和复制；终端长按仍保留 Ghostty 原生粘贴菜单。Android／iOS 真机上的系统选字、复制和粘贴表现仍待验收。

同时保留 OpenCode 2.0.29 的模型选择、待处理消息、附件排布、提问面板、会话用量、未读状态及移动时间线等上游更新。

### 通知

- Android 13 及以上首次加载前端时会申请通知权限；拒绝后可在系统设置中开启。
- 通知沿用前端的分类开关和去重规则；应用在前台时抑制普通通知。页面仍运行时，点击通知会执行原有会话回调；页面重载或进程重建后，仅恢复通知生成时的页面地址，不保证定位到对应会话。
- 不提供后台保活或推送；锁屏、进程退出或长时间后台后不保证收到新通知。

## 开发构建

需要 JDK 17+ 和 Android SDK 35。使用 Android Studio 打开项目，或配置 `JAVA_HOME`、`ANDROID_HOME` 后执行：

```bash
./gradlew :app:assembleRelease :app:lintRelease :app:testReleaseUnitTest
```

Release APK 默认生成在 `app/build/outputs/apk/release/app-release.apk`。按 `OpenCode-<versionName>.apk` 命名后放在 `apk/` 目录，例如当前源码版本：

```bash
mkdir -p apk
cp app/build/outputs/apk/release/app-release.apk apk/OpenCode-0.1.0.apk
```

`apk/`、`updates/` 和 `scripts/` 不纳入 Git。全新克隆可直接使用 Gradle 构建，不依赖本机辅助脚本。

### 构建签名

Release 默认使用本机 debug 签名。正式分发应配置并持续使用同一签名密钥；不要提交密钥或密码。

自定义签名通过以下环境变量配置：

- `OPENCODE_KEYSTORE`
- `OPENCODE_STORE_PASSWORD`
- `OPENCODE_KEY_ALIAS`
- `OPENCODE_KEY_PASSWORD`

不同开发机的 debug key 通常不同，不能假定各自构建的 APK 可以互相覆盖安装。

### 辅助工具

可复用工具位于纳入 Git 的 `tools/`。Python 脚本需要 Python 3，通知测试需要 Bun；这些工具不是 Gradle 构建的前置条件。

- `sync_dist.py`：从指定前端 dist 复制启动图标。
- `package_update.py`：生成 `dist.zip` 和带 SHA-256 的更新清单。
- `verify_apk.py`：检查 APK 未捆绑前端资源或原生库。
- `rebuild_frontend_patch.py`：根据官方源码 ZIP 和定制源码重新生成完整补丁。
- `notification_bridge.test.ts`：用 Bun 验证标准通知兼容桥接、权限状态和点击回调。

前端路径使用显式参数或 `OPENCODE_DIST`，例如：

```bash
python3 tools/sync_dist.py /path/to/packages/app/dist
python3 tools/verify_apk.py apk/OpenCode-0.1.0.apk
```

通知检查工具另需 Bun：

```bash
bun test tools/notification_bridge.test.ts
```

## 前端更新

前端从本仓库最新的 GitHub Release 下载：

- [manifest.json](https://github.com/ynhuu/Android-OpenCode/releases/latest/download/manifest.json)
- [dist.zip](https://github.com/ynhuu/Android-OpenCode/releases/latest/download/dist.zip)

前端版本代码独立于 APK 版本代码。每个标记为 latest 的 Release 都必须同时提供以上两个资源，即使该次只更新 APK。

## 安全提示

- 更新下载仅使用 HTTPS，最多允许 5 次 HTTPS 重定向，并校验 ZIP 摘要和解压路径。
- SHA-256 不能防止清单和 ZIP 被同时替换，仍需保护发布账号和资源。
- 应用允许 HTTP 后端连接；敏感连接应使用可信 HTTPS 或安全网络。

## 前端定制与迁移

前端维护资料集中在 `docs/frontend/`，不包含前端源码或构建资源：

- [前端修改与迁移指南](docs/frontend/前端修改与迁移指南.md)：当前功能、构建与迁移约束。
- [完整前端补丁](docs/frontend/前端修改与迁移指南.patch)：相对官方 OpenCode 2.0.29 的定制差异。

更多面向代码代理的项目约定见 [`AGENTS.md`](AGENTS.md)。
