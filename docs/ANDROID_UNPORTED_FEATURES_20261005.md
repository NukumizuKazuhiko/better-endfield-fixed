# Android 未移植功能清单（2026-10-05，上游 v3.5.1）

> 对象：上游 `upstream` = `Dr-hydra/Better-Endfield`（只读）→ 本轮 fetch 后 `upstream/main` = `3510fa7`（**v3.5.1**，2026-10-05 20:54）。
> 我方 = `codex/android-thirdparty-alignment` @ `5e09128`（3.4.2 / 30402）。
> 本文**只做清点**，不含合并动作。**取代** `ANDROID_UPSTREAM_SYNC_PREP_20261005.md` 的清点部分（该文基线是旧上游 tip `e8c1246`/v3.5.0，已落后 6 个提交）。

## 0. 摘要

1. **上游这批改动"未移植"的判定是成立的，但主要不是 Android 落后，而是同一个 Android 编译单元被上游重写了。** 最大缺口是 `native/modules/custom_model/module.cpp`：它在**我方 Android 编译面内**（`android/app/src/main/cpp/CMakeLists.txt:124`），我方相对基线 `+108/−55`，上游 `+1448/−206`。上游把「异步/低峰值模型加载 + 模型内容同一性 + 通用模型匹配 + Android LOD 关系表」塞进了这个文件。
2. **上游自己这批改动没有任何构建或实机证据。** 上游仓库**不存在 `.github/`**（`git ls-tree upstream/main -- .github` 为空），且 `native/modules/custom_model/model_runtime_notes.md` 自述：*"No APK build/install/game launch, branch, commit, push, clean or directory removal was performed."* ⇒ 合并成功 ≠ 能用，必须自验。
3. 未移植面按编译面三分：**A 我方不编译（Windows 桌面专有，只记不移）／B 我方编译需处理（真缺口，7 个文件）／C 数据与资源与版本**；另有 **D 反向我方领先上游**（合并时不得被覆盖）。

## 1. 基线与量化（可复现）

```bash
git fetch upstream --prune
BASE=$(git merge-base HEAD upstream/main)      # = 9b1e895  Expand BEM v1.1/v1.2 runtime and tooling
git rev-list --count $BASE..upstream/main      # 上游领先 = 39
git rev-list --count $BASE..HEAD               # 我方领先 = 68
```

| 项 | 值 |
| --- | --- |
| 共同基线 | `9b1e895`（2026-09-26） |
| 上游 HEAD | `3510fa7` v3.5.1（2026-10-05 20:54） |
| 我方 HEAD | `5e09128`（2026-10-05 22:52） |
| 上游领先提交 | **39**（含 1 个 merge） |
| 我方领先提交 | **68** |
| 上游改动文件（全部） | 3418（其中 `research/` 2718、`docs/` 162 —— 非功能面） |
| 上游改动文件（功能面，剔除 research/docs） | **526** |
| 我方改动文件（功能面） | **356** |
| **纯未同步（上游改、我方自基线未动）** | **330** = 我方有旧版 113 + 上游新增 217 |
| **双方都改（冲突面）** | **196** |
| **我方独有（上游未动）** | **160** = 独有独改 138 + 主动删除 22 |

> 校验：`113 + 217 + 196 = 526`（上游功能面）✓；`196 + 138 + 22 = 356`（我方功能面）✓。完整差量与 gap 排名见 `ANDROID_DELTA_VS_UPSTREAM_3510FA7.md` §零 / §零点五。

三分法命令：

```bash
# ⚠ 必须带 core.quotepath=false：默认配置会给非 ASCII 路径加引号，让下面的 grep 漏掉 12 个 research/ 文件
git -c core.quotepath=false diff --name-only $BASE..upstream/main | grep -vE '^(research|docs)/' | sort > up.txt
git -c core.quotepath=false diff --name-only $BASE..HEAD          | grep -vE '^(research|docs)/' | sort > our.txt
comm -23 up.txt our.txt   # 纯未同步 330
comm -12 up.txt our.txt   # 冲突面 196
comm -13 up.txt our.txt   # 我方独有 160
```

**交叉验证（防"我方主动删除"被误报为未同步）**：`comm -23` 里 229 个"我方树不存在"的文件中，相当一部分是我方有意替换掉的 View/XML 页面（见 §4），不是缺失功能 —— 判据见 §4。

## 2. 上游平台支持与 CI 事实

- 上游**自己也在发 Android**（`android/README.md`、`android/app/**`、`.github` 为空但 `android/` 有构建脚本），提交信息含 `fix(android): …`、`feat(android): …`。所以这不是"上游只发 Windows"的处境。
- 但上游**无 CI**、且关键原生改动自述未构建 ⇒ 其 Android 能力**只有作者自述，无可复现证据**。
- 我方 CI：`.github/workflows/{android-build.yml, android-release.yml, codeql.yml}`。

## 3. 逐提交功能条目（上游领先的 39 个，按功能面归并）

| 功能面 | 代表提交 | 我方现状 |
| --- | --- | --- |
| VMD / 角色动作预览 | `e5e7dde` | 已移植（EIEM + 第一人称） |
| MMD 播放（EIEM DirectVmd） | `d1660db`、`617b197` | **已移植**（作品库/命令通道为自研） |
| Android 热更新资源同步 | `2c0cc12` | 已移植 |
| 设置热键统一 / 可选模块门控 | `3b28574` | 已移植 |
| Android 归档式 MMD 导入 | `4a13f2a` | 已移植（我方 `MmdImportPlan/Session` 自研） |
| UI：下载镜像 / 赞助 / Discord | `a4ef685`、`3601444`、`34e8513` | **未移植**（我方无赞助入口） |
| BEM 管理嵌入主导航 | `8424422` | 已移植（Compose） |
| 三方模块基础 + BEM 1.3 | `071288c` | 已移植 |
| 桌面模型/模块/MMD 本地化 | `82d60a1` | **部分未移植**（Android `values-*` 少 4 套） |
| Android 世界模型校验对齐 UI 路由 | `510e83b` | 部分（`world_resource_adapter.inc` 落后） |
| **_TickStatePerformInterrupt hook 可选化** | `8285071` | **已具备**（原判「未移植」有误）：我方 `native/modules/actions/module.cpp:183` 已挂该 hook 描述符，`:1587` 有 "hook unavailable" 退化分支；上游该提交仅 +12/−2 |
| **HookInlineScan（inline hook 目标扫描）** | `678b37c` | **未移植**（`tools/HookInlineScan`，Windows 分析工具） |
| **内建 hook 按目标链式调用 + hook 诊断** | `368edd7` | 头已移植（`HookChain.h` 我方有），`hook_diagnostics.*` **未移植**（Windows 专有） |
| **低峰值加载 / 原地换模 / 通用模型匹配** | `5f82b3c` | **未移植**（最大缺口，见 §4-B） |
| 每角色第一人称档案 + 最终朝向 | `5feab85` | **已移植**（`first_person_profiles.*` 我方有） |
| **Android 模型存储清理 / 悬浮窗透明度自动吸附 / 本地化设置** | `5eeb06d` | 部分（透明度吸附已移植；本地化缺 4 套） |
| 桌面：模型套餐放程序旁 + 模型过滤器 | `8d640eb` | **未移植**（Windows 桌面） |
| BEM 创建器扩展 / 骨别名作用域 | `df44b59`、`94ebd00` | 部分（`bem.cpp` gap 76 行；`tools/CustomModel` 少 9 件） |
| **v3.5.1：模型悬浮窗 + Android 全局 FOV 控件** | `3ef6ed3` | 全局 FOV **仅「配置期」已移植**；`GlobalFovUpdater.java` + JNI `AndroidGlobalFov` 的**运行时热下发（免重启）未移植**（实测两者我方命中均为 0）；**模型悬浮窗未移植**（Windows companion） |
| 工作区重组 + 工具链路径 | `20ebf08` | **未移植**（`scripts/`、`android/workspace.gradle.kts`、`Directory.Build.props`） |
| 发布：artifacts 按版本分组 / tag 保留 / installer-only | `aa795b4`、`d2a8fb6`、`08658a2` | **未移植**（上游 `scripts/PublishRelease.py` 体系） |
| **发布：把 release 签名 pin 到已发布 3.5.0 证书** | `3510fa7` | **不可照搬**（我方自有身份 `8CD6FDC1…8EFD`） |

## 4. 按编译面分级的未移植功能清单

### A. 我方 Android 不编译（Windows/桌面专有 —— 只记不移）

| 路径 | 为什么不移 |
| --- | --- |
| `native/modules/camera/overlay/{main.cpp,mmd_overlay.manifest,mmd_overlay.rc}` | 独立伴生进程 + `.rc` 资源（MMD 悬浮窗 Windows 版） |
| `native/modules/custom_model/overlay/{main.cpp,model_library.h,win32_overlay_window.h,model_overlay.manifest,.rc,INTEGRATION.md}` | 模型悬浮窗 companion 进程 |
| `native/modules/custom_model/model_overlay_host.h`、`model_overlay_protocol.h`、`runtime_ini_win32.h`、`model_overlay_hotkey.h` | 上游在 `custom_model/module.cpp` 里用 **`#if defined(_WIN32)`** 包住引入（实测第 19–22 行）⇒ Android 侧不编译 |
| `native/shared/host/hook_diagnostics.{h,cpp}` | 头部 `#include <Windows.h>`；进 `native/CMakeLists.txt` 的 Host 目标并链 `minhook`/`ws2_32`（Windows 库） |
| `tools/HookInlineScan/*`（Python） | 针对 x64 inline hook 位置的静态扫描工具 |
| `tools/ThirdPartyModules/echo/*` | Windows 示例三方模块（上游 `native/CMakeLists.txt` 里以 SHARED 目标构建） |
| `tools/CustomModel/CreatorProjectChecks/*`（C#） | C# 校验器 |
| 桌面 UI `ui/BetterEndfield.UI/*` | WPF（`MainWindow.xaml/.cs`，上游 +580/−65） |

> **手法**：这些**只取源码、绝不取构建条目**（上游 `native/CMakeLists.txt` 里依赖 `minhook`/`ws2_32`/`mfplat`/`.rc` 的目标一律不带进我方构建脚本）。

### B. 我方 Android **会编译**、仍是旧实现（真缺口）

> 判据：文件在我方 `android/app/src/main/cpp/CMakeLists.txt` 的编译列表里（直接列出或经 `add_subdirectory(custom_model)` 进入），且上游相对基线增量远大于我方。

| 文件 | 我方 +/− | 上游 +/− | 上游领先 | 性质 |
| --- | --- | --- | --- | --- |
| **`native/modules/custom_model/module.cpp`** | +108/−55 | **+1448/−206** | **1491** | 异步加载 / 内容同一性 / 作业泵 / 通用匹配 |
| `native/modules/camera/first_person_runtime.inc` | +433/−71 | +775/−77 | 348 | **第一人称专有 ⇒ 按铁律取我方** |
| `android/.../cpp/modules/custom_model/world_resource_adapter.inc` | +45/−22 | +268/−79 | 280 | Android 世界资源绑定 |
| `native/modules/camera/module.cpp` | +1290/−91 | +1433/−217 | 269 | 相机侧（含第一人称接线） |
| `android/.../cpp/native_bridge.cpp` | +89/−1 | +228/−42 | 180 | JNI 桥（含 v3.5.1 新增） |
| `android/.../java/.../GameOverlay.java` | +724/−379 | +785/−502 | 184 | 悬浮窗宿主 |
| `android/.../java/.../BemInstaller.java` | +154/−6 | +256/−77 | 173 | BEM 安装器 |
| `native/modules/ui/module.cpp` | +13/−2 | +119/−35 | 139 | UI 桥 |
| `android/.../cpp/core/runtime.cpp` | +53/−0 | +89/−21 | 57 | 运行时 |
| `native/modules/custom_model/bem.cpp` / `.h` | +444/−71 / +60/−4 | +515/−76 / +74/−4 | 76 / 14 | BEM 解析（近乎持平） |
| `android/.../cpp/modules/custom_model/custom_model_module.cpp` | +11/−10 | +61/−17 | 57 | 模块入口 |
| `native/modules/custom_model/generic_model_matcher.h` | +158/−0 | +215/−0 | 57 | 通用模型匹配 |
| `android/app/src/main/AndroidManifest.xml` | +12/−0 | +56/−1 | 45 | 清单 |
| `native/CMakeLists.txt` | +49/−0 | +94/−1 | 46 | 构建条目（只增不覆） |
| `android/.../java/.../BemOptions.java`、`BemInstalledResources.java`、`FrameworkSettings.java` | — | — | 少量 | 设置/选项 |
| `native/tests/*`（`custom_model_binding_tests.cpp` +933、`android_world_binding_tests.cpp` +629、`generic_model_matching_tests.cpp`） | — | — | 大 | **上游测试，便于我方验证缺口** |

**`custom_model/module.cpp` 内新增且已在 Android 编译的件**（实测引入点无守卫）：

| 新件 | 引入位置 | 平台 |
| --- | --- | --- |
| `async_loading.h` | module.cpp:8（无守卫） | Android 编译 |
| `model_content_identity.h` | module.cpp:9（无守卫） | Android 编译 |
| `generic_model_matcher.inc` | module.cpp:1858（无守卫） | Android 编译 |
| `model_asset_cache.inc` | module.cpp:3262（无守卫） | Android 编译 |
| `model_job_runtime.inc` | module.cpp:3589（无守卫） | Android 编译 |
| `android_lod_relations.generated.h` | module.cpp:17，**`#if defined(__ANDROID__)`** | Android 专有 |
| `model_overlay_host.h`、`runtime_ini_win32.h` | module.cpp:21–22，`#if defined(_WIN32)` | Windows 专有（不入我方） |

**上游自述的功能语义**（`model_runtime_notes.md`）：单一 `AsyncBemLoader`；任务持有 detached `ConstructionScope`，泵每步激活一个；纹理/几何用分帧许可；256 MiB **估算输入字节**空闲租约预算、10 s TTL、512 弱条目；内容同一性用 worker 产出的 SHA-256（选中解码内容 + 描述 + 文件租约世代 + 外观/选项）；空闲计划 10 s 过期。**Windows 租约拒绝写入，Android 依赖既有不可变导入文件世代。**

### C. 数据 / 资源 / 版本

| 项 | 我方 | 上游 | 缺口 |
| --- | --- | --- | --- |
| `android/resources/**` | 3 | **9** | 真缺 **6 件**：`manifests/model/action-manifest.json`、`manifests/voice/voice-event-media-manifest.json`、`manifests/shared/resource-manifest-report.md`、`resource-source.json`、`voice-index-android-evidence.json`、`voice-index-merge-evidence.json`。**另**：`character-names.json`／`character-presets.json`／`voice-catalog-index.json` 我方**存在但陈旧**（presets 我方 34557 行 / 2.02 MB vs 上游 35000 行 / 2.05 MB）—— 原判「缺 character-names/presets」有误 |
| `android/app/src/main/res/values-*` | 4 | **8** | 缺 `values-en`/`values-ja`/`values-ko`/`values-zh-rTW` 四套 `model_management.xml` + `overlay_settings.xml` |
| `native/modules/combat_stats/assets/avatars/` | 44 文件 | 46 | 缺 `chr_0034_typhoea.png`、`chr_0038_purrche.png` |
| `tools/CustomModel/` | 122 | 131 | 缺 `generate_android_lod_relations.py`、`catalog/chr_0038_purrche.json`、`bem_upload_report.py`、`blender_addon/**`、`skills/bem-creator/references/*`（英/中对照） |
| 版本 | 3.4.2 / 30402 | **3.5.1** | 版本号递进由我方决定 |
| 签名/配置 | 我方 `8CD6FDC1…8EFD` | `config/android-signing.local.example.properties`（pin 到 3.5.0 证书） | **不可照搬** |

### D. 反向：我方领先上游（合并时不得被覆盖）

| 能力 | 我方文件 |
| --- | --- |
| 兼容层陀螺仪/look-pad 注入 | `native/shared/android_compat/android_win32.{cpp,h}`、`android_virtual_keys.h`（含上游没有的 `AddVirtualMouseDelta`/`DrainVirtualMouseDelta`） |
| 命令通道（分帧修复） | `android/.../cpp/input_relay.cpp`、`core/panel_commands.cpp`、`NativeCommandBridge.java`、`ModuleCommandRouter.java` |
| 第一人称/陀螺仪扩展 | `first_person_look_probe.cpp`、`GyroscopeController.java`、`FirstPersonPage.kt`、`first_person_profiles.*` |
| 悬浮窗 Compose 三件套 | `OverlaySurface.kt`/`OverlayPanel.kt`/`OverlayControls.kt`/`OverlayTheme.kt` |
| MMD 作品库 | `MmdLibraryFiles/MmdImportPlan/MmdLibraryInstaller/MmdSlotFiles/MmdVmdParser/MmdPage.kt` |
| 头饰/BEM 网格 | `android/.../cpp/modules/custom_model/android_headwear_canary.cpp`、`native_mesh_layout_android.cpp` |
| CI | `.github/workflows/*`（上游无） |

## 5. 上游新增文件里"不是缺口"的那部分（交叉验证结论）

`comm -23` 里 229 个"我方树不存在"的上游新增文件，其中 48 个在 `android/app`，**绝大多数是我方有意替换的 View/XML 页面或引擎类**，不是缺失功能：

| 上游新增（`.java`，View/XML） | 我方对等物（Compose `.kt` / 自研 Java） |
| --- | --- |
| `AboutPage.java`、`EnhancementSettingsActivity.java` | `SettingsPages.kt`、`ExperiencePage.kt` |
| `BemInstallPage.java` | `BemInstallActivity.kt` + `BemInstallScreen.kt` |
| `MmdLibraryActivity.java`、`MmdImportActivity.java`、`MmdInstaller.java`、`MmdInstalledResources.java`、`MmdImportArchive.java`、`MmdAudio.java`、`MmdVmdParser.java` | `MmdPage.kt` + `MmdVmdParser/MmdLibraryFiles/MmdImportPlan/MmdLibraryInstaller/MmdSlotFiles.java`（**自研设计**） |
| `ThirdPartyModulesPage.java` | `ThirdPartyModulesActivity.kt` |
| `OverlaySettingsPage.java`（**页面**） | 我方悬浮窗 Compose（`OverlayPanel.kt`）—— 页面层可替代 |
| `OverlaySettingsClient.java`、`OverlaySettingsProvider.java`、`OverlayWritePolicy.java`、`OverlayWriteAuthorization.java`、`OverlayGeometry.java`（**引擎/通道，不是页面**） | **真缺**（我方 5 个类命中全为 0）：这是「游戏进程 → app 进程」的跨进程写通道 + provider 鉴权。我方那个 exported provider 是 `.RuntimeJournalProvider`（authority `${applicationId}.journal`，只读日志）**不是同一个物** ⇒ 原判「已由 Compose 替代」有误 |
| `GlobalFovUpdater.java` | **真缺**（我方只有配置期路径；运行时热下发未做） |
| `BemHotSwitchUpdate/Updater.java`、`BemImportRequest/Stream.java`、`RuntimeSnapshot.java`、`ThirdPartyRuntimeUpdater.java` | **已核实真缺**（我方均 0 命中） |
| `FrameworkServiceWait.java`、`ControlIcon.java` | **需复核**（页面/控件层，可能已由 Compose 替代） |
| `SponsorDialog.java` + `bg_sponsor_button.xml` + `sponsor_wechat.png` | **真缺**（赞助入口） |

> 因此 §4-B 的"真缺口"以**同一文件双方都改**为准；`comm -23` 命中的 `.java` 页面默认视为**已用 Compose 替代**。

## 6. 合并风险与最小修法

| 风险 | 判据 | 最小修法 |
| --- | --- | --- |
| `custom_model/module.cpp` 双方大改同一文件（我方 +108/−55 vs 上游 +1448/−206） | `--numstat` 双测 | **不整体覆盖**；按子系统前向移植（overlay-host 相关 Windows 段不取；async / content-identity / job-runtime / lod-relations 段取上游）。先确认我方 108 行改动在上游是否已存在 |
| `first_person_runtime.inc` 上游领先 348 行 | 同上 | **取我方**（第一人称专有子系统） |
| `android/.../cpp/CMakeLists.txt`、`native/CMakeLists.txt` 双方都改 | 同上 | **只增不覆**，改完 `git diff --name-only --diff-filter=M` 对这两个文件必须为 0（除有意为之） |
| Windows 专有目标混入我方构建脚本 | 上游 `native/CMakeLists.txt` 新增目标依赖 `minhook`/`ws2_32`/`mfplat`/`.rc` | **只取源文件，不取构建条目** |
| 合并后编译面炸裂 | 见下表实测 | 逐头 `-fsyntax-only` |
| 上游这批改动未经上游验证 | `model_runtime_notes.md` 自述未构建；上游无 CI | 我方必须自建 + 真机验证才可宣告可用 |

**编译器实测（NDK r27c，`aarch64-linux-android24-clang++ -fsyntax-only -std=c++20 -U_WIN32`）**：

| 上游新头 | 实测 | 说明 |
| --- | --- | --- |
| `async_loading.h` | **OK** | Android-clean |
| `model_content_identity.h` | **OK** | Android-clean |
| `runtime_ini.h` | **OK** | Android-clean |
| `android_lod_relations.generated.h` | **OK（需前置）** | 单独编译报 `undeclared identifier 'std'/'GenericMatching'` 是**假阳性**——它是生成片段，须在 `<span>` + `generic_model_matcher.h` 之后展开；加前置后通过 |
| `model_overlay_host.h`、`win32_overlay_window.h` 等 | 不测 | 上游已用 `#if defined(_WIN32)` 排除，本就进不了 Android |

> 已排除的坑：宿主是 Windows 时 `_WIN32` 会被预定义，必须 `--target=aarch64-linux-android24` **且 `-U_WIN32`**，否则替身层 `Windows.h` 的 `#error` 会造成全量假阳性。

## 7. 建议动作

| 优先级 | 动作 | 理由 |
| --- | --- | --- |
| **P0** | `custom_model/module.cpp` 子系统级前向移植：`async_loading.h` + `model_content_identity.h` + `model_job_runtime.inc` + `model_asset_cache.inc` + `generic_model_matcher.inc` + `android_lod_relations.generated.h`；**不取** `_WIN32` 段 | 在 Android 编译面内、缺口最大（1491 行）；头已被实测为 Android-clean |
| **P0** | 同步上游对应测试（`custom_model_binding_tests.cpp` +933、`android_world_binding_tests.cpp` +629）作为回归门 | 上游这批改动无验证，测试是我方唯一可用判据 |
| P1 | `android/resources` 6 件 + `values-en/ja/ko/zh-rTW` 4 套本地化 | 纯数据，风险低；不做会与上游配置键名漂移 |
| P1 | `world_resource_adapter.inc`（g 280）+ `native_bridge.cpp`（180） | Android 编译面内，中等缺口 |
| P2 | `combat_stats` 2 头像、`tools/CustomModel` 9 件（含 `generate_android_lod_relations.py`）、BEM `bem.cpp` gap 76 | 数据/工具，可增量 |
| P2 | UI 重写：赞助入口、Discord 链接、模型下载镜像 | 上游为 View/XML，须 Compose 重写；先做键名/默认值对照表 |
| 不做 | Windows companion overlay、`HookInlineScan`、`Echo`、`hook_diagnostics`、桌面 WPF、工作区重组、`PublishRelease.py` 体系 | 非我方平台或无价值 |

## 8. 与既有报告的口径差异（本轮复核更正）

| 既有说法（`ANDROID_UPSTREAM_SYNC_PREP_20261005.md`） | 本轮实测 |
| --- | --- |
| 上游 HEAD = `e8c1246`（v3.5.0），"无更新" | **已过时**：上游 = `3510fa7`（**v3.5.1**），多 6 个提交 |
| 上游独有提交 33 / 我方 58 / 冲突面 118 | 现为 **39 / 68 / 196**（冲突面再扩大） |
| 上游独有文件 412（`native/modules` 111、`android/app` 44 …） | 现为功能面纯未同步 **330**（含上游新增 **217**），其中 `native/tests` 66、`android/app` 48 在上游新增里 |
| 上游改动文件 530 | 现为功能面 **526**（全部 3418，含 `research/` 2718 非功能面） |

> **方法提示**：`research/`（2718 文件）与 `docs/`（162）会让"上游改了 3418 个文件"这种数字虚高一个量级，统计前必须先剔除。

## 9. 与 `ANDROID_FEATURE_DEPENDENCY_TREE.md` 的口径核对（2026-10-05）

两文锚点**完全相同**（我方 `5e09128` ↔ 上游 `3510fa7`/v3.5.1），但覆盖维度不同，且有 **4 处结论冲突**（本文已按实测更正，见 §9.2）。下列判定全部经仓库实测：`git ls-tree -r --name-only`（存在性）、`git grep`（符号）、`git diff --numstat`（陈旧度）。

### 9.1 覆盖维度：互补，不是互斥

| 维度 | `ANDROID_FEATURE_DEPENDENCY_TREE.md` | 本文 |
| --- | --- | --- |
| 我方**已有**功能的依赖链（F1–F11） | ✓ | ✗ |
| 上游**独有**功能的依赖树（U1–U9 / §14.1–14.7） | ✓ | ✗ |
| 编译面归属（STATIC 丢弃、替身层、四条铁律 §13） | ✓ | 仅一句判据 |
| 逐提交功能条目（上游领先 39 个） | ✗ | ✓ §3 |
| 量化与三分法（领先提交 / 文件数 / 冲突面） | ✗ | ✓ §1 |
| 编译器实测表（NDK `-fsyntax-only`） | 引用 | ✓ §6 |
| 建议动作 P0/P1/P2/不做 | ✗ | ✓ §7 |

⇒ **两文合起来才是完整的未移植视图**；单看任一份都会漏。

### 9.2 四处结论冲突（定论以实测为准）

| 项 | 依赖树文档 | 本文（更正前） | 实测证据 | 定论 |
| --- | --- | --- | --- | --- |
| **全局 FOV 运行时热下发** | §14.5：本仓**不缺**全局 FOV，缺的是**免重启**；`GlobalFovUpdater` / `AndroidGlobalFov` 未移植 | §3/§5：**已移植** | `GlobalFovUpdater.java` 我方 0 / 上游 1；`AndroidGlobalFov` 我方 **0 命中**、上游 `native_bridge.cpp`+`camera/module.cpp` | **依赖树文档对**；本文把「配置期路径」当成了整条功能 |
| **悬浮窗跨进程设置写入通道（U2）** | §14.1/14.2：**必须新增** provider（本仓那个 exported provider 是 `.RuntimeJournalProvider`，不是同一个物） | §5：已由「我方悬浮窗 Compose」替代 | `OverlaySettingsClient/Provider/WritePolicy/WriteAuthorization/Geometry` 我方 **5 个类全 0** | **依赖树文档对**；本文把「引擎/通道」误当「页面」 |
| **`_TickStatePerformInterrupt` hook 可选化** | 未提 | §3：**未移植** | 我方 `native/modules/actions/module.cpp:183` 已挂该 hook、`:1587` 有 unavailable 退化分支；上游该提交仅 +12/−2 | **两文均误**：本文误报「未移植」，依赖树文档整条漏记 |
| **`android/resources` 缺口** | F1：把 `character-names/presets.json` 列为**已有** | §4-C：列为**缺** | 两者**都在**（presets 我方 34557 行 vs 上游 35000 行）；真缺 6 件 | **依赖树文档对**；但**两文都没指出这三个 JSON 是「陈旧版」** |

### 9.3 依赖树文档未覆盖、本文独有的条目

`Discord` 链接、模型下载镜像、`resource-source.json`、`tools/CustomModel` 9 件、`combat_stats` 2 头像、发布体系（`PublishRelease.py` / `scripts/` / artifacts 分组）、`_TickStatePerformInterrupt`（错记）、逐提交与量化视角。

### 9.4 本文未覆盖、依赖树文档独有的条目（本文已补进 §3/§5）

- **PC 界面布局开关（U3）**：`pc_ui_enabled` 键 + `DetourChangeInputType` 重定向 + `pc_active` 判据。本文原只在 §4-B 数字表里出现 `ui/module.cpp`，**没有命名该功能**；实测我方 `pc_ui|ui_pc` 命中 **0**、上游 5 文件。
- **本仓 `PumpInputType` 的隐式 Keyboard 推送缺陷**：`ModuleSettings.java:225` 恒写 `mobile_ui_enabled=false` ⇒ `active = g_mobile_ui_enabled` ⇒ **手机在 Touch(1) 时会被推成 PC/桌面布局**。这是已知缺陷不是功能。本文原未提。
- **运行时状态快照子系统**：`core/runtime_status.h` + `core/jni_binding.h` + `RuntimeSnapshot.java`，产出 `"BE_RUNTIME_V1\n<id>=<state>\n"`（上限 8192 B），上游的「模块将加载 / 不会加载」徽章由它驱动。我方 `BE_RUNTIME_V1` **0 命中**。本文原只把 `RuntimeSnapshot.java` 放进「需复核」，且漏了 native 两件。
- **MMD 引擎类逐件对照**：上游 `MmdInstaller`/`MmdImportArchive`（ZIP/7z 解包 + 预算）/`MmdInstalledResources`/`MmdAudio` 我方 0 命中；我方是**自研** `MmdImportPlan`/`MmdImportSession`/`MmdLibraryFiles`/`MmdLibraryInstaller`/`MmdSlotFiles`（无 ZIP/7z 解包）。**能力对等但实现不同** —— 本文 §5 记「已替代」与依赖树文档 §14.6 记「本仓全缺」各说一半。

### 9.5 两文一致的结论（互相印证，可信度最高）

| 一致结论 |
| --- |
| 最大缺口 = `native/modules/custom_model/module.cpp`（async / content-identity / job-runtime / asset-cache / generic-matcher / android-lod-relations），**在 Android 编译面内** |
| Windows 专有（companion overlay / `hook_diagnostics` / `HookInlineScan` / echo / WPF）**只记不移**，只取源码不取构建条目 |
| 第一人称专有（`first_person_runtime.inc`）**取我方** |
| 上游**无 CI** 且自述未构建 ⇒ 合并成功 ≠ 能用 |
| `input_relay.cpp` / `panel_commands.cpp` 上游已删，**我方不可跟删** |
| `values-en/ja/ko/zh-rTW` 四套本地化缺失；`combat_stats` 缺 2 头像 |
