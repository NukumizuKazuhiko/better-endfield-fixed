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

依赖审计显示：本分支 BEM 解析器仍只支持 1.0–1.2，而上游 `071288c` 的 BEM 1.3（形变与体型参数）是 `5f82b3c` 低峰值加载、通用匹配和场景热切换实现的前置合同。直接应用 1.3 补丁到本分支的解析器、注册表和运行时全部冲突；因此没有添加空的“加载速度优先”开关，也没有声称完成低峰值加载或热切换。后续要先移植 BEM 1.3 的解析、参数选择、Android 安装与运行时合同，再接入分批贴图上传、原版资产保存和实例重建，最后做设备显存、帧时与切换回滚验收。上游 `5f82b3c` 的完整通用匹配（含独立的场景原网格身份与阴影代理 owner）也仍未闭合。

HookInlineScan 辅助工具未移植。Windows 模型存储位置迁移不属于 Android 范围。

## 本阶段验收与限制

使用 `D:/CodexData/headwear-audit/all-characters/catalog-bundled` 作为固定头饰输入，运行 `:app:assembleDebug :app:assembleDebugAndroidTest -PheadwearCatalogDir=... --offline --no-daemon`，2026-10-04 最终构建成功；`verifyDesktopModelHookParity` 核对 15 条模型 Hook。D 盘 Android SDK 与 Gradle 缓存用于构建。CMake 宿主测试 `BetterEndfield.AndroidWorldBindingTests`、`BetterEndfield.GenericModelMatchingTests` 通过，后者覆盖 67 项匹配条件；第一人称资料生成检查、5 项 Python 测试和 1 项 C++ 测试通过；Android Hook 链及模拟 Dobby Broker 测试均通过。Android instrumentation 测试已编译进测试 APK，但 `adb devices -l` 没有设备，尚未运行。真实 Compose 页面、游戏内模型替换、清理、Hook 并存与相机画面均不能视为已验收。

Debug APK：`android/app/build/outputs/apk/debug/app-debug.apk`，79,204,939 字节，SHA-256 `6fe7136f2dcd6352cc9f0ac028ac614459ef0c5221575f744785d90979a983c8`。包内有 `assets/headwear-v3/manifest.tsv`、834 个 `.behw` 和 ARM64 的两个原生库；`apksigner verify --verbose` 确认 v2 签名有效。测试 APK：`android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，SHA-256 `adb78c97e521a0c93794cd8d8f1c400f658ff4eb771e369d4b0b5dc553d2dae0`。这是开发 Debug 包，不是 Release 发布物；当前独立工作树没有发布签名身份。

构建日志仍有项目既有的 SDK XML/CMake、Gradle `srcDir` 和原生弃用提示，以及部分已有的未使用函数警告；这些不来自本次新增功能，后续工具链维护单独处理。当前分支没有设备侧性能、画面或数据清理证据。

## 合入本地 Android 主线并重编 Debug 包（2026-10-04）

仅在 `codex/android-upstream-344-port` 工作树合入本地 `F:\bem` 的 `main`（`038b40f`），未修改主线工作树。合并保留了本分支悬浮窗、模型管理和上游移植功能，同时纳入主线的第一人称上下观察角度限制、头饰处理与 Android 发布流程调整。`android/README.md` 的同位置文案冲突保留了双方说明。原生编译发现主线新增的角度限制需写回相机旋转，而本分支该变量原为只读；在本分支改为可写后构建通过。

使用 JDK 21、现有 Android SDK/NDK/CMake 和 `D:/CodexData/headwear-audit/all-characters/catalog-bundled` 运行 `:app:assembleDebug -PheadwearCatalogDir=... --offline --no-daemon` 与 `:app:testDebugUnitTest -PheadwearCatalogDir=... --offline --no-daemon`，均通过；重新编译并运行 `BetterEndfield.FirstPersonMeshTests` 通过。为补齐 D 盘 Gradle 缓存中的依赖，首次准备阶段曾通过本机代理联网；最终构建与单元测试均离线运行。APK 签名验证通过（v2）。

新 Debug APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`，大小 79,217,894 字节，SHA-256 为 `3ad742562e0659496433bf699e9e2371cf70fbdc48e912cce86b8cb486d3bb9a`。`output-metadata.json` 标明 `debug`、versionCode `30401`、versionName `3.4.1`；该版本号沿用项目配置，不代表已经发布 3.4.1 新版。包内有 `assets/headwear-v3/manifest.tsv`、834 个 `.behw`、两个 Better Endfield ARM64 原生库和一个 AndroidX ARM64 库，没有其他 ABI 的原生库。本轮未运行设备测试，实机中的悬浮窗、第一人称、头饰与模型功能仍需验证。
