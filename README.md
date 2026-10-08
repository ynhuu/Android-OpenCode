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

### 版本说明

实际发布版本以 [Release 页面](https://github.com/ynhuu/Android-OpenCode/releases) 为准，不一定与当前源码一致。

| 项目 | 发布版本 | 版本代码 |
| --- | --- | --- |
| APK | 0.1.0 | 1 |
| 已发布前端 | 2.0.25-custom.1 | 20251 |

本次前端基于官方 OpenCode 2.0.25 应用仓库中的 [完整补丁](docs/frontend/前端修改与迁移指南.patch) 构建，保留会话长按、重命名、删除交互修复，以及移动终端滑动和轻点聚焦修复。APK 保持 0.1.0 / versionCode 1，文件不变。定制功能与迁移约束见 [前端修改与迁移指南](docs/frontend/前端修改与迁移指南.md)。

前端 20251 使用官方目录选择器触摸滚动实现，移除重复的滚动依赖补丁，保留触摸后的残留高亮修复。

### 通知

- Android 13+ 首次加载前端时申请系统通知权限；拒绝后可在系统设置中的 OpenCode 通知页面开启。
- 通知无需修改或升级现有前端。APK 提供标准 `Notification` API 的兼容桥接，沿用原前端的通知发送逻辑。
- 完成和错误通知保留原前端的分类开关、去重及「只通知本机已打开标签的会话」限制。原版 2.0.24 的权限开关未接入发送事件，本应用未新增该行为。
- 应用在前台时抑制普通通知；切到后台时移交输入焦点，避免前端误判页面仍有焦点。
- 页面仍在运行时，点击通知执行原有会话回调。页面重载或进程重建后，只恢复通知生成时的页面地址，不保证进入产生事件的会话。
- 不提供后台保活或推送，锁屏或长时间后台后不保证产生新通知。

<details>
<summary>通知功能的历史验证记录</summary>

2026-10-08，在 Android 12L 手机、20242 前端、关闭 WebView 调试接口的 Release 构建中，已验证焦点修复后真实完成通知进入系统通知栏。验证时使用的临时日志已从最终源码移除。此前点击回调验证通过；Android 13+ 授权和进程重建后的点击尚未实测。

此记录不代表所有已发布 APK 和前端组合均已验证。

</details>

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

前端版本代码独立于 APK 版本代码。发布新前端时应递增前端版本代码；每个标记为 latest 的 Release 都必须提供以上两个资源，即使只更新 APK。

## 安全提示

- 更新下载仅使用 HTTPS，最多允许 5 次 HTTPS 重定向，并校验 ZIP 摘要和解压路径。
- SHA-256 不能防止清单和 ZIP 被同时替换，仍需保护发布账号和资源。
- 应用允许 HTTP 后端连接；敏感连接应使用可信 HTTPS 或安全网络。

## 前端定制与迁移

前端维护资料集中在 `docs/frontend/`，不包含前端源码或构建资源：

- [前端修改与迁移指南](docs/frontend/前端修改与迁移指南.md)：当前功能、构建与迁移约束。
- [完整前端补丁](docs/frontend/前端修改与迁移指南.patch)：相对官方 OpenCode 2.0.24 的定制差异。

更多面向代码代理的项目约定见 [`AGENTS.md`](AGENTS.md)。
