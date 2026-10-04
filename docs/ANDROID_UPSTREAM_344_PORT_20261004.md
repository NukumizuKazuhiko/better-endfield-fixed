# Android 上游 3.4.4 选择性移植记录（2026-10-04）

## 来源与范围

工作分支 `codex/android-upstream-344-port` 从本仓库 `d9bd612` 创建，位于独立工作树；原 `F:\bem` 的未提交改动未纳入。来源为 `Dr-hydra/Better-Endfield` 的 `main`，本轮核对到 `e180ede`（3.4.4）。上游 Android 增量主要在 `5eeb06d`。

本分支只交付 Android 改动，不包含此前误做的桌面动作钩子和 WinUI 模型页提交。

## 已移植

| 功能 | 唯一 owner 与接线 | 行为边界 |
| --- | --- | --- |
| 悬浮窗透明度 | `ModuleSettings.OverlayAppearance` 保存并限制 0–80%；`GameOverlay` 对整个 host 应用 alpha；Compose 工具页显示滑块 | 默认不透明；按钮和面板一起变化；透明度不会代替隐藏/播放时的显示状态 |
| 拖动后靠边吸附 | Compose 按钮只报告拖动结束，`GameOverlay` 根据当前位置选左右边并布局 | 默认关闭；启用时仅改变悬浮按钮位置，展开面板仍可操作 |
| 关闭全部模型 | `BemInstaller.disableAll` 一次写回安装索引；Compose 模型页消费 `BemInstallState` | 保留包文件、外观与组件选项；运行中的游戏需重启才更新模型 |

## 其他上游增量

上游 `5eeb06d` 的包副本清理涉及模块 App、LSPosed 远程文件和游戏私有目录三处数据，且会删除文件；本轮未移植。`5f82b3c` 的热切换、低峰值加载和通用匹配跨共享模型运行时，当前产品合同要求切换后重启；`5feab85` 的第一人称角色档案与原工作区正在修改的相机链重叠。Hook 链与诊断（`368edd7`）则需要双端 ABI 和设备验收。这些功能需分别设计数据完整性与运行时门禁，不能复制到 Android 页面后视作完成。

## 验收

本轮使用 `D:/CodexData/headwear-audit/all-characters/catalog-bundled`（`coverage.json` 与 834 个 `.behw`）作为固定输入，执行 `:app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon`。编译使用 D 盘 Android SDK、D 盘 Gradle 工作缓存和本机只读依赖缓存；Dobby 源码从原工作区复制到新工作树中被忽略的工具目录。首次编译输出头饰打包数量 834、15 条模型 Hook 对齐；Debug APK 和测试 APK 均成功生成。

Debug APK：`android/app/build/outputs/apk/debug/app-debug.apk`（77,480,838 字节），SHA-256 为 `1b22dfd68a411ebeecc2733c72cfef0979dce28c9c075926421853ae4ee25f9e`。包内含 `assets/headwear-v3/manifest.tsv`、834 个 `.behw`，以及 `libbetterendfield_android.so`、`libbetterendfield_installer.so`；`apksigner verify --verbose` 确认 v2 签名有效。测试 APK 位于 `android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，SHA-256 为 `0f29f6992e6d45c8189d08aa29acf2168d0ab7fc438d3915c86a622ac186536a`。`coverage.json` 是构建输入，不要求随 APK 分发。

设置持久化与批量关闭的 Android instrumentation 测试源码已编译进测试 APK；`adb devices -l` 没有设备，测试方法和真实 Compose 界面、游戏内拖动交互尚未运行。Release 构建停在 `:app:checkReleaseSigning`：独立工作树没有发布签名身份；因此没有 Release APK，不可把 Debug 包当作发布包。

本轮构建还观察到项目既有的 Android SDK XML/CMake 版本提示、Gradle `srcDir` 弃用提示、原生 `u8path` 弃用警告，以及 `GameOverlay` 既有 WindowInsets API 弃用提示；它们未由这三项 Android 功能引入，作为后续工具链与原生代码债务单独处理，不纳入本轮功能改动。
