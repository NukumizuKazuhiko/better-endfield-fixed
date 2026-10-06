# 上游 v3.5.1 相对本仓的安卓侧差量清单

**锚点**

| 角色 | 提交 | 说明 |
|---|---|---|
| 本仓 | `5e09128` | 分支 `codex/android-thirdparty-alignment`，versionName 3.4.2 / codec 30402 |
| 上游 | `3510fa7` | `Dr-hydra/Better-Endfield`，v3.5.1（2026-10-05 20:54） |
| 共同基线 | `9b1e895` | "Expand BEM v1.1/v1.2 runtime and tooling" |

**口径**：`git diff 5e09128 3510fa7`，`+` = 上游有而本仓无，`−` = 本仓有而上游无。`.behw` 不计入差异——它是构建期产物（工作树里 1668 个，全在 `android/app/build/` 下），由 `android/.gitignore:3:**/build/` 忽略，两侧版本库均无此物，来源是 `-PheadwearCatalogDir`。

> **统计前必读**：① `research/`（2718 文件）与 `docs/`（162）必须剔除，否则"上游改了 3418 个文件"虚高一个量级；② `git diff` 默认给非 ASCII 路径加引号，会让 `^(research|docs)/` 过滤**漏掉 12 个文件** ⇒ 一律用 `git -c core.quotepath=false diff --name-only`。本文所有数字均按此重算。

**一句话结论**：页面层零重叠（两侧各 59 个类，32 个上游独有、32 个本仓独有、27 个同名）；可直移的是**引擎类**（MMD 安装器、跨进程设置通道、模型覆盖层工具），必须重写的是**所有页面**。原生侧只有三块需要动：`ui/module.cpp`（PC UI）、`custom_model`（模型覆盖层）、`shared/android_compat`（**只增不覆**）。

---

## 零、量化总览

### 0.1 锚点与领先量

```bash
git fetch upstream --prune
BASE=$(git merge-base HEAD upstream/main)   # 9b1e895
git rev-list --count $BASE..upstream/main   # 39
git rev-list --count $BASE..HEAD            # 68
```

| 项 | 值 |
|---|---|
| 共同基线 | `9b1e895`「Expand BEM v1.1/v1.2 runtime and tooling」（2026-09-26） |
| 上游 HEAD | `3510fa7` **v3.5.1**（2026-10-05 20:54），领先 **39** 个提交（含 1 个 merge） |
| 本仓 HEAD | `5e09128`（2026-10-05 22:52），领先 **68** 个提交 |
| 上游改动文件 | 全部 **3418**；功能面 **526**（剔 `research/` 2718 + `docs/` 162） |
| 本仓改动文件 | 全部 **386**；功能面 **356** |

### 0.2 文件层四分法（功能面）

```bash
BASE=9b1e895
git -c core.quotepath=false diff --name-only $BASE..upstream/main | grep -vE '^(research|docs)/' | sort > up.txt
git -c core.quotepath=false diff --name-only $BASE..HEAD          | grep -vE '^(research|docs)/' | sort > our.txt
comm -23 up.txt our.txt   # 纯未同步
comm -12 up.txt our.txt   # 冲突面
comm -13 up.txt our.txt   # 本仓独有
```

| 桶 | 数 | 定义 | 处置 |
|---|---|---|---|
| **纯未同步 · 本仓有旧版** | **113** | 上游改、本仓自基线未动，且文件在本仓树里 | 直接取上游版（无冲突） |
| **纯未同步 · 上游新增** | **217** | 上游改、本仓树里根本没有 | 按 §三 编译面分级取舍 |
| **冲突面** | **196** | 双方自基线都改过 | 逐 hunk / 小侧前向移植，**绝不整文件覆盖** |
| **本仓独有 · 独有独改** | **138** | 本仓新增或改、上游未动 | **保护**，合并时不得被覆盖 |
| **本仓独有 · 主动删除** | **22** | 本仓删、上游未动 | 本仓有意为之（Compose 迁移删 View/XML） |

**校验**：`113 + 217 + 196 = 526` = 上游功能面 ✓；`196 + 138 + 22 = 356` = 本仓功能面 ✓

> **"上游独有"≠"上游新增"**：22 个本仓主动删除的文件（`ColorWheelView.java`、`ValueSlider.java`、15 个 `res/drawable/bg_*.xml`、3 个 `res/color/*.xml`、2 个 `res/layout/bem_spinner_*.xml`）在 `git diff 本仓 上游` 里同样显示为"上游新增"，实际是本仓删掉的。判"真缺 vs 被替换"必须过一遍 `git cat-file -e HEAD:<path>`。

### 0.3 目录分布

| 目录 | 上游改 | 本仓改 | 纯未同步 | 冲突面 |
|---|---|---|---|---|
| `native/modules` | 142 | 100 | 64 | **78** |
| `android/app` | 97 | 128 | 49 | **48** |
| `native/tests` | 75 | 25 | **67** | 8 |
| `tools/CustomModel` | 44 | 16 | 28 | 16 |
| `ui/BetterEndfield.UI` | 37 | 4 | 33 | 4 |
| `native/shared` | 29 | 22 | 8 | **21** |
| `android/resources` | 9 | 3 | 9 | 0 |
| `tools/HookInlineScan` | 5 | 0 | 5 | 0 |
| `tools/ThirdPartyModules` | 7 | 6 | 1 | 6 |

> 两侧都最重的两个目录即主战场：`native/modules` 与 `android/app`。`native/tests` 上游新增 66 件**本仓一件没有** —— 那是上游自带回归测试，是本仓唯一可用的验收判据。

### 0.4 与既有清点文档的数字差异（本次更正）

| 项 | `ANDROID_UNPORTED_FEATURES_20261005.md` 原文 | 本次实测 |
|---|---|---|
| 上游功能面改动文件 | 538 | **526**（原数含 12 个被引号转义而漏过滤的 `research/` 文件） |
| 纯未同步 | 342（= 113 + 229） | **330**（= 113 + **217**） |
| 冲突面 / 本仓功能面 | 196 / 356 | 一致 ✓ |
| 本仓独有 | 未拆 | **160** = 独有独改 138 + 主动删除 22 |

---

## 零点五、冲突面 196 件：按上游领先排序

`gap = (上游 +/−) − (本仓 +/−)`；`git diff --numstat $BASE..<ref>` 双测后 join 排序。**gap ≥ 7 的 32 件全列于下；其余 164 件 gap ≤ 6（其中 157 件 gap = 0，即双方改动量相当，多为同一功能各改各的）。**

| # | 文件 | 本仓 | 上游 | gap | 判定 |
|---|---|---|---|---|---|
| 1 | `native/modules/custom_model/module.cpp` | +108/−55 | **+1448/−206** | **1491** | 最大缺口 · **在 Android 编译面内** |
| 2 | `native/tests/custom_model_binding_tests.cpp` | +18/−0 | +933/−9 | 924 | 上游测试，本仓只有 18 行 |
| 3 | `ui/BetterEndfield.UI/MainWindow.xaml.cs` | +71/−1 | +580/−65 | 573 | Windows WPF · 不移 |
| 4 | `tools/CombatDataExporter/export_combat_data.py` | +25/−7 | +249/−280 | 497 | 工具 |
| 5 | `native/tests/android_world_binding_tests.cpp` | +168/−0 | +629/−0 | 461 | 上游测试 |
| 6 | `native/modules/camera/first_person_runtime.inc` | +433/−71 | +775/−77 | 348 | **第一人称专有 ⇒ 取本仓** |
| 7 | `android/.../custom_model/world_resource_adapter.inc` | +45/−22 | +268/−79 | 280 | Android 世界资源绑定 |
| 8 | `native/modules/camera/module.cpp` | +1290/−91 | +1433/−217 | 269 | 重度分叉 · 绝不整体替换 |
| 9 | `ui/BetterEndfield.UI/MainWindow.xaml` | +66/−0 | +289/−5 | 228 | Windows WPF |
| 10 | `android/.../java/.../GameOverlay.java` | +724/−379 | +785/−502 | 184 | 悬浮窗宿主 |
| 11 | `android/.../cpp/native_bridge.cpp` | +89/−1 | +228/−42 | 180 | JNI 桥 |
| 12 | `android/.../java/.../BemInstaller.java` | +154/−6 | +256/−77 | 173 | BEM 安装器 |
| 13 | `native/modules/ui/module.cpp` | +13/−2 | +119/−35 | 139 | **缺 PC UI**（本仓 `PumpInputType` 仍逐字同基线） |
| 14 | `native/modules/custom_model/bem.cpp` | +444/−71 | +515/−76 | 76 | 近乎持平 |
| 15 | `README.en.md` | +32/−148 | +130/−122 | 72 | 文档 |
| 16 | `android/.../cpp/core/runtime.cpp` | +53/−0 | +89/−21 | 57 | 运行时 |
| 17 | `android/.../custom_model_module.cpp` | +11/−10 | +61/−17 | 57 | 模块入口 |
| 18 | `native/modules/custom_model/generic_model_matcher.h` | +158/−0 | +215/−0 | 57 | 通用匹配 |
| 19 | `native/CMakeLists.txt` | +49/−0 | +94/−1 | 46 | **只增不覆** |
| 20 | `android/app/src/main/AndroidManifest.xml` | +12/−0 | +56/−1 | 45 | 清单 |
| 21 | `android/.../java/.../BemOptions.java` | +6/−1 | +38/−3 | 34 | 选项 |
| 22 | `README.md` | +26/−254 | +104/−206 | 30 | 文档 |
| 23 | `native/modules/custom_model/mod_registry.cpp` | +42/−4 | +68/−4 | 26 | 注册表 |
| 24 | `native/tests/generic_model_matching_tests.cpp` | +142/−0 | +167/−0 | 25 | 上游测试 |
| 25 | `tools/CustomModel/package_toolchain.py` | +6/−3 | +26/−7 | 24 | 工具 |
| 26 | `android/.../res/layout/activity_main.xml` | +0/−531 | +51/−500 | 20 | ★ 本仓删除、上游改写 |
| 27 | `android/.../java/.../BemInstalledResources.java` | +18/−5 | +30/−11 | 18 | 资源物化 |
| 28 | `native/modules/actions/pose_overlay.inl` | +1/−1 | +18/−0 | 16 | actions |
| 29 | `native/modules/custom_model/bem.h` | +60/−4 | +74/−4 | 14 | 头 |
| 30 | `android/.../cpp/CMakeLists.txt` | +34/−1 | +39/−4 | 8 | **只增不覆** |
| 31 | `android/.../java/.../BemInstallActivity.java` | +0/−203 | +26/−185 | 8 | ★ 本仓删除、上游改写 |
| 32 | `android/.../cpp/installer/install_jni.cpp` | +2/−0 | +6/−3 | 7 | 安装器 |

> 路径缩写：`android/.../` = `android/app/src/main/`；`android/.../java/.../` = `android/app/src/main/java/dev/betterendfield/android/`；`android/.../cpp/` = `android/app/src/main/cpp/`；`android/.../cpp/modules/…` 同理。

**其中 6 件是「本仓整文件删除、上游同期又改写」**（本仓侧 `+0/−N`）：

| 文件 | 本仓 | 上游 |
|---|---|---|
| `MainActivity.java` | +0/−1031 | +189/−406 |
| `res/layout/activity_main.xml` | +0/−531 | +51/−500 |
| `BemInstallActivity.java` | +0/−203 | +26/−185 |
| `SettingRow.java` | +0/−157 | +2/−44 |
| `res/layout/activity_bem_install.xml` | +0/−111 | +3/−110 |
| `SectionCard.java` | +0/−110 | +2/−43 |

> 这 6 件**不是"上游新增"**，是本仓 Compose 迁移时删掉的 View/XML，上游同期又改了它们 ⇒ 合并时**不要按上游 resurrect**。

---

## 一、页面层：Java/Kotlin 类清点

统计口径：`android/app/src/main/java/dev/betterendfield/android/`

| 集合 | 数量 |
|---|---|
| 上游类总数 | 59 |
| 本仓类总数 | 59 |
| A. 上游独有 | 32 |
| B. 本仓独有 | 32 |
| C. 两侧同名（含重度分叉） | 27 |

### A. 上游独有 32 类（按可移植性分三档）

#### A1 — 引擎/逻辑类：**可近乎直移**（9 个，共 1,155 行）

无 `extends View/Activity/ContentProvider`，只依赖 `Context + SharedPreferences + java.*`。

| 类 | 行 | 职责 | 本仓对应物 |
|---|---|---|---|
| `MmdInstaller.java` | 263 | MMD 作品安装/重发布/移除，`installed_mmd_works` 索引，单线程 worker + `busy/status` 静态量 | 本仓 `MmdLibraryInstaller.java`(358) + `MmdLibraryFiles.java`(155) + `MmdSlotFiles.java`(240) → **功能同源、接口不同** |
| `MmdImportArchive.java` | 223 | ZIP/7z 解包，预算控制（总 1 GiB / 音频 512 MiB / 4096 条目 / 深度 16），zip-slip 防护 | 本仓无等价物（本仓走 `MmdImportSession` 另一路径） |
| `MmdInstalledResources.java` | 85 | 把索引里的资源物化进游戏目录 | 本仓 `MmdLibraryFiles.materialize` 同职责 |
| `MmdAudio.java` | 139 | 游戏进程内 MMD 音频播放（`MediaPlayer` + `HandlerThread`，串行 token，seek 队列） | 本仓**无**（本仓音频走原生） |
| `RuntimeSnapshot.java` | 46 | 解析 `BE_RUNTIME_V1\n<id>=<state>\n` 运行时状态串 | 本仓 `RuntimeLog.java`(149) 是另一套 |
| `BemImportRequest.java` | 68 | 从 `Intent` 提取 `.bem` URI（VIEW/SEND、`EXTRA_STREAM`、`ClipData` 合并） | 本仓 `BemInstallActivity.kt` 内联 |
| `BemImportStream.java` | 67 | 流式拷贝 + `BEM\0PKG\0` magic 校验，上限 2 GiB | 本仓**无独立件** |
| `BemHotSwitchUpdater.java` | 50 | 模型热切换守护（轮询远端 prefs 变化 → 下发） | 本仓**无**（热切换整体未移植） |
| `ThirdPartyRuntimeUpdater.java` | 41 | 第三方模块运行时守护，同模式 | 本仓**无** |

**判据**：这 9 个类没有 Android UI 依赖，只吃 `Context`/`SharedPreferences`/`java.*`，前向移植后由本仓 Compose 页面调用即可。真正的工作量在**接口适配**（本仓 `Mmd*` 三件套已被 `MmdPage.kt` 依赖，不能简单替换）。

#### A2 — 页面/Activity 类：**必须按功能重写**（9 个，共 2,547 行）

View/XML + `Activity`。本仓是 Compose，**不可直移**。

| 类 | 行 | 上游页面 | 本仓 Compose 对应 |
|---|---|---|---|
| `MainActivity.java` | 814 | 6 页签主壳 | `MainActivity.kt`(93) + `SettingsShell.kt`(243) + `SettingsState.kt`(2101) |
| `BemInstallPage.java` | 503 | 第三方模型（BEM）页 | `BemInstallScreen.kt`(351) + `BemInstallState.kt`(357) + `BemInstallActivity.kt`(70) |
| `MmdLibraryActivity.java` | 423 | MMD 作品库页 | `MmdPage.kt`(591) |
| `EnhancementSettingsActivity.java` | 346 | 增强功能二级页（5 页） | `ExperiencePage.kt`(261) + `CameraMotionPage.kt`(207) + `FirstPersonPage.kt`(273) + `ToolPages.kt`(294) |
| `OverlaySettingsPage.java` | 293 | **v3.5.1 新增**：悬浮窗「游戏视野」+「模型管理」双 tab | 本仓**无任何对应** |
| `MmdImportActivity.java` | 195 | MMD 导入页 | 本仓 `MmdPage.kt` 内联导入 |
| `AboutPage.java` | 180 | 关于页 | 本仓 `SettingsPages.kt` 内 |
| `BemInstallActivity.java` | 44 | `.bem` 导入口（`exported=true`） | `BemInstallActivity.kt`(70) |
| `FrameworkServiceWait.java` | 30 | 框架服务等待页 | 本仓 `HomePage.kt` 内 |

#### A3 — 自绘控件/样式类：**并入本仓 Compose 组件库**（14 个，共 1,155 行）

上游手写 `View`/`LinearLayout` 组件。本仓有 `UiComponents.kt`(1019) + `UiTheme.kt`(107) + `UiTokens.kt`(174) 等价体系 ⇒ **不移植，只做视觉/交互对齐**。

| 类 | 行 | 职责 |
|---|---|---|
| `ColorWheelView.java` | 201 | 色轮（登录主题色选择） |
| `ValueSlider.java` | 159 | 标签+数值+`SeekBar` 的滑条行 |
| `SponsorDialog.java` | 146 | 赞助弹窗（微信/爱发电/PayPal + 保存赞赏码到相册） |
| `SettingRow.java` | 115 | 开关行（`Switch` + 主副标题） |
| `ControlIcon.java` | 76 | `Canvas` 手绘悬浮窗图标（7 种） |
| `ThirdPartyModulesPage.java` | 70 | 第三方模块列表页块 |
| `SectionCard.java` | 69 | 卡片容器 |
| `OverlayGeometry.java` | 63 | 悬浮窗布局/贴边几何（`usableArea` / `normalized` / `nearestHorizontalEdge`）— **纯数学，可直移** |
| `OverlaySettingsProvider.java` | 92 | **`exported=true` ContentProvider**，只实现 `call()` |
| `OverlayWritePolicy.java` | 93 | 写权限判据（UID / 分割 UID / token 常量时间比对）— **纯逻辑，可直移** |
| `OverlayWriteAuthorization.java` | 50 | `AtomicFile` 持久化 64 位 hex token |
| `OverlaySettingsClient.java` | 44 | 游戏进程侧单线程发送端 |
| `GlobalFovUpdater.java` | 27 | 500 ms 轮询权威 prefs → `NativeCommandBridge.globalFov` |
| `BemHotSwitchUpdate.java` | 32 | 热切换状态数据类 |

> 注：`OverlayGeometry` / `OverlayWritePolicy` / `OverlayWriteAuthorization` / `OverlaySettingsClient` / `GlobalFovUpdater` 五个虽列在 A3，但**无 View 依赖**，实为可直移项，一并归入 A1 的移植批次。

### B. 本仓独有 32 类（上游已删除或从未有）

| 类 | 行 | 性质 |
|---|---|---|
| `GyroscopeController.java` | 294 | **本仓核心增量**：陀螺仪输入链 |
| `HeadwearAssetStore.java` / `HeadwearAssets.java` | 209 / 23 | 头饰资产物化（`-PheadwearCatalogDir`） |
| `OverlayViewOwners.java` | 21 | 悬浮窗宿主追踪 |
| `RuntimeLog.java` / `RuntimeJournalProvider.java` | 149 / 78 | 本仓运行时日志通道（上游改用 `RuntimeSnapshot` + `runtime_status.h`） |
| `MmdLibraryFiles.java` / `MmdLibraryInstaller.java` / `MmdSlotFiles.java` | 155 / 358 / 240 | 本仓 MMD 落地链（上游用 `MmdInstaller` 单件） |
| `CameraVmdFile.java` | 90 | VMD 文件物化（跨 UID 流式落地） |
| Compose 层：`SettingsShell` `SettingsPages` `SettingsState` `UiComponents` `UiTheme` `UiTokens` `HomePage` `ExperiencePage` `MmdPage` `FirstPersonPage` `CameraMotionPage` `BemInstallScreen` `BemInstallState` `ThirdPartyModulesActivity`(kt) `MainActivity`(kt) `OverlayPanel` `OverlaySurface` `OverlayControls` `OverlayTheme` `FloatingHandle` `ToolPages` | 4,624 | 全部 UI，与上游零交集 |

### C. 两侧同名 27 类（差异规模，按行数降序）

| 类 | 上游新增 | 本仓独有 | 判定 |
|---|---|---|---|
| `ModuleSettings.java` | 234 | 1098 | **重度分叉**：本仓用 Compose 键集，上游是 View 键集 + v3.5.1 的 FOV 重构 |
| `GameOverlay.java` | 791 | 853 | **重度分叉**：上游 7 页签 View，本仓 `OverlayPanel/Surface`+`FloatingHandle` |
| `XposedEntry.java` | 59 | 504 | **重度分叉**：本仓多出整套 LSPosed 双进程/IPC 装配 |
| `FrameworkSettings.java` | 75 | 250 | **重度分叉**：本仓远端快照字段不同 |
| `RuntimeBootstrap.java` | 35 | 221 | **重度分叉**：本仓 `Os.setenv` 注入面更大 |
| `NativeCommandBridge.java` | 33 | 146 | **重度分叉**：本仓是文件分帧中继，上游是 JNI |
| `MmdImportPlan.java` | 72 | 253 | 中度：上游重写了归档规划 |
| `MmdImportSession.java` | 65 | 233 | 中度 |
| `MmdVmdParser.java` | 12 | 68 | 中度 |
| `BemInstaller.java` | 202 | 171 | 中度：上游拆出 `BemImportStream`/`BemImportRequest` |
| `BemInstalledResources.java` | 19 | 13 | 轻度 |
| `BemOptions.java` | 37 | 7 | 轻度 |
| `Hotkeys.java` | 2 | 31 | 轻度 |
| `OverlayFeatures.java` | 10 | 18 | 轻度：本仓多了 `vmdCamera` 预留位 |
| `ModuleCommandRouter.java` | 2 | 15 | 轻度 |
| `ModuleConfigurations.java` | 7 | 15 | 轻度 |
| `ThirdPartyModuleStore.java` | 2 | 16 | 轻度 |
| `ThirdPartyRuntimeMaterializer.java` | 1 | 1 | 基本一致 |
| `ActionPoseAssets` `AstcSupport` `BemParameters` `ModelPresetIndex` `ModuleApplication` `ThirdPartyModuleActivity` `ThirdPartyModulePackage` `VoiceCatalogIndex` `VoiceCatalogMaterializer` | 0 | 0 | **逐字相同** |

---

## 二、res 资源差量

| 类别 | 上游有我方无 | 差异 | 归属（实测） |
|---|---|---|---|
| `res/drawable/` | 16 个 | `bg_avatar` `bg_badge` `bg_card` `bg_chip` `bg_ghost_button` `bg_header` `bg_input` `bg_notice` `bg_primary_button` `bg_setting_row` `bg_status` `bg_tab` `bg_tabs` `bg_voice_row` `ic_info` `bg_sponsor_button` | **15 个是本仓主动删除**（Compose 迁移，非上游新增）；只有 `bg_sponsor_button` 是上游新增 |
| `res/layout/` | 5 个 | `activity_main` `activity_bem_install` `view_bem_install` `bem_spinner_item` `bem_spinner_dropdown_item` | **4 个是本仓主动删除**；只有 `view_bem_install` 是上游新增 |
| `res/color/` | 3 个 | `switch_thumb` `switch_track` `tab_text` | **3 个全是本仓主动删除** |
| `res/raw/` | 1 个 | `sponsor_wechat.png` | 上游新增（赞助入口） |
| `res/values*/` | 8→6 个 | 上游把 `model_overlay.xml` **改名**为 `model_management.xml`，新增 `overlay_settings.xml`（透明度/自动贴边） | 上游新增 |
| `AndroidManifest.xml` | M | 见下 | 双方都改 |

**Manifest 差异**（核验过）：

```xml
<!-- 上游 3510fa7 -->
<provider android:name=".OverlaySettingsProvider"
    android:authorities="dev.betterendfield.android.overlay.settings"
    android:exported="true" android:grantUriPermissions="false" />

<!-- 本仓 5e09128 -->
<provider android:name=".RuntimeJournalProvider"
    android:authorities="${applicationId}.journal"
    android:exported="true" />
```

两侧各有一个 `exported=true` 的 ContentProvider，**不是同一个**：
- 上游 = `OverlaySettingsProvider`（跨进程**写**设置，只实现 `call()`，query/insert/update/delete 全抛 `SecurityException`）；
- 本仓 = `RuntimeJournalProvider`（只读日志/运行时状态，上游无此物，属本仓 32 个独有类之一）。

⇒ 若移植第 3 项（跨进程设置通道），需在 Manifest 里**新增**这个 provider，而非替换本仓已有的 journal provider。其余 Activity 两侧均为 `exported=false`，只有导入口不同（上游 `.BemInstallActivity` + `.ThirdPartyModuleActivity`，本仓 `.BemInstallActivity` + `.ThirdPartyModulesActivity` + `.ThirdPartyModuleActivity`）。

**strings 差量**：本仓 282 条 vs 上游 170 条（命名体系不同，不可直接比对）。上游独有的 74 条中，**属于功能声明**的关键项：

```
ui_pc / ui_pc_hint                      ← PC 界面布局开关（界面增强卡片第 3 个开关）
camera_advanced_* / camera_motion_* / camera_orbit_speed / camera_smoothing
camera_keyframe_* / camera_fp_side_limit / camera_fp_turn_speed
mmd_enable / mmd_loop / mmd_body / mmd_face / mmd_cloth_* / mmd_terrain / mmd_gain
mmd_motion_scale / mmd_offset / mmd_publish / mmd_remove / mmd_seek / mmd_import
custom_model_page / model_page / model_basic_section / model_description
about_page / sponsor* / voice_page / voice_list_caption / dash_card_subtitle
```

---

## 三、安卓侧原生层

### 三.1 — `android/app/src/main/cpp/`（JNI 桥，本仓 39 件 / 上游 36 件）

| 变化 | 文件 |
|---|---|
| 上游**新增** | `core/jni_binding.h`（统一 `BindContextLoaderNatives`：走当前线程 `ClassLoader.loadClass` 注册 natives，**不做双命名空间兜底**）、`core/runtime_status.h`（`RuntimeStatus` 类，产出 `BE_RUNTIME_V1\n<id>=<state>\n`） |
| 上游**删除**（本仓独有） | `core/panel_commands.cpp`、`input_relay.cpp`、`modules/camera/first_person_look_probe.{cpp,h}`、`modules/custom_model/android_headwear_canary.cpp` |
| 双方都有、内容不同 | `CMakeLists.txt`、`core/command_pump.{cpp,h}`、`core/hook_broker.cpp`、`core/log.{cpp,h}`、`core/runtime.{cpp,h}`、`installer/install_jni.cpp`、`modules/custom_model/*`(10 件)、`modules/desktop/desktop_module.{cpp,h}`、`modules/login_model/login_model_module.cpp`、`native_bridge.cpp` |

> **注意**：上游删除 `panel_commands.cpp` / `input_relay.cpp` 是**架构重整**，不是功能删除——上游把面板命令与输入中继的 JNI 面收进 `jni_binding.h` + `RuntimeStatus`。本仓这套文件是**本仓 MMD 命令分帧链的载体**（`input_relay.cpp` 承担 `\u001f` 折叠还原），**不可照上游删除**。

### 三.2 — `native/` 共享模块（Android 与 Windows 同源）

| 路径 | 上游新增 | 本仓独有 | 处置 |
|---|---|---|---|
| `native/modules/ui/module.cpp` | 121 | 48 | **缺 PC UI**：上游 `pc_ui_enabled` 解析 + `g_keyboard_input_type` + PC 分支 + 250 ms 漂移重检 + `DetourChangeInputType`。本仓 `PumpInputType` 与基线 `9b1e895` 逐字相同 ⇒ 仍保留**隐式 Keyboard 推送**副作用 |
| `native/modules/custom_model/module.cpp` | 1451 | 262 | **缺模型覆盖层**：上游加了 `model_job_runtime.inc`(1021) `async_loading.h`(638) `generic_model_matcher.{h,inc}` `model_asset_cache.inc` `model_content_identity.h` `model_overlay_host.h` `model_overlay_protocol.h` `model_overlay_hotkey.h` `android_lod_relations.generated.h` 等（含 Windows-only 的 `overlay/` 目录，本仓不适用） |
| `native/modules/camera/*` | A 3 / D 21 / M 6 | — | **本仓保留 21 件上游已删的第一人称文件**（`first_person_cap_upload.inc`、`facing_runtime.inc`、`motion_runtime.inc`、`readback_runtime.inc`、`retract_runtime.inc`、`scale_runtime.inc`、`headwear_fixture.h` …）；`first_person_runtime.inc` 双方重度分叉（上游 +792 / 本仓 −456）。**第一人称区域一律保留我方** |
| `native/shared/android_compat/android_win32.cpp` | 35 | 227 | ⚠️ **禁区**：本仓含 `AddVirtualMouseDelta` / `DrainVirtualMouseDelta`（陀螺仪 + look pad 注入链），上游没有。**只增不覆** |
| `native/shared/android_compat/include/android_win32.h` | 11 | 56 | ⚠️ 同上 |
| `native/shared/android_compat/android_virtual_keys.h` | 2 | 25 | ⚠️ 同上 |
| `native/shared/android_compat/android_panel_commands.h` | 0 | 34 | 本仓独有，上游已删 |
| `native/shared/host/hook_broker.{cpp,h}` | 311+52 | 62+6 | 分叉：上游新增 `hook_diagnostics.{cpp,h}`(197+40) |
| `native/shared/host/host_runtime.{cpp,h}` `module_manager.cpp` | 27+4+3 | 6 | 轻度 |
| `native/shared/input/hotkey.h` | 99（vs 基线**新增文件**） | 96（我方 `ba18649` 同期独立新增） | **实为冲突面**：两侧各自新建，非"上游领先"；我方版本已含 `Input::IsDown` / `Input::ParseKey`，第 2 项无前置缺口 |
| `native/shared/include/BetterEndfield/ModuleApi.h` | 6 | 0 | 轻度 |

---

## 四、构建与打包

| 文件 | 上游新增 | 本仓独有 | 处置 |
|---|---|---|---|
| `android/app/build.gradle.kts` | 116 | 457 | **重度分叉**。上游 v3.5.1 新增 `signingConfigs.persistentRelease`（pin 到 3.5.0 已发布证书）+ `buildStagingDirectory` + `noCompress += "bin"`；本仓有 CI 签名与 headwear 相关配置 |
| `android/settings.gradle.kts` | 63 | 0 | 上游把构建根与工具链路径配置化 |
| `android/workspace.gradle.kts` | 24 | 0 | 上游**新增文件**：工作区路径配置 |
| `android/build.gradle.kts` | 2 | 17 | 轻度 |
| `android/app/proguard-rules.pro` | 1 | 75 | 本仓独有 keep 规则（**必须保留**：本仓名字在 JVM 外被解析） |
| `android/keystore/bem-debug.keystore` + `debug.properties` | 0 | 删除 | 上游**删掉了 debug 密钥库**（改由 CI 生成）。本仓保留——**本机唯一的签名材料** |
| `android/gradlew` | 0 | 0 | 仅权限位：本仓 `100755` → 上游 `100644` |
| `android/AGENTS.md` | 新增 | — | 上游新增仓库内 AI 协作说明 |
| `android/tools/CheckOverlayHost.ps1` | 35 | — | 上游新增悬浮窗宿主自检脚本 |
| `android/tools/HeadwearAssetStoreTest.java` | — | 121（上游删） | 本仓头饰夹具 |
| `android/Test-FirstPersonPreflight.ps1` | — | 删除 | 本仓已删的预检脚本 |
| `android/.gitignore` | M | M | 轻度 |

---

## 五、`android/resources/` 生成物

| 文件 | 上游新增 | 说明 |
|---|---|---|
| `manifests/voice/voice-event-media-manifest.json` | 460,561 | 配音事件媒体清单（差异大头） |
| `manifests/model/action-manifest.json` | 35,107 | 模型动作清单 |
| `resource-source.json` | 6,077 | 资源来源表 |
| `character-presets.json` | 444 | 角色预设增补 |
| `manifests/shared/resource-manifest-report.md` | 新增 | 清单报告 |
| `voice-index-android-evidence.json` / `voice-index-merge-evidence.json` | 新增 | 配音索引证据 |
| `character-names.json` / `voice-catalog-index.json` | M | 轻度 |

这些是**上游构建期产的资源索引**，本仓若跟进配音/模型清单需按本仓 `-PheadwearCatalogDir` 流水线重新生成，不宜直接拷贝。

---

## 六、可执行移植清单（按性价比排序）

| # | 项 | 落点 | 规模 | 风险 |
|---|---|---|---|---|
| 1 | **修 PC UI 隐式推送** | `native/modules/ui/module.cpp` 的 `PumpInputType()` | ~5 行（`!active && restore < 0` 直接返回） | 低。改前先真机取 `Input type pushed to 0` 证据 |
| 2 | **移植完整 PC UI**（含 `ui_pc` 开关） | 同上 + Compose 界面增强页加第 3 个开关 | 上游 +121 行 | 中。需 IL2CPP 解析 Keyboard(=0)，失败则整功能不启用 |
| 3 | **跨进程设置通道** — **已完成**（`30503d3`） | `OverlaySettings{Client,Provider}` + `OverlayWritePolicy/Authorization` + Manifest 组件 | **269 行**新增 + 补齐 §10.4-4 的 3 处缺口（另 2 处由 §10.5 的 `command_pump` 路线免掉） | `exported=true` 组件已按安全审落地：只实现 `call()`，UID + 令牌双因子（§10.6） |
| 4 | **模型管理悬浮窗页** — **已完成**（`30503d3`） | 按功能重写为 Compose 页（`OverlayModelPage.kt`，含 3 个自绘控件） | 约 560 行；`OverlayFeatures` 已加 `models` 第 8 字段门禁 | 已落地并真机跑通；**滑条/选择器的实际交互须人工确认**（§10.6） |
| 5 | **全局 FOV 运行时下发** — **已完成**（`d3c7fa4`） | 走本仓既有的 `command_pump`：原生 `DrainGlobalFovCommand()` + 设置 app 在"仅 `global_fov=` 行变化"时改发 `global_fov` 命令 | **149 行**（原生 +100 / Java +49） | 已落地并编译验证；**须真机**确认生效（§10.5） |
| 6 | **MMD 安装器统一** | `MmdInstaller` + `MmdImportArchive` + `MmdInstalledResources` + `MmdAudio` | 710 行可直移 | 中。要与本仓 `MmdPage.kt` 接口对齐 |
| 7 | **模型热切换** | `BemHotSwitchUpdater` + `BemHotSwitchUpdate` | 82 行 | 中。上游 native 侧依赖 `model_overlay_host.h` |
| 8 | **运行时状态串** | `RuntimeSnapshot` + `core/runtime_status.h` | 46 + 22 | 低。可替换本仓 `RuntimeLog` 或并存 |
| 9 | **JNI 注册收敛** | `core/jni_binding.h` | 45 | 低。收益是注册路径统一 |
| 10 | **构建配置化** | `workspace.gradle.kts` + `settings.gradle.kts` | 87 | 低。本仓已有 CI 路径，按需 |

---

## 七、禁区（不可合并）

1. **`native/shared/android_compat/*`** — 只增不覆。覆盖会静默删掉 `AddVirtualMouseDelta` / `DrainVirtualMouseDelta`，断掉陀螺仪 + look pad 注入链。
2. **`android/app/proguard-rules.pro` 的本仓 75 行** — 名字在 JVM 外被解析，删了 R8 会吃掉入口。
3. **第一人称与输入线程区域** — `native/modules/camera/first_person_*` 与 `native/shared/input/*` 一律保留我方。
4. **`module.cpp`（`native/modules/camera/`）** — 双方重度分叉（+1339/−1322），绝不整体替换。
5. **页面层** — 32 个上游独有类全部是 View/XML，本仓 Compose 层不可合并，只能按功能名重写。
6. **`android/keystore/`** — 上游已删，本仓是本机唯一签名材料，**保留**。
7. **`android/app/src/main/cpp/input_relay.cpp` + `panel_commands.cpp`** — 上游删除是架构重整，本仓这两个文件是 MMD 命令分帧链载体，**不可跟删**。
8. **本仓主动删除的 22 件**（`ColorWheelView.java`、`ValueSlider.java`、15 个 `res/drawable/bg_*.xml`、3 个 `res/color/*.xml`、2 个 `res/layout/bem_spinner_*.xml`）— Compose 迁移的有意删除，**不要按上游 resurrect**（尤其中 6 件上游同期又改写过，见 §零点五）。

---

## 八、编译面分级与编译器实测

### 8.1 未移植面按「是否进本仓 Android 编译面」分级

| 级 | 内容 | 处置 |
|---|---|---|
| **A 本仓不编译** | `native/modules/{camera,custom_model}/overlay/*`（伴生进程 + `.rc`）、`native/shared/host/hook_diagnostics.{h,cpp}`、`tools/HookInlineScan`、`tools/ThirdPartyModules/echo`、桌面 WPF `ui/BetterEndfield.UI`、`tools/CustomModel/CreatorProjectChecks`（C#） | **只记不移**；只取源文件、**不取构建条目**（依赖 minhook / ws2_32 / mfplat / `.rc`） |
| **B 本仓编译、仍是旧实现** | `custom_model/module.cpp`、`world_resource_adapter.inc`、`camera/module.cpp`、`native_bridge.cpp`、`GameOverlay.java`、`BemInstaller.java`、`core/runtime.cpp`、`ui/module.cpp` 等 | 子系统级前向移植，**绝不整文件覆盖** |
| **C 数据 / 资源 / 版本** | `android/resources` 缺 6 件、`values-en/ja/ko/zh-rTW` 4 套、`combat_stats` 缺 2 头像、`tools/CustomModel` 缺 9 件 | 纯增量，风险低 |
| **D 反向：本仓领先** | 兼容层陀螺仪（`AddVirtualMouseDelta`）、命令通道分帧、第一人称扩展、悬浮窗 Compose、MMD 作品库、CI | **合并时不得被覆盖** |

`custom_model/module.cpp` 内新增件**是否进 Android 编译**（实测引入点守卫）：

| 新件 | 引入位置 | 平台 |
|---|---|---|
| `async_loading.h` | module.cpp:8（无守卫） | Android 编译 |
| `model_content_identity.h` | module.cpp:9（无守卫） | Android 编译 |
| `generic_model_matcher.inc` | module.cpp:1858（无守卫） | Android 编译 |
| `model_asset_cache.inc` | module.cpp:3262（无守卫） | Android 编译 |
| `model_job_runtime.inc` | module.cpp:3589（无守卫） | Android 编译 |
| `android_lod_relations.generated.h` | module.cpp:17，`#if defined(__ANDROID__)` | Android 专有 |
| `model_overlay_host.h` / `runtime_ini_win32.h` | module.cpp:21–22，`#if defined(_WIN32)` | Windows 专有（不入本仓） |

### 8.2 编译器实测（NDK r27c）

```bash
D=D:/android-toolchain/android-ndk-r27c/toolchains/llvm/prebuilt/windows-x86_64
"$D/bin/aarch64-linux-android24-clang++.cmd" --target=aarch64-linux-android24 -U_WIN32 \
  -fsyntax-only -std=c++20 -I native/shared/include -I native/shared/android_compat … <上游新头>
```

| 上游新头 | 实测 | 说明 |
|---|---|---|
| `async_loading.h` | **OK** | Android-clean |
| `model_content_identity.h` | **OK** | Android-clean |
| `runtime_ini.h` | **OK** | Android-clean |
| `android_lod_relations.generated.h` | **OK（需前置）** | 单独编译报 `undeclared identifier 'std'/'GenericMatching'` 是**假阳性**——生成片段须在 `<span>` + `generic_model_matcher.h` 之后展开 |
| `model_overlay_host.h` / `win32_overlay_window.h` 等 | 不测 | 上游已用 `#if defined(_WIN32)` 排除 |

> **已排除的坑**：宿主是 Windows 时 `_WIN32` 会被预定义，必须 `--target=aarch64-linux-android24` **且 `-U_WIN32`**，否则替身层 `Windows.h` 的 `#error` 会造成全量假阳性。

---

## 九、文档分工与复现命令

| 问题 | 看哪份 |
|---|---|
| 到底有哪些文件不一样、数量与规模是多少 | **本文档 §零 / §零点五** |
| 改一个文件到底会不会进 `.so` | `ANDROID_FEATURE_DEPENDENCY_TREE.md` §13 |
| 本仓有哪些功能、各自依赖哪些文件 | 同上 **F1–F11** |
| 上游有哪些功能是本仓没有的、依赖哪些文件 | 同上 **§14（U1–U9）+ §15** |
| 上游领先提交的逐条功能面 + 建议优先级 | `ANDROID_UNPORTED_FEATURES_20261005.md` |
| 两份清点的口径核对（4 处冲突及定论） | 同上 **§9** |

**复现本文全部数字**（仓库根执行）：

```bash
BASE=$(git merge-base HEAD upstream/main)   # 9b1e895
git -c core.quotepath=false diff --name-only $BASE..upstream/main | grep -vE '^(research|docs)/' | sort > up.txt
git -c core.quotepath=false diff --name-only $BASE..HEAD          | grep -vE '^(research|docs)/' | sort > our.txt
comm -23 up.txt our.txt | wc -l    # 330 纯未同步（内分 113 本仓有旧版 / 217 上游新增）
comm -12 up.txt our.txt | wc -l    # 196 冲突面
comm -13 up.txt our.txt | wc -l    # 160 本仓独有（138 独改 + 22 主动删除）
git diff --numstat $BASE..HEAD > ns_our.txt ; git diff --numstat $BASE..upstream/main > ns_up.txt   # 再 join 得 gap 排名
```

**二分「真缺 vs 被替换」**（判 217 个上游新增里哪些是真缺口）：

```bash
while IFS= read -r f; do git cat-file -e "HEAD:$f" 2>/dev/null && echo "本仓有旧版: $f" || echo "上游新增: $f"; done < <(comm -23 up.txt our.txt)
```

---

## 十、接管执行记录

### 10.1 第 1 项已完成（`2d4ef64`，2026-10-06）

**改动**：`native/modules/ui/module.cpp` `PumpInputType()`。原实现 `!active` 时 `target = restore >= 0 ? restore : 0`，即**未启用任何 UI 覆盖时无条件把 `InputType` 推成 `0`（Keyboard）**；改为 `!active && restore < 0` 直接标记 generation 并返回，且把"交还给游戏"改为一次性（应用成功或已等于目标时清零 `g_restore_input_type`），避免后续 generation 把陈旧值重新压回玩家手动选过的布局。

**静态确证（无需设备）**，三处闭合：

1. `ModuleSettings.setInterfaceSettings()`（`android/app/src/main/java/dev/betterendfield/android/ModuleSettings.java:216–227`，注释在 **223–224** 行）把 `mobile_ui_enabled` **硬编码为 `false`**，注释：*"The touch layout and the Android/cloud platform claim only exist to make a desktop client look like a phone. This is a phone."* ⇒ `active` 恒为 `false`，原实现每次都走 push-0 分支。
   > **出处于 2026-10-06 更正**（原先误记为"本仓立场"，已撤回）：该注释由**共同祖先**引入 —— `8661865`「Integrate Android BEM installation, module UI, and shared desktop features」，作者 Dr-hydra，2026-09-22（早于基线 `9b1e895` 的 09-26）。`git merge-base --is-ancestor 8661865` 对 `9b1e895` / `HEAD` / `upstream/main` **三者皆真** ⇒ 基线、本仓、上游三方同含此段，**不是本 fork 的产品决定**。
2. 触发链闭合：`ui_style.awake` / `ui_style.update` / `UnityEngine.EventSystems.EventSystem.Update` 三个 contract **不在**上游 Android 跳过的 `device.` / `app.` / `cloud_` 前缀内 ⇒ 本机同样安装 detour，任一次 `ConfigurationChanged`（如开关"隐藏 HUD"）后第一个 UIStyle 帧即触发。
3. 结论：`mobile_ui_enabled` 恒 `false`（三方一致）叠加旧 `PumpInputType` 的 push-0 兜底，使"只开隐藏 HUD/UID"的纯配置也去驱动游戏 `ChangeInputType`，把手机上的触屏布局顶成键盘布局。**这与上游 `3510fa7` 修的是同一个缺陷**（上游那行 `!active && restore < 0` 早退即为此而加），属同步上游修复，而非推翻本仓决定。

**产出物判据**：`aarch64-linux-android24 --target -fsyntax-only` 零错（仅 1 条既有 `unused function 'Contract'` 告警）；`:app:externalNativeBuildDebug` BUILD SUCCESSFUL；`libbetterendfield_android.so` `48715944 → 48716592` 字节、SHA-256 `425ff3bd… → 5433acc9…`，`llvm-nm` 中 `PumpInputType` 为 772 字节本地符号。

**遗留**：真机日志证据（`Input type pushed to 0`）未取——本机仅连 `MGFNW19822014904`（非游戏验收机），`PJX110` 未接入。若需复现，用 `PJX110` 打开诊断开关、只开"隐藏 HUD"，看旧包是否出现该行。

### 10.2 第 2 项（完整 PC UI）的前置与语义澄清

前置件在我方**已齐备**（无需先移植）：`native/shared/android_compat/android_frame.h` 已提供 `FrameClient::Ui` / `OnAndroidFrameThread` / `SetAndroidFrameClient` / `PublishAndroidHudState`；`BetterEndfield::Input::IsDown/ParseKey`（`hotkey.h`）与 `Unbox()`（`native/modules/ui/module.cpp:380`）均已在位。

> **原先"与本仓产品立场冲突、须产品决策"的结论已撤回**（2026-10-06，依据不足，属过度解读）。核实结果：上游 v3.5.1 **自己**就在保留 `mobile_ui_enabled=false` 的同时新增了 `pc_ui_enabled`，并把那段注释改写为 —— `upstream/main:android/app/src/main/java/dev/betterendfield/android/ModuleSettings.java:173–178`：

```java
+ "hide_hud_enabled=" + hideHud + "\n"
+ "pc_ui_enabled=" + isPcUiEnabled(context) + "\n"
+ "hide_hud_hotkey=" + Hotkeys.HIDE_HUD_NAME + "\n"
// PC UI changes only input type; it never spoofs the platform.
+ "mobile_ui_enabled=false\n"
+ "platform_spoof_enabled=false\n"
```

三件事语义互不重叠、**并存不冲突**：

| 键 | 语义 | 三方取值 |
|---|---|---|
| `mobile_ui_enabled` | 强制**触摸**布局（供桌面客户端伪装手机） | 恒 `false` |
| `platform_spoof_enabled` | 伪装 Android / 云平台身份 | 恒 `false` |
| `pc_ui_enabled`（v3.5.1 新增） | 强制 **Keyboard / 桌面**布局，**只改 inputType，不伪装平台** | 由用户开关决定 |

⇒ 第 2 项**可以按原优先级推进**，不构成立场反转。唯一真实门槛仍是 §六 第 2 项风险栏所列：IL2CPP 能否解析出 `DeviceInfo/InputType` 的 `Keyboard(=0)` 枚举值，失败则该功能整体不启用（`g_keyboard_input_type = -1` ⇒ `pc_active=false`，安全退化）。

### 10.3 第 2 项已完成（完整 PC UI，2026-10-06）

**改动落点（5 个文件）**

| 文件 | 改动 |
|---|---|
| `native/modules/ui/module.cpp` | 三方合并上游 `3510fa7` 全部改动（+115/−56） |
| `ModuleSettings.java` | `PC_UI_ENABLED` 常量、`isPcUiEnabled` / `setPcUiEnabled`、配置写出条件加 `pcUi`；**拼串抽成纯函数** `interfaceConfiguration(hideUid, hideHud, pcUi)`（行为不变，为设备外验证） |
| `SettingsState.kt` | `pcUi` 状态 + 载入 + `updatePcUi` + `loadedModuleIds()` / `interfaceCardStatus` 条件 |
| `ExperiencePage.kt` | 界面增强卡片第 3 个 `SwitchRow` |
| `res/values/strings.xml` | `ui_pc` / `ui_pc_hint`（照上游中文文案） |

**原生侧并入的内容**：`pc_ui_enabled` 解析；`g_keyboard_input_type`（Android 经 IL2CPP 元数据解析 `DeviceInfo/InputType.Keyboard`）；`DetourChangeInputType` 的 PC 分支；`PumpInputType` 250 ms 漂移重检 + `report_status` + 回读校验；`ParseVirtualKey` → `BetterEndfield::Input::ParseKey`；`PumpHudVisibility` → `Input::IsDown`；`InstallHooks` 在 Android 跳过 `device.*`（保留 `device.change_input_type`）/ `app.*` / `cloud_*`；`AndroidUiFrame` + `SetAndroidFrameClient(FrameClient::Ui, …)`；`ConfigurationChanged` / `Shutdown` 的 PC 侧清理与日志。

**冲突处理**：仅 `PumpInputType` 三处冲突（我方第 1 项的修复与上游改动同区域），**全部取上游实现**——上游版已含等价的 `!active && restore < 0` 早退与 restore 一次性清零。合并后与本仓原独有部分的差异**恰为 13/2 行**（即两处日志 summary），已核验保留。

**证据**

| 判据 | 结果 |
|---|---|
| `aarch64-linux-android24 -fsyntax-only` | 零错（仅既有 `unused function 'Contract'` 告警） |
| `:app:assembleDebug` | BUILD SUCCESSFUL（原生 + Kotlin + Java 全链） |
| `.so` | `48716592 → 48790264` 字节；SHA-256 `5433acc9… → 0e727f8f…`；`llvm-nm` 中 `AndroidUiFrame` = 96 字节本地符号 |
| **设置写入器 JVM 验证** | 真实 `ModuleSettings.interfaceConfiguration()` 跑 8 种开关组合：三开关全关时输出**空配置**（= 模块不进游戏进程），任一开启才写出；`pc_ui_enabled` 随标志位正确变化；**写出的每个键都落在原生解析器认得的 8 个键之内**（`schema_version` 除外——版本标记，非开关） |

**未完成 / 未验证**：`DeviceInfo/InputType.Keyboard` 的 IL2CPP 解析是否成功**须真机日志**——成功为 `Android PC layout: resolved InputType.Keyboard by metadata.`，失败为 `Android PC layout: Keyboard enum unavailable; leaving game layout unchanged.`。设备未接入，该项待 `PJX110`。

**上游一致性**：上游**未**在悬浮窗侧（`OverlaySettingsPage` / `FrameworkSettings`）暴露该开关，故不改本仓悬浮窗；悬浮窗暴露属第 4 项（依赖第 3 项的跨进程设置通道）。

### 10.4 第 3+5 项前置核查：两处与 §六 估计不符（2026-10-06，未落地）

开工前的依赖清点推翻了 §六 对这两项"可直移 / 低风险"的判断。**结论：两项都不能按 §六 原样推进**，需先做决定（见 §10.5）。

**（1）第 3 项的 4 个文件可直移，但唯一消费者在第 4 项 ⇒ 单独做是死代码。**

上游调用方清单（`git grep` 全上游树）：

| 上游调用点 | 说明 |
|---|---|
| `OverlaySettingsPage.java:67/90/101` | **唯一的 `OverlaySettingsClient.call` 调用方**，是第 4 项（293 行 View/XML，须重写为 Compose） |
| `XposedEntry.java:36` | `OverlaySettingsClient.initialize(...)`，游戏进程侧 |
| `XposedEntry.java:77` | `GlobalFovUpdater.start(...)` |
| `FrameworkSettings.java:26/197-199` | 授权令牌的初始化与发布（跨进程偏好） |
| `BemInstaller.java:386` | `OverlayWritePolicy.apply` 接入 `saveAll` |

即：`OverlaySettingsClient` + `OverlayWriteAuthorization` + `OverlayWritePolicy` + `OverlaySettingsProvider` 这 279 行，**服务对象就是第 4 项那个悬浮窗设置页**。本仓无 `OverlaySettingsPage` ⇒ 单独移植只会在设置 app 里新增一个 `exported=true` 的 Provider 和一堆无人调用的类。**应与第 4 项合成一批。**

**（2）第 5 项的 `NativeCommandBridge.globalFov` 在本仓走不通——两侧 `NativeCommandBridge` 不是同一种实现。**

| 侧 | `NativeCommandBridge` 形态 |
|---|---|
| 上游 | **JNI**（`static native boolean globalFov(boolean, float)`；`native_bridge.cpp` 用 `BindContextLoaderNatives` 绑定到游戏 classloader） |
| 本仓 | **文件中继**（`NativeCommandBridge.java` 注释原文：*"This used to be a JNI bridge, but the runtime library registers under the game's classloader while these classes belong to the LSPosed module classloader — and Android scopes JNI symbol lookup (and .so openings) per classloader, so every call threw UnsatisfiedLinkError"*） |

本仓同源证据：`native_bridge.cpp` 的 `submit` / `status` / `key` / `releaseKeys` JNI 导出**仍在编译进 `.so`，但 Java 侧已无对应 native 声明**（`NativeCommandBridge.java` 无 `native` 方法），是历史遗留；`AndroidMmdCommand` / `AndroidMmdStatus`（`native/shared/android_compat/android_camera.h`）同样**只有定义、无调用方**——本仓 MMD 实际走 relay 的 `c <payload>` 行 → `command_pump` → `AcquirePanelCommand("mmd")`。

⇒ 第 5 项必须走**本仓自己的通道**（`input_relay.cpp` 的 relay 行，或 `command_pump` 的 `AcquirePanelCommand`），并自建发送方。规模不是 §六 写的 27+7 行。

**（3）第 5 项的真实价值取决于一个产品口径：FOV 是否脱离全量重载。**

本仓现状（已实测）：

- `SettingsState.updateGlobalFov()` / `updateGlobalFovEnabled()`（`SettingsState.kt:960-976`）→ `ModuleSettings.setGlobalFov()`（**只写偏好**，`ModuleSettings.java:305`）→ 再调 `saveCameraSettings()` → `setCameraSettings()` → `ModuleCommandRouter.issue("camera_config", configuration)`（`ModuleSettings.java:1363`）。
- 即本仓 FOV 改动**已经会生效**，走的是**全量配置重载**（configuration 文本里含 `global_fov_enabled` / `global_fov`，见 `ModuleSettings.java:1293-1294`；原生 `module.cpp:2533-2534` 解析、`2965-2971` 写原子量）。
- 原生消费端齐备：`g_global_fov_enabled` / `g_global_fov`（`module.cpp:265-266`）→ `ScopedGlobalFovState`（`free_camera_runtime.inc:767`）在 Cinemachine push 时覆盖主相机 lens FOV。

上游把 FOV 摘出重载，是为了避开重载的副作用（重建全部相机状态、处理退出）。本仓要拿到同样的收益，必须让 FOV 变化**不再触发** `saveCameraSettings()`——而 `CAMERA_CONFIGURATION` 偏好是**游戏进程的引导配置**（`ModuleConfigurations.java:25` 读它 → `RuntimeBootstrap.prepare(..., configs, ...)`），不同步更新会导致**重启后 FOV 回退**。因此这一步只能通过拆分 `setCameraSettings`（写偏好 / 下发分离）实现，属**中等风险、须真机验证**的改动，不宜与"新增通道"混在一批。

**（4）其余依赖缺口**（供第 3、5 项落地时补齐）

| 上游符号 | 本仓现状 |
|---|---|
| `RuntimeBootstrap.loaded()` | 缺访问器（有 `private static volatile boolean loaded`） |
| `ModuleSettings.getGlobalFieldOfView` / `saveGlobalFov` | 命名不同：本仓为 `getGlobalFov(ctx)`（返回 float）/ `setGlobalFov(ctx, enabled, value)` |
| `BemOptions.appearance(entry, value)` | **缺**（上游 `BemOptions` 123 行 / 本仓 93 行）——`OverlayWritePolicy.apply` 依赖它 |
| `AndroidGlobalFov(enabled, fov)` | 缺（`android_camera.h` 只有 `AndroidMmdCommand` / `AndroidMmdStatus` / `AndroidCameraValues`） |
| `g_android_global_fov_ready` | 缺（上游独立原子量；本仓等价门禁是 `g_push_state_hook_ready && g_state_layout.ready`，`module.cpp:383/465`） |
| `OverlayGeometry.java` | 缺（上游 63 行；第 4 项页面用到） |
| `android/app/src/test/`、`testHost/` 源集 | 本仓**无测试源集**；上游的 `OverlayWritePolicyTest`（106 行）/ `OverlayGeometryTest` / `ModuleSettingsFovTest` 无法直接落地 |

**（5）附带更正**：§六 第 4 项的"293 行需重写"低估了形态差异——本仓悬浮窗已是 **Compose**（`OverlayPanel.kt` + `HomePage.kt` / `CameraMotionPage.kt` / `SettingsPages.kt` / `ToolPages.kt` / `MmdPage.kt` / `ExperiencePage.kt`），上游的 `OverlaySettingsPage` 是 View/XML，因此第 4 项是"**按功能重写一个 Compose 页**"而非"移植页面"，且需一并处理 `OverlayFeatures` 的控件可见性门禁。

### 10.5 第 5 项已完成：全局 FOV 免重载下发（`d3c7fa4`，2026-10-06）

**做法**（比 §10.4 预判的规模小得多——不需要 relay 新行、新 JNI 通道、新契约，也不需要 `GlobalFovUpdater` 轮询线程）：

| 侧 | 改动 |
|---|---|
| `native/modules/camera/module.cpp` | 新增 `ParseGlobalFovCommand()` + `DrainGlobalFovCommand()`（+100 行）；前向声明后由 `PumpFromEngineTick` 在 `DrainConfigurationReload()` 之后 drain，与 `mmd` / `camera_config` 共用既有 pump |
| `ModuleSettings.java` | 新增 `onlyGlobalFovChanged()` / `globalFovCommand()`（+49/−1）；`setCameraSettings` 末尾按前者选择命令名：`global_fov` 或 `camera_config` |

**为什么发送方是设置 app 而不是游戏进程轮询**：本仓 `ModuleCommandRouter.issue()` 已封装"优先 `FrameworkSettings.writeRemoteCommand` → 游戏进程 `command.next` → relay `c` 行 → pump"的完整链路，设置 app 直接调用即可送达。上游需要 `GlobalFovUpdater` + JNI，是因为它的 `NativeCommandBridge` 是 JNI 直调（§10.4-2）；本仓在设置 app 侧发命令反而更短，也符合本仓"app 只是遥控器"的既有分工。

**边界（有意排除）**：只有 **`global_fov=` 值变化**才走轻量命令。`global_fov_enabled=` 变化仍走全量重载——开关决定 override 是否存在，且 `g_state`（`module.cpp:3057`）与 `g_global_fov_enabled` 门禁（`2965-2971`）都由配置应用建立，轻量命令不碰这些。`CAMERA_CONFIGURATION` 偏好**两种情况都照旧提交**，故重启后 FOV 不回退（`ModuleConfigurations.java:25` 读它做引导配置）。

**原生命令体与拒绝语义**：`enabled=<bool>\nvalue=<float>`，**两个键都必须出现**；未知键、不可解析的布尔、非有限值一律 `AcknowledgePanelCommand("rejected")` 并记日志——**不使用 `ParseBoolean`**，因为它的默认值兜底会把拼错的键（如 `enable=`）静默当成 `off`。范围 `[kFreeMinFov, kFreeMaxFov]`（5–150）；门禁 `g_push_state_hook_ready && g_state_layout.ready`，不满足则拒绝并记日志（而非静默接受）。写入顺序**先 `g_global_fov` 后 `g_global_fov_enabled`**，与 `ScopedGlobalFovState`（`free_camera_runtime.inc:773/786`）的读取顺序对齐，避免 enabled 先于度数可见。

**证据**

| 判据 | 结果 |
|---|---|
| `aarch64-linux-android24 -fsyntax-only` | 0 错误（6 条既有 `unused function` 告警，均为 `_WIN32` 专有） |
| `:app:assembleDebug` | BUILD SUCCESSFUL（原生 + Kotlin + Java） |
| `.so` | `48790264 → 48837728` 字节；SHA-256 `0e727f8f… → 3472b79f…` |
| `llvm-strings` | 三个新日志字面量 + 命令名 `global_fov` **均在 `.so` 内**；同法对照既有 `Camera configuration reloaded` = 1 命中（证明方法有效） |
| **设备外判定表**（真实 `onlyGlobalFovChanged` / `globalFovCommand`） | 7 例全过：值变→true；开关变／无关行变／混合变／文本相同／行数不等／无旧文本→**均 false**（落回重载） |
| **设备外线格式** | `globalFovCommand` 产出的键**恰为 `enabled` / `value`**（与 `ParseGlobalFovCommand` 一致），且值原样穿过（`95.0` / `true`） |

**踩坑记录（本次）**：验证字符串时先用了 `strings`，但**本机无该命令**，`grep -qF` 拿到空输入、全部报"未命中"——差点把"改动没进产物"当成结论。改用 NDK 自带的 `llvm-strings.exe` 后全部命中。**根因**：管道左侧命令不存在时，`2>/dev/null` 会把"命令未找到"也吞掉，只剩恒假的比较结果。**规则**：二进制字符串核验一律用 `$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-strings.exe`，并**必须**带一个既有字面量作阳性对照。

**未验证**：真机生效（须 `PJX110` 且相机模块在进程内）。预期日志：`Global FOV set to <n> degrees without reloading the configuration.`；被拒时 `Global FOV rejected: expected enabled=<bool> and value=<5..150>.` 或 `Global FOV unavailable: the camera state or the push hook was not resolved.`；状态串见 `status.txt` 的 `applied` / `rejected`。

### 10.6 第 3+4 项已完成：跨进程设置通道 + 模型管理悬浮窗页（`30503d3`，2026-10-06）

**（1）按 §10.4-1 的判定合批**：通道（第 3 项）与页面（第 4 项）合成一个提交 `30503d3`（16 文件 / +1055 −4）。§10.4-4 列的 5 处缺口，3 处补齐、2 处不再需要：

| §10.4-4 缺口 | 处置 |
|---|---|
| `RuntimeBootstrap.loaded()` | 补访问器（`+11`） |
| `BemOptions.appearance(entry, value)` | 补齐（`+16`）——`OverlayWritePolicy.apply` 依赖它 |
| `ModuleSettings` 的 FOV 命名不同 | **不逐字对齐上游命名**，改为新增 `applyGlobalFov(ctx, enabled, value)`（`+37`），内部复用本仓既有 `getGlobalFov/setGlobalFov` + `withIniValue` 重写 |
| `AndroidGlobalFov` / `g_android_global_fov_ready` | **不需要**——FOV 已走 §10.5 的 `command_pump` 路线，通道侧只读 `fovState(app)` 字符串 |
| `OverlayGeometry.java`（63 行） | 未移植；页面用 Compose 布局 + `Modifier.alphaIf` 自绘 |
| 上游测试源集（`OverlayWritePolicyTest` 等 106 行） | 本仓无测试源集 ⇒ 改为**设备外 harness 直跑真实生产类**（`OverlayPolicyHarness`，**PASS: 56 checks / 0 failures**），不用"编译通过"代替判定 |

**（2）两处与上游不同的实现口径（有意为之）**

- **令牌交付走远程偏好，不落 app 私有 XML**：`OverlayWriteAuthorization` 只把令牌写进 `AtomicFile("overlay-write-authorization")`（读取上限 65 字节），再由 `FrameworkSettings.publish()` 经 `service.getRemotePreferences("module_settings")` 交给游戏进程。
- **补丁语义**：`OverlayWritePolicy` 保持零 Android 依赖（可设备外判定），模型分支要求合法 UUID 且数组长度 ≥2，FOV 分支允许 `enabled` / `value` 单独出现且值域 5..150，**空补丁一律拒绝**；`BemInstaller` 侧失败回滚。

**（3）门禁**：`android/app/build.gradle.kts` 的 `verifyReleaseEntryPoints` 已把 `Ldev/betterendfield/android/OverlaySettingsProvider;` 加入 `manifestComponents`（R8 的按名解析面），否则 release 混淆会删掉类而 manifest 仍指向它。

**（4）证据**

| 判据 | 结果 |
|---|---|
| `:app:assembleDebug` | BUILD SUCCESSFUL；APK **78,972,061 B**，SHA-256 `2873eb10…c366`，versionName 3.4.2 / versionCode 30402 |
| dex 扫描（带阳性对照） | `OverlaySettingsProvider` 6 / `OverlaySettingsClient` 6 / `OverlayWritePolicy` 4 / `OverlayWriteAuthorization` 3 / `OverlayModelPageKt` 96，authority 串 `dev.betterendfield.android.overlay.settings` 命中 |
| `HLK-AL00`（无游戏） | Provider 注册成功、owner-only 鉴权生效、设置 app 四 tab 遍历无崩溃 |
| `PJX110`（`b992bd53`） | 安装成功（21:34:07）；**令牌 `50c7ba0c…c32e` 与 LSPosed 数据库 `module_configs`（group=`module_settings`）逐字一致**，`schemaVersion`=1 / `generation`=867 |
| 游戏进程侧 | 日志显示 Provider 被反复调用（`unfreeze uid: 10566` / `reason: Provider` / `SyncBinder`）；模块 attached，`native runtime loaded; ui=true camera=true actions=true`；已进入第一人称游玩 |

**（5）仍未验证**：悬浮窗「模型管理 / 游戏视野」页的**实际点选与拖动**（选模型、拉 FOV 滑条）须在 `PJX110` 上人工确认——设备外只能证到"策略判定与线格式正确"，证不到"触摸命中与落盘"。release 出包仍须走 CI（本机无 release 密钥库）。

**（6）顺带纠正两条此前判断**

- **LSPosed 远程偏好不是文件**：`service.getRemotePreferences(group)` 落在 `/data/adb/lspd/config/modules_config.db` 的 `module_configs` 表（value 为 Java 序列化），因此在 app 私有 `shared_prefs/module_settings.xml` 里查不到 `overlay_write_authorization_v1` 是**正确**行为，此前误判为"发布失败"。
- **`XposedServiceHelper` 经 Provider 代理取 Binder，不要求设置 app 被注入、也不要求它在 LSPosed scope 中**：实测设置 app 未在 scope 内，令牌仍成功发布、Provider 仍被游戏进程调用。此前"未注入 ⇒ 不发布"的判断被推翻。

---


