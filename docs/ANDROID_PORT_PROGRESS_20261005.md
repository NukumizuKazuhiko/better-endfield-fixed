# Android 移植进度快照（2026-10-05）

> 分支 `codex/android-thirdparty-alignment` @ `5e62a34`；`versionCode 30402` / `versionName 3.4.2`。
> 上游基线 `upstream/main` = `e8c1246`（v3.5.0）；共同基线 `9b1e895`。
>
> **口径**：每个功能块分三档独立记，**不合并**——
> **① 代码**（文件在不在）、**② 构建**（能不能编译进 Android 目标）、**③ 设备**（有没有真机证据）。
> 「构建通过」不构成「能用」的结论。

## 0. 总览

| # | 功能块 | ① 代码 | ② 构建 | ③ 设备证据 | 归属 |
| --- | --- | --- | --- | --- | --- |
| 1 | 第一人称（含陀螺仪接入） | 已落地 | 通过 | 部分（3.4.4 报告已验；陀螺仪校准未闭环） | 我方扩展 |
| 2 | 自由镜头 + VMD **相机**轨迹 | 已落地 | 通过 | 已验（导入路径已接通） | 双方共有 |
| 3 | 悬浮窗 UI（透明度/吸附/关闭全部模型） | 已落地 | 通过 | 已验 | 我方扩展 |
| 4 | 自定义模型（体型/骨骼别名/贴图/通用匹配） | 部分 | 通过 | BEM 1.3 参数已验，其余未验 | 双方 |
| 5 | 三方模块宿主（导入→dlopen→HTTP 桥） | 全链路已落地 | 通过 | **仅独立测试程序**，游戏内零验收 | 我方新增 |
| 6 | Hook 链 / 动作诊断 | 部分 | 通过 | 未验 | 双方 |
| 7 | MMD 播放（作品库 + 导演 + 相机路径） | **0 文件** | — | 无 | 上游已有，待移植 |
| 8 | EIEM 身体动作（DirectVmd） | **0 文件** | 编译面已实测通过 | 无 | 上游已有，待移植 |
| 9 | 本地音轨时间轴 | **0 文件** | — | 无 | 上游已有，待移植 |
| 10 | 数据与版本资源（F 块） | 未开始 | — | 无 | 双方 |

## 1. 已落地（代码 + 构建 + 设备三档齐全）

**第一人称**：`native/modules/camera/first_person_*.{h,inc}` 共 **31 件**（我方独有，上游无此集合），加 Android 侧 `android/app/src/main/cpp/modules/camera/first_person_look_probe.{cpp,h}`（5,391 B）、`GyroscopeController.java`（12,688 B）、`FirstPersonPage.kt`（14,651 B）。
证据：`assembleDebug` + `assembleDebugAndroidTest` BUILD SUCCESSFUL（本次实跑），APK 79,769,895 B / SHA-256 `e7a36a76…a6a2` / 834 个 `.behw`。
遗留：陀螺仪灵敏度标定（`PIXELS_PER_RADIAN`、像素→百分比换算）尚未在真机闭环。

**自由镜头 + VMD 相机轨迹**：`free_camera_runtime.inc`（含 `LoadVmdCamera`，64 MiB 上限、30 fps、相机段解析）、`camera_follow.h`（两端都有）。UI 门控见 `OverlayFeatures.java` 的 `vmdCamera` 字段（要求 `CAMERA_FREE && CAMERA_VMD_IMPORTED`）。
**边界**：这是**相机轨迹**回放，与 MMD 骨骼动作（第 8 块）是两条独立链路，不可混为一谈。

**悬浮窗**：`GameOverlay.java`（`autoSnap` / `snapToEdge()`）、`OverlayFeatures.java`、`OverlayControls.kt`、`OverlayPanel.kt`、`OverlaySurface.kt`、`OverlayTheme.kt`、`OverlayViewOwners.java`、`FloatingHandle.kt`。

**自定义模型（部分）**：已落地 `bem.cpp`（骨骼别名作用域）、BEM 1.3 体型参数、通用匹配（67 条件宿主测试）、`android_mesh_builder.cpp`、`world_resource_adapter.inc`。
缺口：低峰值加载、场景原网格身份与阴影代理、分批贴图上传、场景热切换与原版恢复。

**三方模块宿主（`35e92c8`）**：`ThirdPartyModuleStore/Package/Materializer/Activity(s).java|kt` + `native/shared/third_party_modules/third_party_host.cpp` + 宿主测试 `native/tests/third_party_host_android/`。链路已逐行核实：导入 ZIP → 私有目录 → ARM64 `dlopen` → 本地 HTTP 桥（`127.0.0.1` 已在 `module_network_security.xml` 白名单）。
**未验**：LSPosed 框架服务真实 ZIP 导入、游戏进程 `dlopen`、网页桥端到端、Release 签名环境。

## 2. 待移植（上游已有实现，我方 0 文件）

差异口径：`git ls-tree -r --name-only HEAD/upstream -- <dir>` 直接 `comm` 对比文件清单。

| 块 | 上游文件 | 我方状态 |
| --- | --- | --- |
| MMD 原生 | `mmd_library.h`、`camera_path.h`、`camera_file_worker.h`、`mmd_director_runtime.inc`、`character_motion_runtime.inc` | 全部缺失 |
| MMD Java 引擎 | `MmdInstaller`、`MmdImportArchive`、`MmdImportPlan`、`MmdImportSession`、`MmdInstalledResources`、`MmdAudio`、`MmdVmdParser`（7 件，UI 无耦合） | 全部缺失 |
| MMD 页面 | `MmdLibraryActivity`、`MmdImportActivity`（View/XML，五语言资源） | 缺失，须用 Compose 重写 |
| EIEM | `native/modules/camera/eiem/**` **54 文件 / 1,546,488 B**（AGPL-3.0，`LICENSE.EIEM` + `UPSTREAM.md`） | 0 文件 |
| motion 数据层 | `native/shared/motion/{vmd,character_pose,character_mapping,pose_lease_registry}.h`、`native/shared/host/pose_lease.cpp` | 整目录缺失 |
| 本地音轨 | `native/modules/music/local_track.inc`、`android/app/src/main/cpp/core/local_music_android.{cpp,h}` | 缺失（我方 `music` 模块只有 `module.cpp`/`music_bank.h`/`omni_pcm_abi.h`） |
| 输入 | `native/shared/input/hotkey.h` | 缺失 |
| 构建接线 | `android/app/src/main/cpp/core/jni_binding.h`、`runtime_status.h` | 缺失（我方有 `panel_commands.cpp`） |

**编译可行性（已实测，非推断）**：补齐上游兼容层缺件后，NDK r27c clang `--target=aarch64-linux-android21 -U_WIN32 -fno-char8_t -std=c++20 -fsyntax-only` 编译真实 TU：`eiem_body.cpp` + `eiem_slot0..3.cpp` **5/5 OK**；MMD/motion 8 个头 **8/8 OK**。
→ **该块没有已证实的编译阻断**，属定向文件级移植。

兼容层缺件（我方无、上游有）：`android_frame.{cpp,h}`、`android_camera.h`、`loaded_il2cpp.h`。其中 `loaded_il2cpp.h` 被 `eiem/compat/android_slot.inc` 以**相对路径**引用，必须落在 `native/shared/android_compat/loaded_il2cpp.h`，加 `-I` 无效。

## 3. 已排除的阻断与不移植项

1. `LONG` 未定义 → **已修**（`android_win32.h`）。修复前后 NDK clang 实测：`mmd_overlay_protocol.h` 5 error → 0 error。
2. `module.cpp` 顶部 include 无 `__ANDROID__` 守卫：仅在把 Windows 专有头拖进共享 TU 时成立；eiem 独立 TU 不受影响。
3. **不移植**：Windows 伴生悬浮窗 `native/modules/camera/overlay/{main.cpp, mmd_overlay.manifest, mmd_overlay.rc}`、`mmd_overlay_protocol.h`、`HookInlineScan`。
4. 上游**无 `.github/`（无 CI）**，其 Android 能力只有作者自述，**不得作为验收依据**。

## 4. 采样口径告诫

`eiem/**` 的 `.h` **不能**按单头 `-fsyntax-only` 逐个判定：它是「按固定顺序 include 的片段集合」，单测会因缺前置声明报 `unknown type name 'VmdVec3'/'Quat'`（实测 34 头中 21 个此类假失败）。
**唯一有效门禁 = 编译 `eiem_slot*.cpp` / `eiem_body.cpp` 真实 TU**，外加设备侧 `eiem/compat/tests/{android_slot_tests,android_multislot_tests}`（含 4 份 fixture VMD）。

## 5. 下一步（顺序不变）

1. 三方模块设备闭环（代码已在，路径最短）
2. MMD/EIEM **原生层**（eiem + motion + pose_lease + director + 本地音轨）→ 先过 ARM64 编译 + 宿主测试
3. MMD Java 引擎 + Compose 页面（先出键位对照表，避免存量配置漂移）
4. 模型剩余链路
5. Hook 诊断复核 + 设置页逐页回归
6. 数据资源单独提交
7. 全面 Android 验收 + Release 签名环境

---

*本文是**状态快照**；要做的事、合同与执行顺序见 `ANDROID_UPSTREAM_SYNC_PREP_20261005.md`。*
