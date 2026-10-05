# Android 上游 3.4.4 选择性移植记录（2026-10-04）

## 来源与范围

工作分支 `codex/android-upstream-344-port` 从本仓库 `d9bd612` 创建，位于独立工作树；原 `F:\bem` 的未提交改动未纳入。来源为 `Dr-hydra/Better-Endfield` 的 `main`，2026-10-04 复核到 `98dddf4`。3.4.4 的功能提交仍截至 `e180ede`；`98dddf4` 只整理创作者文档。

本分支只交付 Android 改动，不包含此前误做的桌面动作钩子和 WinUI 模型页提交。

## 已移植

| 功能 | 唯一 owner 与接线 | 行为边界 |
| --- | --- | --- |
| 悬浮窗透明度 | `ModuleSettings.OverlayAppearance` 保存并限制 0–80%；`GameOverlay` 对整个 host 应用 alpha；Compose 工具页显示滑块 | 默认不透明；按钮和面板一起变化；透明度不会代替隐藏/播放时的显示状态 |
| 拖动后靠边吸附 | Compose 按钮只报告拖动结束，`GameOverlay` 根据当前位置选左右边并布局 | 默认关闭；启用时仅改变悬浮按钮位置，展开面板仍可操作 |
| 关闭全部模型 | `BemInstaller.disableAll` 一次写回安装索引；Compose 模型页消费 `BemInstallState` | 保留包文件、外观与组件选项；运行中的游戏需重启才更新模型 |

## 第二阶段：按依赖关系接入

| Android 链路 | 本分支结果 | 边界 |
| --- | --- | --- |
| 模型资源识别 | 从模型资源名安全剥离 `(Clone)#序号`；UI LOD0 以完整原网格名、索引数、角色、资源及根内路径唯一匹配渲染器；场景 LOD1 使用真实资源根内路径，分别报告缺少渲染器和网格空间不匹配 | 只接受目前可验证的精确 LOD0→LOD1 名称关系；没有上游完整的跨 LOD 原网格关系表，也没有场景实例热重绑定 |
| Android Hook 链 | 内置模块同目标 Hook 顺序执行，释放单个模块后节点透传；Broker 记录当前 owner 顺序；动作打断 Hook 缺失时不阻断其他动作 Hook | 当前仓库缺上游完整第三方模块 Host/SDK，不能宣称第三方模块接入完毕；Windows 专用调用点诊断不进入 Android |
| 第一人称 | 接入 33 角色骨骼资料，头部与发饰按当前模型根和完整路径判定；朝向使用游戏最终 CameraState（含校正与 Dutch），保留现有 Android 扩展俯仰、动画、阴影及头饰资源链 | 未接入上游局部顶点阈值与调色板重定向；角色眼位、阴影、裁切须实机核对 |
| 模型管理与存储 | Compose 页按角色筛选、可选保留本地副本、显式清理未使用数据；转换可读取框架副本；游戏启动前清理未引用的私有模型副本 | 清理只针对已识别的 UUID 代际、stage 和游戏私有 `.bem/.tmp`；索引无效、远端列举失败或删除失败均报告未完成；实际文件清理尚未在设备执行 |
| 设置文案 | 模型管理和悬浮窗新增项的标签、提示补充英、日、韩、繁中 Android 资源 | 仅覆盖本轮新增项；原有页面中未抽入资源的文案仍按现有中文显示 |

当时的依赖审计显示：本分支 BEM 解析器仍只支持 1.0–1.2，而上游 `071288c` 的 BEM 1.3（形变与体型参数）是 `5f82b3c` 低峰值加载、通用匹配和场景热切换实现的前置合同。当时直接应用 1.3 补丁到本分支的解析器、注册表和运行时全部冲突，因此未添加空的“加载速度优先”开关，也未声称完成低峰值加载或热切换。BEM 1.3 的接入结果见文末 2026-10-05 记录；分批贴图上传、原版资产保存和实例重建仍需另行接入并做设备显存、帧时与切换回滚验收。上游 `5f82b3c` 的完整通用匹配（含独立的场景原网格身份与阴影代理 owner）也仍未闭合。

HookInlineScan 辅助工具未移植。Windows 模型存储位置迁移不属于 Android 范围。

## 本阶段验收与限制

使用 `D:/CodexData/headwear-audit/all-characters/catalog-bundled` 作为固定头饰输入，运行 `:app:assembleDebug :app:assembleDebugAndroidTest -PheadwearCatalogDir=... --offline --no-daemon`，2026-10-04 最终构建成功；`verifyDesktopModelHookParity` 核对 15 条模型 Hook。D 盘 Android SDK 与 Gradle 缓存用于构建。CMake 宿主测试 `BetterEndfield.AndroidWorldBindingTests`、`BetterEndfield.GenericModelMatchingTests` 通过，后者覆盖 67 项匹配条件；第一人称资料生成检查、5 项 Python 测试和 1 项 C++ 测试通过；Android Hook 链及模拟 Dobby Broker 测试均通过。Android instrumentation 测试已编译进测试 APK，但 `adb devices -l` 没有设备，尚未运行。真实 Compose 页面、游戏内模型替换、清理、Hook 并存与相机画面均不能视为已验收。

Debug APK：`android/app/build/outputs/apk/debug/app-debug.apk`，79,204,939 字节，SHA-256 `6fe7136f2dcd6352cc9f0ac028ac614459ef0c5221575f744785d90979a983c8`。包内有 `assets/headwear-v3/manifest.tsv`、834 个 `.behw` 和 ARM64 的两个原生库；`apksigner verify --verbose` 确认 v2 签名有效。测试 APK：`android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，SHA-256 `adb78c97e521a0c93794cd8d8f1c400f658ff4eb771e369d4b0b5dc553d2dae0`。这是开发 Debug 包，不是 Release 发布物；当前独立工作树没有发布签名身份。

构建日志仍有项目既有的 SDK XML/CMake、Gradle `srcDir` 和原生弃用提示，以及部分已有的未使用函数警告；这些不来自本次新增功能，后续工具链维护单独处理。当前分支没有设备侧性能、画面或数据清理证据。

## 合入本地 Android 主线并重编 Debug 包（2026-10-04）

仅在 `codex/android-upstream-344-port` 工作树合入本地 `F:\bem` 的 `main`（`038b40f`），未修改主线工作树。合并保留了本分支悬浮窗、模型管理和上游移植功能，同时纳入主线的第一人称上下观察角度限制、头饰处理与 Android 发布流程调整。`android/README.md` 的同位置文案冲突保留了双方说明。原生编译发现主线新增的角度限制需写回相机旋转，而本分支该变量原为只读；在本分支改为可写后构建通过。

使用 JDK 21、现有 Android SDK/NDK/CMake 和 `D:/CodexData/headwear-audit/all-characters/catalog-bundled` 运行 `:app:assembleDebug -PheadwearCatalogDir=... --offline --no-daemon` 与 `:app:testDebugUnitTest -PheadwearCatalogDir=... --offline --no-daemon`，均通过；重新编译并运行 `BetterEndfield.FirstPersonMeshTests` 通过。为补齐 D 盘 Gradle 缓存中的依赖，首次准备阶段曾通过本机代理联网；最终构建与单元测试均离线运行。APK 签名验证通过（v2）。

新 Debug APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`，大小 79,217,894 字节，SHA-256 为 `3ad742562e0659496433bf699e9e2371cf70fbdc48e912cce86b8cb486d3bb9a`。`output-metadata.json` 标明 `debug`、versionCode `30401`、versionName `3.4.1`；该版本号沿用项目配置，不代表已经发布 3.4.1 新版。包内有 `assets/headwear-v3/manifest.tsv`、834 个 `.behw`、两个 Better Endfield ARM64 原生库和一个 AndroidX ARM64 库，没有其他 ABI 的原生库。本轮未运行设备测试，实机中的悬浮窗、第一人称、头饰与模型功能仍需验证。

## 悬浮窗预览闪退修复（2026-10-05）

在 HLK-AL00 设备上安装当前 Debug 包后，设置页“工具 → 预览悬浮窗”能打开面板，但点“结束预览”会使设置应用退出。崩溃日志显示 `NoClassDefFoundError: io.github.libxposed.api.XposedModule`，调用链到 `MainActivity.showOverlayPreview`。预览运行于普通设置应用进程，没有 libxposed API 类；`GameOverlay.remove()` 却无条件调用只在游戏 Hook 进程可用的 `XposedEntry.clearVolumeKeyListener()`，而预览从未注册该监听器。预览中的日志导出也可能触达 `XposedEntry.activityResultRelayReady()`。现在两处仅在游戏内面板路径调用框架入口；预览关闭只清理其实际注册的状态，预览导出使用已有分享路径。

`testOverlayPreviewLifecycleWithoutFramework` 在修改前于设备上稳定失败，异常同为 `NoClassDefFoundError`；修改后通过。重新运行 `testOverlayAppearance` 通过。设备上连续两次打开并结束预览后，设置应用 PID 保持不变、页面仍可操作；截图保存于 `D:/CodexData/bem-preview-diagnosis-20261004/preview-fixed.png`。设备没有游戏资源，因此这些证据只覆盖设置应用中的预览，不覆盖游戏内悬浮窗或模型、Hook、第一人称运行时。

重新运行 `:app:assembleDebug :app:assembleDebugAndroidTest -PheadwearCatalogDir=... --offline --no-daemon` 通过。新 Debug APK 大小 79,217,894 字节，SHA-256 为 `67a55c76e9602f9fd9e9595a501a347629bb2f3b5df4bbc1417b9d8371f003c7`；v2 签名有效，包内有资源清单和 834 个 `.behw`。设备上原有的 3.3.21 安装与当前 Debug 包签名不同，按用户授权卸载后安装新包；旧 APK 备份在 `D:/CodexData/bem-device-backups/HLK-AL00-20261004/betterendfield-3.3.21-installed.apk`，原应用数据随卸载清除。

## 3.4.2 开发分支构建（2026-10-05）

`origin` 已有 `v3.4.1`，尚无 `v3.4.2`，因此 Android 版本调整为 `versionName=3.4.2`、`versionCode=30402`；发布工作流的手动触发默认值同步为 3.4.2。版本调整不改变已发布的 3.4.1 下载入口。本分支构建与验收后推送，以工作流的 `build_only` 模式验证正式签名 APK，不创建标签或 GitHub Release。

在 JDK 21、D 盘 Gradle 主缓存和固定 834 份头饰输入下执行 `:app:assembleDebug :app:assembleDebugAndroidTest -PheadwearCatalogDir=... --no-daemon --quiet` 成功。原离线临时缓存出现 Kotlin DSL 类解析错误；D 盘主缓存联网补齐缺少的构建依赖后成功，源码无需为缓存问题改动。`output-metadata.json` 显示 3.4.2 / 30402；Debug APK SHA-256 为 `C44C6845088BA8E64D57BFF1BDCA8991DDF18CC0E9279F7686A74CB97277BA71`，含清单与 834 个 `.behw`。在 HLK-AL00 上覆盖安装主 APK 和测试 APK 后，`testOverlayPreviewLifecycleWithoutFramework`、`testOverlayAppearance` 均通过。设备仍没有游戏资源，模型、Hook 和第一人称运行时无法据此宣称通过。

## 上游 3.5.0 骨骼别名范围修复（2026-10-05）

BEM 1.2 的 `bone_name_aliases` 已声明所属资源，但原解析结果把 world 与 UI 别名合为同一组。现保留原集合供现有通用绑定流程使用，同时在解析时记录每个别名的资源。Android 场景路径查找和名称校验只接受 world 别名，详情资源读回只接受 UI 别名。场景从 UI 骨骼路径查找时，先尝试包声明的标准骨名，再尝试 world 别名，因此 UI 专属别名不会误导场景搜索。

本轮 `:app:assembleDebug -PheadwearCatalogDir=D:/CodexData/headwear-audit/all-characters/catalog-bundled --offline --no-daemon --quiet` 成功；`BetterEndfield.CustomModelBindingTests.exe --resource-alias` 通过，覆盖同一骨骼的标准名、world 别名、UI 别名和跨资源拒绝。APK SHA-256 为 `660CC35687896AE00B8B11370D2C6570E4B35BF2AC64BAB6C26292521D8969F2`，含 1 份清单、834 份 `.behw` 和两个 Better Endfield ARM64 库。设备没有游戏资源，真实 BEM 场景和详情替换仍待验收。已有 Visual Studio 17 构建目录在当前机器不可用，改用原有 Visual Studio 18 测试目录完成编译；MinHook 第三方代码仍有既有 C4701 警告。

## 上游 3.5.0 BEM 1.3 体型参数（2026-10-05）

共用 BEM 读取器和创作者工具采用上游 1.3 的参数、形变帧与负载选择合同；本仓库 Android 安装器、Compose 模型页和原生运行配置接入参数选择。模型注册表及负载缓存身份包含参数，修改滑条后不会误用旧形变。原有第一人称、头饰与 Android 资源匹配链路保留。安装索引跨包更新保存仍有效的滑条值，失效值回退包默认值；参数值必须符合声明范围与步长。

本轮执行 `py -m unittest test_bem_v13 test_bem_tasks -q`，25 项通过；`BetterEndfield.BemV13MorphTests.exe` 通过 86 项检查。固定 834 份头饰目录下执行 `:app:assembleDebug :app:assembleDebugAndroidTest -PheadwearCatalogDir=... --offline --no-daemon --quiet` 成功。HLK-AL00（Android 10）覆盖安装两份 Debug APK 后，`BemInstallerTest` 整套设备测试通过，包含参数保存、非法步长拒绝、配置生成及模型安装。APK SHA-256 为 `23CD0E33173FD6A8230AF75EA353B9AFE7FCBEF3AAD331622356B9ADD24996CB`，包内有一份头饰清单、834 个 `.behw` 和两个 Better Endfield ARM64 库。

设备没有游戏资源，形变在世界/详情场景的画面、角色切换和运行时资源峰值未实测。当前安装与选项仍在下次启动游戏时生效；模型热切换和低峰值加载不是本阶段结果。

## 自由镜头跟随人物位移（2026-10-05）

沿用上游 `camera_follow.h` 的身份与位置锚点，接入本仓库已有自由镜头循环。只在自由镜头手动操控时补偿人物位移，不改镜头方向；角色或模型身份变化、大幅跳跃、位置无效、关闭选项与退出自由镜头时重置锚点。Compose 体验页的开关进入相机配置，不影响第一人称独立相机路径。

`BetterEndfield.CameraFollowTests.exe` 通过身份重绑、正常位移、跳跃及非有限坐标检查；固定头饰目录下 `:app:assembleDebug --offline --no-daemon --quiet` 成功。HLK-AL00 Android 10 设置页实际打开、开关、检查持久配置后恢复原有关闭状态；截图保存在 `D:/CodexData/bem-follow-screen.png`。Debug APK SHA-256 为 `9C5A87ACE8A40DD5D7B601A1C4B58655CFFDAF73D506A13A0550B246420229DA`，含一份头饰清单、834 个 `.behw` 与两个 Better Endfield ARM64 库。设备无游戏资源，镜头实时跟随与场景切换画面未验收。

## 普通镜头全局 FOV（2026-10-05）

上游全局 FOV 语义接入本仓库相机模块及 Compose 设置页，独立于已有自由镜头基准 FOV 与第一人称 FOV。只在主透视相机的 Cinemachine 推送中临时改写 FOV，推送后恢复原 CameraState；原生契约缺失时关闭该功能并记日志。设置开关可独立启用相机模块，关闭后无其他相机功能时不保留活动配置。

固定 834 份头饰目录下运行 `:app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon --quiet` 成功。HLK-AL00 Android 10 的 `testCameraSettingsRoundTrip` 通过独立启用、关闭后停用及跟随选项持久化检查；设置页实际打开、切换、拖动数值并核对持久配置，最后恢复关闭状态与原默认角度。截图保存在 `D:/CodexData/bem-global-fov.png`。补齐第一人称视觉退出期间的隔离后重新构建通过，最终 Debug APK SHA-256 为 `688DA75F58B33D250ED800841AB1547B61EFA26379757DD6B978A1E41E5B8CDF`，包内有一份头饰清单、834 个 `.behw` 和两个 Better Endfield ARM64 库。设备无游戏资源，普通镜头的实际视野变化及与游戏相机模式切换的交互仍未验证。
