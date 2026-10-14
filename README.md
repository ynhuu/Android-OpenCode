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

| 项目 | 发布版本 | 版本代码 |
| --- | --- | --- |
| APK | 0.1.0 | 1 |
| 已发布前端 | 2.0.29-custom.3 | 20293 |
| 网站前端 | 2.0.29-custom.3 | 20293 |

当前源码补丁基于官方 OpenCode 2.0.29，前端 2.0.29-custom.3 / 20293 已发布至 GitHub Releases，网站也已部署相同版本。[完整补丁](docs/frontend/前端修改与迁移指南.patch) 保留移动导航「会话、更改、文件、终端」四个等宽标签和右侧独立「…」按钮，以及会话长按、重命名、删除、移动终端滑动和轻点聚焦修复。保留上游消息提交即时显示、移动时间线布局、未读状态及项目自动授权等更新，并整合 2.0.29 的模型选择保留、待处理消息、附件排布、提问面板及会话用量显示。APK 保持 0.1.0 / versionCode 1，原签名文件不变，无需重装。定制功能与迁移约束见 [前端修改与迁移指南](docs/frontend/前端修改与迁移指南.md)。

保留会话切换列表直接滑动、长按约 350 ms 后拖动排序及菜单抑制，沿用官方目录选择器触摸滚动与残留高亮修复。已发布网站版本也包含这些定制行为。

移动终端辅助键栏最前方增加统一线条的键盘＋向下箭头图标按钮，点击后清除 Ctrl 状态并让终端输入框失焦，不发送终端输入。此改动已部署到网站并发布 Android 前端更新；软键盘收起仍需真机验收。终端触摸选择复制尚未实现。

前端 20273 增加移动视图切换后的历史阅读位置恢复和 iOS 终端键盘避让；普通终端辅助键不主动唤起键盘，单独 Ctrl 按钮保留唤起，已打开键盘时保持输入焦点。

2.0.29 源码整合检查：完整补丁在全新官方源码上应用并逐文件比对一致，正式构建与应用单元／浏览器测试共 920 项通过；全仓库 `bun run check` 命令通过，但仍报告未修改上游文件的 lint 警告。补丁涉及的 24 个 TypeScript 文件格式与零警告 `lint:changed` 通过。使用同源测试夹具模式的生产构建触摸专项 8 项通过，覆盖主页长按／重命名／删除、会话列表滑动／长按排序及菜单抑制、终端滑动和辅助键焦点；新增隐藏键盘断言在旧实现上失败、新实现上通过，终端专项 4 项通过。图标调整后完成格式、lint、全仓库检查和正式构建，未重新执行该专项。首轮未覆盖同源模式的 93 项回归为 5 通过、88 失败，抽查失败停在连接页；其余用例尚未在正确环境下复查。完整回归、性能基准对比与 Android／iOS 真机验收未完成。

2.0.29-custom.3 发布资源已在草稿及发布后下载核验，固定 latest 的 `manifest.json`／`dist.zip` 入口验证通过，更新包不包含 `.map` 文件。随附 APK 与上一版完全相同，未重新签名。

已发布 2.0.28-custom.1 的历史检查：更新包资源及固定 latest 入口核验通过；生产构建 E2E 专项回归 95 通过、14 失败，官方原版同范围 88 通过、7 失败。其余定制版失败中，6 项存在与直接标签、长按排序及菜单抑制行为冲突的测试假设，1 项为底部导航下切换会话后抽屉未关闭。此记录不代表 2.0.29 的回归结果；补丁范围外的 E2E 类型错误及真机验收仍需核实。

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
- [完整前端补丁](docs/frontend/前端修改与迁移指南.patch)：相对官方 OpenCode 2.0.28 的定制差异。

更多面向代码代理的项目约定见 [`AGENTS.md`](AGENTS.md)。
