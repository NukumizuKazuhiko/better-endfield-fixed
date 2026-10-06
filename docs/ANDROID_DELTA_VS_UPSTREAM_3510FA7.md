# 上游 v3.5.1 相对本仓的安卓侧差量清单

**锚点**

| 角色   | 提交        | 说明                                                                            |
| ---- | --------- | ----------------------------------------------------------------------------- |
| 本仓   | `db021cd` | 分支 `codex/android-thirdparty-alignment`，versionName 3.4.2 / versionCode 30402 |
| 上游   | `d514f2f` | `Dr-hydra/Better-Endfield`，**v3.5.2**（2026-10-06，14 个提交）                      |
| 共同基线 | `9b1e895` | "Expand BEM v1.1/v1.2 runtime and tooling"                                    |

> **复核记录**：§零 / §零点五 / §0.3 / §一 的数字于 2026-10-06 23:25 按新锚点（本仓 `8b8a0b5 → 1c65eab`、上游 `3521627 → d514f2f`）**全部重算**。上一版数字为「上游领先 40 / 本仓领先 75 / 功能面 545 与 361 / 冲突面 201」，凡与其不同处一律以本版为准；差异来源见 §10.8 与 §11。**上游这 14 个提交（v3.5.2）含本仓 Android 编译面内的实质内容，分类见 §11。**
>
> **2026-10-07 00:25 再记**：本仓锚点第二次前移 `1c65eab → cbbca19`（第 11 项的三次实现 + 一次回归修复，共 4 个提交）。**§零 的桶计数有一处变动**：冲突面 207 → **208**、上游新增 270 → **269**（`core/jni_binding.h`、`RuntimeBootstrap.java` 由「纯未同步」升为「双方都改」），本仓功能面 367 → **368**；「本仓有旧版 122 / 独有独改 138 / 主动删除 22」三桶**不变**。§零点五 榜仍是 36 件（`jni_binding.h` gap = 0 不入榜），但 **`native_bridge.cpp` 由 gap 194 收敛到 90、名次第 9 → 第 13**。§十一 已按最终形态重写。
>
> **2026-10-07 01:00 再记（第 8 项落地）**：本仓锚点第三次前移 `cbbca19 → b0d7382`（第 8 项「运行时状态串」1 个提交，另两条为文档提交）。**§零 桶计数全部重算**：纯未同步 391 → **389**（= 本仓有旧版 **122** + 上游新增 **269 → 267**）、冲突面 208 → **210**、本仓独有 160 → **162**（独有独改 138 → **140**，主动删除 **22** 不变），本仓功能面 368 → **372**（新增 4 件）。校验：`389 + 210 = 599` ✓、`210 + 162 = 372` ✓。**§零点五 榜仍是 36 件**（两个新件 gap = 0 不入榜），但 **`native_bridge.cpp` 由 gap 90 收敛到 40、名次第 13 → 第 23**，`camera/module.cpp` 169 → **154**、`GameOverlay.java` 184 → **161**。执行记录见 §10.9。
>
> **2026-10-07 02:00 再记（第 6 项落地）**：本仓锚点第四次前移 `b0d7382 → db021cd`（第 6 项「MMD 本地音轨」1 个代码提交）。**§零 桶计数全部重算**：纯未同步 389 → **388**（= 本仓有旧版 **122** + 上游新增 **267 → 266**）、冲突面 210 → **211**、本仓独有 162 → **168**（独有独改 140 → **146**，主动删除 **22** 不变），本仓功能面 372 → **379**（新增 7 件）。校验：`388 + 211 = 599` ✓、`211 + 168 = 379` ✓。三处涨落的来路：`MmdAudio.java` 由「纯未同步（上游新增）」升入**冲突面**（两侧同 blob `4f9cee72`，上游 +139 / 本仓 +139 ⇒ **gap = 0，不入榜**）；本仓独有 +6 件是宿主夹具（`testHost/mmd_audio_fixture.py` 1 件 + `testHost/mmd_audio/**` 5 件）。**§零点五 榜仍是 36 件，成员与排序集合未变**，只有 **`native_bridge.cpp` gap 40 → 25、真名次 23 → 25**（本仓侧 `+244/−2` → `+257/−2`）。执行记录见 §10.11。

**口径**：`git diff 9b1e895 3521627`，`+` = 上游有而本仓无，`−` = 本仓有而上游无。`.behw` 不计入差异——它是构建期产物（工作树里 1668 个，全在 `android/app/build/` 下），由 `android/.gitignore:3:**/build/` 忽略，两侧版本库均无此物，来源是 `-PheadwearCatalogDir`。

> **统计前必读**：① `research/`（2718 文件）与 `docs/`（162）必须剔除，否则"上游改了 3418 个文件"虚高一个量级；② `git diff` 默认给非 ASCII 路径加引号，会让 `^(research|docs)/` 过滤**漏掉 12 个文件** ⇒ 一律用 `git -c core.quotepath=false diff --name-only`。本文所有数字均按此重算。

**一句话结论**：页面层零重叠（上游 61 个类 / 本仓 65 个，29 个上游独有、33 个本仓独有、32 个同名）；可直移的是**引擎类**（MMD 安装器、跨进程设置通道、模型覆盖层工具），必须重写的是**所有页面**。原生侧只有三块需要动：`ui/module.cpp`（PC UI）、`custom_model`（模型覆盖层）、`shared/android_compat`（**只增不覆**）。

**进度（2026-10-07 01:00）**：§六 清单一至五项 **+ 第 8 项（运行时状态串，见 §10.9）已落地并编译验证**，**第 11 项（上游 v3.5.2 的 PC 布局鼠标捕获，见 §11）已落地并真机通过**。第 11 项走了三版（中继 → 上游 JNI → 帧泵接线），真机结论为「滑动连续转向、点击生效」。**两条关键更正**：① §11.2.2 —— 原「`NativeCommandBridge` 上的 JNI 一律不可直移」的铁律**已作废**，可用上游的 `BindContextLoaderNatives` + context classloader 前置越过 classloader 边界；② §11.2.3 —— 鼠标无效的**真根因是帧泵缺链**（`DispatchAndroidFrame()` 本仓全仓无调用方），与传输层无关。原生侧三块中 `ui/module.cpp`、`android_compat/android_frame.cpp`、`android_compat/android_pc_mouse.h` **均已与上游同步**；**最大剩余缺口仍是 `native/modules/custom_model/module.cpp`，gap 1491 → 1845**（上游 v3.5.2 又在该文件加了 815 行），其余为第 6、7、10 项（第 8 项本轮完成、第 9 项随第 11 项完成，依赖闭包排序见 §10.9.1）。

---

## 零、量化总览

### 0.1 锚点与领先量

```bash
git fetch upstream --prune
BASE=$(git merge-base HEAD upstream/main)   # 9b1e895
git rev-list --count $BASE..upstream/main   # 54
git rev-list --count $BASE..HEAD            # 81
```

| 项       | 值                                                               |
| ------- | --------------------------------------------------------------- |
| 共同基线    | `9b1e895`「Expand BEM v1.1/v1.2 runtime and tooling」（2026-09-26） |
| 上游 HEAD | `d514f2f`（**v3.5.2**，2026-10-06），领先 **54** 个提交                  |
| 本仓 HEAD | `b0d7382`（2026-10-07 01:00），领先 **84** 个提交                       |
| 上游改动文件  | 功能面 **599**（剔 `research/` + `docs/`）                            |
| 本仓改动文件  | 功能面 **372**                                                     |

> 与上一版（`3521627` / `8b8a0b5`）的差：上游 +14 提交（v3.5.2：PC 布局鼠标捕获、custom-model BEM 1.4 与 Android LOD 一批、Workshop 导航、mod-center 双语）⇒ 功能面 545 → **599**；本仓 +6 提交（第 11 项的三次实现 + 一次回归修复 + 文档）⇒ 功能面 361 → **368**（2026-10-07 再记：`1c65eab` 时为 367；锚点前移到 `cbbca19` 后本仓新增 `core/jni_binding.h`，`RuntimeBootstrap.java` 由「未改」升为「已改」）。**2026-10-07 01:00 再记**：本仓 +3 提交（第 8 项 1 个 + 文档 2 个）⇒ 功能面 368 → **372**（新增 `core/runtime_status.h`、`RuntimeSnapshot.java`、`testHost/runtime_snapshot_fixture.py`、`testHost/.../RuntimeSnapshotHostTest.java` 共 4 件）。

### 0.2 文件层四分法（功能面）

```bash
BASE=9b1e895
git -c core.quotepath=false diff --name-only $BASE..upstream/main | grep -vE '^(research|docs)/' | sort > up.txt
git -c core.quotepath=false diff --name-only $BASE..HEAD          | grep -vE '^(research|docs)/' | sort > our.txt
comm -23 up.txt our.txt   # 纯未同步
comm -12 up.txt our.txt   # 冲突面
comm -13 up.txt our.txt   # 本仓独有
```

| 桶                | 数       | 定义                   | 处置                           |
| ---------------- | ------- | -------------------- | ---------------------------- |
| **纯未同步 · 本仓有旧版** | **122** | 上游改、本仓自基线未动，且文件在本仓树里 | 直接取上游版（无冲突）                  |
| **纯未同步 · 上游新增**  | **267** | 上游改、本仓树里根本没有         | 按 §三 编译面分级取舍                 |
| **冲突面**          | **210** | 双方自基线都改过             | 逐 hunk / 小侧前向移植，**绝不整文件覆盖**  |
| **本仓独有 · 独有独改**  | **140** | 本仓新增或改、上游未动          | **保护**，合并时不得被覆盖              |
| **本仓独有 · 主动删除**  | **22**  | 本仓删、上游未动             | 本仓有意为之（Compose 迁移删 View/XML） |

**校验**：`122 + 267 + 210 = 599` = 上游功能面 ✓；`210 + 140 + 22 = 372` = 本仓功能面 ✓

> **较上一版的变化**（逐件核对，非估算）：
>
> - **上游新增桶 231 → 270 → 269**：主体是 v3.5.2 的 `tools/CustomModel/**`（BEM 1.4 工具链与 drafts）、`native/tests/**` 回归夹具、`native/modules/custom_model/*.inc`，另含本仓 Android 编译面内的 `android_pc_mouse.h` / 两个 `.inc`（**已在本轮移植入本仓**，§11）与 `PcUiMouseBridge.java`（同）。**再记（10-07）**：`core/jni_binding.h` 随第 11 项移植入本仓，该桶 −1（270 → 269）。
> - **冲突面 201 → 207 → 208（+7）**：v3.5.2 触及的 `native/modules/ui/module.cpp`、`native/shared/android_compat/android_frame.cpp`、`native_bridge.cpp`、`XposedEntry.java`、`BemOptions.java`、`tools/CustomModel/bem_tool.py`；**再记（10-07）**另加 `core/jni_binding.h`（本仓新增）与 `RuntimeBootstrap.java`（本仓新增改动）。其中 `ui/module.cpp`、`android_frame.cpp`、`XposedEntry.java`、`native_bridge.cpp` 本轮**均已按上游同步**（§11）—— `native_bridge.cpp` 的差异**不再是"有意保留"**，它的 pc-mouse JNI 面已移植并真机通过。
> - **本仓有旧版 113 → 122（+9）**：上游在新批次里又改了一批自基线未动的文件（`native/tests/android_world_binding_tests.cpp`、`native/modules/custom_model/model_job_runtime.inc` 等）。
> - **本仓独有（138 独改 + 22 主动删除）两桶三轮完全不变** —— 这两桶才是"零冲突可直接取"的口径。
>
> - **2026-10-07 01:00 第 8 项落地后的移动**（逐件复核）：**纯未同步 391 → 389**（`core/runtime_status.h`、`RuntimeSnapshot.java` 两件由「上游新增」升为「双方都改」⇒ 上游新增 269 → **267**；本仓有旧版 **122** 不变）；**冲突面 208 → 210**（同两件）+ 本仓独有 **160 → 162**（新增 2 件宿主夹具，属独有独改 ⇒ 138 → **140**）。**注意**：这两件是"上游新增、本仓也新增"⇒ 落入冲突面而**不是**"本仓独有"，判据仍是 §0.2 的四分法口径，不要按"我新写的文件就算我独有"直觉归类。

> **"上游独有"≠"上游新增"**：22 个本仓主动删除的文件（`ColorWheelView.java`、`ValueSlider.java`、15 个 `res/drawable/bg_*.xml`、3 个 `res/color/*.xml`、2 个 `res/layout/bem_spinner_*.xml`）在 `git diff 本仓 上游` 里同样显示为"上游新增"，实际是本仓删掉的。判"真缺 vs 被替换"必须过一遍 `git cat-file -e HEAD:<path>`。

### 0.3 目录分布

**（a）进 Android 编译面 / 与安卓功能相关**

| 目录                          | 上游改 | 本仓改     | 纯未同步 | 冲突面    |
| --------------------------- | --- | ------- | ---- | ------ |
| `native/modules`            | 147 | 102     | 67   | **80** |
| `android/app`               | 103 | **137** | 47   | **56** |
| `native/shared`             | 30  | 23      | 8    | **22** |
| `tools/CustomModel`         | 69  | 16      | 53   | 16     |
| `tools/ThirdPartyModules`   | 7   | 6       | 1    | 6      |
| `tools/FirstPersonProfiles` | 6   | 6       | 0    | 6      |
| `android/resources`         | 9   | 0       | 9    | 0      |
| `tools/CombatDataExporter`  | 4   | 1       | 3    | 1      |

**（b）上游自带回归测试**

| 目录             | 上游改 | 本仓改 | 纯未同步   | 冲突面 |
| -------------- | --- | --- | ------ | --- |
| `native/tests` | 84  | 25  | **76** | 8   |
| `ui/tests`     | 4   | 0   | 4      | 0   |

**（c）不进本仓 Android 编译面（只记不移）**

| 目录                     | 上游改 | 本仓改 | 纯未同步 | 冲突面 | 性质                              |
| ---------------------- | --- | --- | ---- | --- | ------------------------------- |
| `web`                  | 29  | 6   | 27   | 2   | 下载站 / 数据可视化前端（含本轮 `mod-center`） |
| `scripts`              | 28  | 0   | 28   | 0   | 发布 / 打包脚本                       |
| `config`               | 11  | 0   | 11   | 0   | 桌面配置                            |
| `manifests`            | 6   | 0   | 6    | 0   | WPF 清单                          |
| `ui/BetterEndfield.UI` | 44  | 4   | 40   | 4   | Windows WPF 桌面端                 |
| `tools/HookInlineScan` | 5   | 0   | 5    | 0   | 依赖 minhook                      |

> **上一版此表只列了 9 行**，漏掉 `web` / `scripts` / `config` / `manifests` / `ui/tests` / `tools/FirstPersonProfiles` / `tools/CombatDataExporter` 共 7 个目录（其中 `scripts`、`web` 各 28 件，量级与 `native/shared` 相当）。本版补齐，并**明确标注（c）类不进本仓 Android 编译面** —— 这是此前把"上游改了 3400+ 文件"读成压力来源的原因。
>
> 两侧都最重的两个目录即主战场：`native/modules` 与 `android/app`（本仓改 133 > 上游 97，方向已反转）。`native/tests` 上游的 67 件里 **66 件本仓一件没有** —— 那是上游自带回归测试，是本仓唯一可用的验收判据。

### 0.4 与既有清点文档的数字差异（本次更正）

| 项           | 清点文档原文（`3510fa7`） | 上一版实测（`8b8a0b5` / `3521627`） | **本版实测（`1c65eab` / `d514f2f`）** |
| ----------- | ----------------- | ---------------------------- | ------------------------------- |
| 上游功能面改动文件   | 538（`3510fa7`）    | 545（`3521627`）               | **599**                         |
| 纯未同步        | 342               | 344（= 113 + 231）             | **391（= 122 + 269）**            |
| 冲突面 / 本仓功能面 | 196 / 356         | 201 / 361                    | **208 / 368**                   |
| 本仓独有        | 未拆                | 160 = 138 + 22               | **160（三轮均未变）**                  |

> 三列（现为四列）数字**口径完全一致**，差异只来自锚点前移：上游 +15 提交、本仓 +9 提交。清点文档 `ANDROID_UNPORTED_FEATURES_20261005.md` 的逐条功能面仍以 `3510fa7` 为基线，**数字已再次过时**，但其"未同步功能清单"的条目本身未失效。

---

## 零点五、冲突面 211 件：按上游领先排序

`gap = (上游 +/−) − (本仓 +/−)`，按**总改动行数**（`+` 与 `−` 之和）之差排序；`git diff --numstat $BASE..<ref>` 双测后 join。**gap ≥ 7 的 36 件全列于下；其余 175 件 gap ≤ 6（其中 127 件 gap = 0，即双方改动量相当，多为同一功能各改各的）。**

| #  | 文件                                                                | 本仓        | 上游             | gap      | 判定                                                    |
| -- | ----------------------------------------------------------------- | --------- | -------------- | -------- | ----------------------------------------------------- |
| 1  | `native/modules/custom_model/module.cpp`                          | +108/−55  | **+1640/−368** | **1845** | 最大缺口 · **在 Android 编译面内**（v3.5.2 又 +192/−162）         |
| 2  | `native/tests/custom_model_binding_tests.cpp`                     | +18/−0    | +1170/−14      | 1166     | 上游测试，本仓只有 18 行                                        |
| 3  | `native/tests/android_world_binding_tests.cpp`                    | +168/−0   | +808/−0        | 640      | 上游测试                                                  |
| 4  | `ui/BetterEndfield.UI/MainWindow.xaml.cs`                         | +71/−1    | +582/−75       | 585      | Windows WPF · 不移                                      |
| 5  | `tools/CombatDataExporter/export_combat_data.py`                  | +25/−7    | +249/−280      | 497      | 工具                                                    |
| 6  | `native/modules/camera/first_person_runtime.inc`                  | +433/−71  | +775/−77       | 348      | **第一人称专有 ⇒ 取本仓**                                      |
| 7  | `ui/BetterEndfield.UI/MainWindow.xaml`                            | +66/−0    | +314/−33       | 281      | Windows WPF                                           |
| 8  | `android/.../cpp/modules/custom_model/world_resource_adapter.inc` | +45/−22   | +268/−79       | 280      | Android 世界资源绑定                                        |
| 9  | `android/.../java/.../GameOverlay.java`                           | +747/−379 | +785/−502      | 161      | 悬浮窗宿主                                                 |
| 10 | `native/modules/camera/module.cpp`                                | +1405/−91 | +1433/−217     | 154      | 重度分叉 · 绝不整体替换                                         |
| 11 | `android/.../java/.../BemInstaller.java`                          | +187/−6   | +263/−77       | 147      | BEM 安装器                                               |
| 12 | `native/modules/custom_model/bem.cpp`                             | +444/−71  | +561/−88       | 134      | 中度分叉                                                  |
| 13 | `android/.../cpp/native_bridge.cpp` | **+257/−2** | +242/−42 | **25**（第 6 项后） | JNI 桥 · pc-mouse 面**已按上游移植并真机通过（§11.2）**，第 8 项回填 `runtimeStatus` 导出（§10.9），第 6 项再复用同一 `jclass` 出参接 `AndroidLocalMusicApi()` 要解析的那份类（§10.11）⇒ gap **194 → 90 → 40 → 25**，**三轮都不是欠账**；名次 **第 9 → 第 13 → 第 23 → 第 25** |
| 14 | `native/modules/custom_model/mod_registry.cpp`                    | +42/−4    | +119/−12       | 85       | 注册表                                                   |
| 15 | `android/.../java/.../BemOptions.java`                            | +22/−1    | +100/−3        | 80       | 选项 · v3.5.2 又改                                        |
| 16 | `README.en.md`                                                    | +32/−148  | +130/−122      | 72       | 文档                                                    |
| 17 | `native/modules/custom_model/generic_model_matcher.h`             | +158/−0   | +222/−0        | 64       | 通用匹配                                                  |
| 18 | `tools/CustomModel/bem_v11.py`                                    | +36/−11   | +77/−28        | 58       | 工具                                                    |
| 19 | `android/.../cpp/modules/custom_model/custom_model_module.cpp`    | +11/−10   | +61/−17        | 57       | 模块入口                                                  |
| 20 | `native/CMakeLists.txt`                                           | +49/−0    | +99/−1         | 51       | **只增不覆**                                              |
| 21 | `tools/CustomModel/package_toolchain.py`                          | +6/−3     | +51/−7         | 49       | 工具                                                    |
| 22 | `native/tests/generic_model_matching_tests.cpp`                   | +142/−0   | +187/−0        | 45       | 上游测试                                                  |
| 23 | `android/.../AndroidManifest.xml`                                 | +17/−0    | +57/−1         | 41       | 清单                                                    |
| 24 | `README.md`                                                       | +26/−254  | +104/−206      | 30       | 文档                                                    |
| 25 | `native/modules/custom_model/bem.h`                               | +60/−4    | +90/−4         | 30       | 头                                                     |
| 26 | `tools/CustomModel/bem_tool.py`                                   | +94/−22   | +114/−22       | 20       | 工具 · v3.5.2 又改                                        |
| 27 | `android/.../java/.../BemInstalledResources.java`                 | +18/−5    | +30/−11        | 18       | 资源物化                                                  |
| 28 | `android/.../res/layout/activity_main.xml`                        | +0/−531   | +49/−500       | 18       | ★ 本仓删除、上游改写                                           |
| 29 | `native/modules/actions/pose_overlay.inl`                         | +1/−1     | +18/−0         | 16       | actions                                               |
| 30 | `native/modules/custom_model/mod_registry.h`                      | +19/−1    | +33/−2         | 15       | 头                                                     |
| 31 | `android/.../cpp/core/runtime.cpp`                                | +93/−6    | +89/−21        | 11       | 宿主封装 · 已并入上游 `ReadFieldObject`（§10.7）                 |
| 32 | `tools/CustomModel/bem_projects.py`                               | +9/−1     | +17/−2         | 9        | 工具                                                    |
| 33 | `android/.../cpp/CMakeLists.txt`                                  | +34/−1    | +39/−4         | 8        | **只增不覆**                                              |
| 34 | `android/.../cpp/installer/install_jni.cpp`                       | +2/−0     | +7/−3          | 8        | 安装器                                                   |
| 35 | `android/.../java/.../BemInstallActivity.java`                    | +0/−203   | +26/−185       | 8        | ★ 本仓删除、上游改写                                           |
| 36 | `tools/CustomModel/bem_export.py`                                 | +81/−0    | +88/−0         | 7        | 工具                                                    |

> 路径缩写：`android/.../` = `android/app/src/main/`；`android/.../java/.../` = `android/app/src/main/java/dev/betterendfield/android/`；`android/.../cpp/` = `android/app/src/main/cpp/`；`android/.../cpp/modules/…` 同理。

**榜面变动（逐件复核，非估算）**：上一版 31 件 → 本版 **36 件**。**再记（2026-10-07）**：锚点前移 `1c65eab → cbbca19` 后 **`native_bridge.cpp` 由 gap 194 收敛到 90（第 9 → 第 13）**，其余 35 行数字全部未变（第 11 项触及的另外 3 个 Java/native 文件 gap 均为负，不进榜）。**2026-10-07 01:00 再记（第 8 项）**：榜仍是 **36 件**；`core/runtime_status.h` 与 `RuntimeSnapshot.java` 两侧行数相同 ⇒ **gap = 0，不入榜**；`native_bridge.cpp` gap 90 → **40、第 13 → 第 23 名**，`camera/module.cpp` 169 → **154**（仍第 10），`GameOverlay.java` 184 → **161**（仍第 9）。**2026-10-07 02:00 再记（第 6 项）**：榜仍是 **36 件**、成员与排序集合未变；`MmdAudio.java` 升入冲突面但两侧同 blob ⇒ **gap = 0，不入榜**；`native_bridge.cpp` gap 40 → **25、第 23 → 第 25 名**。**掉出的仍是 `native/modules/ui/module.cpp`**（第 2 项与第 11 项两次并入上游，**本轮再次同步后仍不在榜**）；**新进榜 6 件**全部来自 v3.5.2：`native/modules/custom_model/{mod_registry.cpp, bem.cpp, bem.h, mod_registry.h}`（上游 BEM 1.4 重构）、`android/.../BemOptions.java`、`tools/CustomModel/{bem_v11.py, bem_tool.py, bem_export.py}`。三处值得注意：

- `camera/module.cpp` **269 → 169 → 154**（本仓 +1290/−91 → +1405/−91，持续投入）；
- `BemInstaller.java` **173 → 140**（本轮新增 `saveChanges()` 后向上游靠拢，见 §10.6）；
- `core/runtime.cpp` **11**（自 §10.7 起未动；余下 11 是本仓独有的其他改动，不是未同步量）；
- `native_bridge.cpp` **180 → 194 → 90 → 40 → 25**（先纳入 v3.5.2 的 pc-mouse JNI 入口，第 11 项又**按上游移植该面**并真机通过，第 8 项回填 `runtimeStatus` 导出，第 6 项复用同一 `jclass` 出参接 `InitializeAndroidMusic` ⇒ 本仓侧由 `+192/−2` 涨到 `+257/−2`，gap 收敛到 **25**，**名次第 13 → 第 25**；剩余 25 为本仓独有部分，如 relay 侧的历史导出）；
- `camera/module.cpp` **169 → 154**、`GameOverlay.java` **184 → 161**（第 8 项在这两个文件内的落点，见 §10.9）。

**其中 6 件是「本仓整文件删除、上游同期又改写」**（本仓侧 `+0/−N`）：


| 文件                                    | 本仓       | 上游        | gap      | 在主榜    |
| ------------------------------------- | -------- | --------- | -------- | ------ |
| `MainActivity.java`                   | +0/−1031 | +189/−406 | **−436** | 否      |
| `res/layout/activity_main.xml`        | +0/−531  | +51/−500  | +20      | 是（#23） |
| `BemInstallActivity.java`             | +0/−203  | +26/−185  | +8       | 是（#30） |
| `SettingRow.java`                     | +0/−157  | +2/−44    | **−111** | 否      |
| `res/layout/activity_bem_install.xml` | +0/−111  | +3/−110   | +2       | 否      |
| `SectionCard.java`                    | +0/−110  | +2/−43    | **−65**  | 否      |

> 这 6 件**不是"上游新增"**，是本仓 Compose 迁移时删掉的 View/XML，上游同期又改了它们 ⇒ 合并时**不要按上游 resurrect**。

> **⚠ 口径盲区（本版新发现）**：这 6 件里 **4 件的 gap 是负数** —— 本仓删掉整个文件，改动行数（`+0/−N`）**反超**上游的改写量，于是按 gap 排名永远排不上榜。**"gap 排名"会系统性漏掉"本仓已删、上游又改"的文件**，判"上游新增 vs 本仓删除"必须另走 `git cat-file -e HEAD:<path>` 二分（命令见 §九），不能只看本表。（上一版把这 3 件单列在小表里是对的，本版保留此表并补上 gap 实测值。）

---

## 一、页面层：Java/Kotlin 类清点

统计口径：`android/app/src/main/java/dev/betterendfield/android/`

| 集合             | 数量     | 较上一版                                                                             |
| -------------- | ------ | -------------------------------------------------------------------------------- |
| 上游类总数          | **61** | 59 → 61（v3.5.2 新增 `PcUiMouseBridge`、`ThirdPartyModulesActivity`）                 |
| 本仓类总数          | **65** | 64 → 65（`PcUiMouseBridge` 移植入本仓）                                                 |
| A. 上游独有        | **29** | 28 → 29（−`PcUiMouseBridge` 移入 C；+`ThirdPartyModulesActivity`、+`PcUiMouseBridge`） |
| B. 本仓独有        | **33** | 32 → 33（新增 `OverlayModelPage.kt`）                                                |
| C. 两侧同名（含重度分叉） | **32** | 31 → 32（+`PcUiMouseBridge`，逐字直移，§11）                                             |

> **口径**：按 `android/app/src/main/java/dev/betterendfield/android/` 下的 `.java` / `.kt` **文件名集合**比较，与 `git ls-tree -r --name-only <ref> -- <dir>` 一致（校验：`65 − 33 = 32`，`61 − 29 = 32` ✓）。

### A. 上游独有 32 类（按可移植性分三档）

#### A1 — 引擎/逻辑类：**可近乎直移**（9 个，共 1,155 行）

无 `extends View/Activity/ContentProvider`，只依赖 `Context + SharedPreferences + java.*`。

| 类                               | 行   | 职责                                                                      | 本仓对应物                                                                                                       |
| ------------------------------- | --- | ----------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `MmdInstaller.java`             | 263 | MMD 作品安装/重发布/移除，`installed_mmd_works` 索引，单线程 worker + `busy/status` 静态量 | 本仓 `MmdLibraryInstaller.java`(358) + `MmdLibraryFiles.java`(155) + `MmdSlotFiles.java`(240) → **功能同源、接口不同** |
| `MmdImportArchive.java`         | 223 | ZIP/7z 解包，预算控制（总 1 GiB / 音频 512 MiB / 4096 条目 / 深度 16），zip-slip 防护      | 本仓无等价物（本仓走 `MmdImportSession` 另一路径）                                                                         |
| `MmdInstalledResources.java`    | 85  | 把索引里的资源物化进游戏目录                                                          | 本仓 `MmdLibraryFiles.materialize` 同职责                                                                        |
| `MmdAudio.java`                 | 139 | 游戏进程内 MMD 音频播放（`MediaPlayer` + `HandlerThread`，串行 token，seek 队列）        | 本仓**无**（本仓音频走原生）                                                                                            |
| `RuntimeSnapshot.java`          | 46  | 解析 `BE_RUNTIME_V1\n<id>=<state>\n` 运行时状态串                               | 本仓 `RuntimeLog.java`(149) 是另一套                                                                              |
| `BemImportRequest.java`         | 68  | 从 `Intent` 提取 `.bem` URI（VIEW/SEND、`EXTRA_STREAM`、`ClipData` 合并）        | 本仓 `BemInstallActivity.kt` 内联                                                                               |
| `BemImportStream.java`          | 67  | 流式拷贝 + `BEM\0PKG\0` magic 校验，上限 2 GiB                                   | 本仓**无独立件**                                                                                                  |
| `BemHotSwitchUpdater.java`      | 50  | 模型热切换守护（轮询远端 prefs 变化 → 下发）                                             | 本仓**无**（热切换整体未移植）                                                                                           |
| `ThirdPartyRuntimeUpdater.java` | 41  | 第三方模块运行时守护，同模式                                                          | 本仓**无**                                                                                                     |

**判据**：这 9 个类没有 Android UI 依赖，只吃 `Context`/`SharedPreferences`/`java.*`，前向移植后由本仓 Compose 页面调用即可。真正的工作量在**接口适配**（本仓 `Mmd*` 三件套已被 `MmdPage.kt` 依赖，不能简单替换）。

#### A2 — 页面/Activity 类：**必须按功能重写**（9 个，共 2,547 行）

View/XML + `Activity`。本仓是 Compose，**不可直移**。

| 类                                  | 行   | 上游页面                                | 本仓 Compose 对应                                                                                           |
| ---------------------------------- | --- | ----------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `MainActivity.java`                | 814 | 6 页签主壳                              | `MainActivity.kt`(93) + `SettingsShell.kt`(243) + `SettingsState.kt`(2101)                              |
| `BemInstallPage.java`              | 503 | 第三方模型（BEM）页                         | `BemInstallScreen.kt`(351) + `BemInstallState.kt`(357) + `BemInstallActivity.kt`(70)                    |
| `MmdLibraryActivity.java`          | 423 | MMD 作品库页                            | `MmdPage.kt`(591)                                                                                       |
| `EnhancementSettingsActivity.java` | 346 | 增强功能二级页（5 页）                        | `ExperiencePage.kt`(261) + `CameraMotionPage.kt`(207) + `FirstPersonPage.kt`(273) + `ToolPages.kt`(294) |
| `OverlaySettingsPage.java`         | 293 | **v3.5.1 新增**：悬浮窗「游戏视野」+「模型管理」双 tab | 本仓**无任何对应**                                                                                             |
| `MmdImportActivity.java`           | 195 | MMD 导入页                             | 本仓 `MmdPage.kt` 内联导入                                                                                    |
| `AboutPage.java`                   | 180 | 关于页                                 | 本仓 `SettingsPages.kt` 内                                                                                 |
| `BemInstallActivity.java`          | 44  | `.bem` 导入口（`exported=true`）         | `BemInstallActivity.kt`(70)                                                                             |
| `FrameworkServiceWait.java`        | 30  | 框架服务等待页                             | 本仓 `HomePage.kt` 内                                                                                      |

#### A3 — 自绘控件/样式类：**并入本仓 Compose 组件库**（10 个，共 876 行）

上游手写 `View`/`LinearLayout` 组件。本仓有 `UiComponents.kt`(1019) + `UiTheme.kt`(107) + `UiTokens.kt`(174) 等价体系 ⇒ **不移植，只做视觉/交互对齐**。

| 类                            | 行      | 职责                                                                                           |
| ---------------------------- | ------ | -------------------------------------------------------------------------------------------- |
| `ColorWheelView.java`        | 201    | 色轮（登录主题色选择）                                                                                  |
| `ValueSlider.java`           | 159    | 标签+数值+`SeekBar` 的滑条行                                                                         |
| `SponsorDialog.java`         | 146    | 赞助弹窗（微信/爱发电/PayPal + 保存赞赏码到相册）                                                               |
| `SettingRow.java`            | 115    | 开关行（`Switch` + 主副标题）                                                                         |
| `ControlIcon.java`           | 76     | `Canvas` 手绘悬浮窗图标（7 种）                                                                        |
| `ThirdPartyModulesPage.java` | **77** | 第三方模块列表页块（上游 `3521627` 加「下载站」按钮 +7；本仓该页为 `ThirdPartyModulesActivity.kt`，**不跟进**，理由见 §10.8-3） |
| `SectionCard.java`           | 69     | 卡片容器                                                                                         |
| `OverlayGeometry.java`       | 63     | 悬浮窗布局/贴边几何（`usableArea` / `normalized` / `nearestHorizontalEdge`）— **纯数学，可直移**               |
| `GlobalFovUpdater.java`      | 27     | 500 ms 轮询权威 prefs → `NativeCommandBridge.globalFov`                                          |
| `BemHotSwitchUpdate.java`    | 32     | 热切换状态数据类                                                                                     |

> 注：`OverlayGeometry` / `GlobalFovUpdater` 两个虽列在 A3，但**无 View 依赖**，实为可直移项。
>
> **已移出 A3（§11 移植入本仓）**：`PcUiMouseBridge.java`(326) —— 上游把它列在页面层旁，但它不继承任何 `View`/`Activity`，只经 `Application.ActivityLifecycleCallbacks` + 反射操作 Unity 的 View，实为**引擎类**；本仓逐字直移（替换点只在 `NativeInput` 实现的三个方法上，见 §11）。因此 A 桶 29 由「A1 9 件 + A2 9 件 + A3 10 件 + `ThirdPartyModulesActivity` 1 件」构成。
>
> **已移出 A3（第 3 项移植入本仓）**：`OverlaySettingsProvider.java`(92) / `OverlayWritePolicy.java`(93) / `OverlayWriteAuthorization.java`(50) / `OverlaySettingsClient.java`(44) 四件 —— 第 3 项**已完成**，它们不再是"上游独有"，现归 §一 C 桶（同名）。因此 A3 由 14 个 1,155 行降为 **10 个 876 行**，A 桶总数 32 → **28**。

### B. 本仓独有 33 类（上游已删除或从未有）

| 类                                                                                                                                                                                                                                                                                                                                                              | 行               | 性质                                                                                                                                                                                         |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `GyroscopeController.java`                                                                                                                                                                                                                                                                                                                                     | 294             | **本仓核心增量**：陀螺仪输入链                                                                                                                                                                          |
| `HeadwearAssetStore.java` / `HeadwearAssets.java`                                                                                                                                                                                                                                                                                                              | 209 / 23        | 头饰资产物化（`-PheadwearCatalogDir`）                                                                                                                                                             |
| `OverlayViewOwners.java`                                                                                                                                                                                                                                                                                                                                       | 21              | 悬浮窗宿主追踪                                                                                                                                                                                    |
| `RuntimeLog.java` / `RuntimeJournalProvider.java`                                                                                                                                                                                                                                                                                                              | 149 / 78        | 本仓运行时日志通道（上游改用 `RuntimeSnapshot` + `runtime_status.h`）                                                                                                                                     |
| `MmdLibraryFiles.java` / `MmdLibraryInstaller.java` / `MmdSlotFiles.java`                                                                                                                                                                                                                                                                                      | 155 / 358 / 240 | 本仓 MMD 落地链（上游用 `MmdInstaller` 单件）                                                                                                                                                          |
| `CameraVmdFile.java`                                                                                                                                                                                                                                                                                                                                           | 90              | VMD 文件物化（跨 UID 流式落地）                                                                                                                                                                       |
| Compose 层：`SettingsShell` `SettingsPages` `SettingsState` `UiComponents` `UiTheme` `UiTokens` `HomePage` `ExperiencePage` `MmdPage` `FirstPersonPage` `CameraMotionPage` `BemInstallScreen` `BemInstallState` `ThirdPartyModulesActivity`(kt) `MainActivity`(kt) `OverlayPanel` `OverlaySurface` `OverlayControls` `OverlayTheme` `FloatingHandle` `ToolPages` | 4,624           | 全部 UI，与上游零交集                                                                                                                                                                               |
| `OverlayModelPage.kt`                                                                                                                                                                                                                                                                                                                                          | **613**         | **本轮新增（第 4 项）**：悬浮窗「模型管理 / 游戏视野」页，Compose 重写，含 `OverlaySwitch`/`OverlayPicker`/`OverlaySlider` 三个自绘控件。**上游对应物是 `OverlaySettingsPage.java`（View/XML，在 A2），故本件计入 B 桶而非 C** ⇒ B 桶 32 → **33** |

### C. 两侧同名 31 类（差异规模，按行数降序）

> **本桶 +4（27 → 31）**：`OverlaySettings{Client,Provider}` 与 `OverlayWritePolicy` / `OverlayWriteAuthorization` —— 第 3 项本轮把这 4 件从 A 桶移植入本仓，故由"上游独有"转为"同名"。

| 类                                                                                                                                                                                           | 上游新增 | 本仓独有   | 判定                                                                                                                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---- | ------ | ------------------------------------------------------------------------------------------------------------------------- |
| `ModuleSettings.java`                                                                                                                                                                       | 234  | 1098   | **重度分叉**：本仓用 Compose 键集，上游是 View 键集 + v3.5.1 的 FOV 重构                                                                     |
| `GameOverlay.java`                                                                                                                                                                          | 791  | 853    | **重度分叉**：上游 7 页签 View，本仓 `OverlayPanel/Surface`+`FloatingHandle`                                                          |
| `XposedEntry.java`                                                                                                                                                                          | 59   | 504    | **重度分叉**：本仓多出整套 LSPosed 双进程/IPC 装配                                                                                        |
| `FrameworkSettings.java`                                                                                                                                                                    | 75   | 250    | **重度分叉**：本仓远端快照字段不同                                                                                                       |
| `RuntimeBootstrap.java`                                                                                                                                                                     | 35   | 221    | **重度分叉**：本仓 `Os.setenv` 注入面更大                                                                                             |
| `NativeCommandBridge.java`                                                                                                                                                                  | 33   | 146    | **重度分叉**：本仓是文件分帧中继，上游是 JNI                                                                                                |
| `MmdImportPlan.java`                                                                                                                                                                        | 72   | 253    | 中度：上游重写了归档规划                                                                                                              |
| `MmdImportSession.java`                                                                                                                                                                     | 65   | 233    | 中度                                                                                                                        |
| `MmdVmdParser.java`                                                                                                                                                                         | 12   | 68     | 中度                                                                                                                        |
| `BemInstaller.java`                                                                                                                                                                         | 202  | 171    | 中度：上游拆出 `BemImportStream`/`BemImportRequest`                                                                              |
| `BemInstalledResources.java`                                                                                                                                                                | 19   | 13     | 轻度                                                                                                                        |
| `BemOptions.java`                                                                                                                                                                           | 37   | 7      | 轻度                                                                                                                        |
| `Hotkeys.java`                                                                                                                                                                              | 2    | 31     | 轻度                                                                                                                        |
| `OverlayFeatures.java`                                                                                                                                                                      | 10   | 18     | 轻度：本仓多了 `vmdCamera` 与本轮新增的 `models`（第 8 字段，门禁「模型管理」入口）                                                                    |
| `ModuleCommandRouter.java`                                                                                                                                                                  | 2    | 15     | 轻度                                                                                                                        |
| `ModuleConfigurations.java`                                                                                                                                                                 | 7    | 15     | 轻度                                                                                                                        |
| `OverlayWritePolicy.java`                                                                                                                                                                   | 93   | 93     | **本轮移植** · 逐字直移（纯逻辑、Android-free；另有设备外 harness 56 项断言全绿）                                                                  |
| `OverlaySettingsProvider.java`                                                                                                                                                              | 92   | **97** | **本轮移植** · 本仓版比上游多 5 行（fov 分支改用 `OverlayWritePolicy.requireRevision`，模型分支接 `BemInstaller.saveChanges/disableAll`，见 §10.6） |
| `OverlayWriteAuthorization.java`                                                                                                                                                            | 50   | 50     | **本轮移植** · 逐字直移（`AtomicFile` 令牌，读取上限 65 字节）                                                                               |
| `OverlaySettingsClient.java`                                                                                                                                                                | 44   | 44     | **本轮移植** · 逐字直移（单线程 daemon executor + post 回主 Looper）                                                                     |
| `ThirdPartyModuleStore.java`                                                                                                                                                                | 2    | 16     | 轻度                                                                                                                        |
| `ThirdPartyRuntimeMaterializer.java`                                                                                                                                                        | 1    | 1      | 基本一致                                                                                                                      |
| `ActionPoseAssets` `AstcSupport` `BemParameters` `ModelPresetIndex` `ModuleApplication` `ThirdPartyModuleActivity` `ThirdPartyModulePackage` `VoiceCatalogIndex` `VoiceCatalogMaterializer` | 0    | 0      | **逐字相同**                                                                                                                  |

---

## 二、res 资源差量

| 类别                    | 上游有我方无 | 差异                                                                                                                                                                                                               | 归属（实测）                                                         |
| --------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `res/drawable/`       | 16 个   | `bg_avatar` `bg_badge` `bg_card` `bg_chip` `bg_ghost_button` `bg_header` `bg_input` `bg_notice` `bg_primary_button` `bg_setting_row` `bg_status` `bg_tab` `bg_tabs` `bg_voice_row` `ic_info` `bg_sponsor_button` | **15 个是本仓主动删除**（Compose 迁移，非上游新增）；只有 `bg_sponsor_button` 是上游新增 |
| `res/layout/`         | 5 个    | `activity_main` `activity_bem_install` `view_bem_install` `bem_spinner_item` `bem_spinner_dropdown_item`                                                                                                         | **4 个是本仓主动删除**；只有 `view_bem_install` 是上游新增                     |
| `res/color/`          | 3 个    | `switch_thumb` `switch_track` `tab_text`                                                                                                                                                                         | **3 个全是本仓主动删除**                                                |
| `res/raw/`            | 1 个    | `sponsor_wechat.png`                                                                                                                                                                                             | 上游新增（赞助入口）                                                     |
| `res/values*/`        | 8→6 个  | 上游把 `model_overlay.xml` **改名**为 `model_management.xml`，新增 `overlay_settings.xml`（透明度/自动贴边）                                                                                                                       | 上游新增                                                           |
| `AndroidManifest.xml` | M      | 见下                                                                                                                                                                                                               | 双方都改                                                           |

> **第 2 / 4 项落地后的变化**：上游 `res/values*/` 的 `overlay_settings.xml`（悬浮窗设置页的 View 布局）**本仓不需要** —— 第 4 项用 Compose 重写（`OverlayModelPage.kt`，§一 B 桶）；上游 `strings.xml` 里本仓缺的 `ui_pc` / `ui_pc_hint` **已在第 2 项补齐**（照上游中文文案，§10.3）。其余缺失项仍按本表口径：**多数是本仓 Compose 迁移的主动删除，不是上游领先**。

**Manifest 差异**（核验过）：

```xml

<provider android:name=".OverlaySettingsProvider"
    android:authorities="dev.betterendfield.android.overlay.settings"
    android:exported="true" android:grantUriPermissions="false" />



<provider android:name=".RuntimeJournalProvider"
    android:authorities="${applicationId}.journal"
    android:exported="true" />
```

**本仓现有两个 `exported=true` 的 ContentProvider**（第 3 项落地前只有一个）：

| Provider                  | 来源                     | 职责                                                                                                                                                                                                                                           |
| ------------------------- | ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `OverlaySettingsProvider` | **第 3 项新增**（`30503d3`） | 跨进程**写**设置：只实现 `call()`，`query`/`insert`/`update`/`delete` 全抛 `SecurityException("Unsupported")`（已核源码 :92–:96）；鉴权失败抛 `SecurityException("Caller cannot modify module settings")`（:30）。UID + 令牌双因子 + revision 乐观并发 + 有界补丁（`MAX_PATCH=16 KiB`） |
| `RuntimeJournalProvider`  | 本仓原有（上游无此物，属本仓独有类）     | 只读日志/运行时状态                                                                                                                                                                                                                                   |


⇒ **Manifest 是"新增"而非"替换"**：本仓保留自己的 journal provider。其余 Activity 两侧均为 `exported=false`，只有导入口不同（上游 `.BemInstallActivity` + `.ThirdPartyModuleActivity`，本仓 `.BemInstallActivity` + `.ThirdPartyModulesActivity` + `.ThirdPartyModuleActivity`）。

**strings 差量**：本仓 282 条 vs 上游 170 条（命名体系不同，不可直接比对）。上游独有的 74 条中，**属于功能声明**的关键项：

```
ui_pc / ui_pc_hint                      ← PC 界面布局开关（界面增强卡片第 3 个开关）【第 2 项已落地并真机验证，§10.3 / §10.7】
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

| 变化                                   | 文件                                                                                                                                                                                                                                                          |
| ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 上游**新增**                             | `core/jni_binding.h`（统一 `BindContextLoaderNatives`：走当前线程 `ClassLoader.loadClass` 注册 natives，**不做双命名空间兜底**）、`core/runtime_status.h`（`RuntimeStatus` 类，产出 `BE_RUNTIME_V1\n<id>=<state>\n`）                                                                    |
| 上游**删除**（本仓独有）                       | `core/panel_commands.cpp`、`input_relay.cpp`、`modules/camera/first_person_look_probe.{cpp,h}`、`modules/custom_model/android_headwear_canary.cpp`                                                                                                             |
| 双方都有、内容不同                            | `CMakeLists.txt`、`core/command_pump.{cpp,h}`、`core/hook_broker.cpp`、`core/log.{cpp,h}`、`installer/install_jni.cpp`、`modules/custom_model/*`(10 件)、`modules/desktop/desktop_module.{cpp,h}`、`modules/login_model/login_model_module.cpp`、`native_bridge.cpp` |
| `core/runtime.{cpp,h}` — **本轮已同步上游** | 上游强化版 `ReadFieldObject`（`field_get_flags` 判静态 + 枚举字面量的 `System.Enum.Parse` 回退）原是本仓最致命的漏同步项：它让 `ui/module.cpp` 在两平台行为不同、PC UI 开关静默失效。现已按上游实现补齐（`af1d9cd`，§10.7），本仓仅余文件内的既有分叉（gap 11）                                                                         |

> **注意**：上游删除 `panel_commands.cpp` / `input_relay.cpp` 是**架构重整**，不是功能删除——上游把面板命令与输入中继的 JNI 面收进 `jni_binding.h` + `RuntimeStatus`。本仓这套文件是**本仓 MMD 命令分帧链的载体**（`input_relay.cpp` 承担 `\u001f` 折叠还原），**不可照上游删除**。
>
> **✅ 派生结论（2026-10-07 更正，原铁律已作废）**：**`NativeCommandBridge` 上的 JNI 方法可以直移**，前提是同时补齐两个前置：① `core/jni_binding.h` 的 **`BindContextLoaderNatives`**（走当前线程 context classloader 的 `loadClass` 拿 `jclass`，再 `RegisterNatives`）；② `RuntimeBootstrap.load()` 里在 `loadIntoTargetNamespace` **之前**把当前线程 context classloader 临时指向模块族、`finally` 还原。原理：`RegisterNatives` 挂在 **Class 对象**上，故能越过 classloader 边界；而 context classloader 决定 `BindContextLoaderNatives` 能 `loadClass` 到谁。
>
> 原铁律（"凡上游新增的 JNI 方法一律不可直移，本仓该类是单向文件中继"）写于只看到症状、未看到上游解法的阶段，**已被本仓真机实证推翻**：`pcMouseCaptureRequested` / `pcMouseCaptured` / `pcMouseMotion` 三个方法已逐字照搬并跑通（§11.2.2）。
>
> **再推一步**：`input_relay.cpp` **不是"JNI 不可行"的产物，而是"当时没找到注册点"的产物** ⇒ 后续凡以"本仓是文件中继"为由拒绝的直移项，都要用这个新前置**重判一次**。

### 三.2 — `native/` 共享模块（Android 与 Windows 同源）

| 路径                                                             | 上游新增              | 本仓独有                    | 处置                                                                                                                                                                                                                                                                                                             |
| -------------------------------------------------------------- | ----------------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `native/modules/ui/module.cpp`                                 | ~~121~~ **已并入**   | ~~48~~                  | **已完成（`2d4ef64` + `f2c79f5`）**：`pc_ui_enabled` 解析 + `g_keyboard_input_type` + PC 分支 + 250 ms 漂移重检 + `DetourChangeInputType` 全部合入；**本仓已从冲突面 gap 榜掉出**（原 gap 139 → 现不在榜）。真机证据见 §10.3 / §10.7                                                                                                                     |
| `native/modules/custom_model/module.cpp`                       | 1451              | 262                     | **缺模型覆盖层**：上游加了 `model_job_runtime.inc`(1021) `async_loading.h`(638) `generic_model_matcher.{h,inc}` `model_asset_cache.inc` `model_content_identity.h` `model_overlay_host.h` `model_overlay_protocol.h` `model_overlay_hotkey.h` `android_lod_relations.generated.h` 等（含 Windows-only 的 `overlay/` 目录，本仓不适用） |
| `native/modules/camera/*`                                      | A 3 / D 21 / M 6  | —                       | **本仓保留 21 件上游已删的第一人称文件**（`first_person_cap_upload.inc`、`facing_runtime.inc`、`motion_runtime.inc`、`readback_runtime.inc`、`retract_runtime.inc`、`scale_runtime.inc`、`headwear_fixture.h` …）；`first_person_runtime.inc` 双方重度分叉（上游 +792 / 本仓 −456）。**第一人称区域一律保留我方**                                                |
| `native/shared/android_compat/android_win32.cpp`               | 35                | 227                     | ⚠️ **禁区**：本仓含 `AddVirtualMouseDelta` / `DrainVirtualMouseDelta`（陀螺仪 + look pad 注入链），上游没有。**只增不覆**                                                                                                                                                                                                              |
| `native/shared/android_compat/include/android_win32.h`         | 11                | 56                      | ⚠️ 同上                                                                                                                                                                                                                                                                                                          |
| `native/shared/android_compat/android_virtual_keys.h`          | 2                 | 25                      | ⚠️ 同上                                                                                                                                                                                                                                                                                                          |
| `native/shared/android_compat/android_panel_commands.h`        | 0                 | 34                      | 本仓独有，上游已删                                                                                                                                                                                                                                                                                                      |
| `native/shared/host/hook_broker.{cpp,h}`                       | 311+52            | 62+6                    | 分叉：上游新增 `hook_diagnostics.{cpp,h}`(197+40)                                                                                                                                                                                                                                                                     |
| `native/shared/host/host_runtime.{cpp,h}` `module_manager.cpp` | 27+4+3            | 6                       | 轻度                                                                                                                                                                                                                                                                                                             |
| `native/shared/input/hotkey.h`                                 | 99（vs 基线**新增文件**） | 96（我方 `ba18649` 同期独立新增） | **实为冲突面**：两侧各自新建，非"上游领先"；我方版本已含 `Input::IsDown` / `Input::ParseKey`，第 2 项无前置缺口                                                                                                                                                                                                                                 |
| `native/shared/include/BetterEndfield/ModuleApi.h`             | 6                 | 0                       | 轻度                                                                                                                                                                                                                                                                                                             |

---

## 四、构建与打包

| 文件                                                         | 上游新增 | 本仓独有     | 处置                                                                                                                                                                                                                                                                                           |
| ---------------------------------------------------------- | ---- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `android/app/build.gradle.kts`                             | 116  | 457      | **重度分叉**。上游 v3.5.1 新增 `signingConfigs.persistentRelease`（pin 到 3.5.0 已发布证书）+ `buildStagingDirectory` + `noCompress += "bin"`；本仓有 CI 签名与 headwear 相关配置。**本轮新增**：`verifyReleaseEntryPoints` 的 `manifestComponents` 加入 `Ldev/betterendfield/android/OverlaySettingsProvider;`（R8 keep 门禁，§10.6） |
| `android/settings.gradle.kts`                              | 63   | 0        | 上游把构建根与工具链路径配置化                                                                                                                                                                                                                                                                              |
| `android/workspace.gradle.kts`                             | 24   | 0        | 上游**新增文件**：工作区路径配置                                                                                                                                                                                                                                                                           |
| `android/build.gradle.kts`                                 | 2    | 17       | 轻度                                                                                                                                                                                                                                                                                           |
| `android/app/proguard-rules.pro`                           | 1    | 75       | 本仓独有 keep 规则（**必须保留**：本仓名字在 JVM 外被解析）                                                                                                                                                                                                                                                        |
| `android/keystore/bem-debug.keystore` + `debug.properties` | 0    | 删除       | 上游**删掉了 debug 密钥库**（改由 CI 生成）。本仓保留——**本机唯一的签名材料**                                                                                                                                                                                                                                            |
| `android/gradlew`                                          | 0    | 0        | 仅权限位：本仓 `100755` → 上游 `100644`                                                                                                                                                                                                                                                               |
| `android/AGENTS.md`                                        | 新增   | —        | 上游新增仓库内 AI 协作说明                                                                                                                                                                                                                                                                              |
| `android/tools/CheckOverlayHost.ps1`                       | 35   | —        | 上游新增悬浮窗宿主自检脚本                                                                                                                                                                                                                                                                                |
| `android/tools/HeadwearAssetStoreTest.java`                | —    | 121（上游删） | 本仓头饰夹具                                                                                                                                                                                                                                                                                       |
| `android/Test-FirstPersonPreflight.ps1`                    | —    | 删除       | 本仓已删的预检脚本                                                                                                                                                                                                                                                                                    |
| `android/.gitignore`                                       | M    | M        | 轻度                                                                                                                                                                                                                                                                                           |

---

## 五、`android/resources/` 生成物

| 文件                                                                      | 上游新增    | 说明             |
| ----------------------------------------------------------------------- | ------- | -------------- |
| `manifests/voice/voice-event-media-manifest.json`                       | 460,561 | 配音事件媒体清单（差异大头） |
| `manifests/model/action-manifest.json`                                  | 35,107  | 模型动作清单         |
| `resource-source.json`                                                  | 6,077   | 资源来源表          |
| `character-presets.json`                                                | 444     | 角色预设增补         |
| `manifests/shared/resource-manifest-report.md`                          | 新增      | 清单报告           |
| `voice-index-android-evidence.json` / `voice-index-merge-evidence.json` | 新增      | 配音索引证据         |
| `character-names.json` / `voice-catalog-index.json`                     | M       | 轻度             |

这些是**上游构建期产的资源索引**，本仓若跟进配音/模型清单需按本仓 `-PheadwearCatalogDir` 流水线重新生成，不宜直接拷贝。

---

## 六、可执行移植清单（按性价比排序）

| #  | 项                                                 | 落点                                                                                                                         | 规模                                                                   | 风险                                                                                                                                       |
| -- | ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| 1  | **修 PC UI 隐式推送** — **已完成**（`2d4ef64`）             | `native/modules/ui/module.cpp` 的 `PumpInputType()`                                                                         | ~5 行（`!active && restore < 0` 直接返回）                                  | 已落地（§10.1）                                                                                                                               |
| 2  | **移植完整 PC UI**（含 `ui_pc` 开关） — **已完成**（`f2c79f5`） | 同上 + Compose 界面增强页加第 3 个开关                                                                                                 | 上游 +121 行                                                            | 已落地并**真机验证**：`pc_ui_effective=true`、`Input type pushed to 0`、游戏切到桌面 Keyboard 布局。唯一前置（IL2CPP 解析 `Keyboard`）曾失败，根因在宿主封装层，已修（§10.3 / §10.7） |
| 3  | **跨进程设置通道** — **已完成**（`30503d3`）                  | `OverlaySettings{Client,Provider}` + `OverlayWritePolicy/Authorization` + Manifest 组件                                      | **269 行**新增 + 补齐 §10.4-4 的 3 处缺口（另 2 处由 §10.5 的 `command_pump` 路线免掉） | `exported=true` 组件已按安全审落地：只实现 `call()`，UID + 令牌双因子（§10.6）                                                                                |
| 4  | **模型管理悬浮窗页** — **已完成**（`30503d3`）                 | 按功能重写为 Compose 页（`OverlayModelPage.kt`，含 3 个自绘控件）                                                                          | 约 560 行；`OverlayFeatures` 已加 `models` 第 8 字段门禁                       | 已落地并真机跑通；**滑条/选择器的实际交互须人工确认**（§10.6）                                                                                                     |
| 5  | **全局 FOV 运行时下发** — **已完成**（`d3c7fa4`）             | 走本仓既有的 `command_pump`：原生 `DrainGlobalFovCommand()` + 设置 app 在"仅 `global_fov=` 行变化"时改发 `global_fov` 命令                      | **149 行**（原生 +100 / Java +49）                                        | 已落地并编译验证；**须真机**确认生效（§10.5）                                                                                                              |
| 6  | **MMD 安装器统一** — **已完成**（`db021cd`，2026-10-07） | `MmdAudio` + `MmdInstalledResources`（**真新增**）；`MmdInstaller` / `MmdImportArchive`（←→ 本仓 `MmdLibraryInstaller`+`MmdLibraryFiles`，**同源分叉对账**）；2 个上游独有 Activity 属页面层不合并 | 139 + 85（真新增）/ 263 + 223（对账） | 已落地并编译验证（§10.11）。`MmdAudio` 是**真缺口**：本仓 `local_music_android.cpp` 与上游零差异、按名解析其四个方法，但那四个方法不存在、`InitializeAndroidMusic` 从未被调用 ⇒ 本仓 MMD 作品**没有配乐**；`MmdInstalledResources` 经核本仓 `MmdSlotFiles.prepareWorks` **已覆盖**（原子发布 + 剪未广告代际）⇒ 无需移。**未引入** `commons-compress` / `xz`，本仓后端保留，页面层不合并 ⇒ 上游侧仅 139 行（与上游同 blob `4f9cee72`）。**游戏内听声待真机验收** |
| 7  | **模型热切换** | Java 侧 `BemHotSwitchUpdater` + `BemHotSwitchUpdate` **确实只有 82 行**；但消费方在 `native/modules/custom_model/`（`ReloadRegistryAtDelivery` + `g_hot_switch_runtime` + `model_job_runtime.inc`） | 82（Java）；原生 **16 文件 / ~2,700 行** | **与 `custom_model` 最大缺口合并为一项**（§10.10.1）。原判「native 侧依赖 `model_overlay_host.h`」仍属实但非主要障碍；照抄 JNI 会静默无效 —— 本仓 `ResourceConfigurationChanged` 缺 `[CustomModel]` 分支 |
| 8  | **运行时状态串** — **已完成**（`b0d7382`，2026-10-07）                                        | `RuntimeSnapshot` + `core/runtime_status.h`                                                                                | 46 + 22                                                              | 已落地并编译验证：原生侧 13 处写入 + JNI `runtimeStatus`（与 pc-mouse 同法绑定）；Java 侧 `RuntimeSnapshot` 逐字照搬；消费点接在本仓悬浮窗日志头 —— **刻意不重演"有生产者、无消费者"**（§11.2.3 的教训），并有**设备外**宿主夹具（23 项断言，§10.9）。遗留：上游第二个消费点 `OverlaySettingsPage` 本仓是 Compose，未接；`camera.speed/fov` 尚未被本仓 UI 读取（上游用浮点解析器读，`number()` 只解析整数）                                                                                                                 |
| 9 | ~~**JNI 注册收敛**~~ — **本轮顺带完成**（`460dd11`） | `core/jni_binding.h` | 45 | **已落地**：为接回 pc-mouse / `frame` 的 JNI 面而引入 `BindContextLoaderNatives`，与上游**逐字相同**（§11.2.2） |
| 10 | **构建配置化** ~~不适用~~ | `workspace.gradle.kts` + `settings.gradle.kts` + `scripts/workspace_config.py` + `config/` 11 件 | 87 + 208 + configs | **判为「不适用（Android-only 本仓）」**（§10.10.2）：上游 schema 是 Windows 桌面多平台工作区（ResConv / EndfieldUnpacker / wwiser / dobby / iscc / `resource_update`）；本仓 `config/` 不存在、`app/build.gradle.kts` 不读 `beWorkspace`；SDK 位置归一已由 `android/local.properties` 承担 |
| 11 | **PC 布局相对鼠标桥** — **已完成并真机通过**（`1c65eab` → `460dd11` → `6541856`，收尾 `cbbca19`） | `android_pc_mouse.h` + `android_frame.cpp` + 两个 `.inc` + `ui/module.cpp`（原生，逐字）+ `core/jni_binding.h`（逐字）+ `PcUiMouseBridge.java`（逐字）+ `native_bridge.cpp` 的 4 个 JNI 导出 + `XposedEntry` 帧泵 | 上游 1458 行；本仓净 **+1430 / −12，13 文件**（新增 6 件） | **已真机通过**：滑动连续转向、点击生效（§11.3）。走三版 —— 真根因是**帧泵缺链**（§11.2.3），非传输层 |

> **进度（2026-10-07 02:00）**：**第 1~6、8、11 项全部落地**（`2d4ef64` / `f2c79f5` / `d3c7fa4` / `30503d3` / `b0d7382` / `db021cd` / `1c65eab`+`460dd11`+`6541856`+`cbbca19`，另宿主同步 `af1d9cd`），其中第 8 项已编译验证 + 设备外夹具 23 项断言全过（§10.9）、第 6 项同样 23 项断言全过（§10.11）、第 11 项已真机通过（§11.3）。第 11 项来自**上游 v3.5.2**，不在本文档原清单内 —— 它是第 2 项（PC UI）的直系后续：强制桌面输入类型之后，游戏隐藏光标并改读 `Mouse X`/`Mouse Y`，而 Android 的绝对触摸路径永远不喂这两个轴，视角到屏幕边缘就停住。三版演进、真根因与证据见 §11。
>
> **剩余项：只有第 7 项**（第 9 项随第 11 项顺带完成；第 8 项上一轮完成；第 6 项本轮完成并按用户口径「只移两件真新增」落地，见 §10.11；第 10 项经实测**不适用**）。**执行序按「依赖闭包从小到大」定**，但两轮实测下来，三项**性质互不相同、不可按文件数排序**（第二轮逐符号/逐文件实测见 §10.10）——下表保留，供第 7 项与后续复查使用：
>
> | 序 | 项 | 第二轮实测 | 处置 |
> |---|---|---|---|
> | ✅ | **第 6 项 MMD 安装器统一** | 与上游**同源分叉**（`installed_mmd_works` 索引键两边相同）；4 件里 **2 件真新增**（`MmdAudio` 139 / `MmdInstalledResources` 85），另 2 件是分叉对账（上游 `MmdInstaller` 263 + `MmdImportArchive` 223 ←→ 本仓 `MmdLibraryInstaller`+`MmdLibraryFiles`）；新增依赖 `commons-compress` + `xz`；2 个 Activity 属页面层 ⇒ §七.5 **不合并**，按功能名重写 | **已完成**（`db021cd`）：只 `MmdAudio` 是真缺口，`MmdInstalledResources` 本仓已覆盖 ⇒ **未引入新依赖**、本仓后端保留。见 §10.11 |
> | 1 | 第 7 项 模型热切换 | **不是独立项**：Java 侧 82 行属实，但消费方在 `custom_model` 内部（`ReloadRegistryAtDelivery` + `g_hot_switch_runtime` + `model_job_runtime.inc` 1129 行）。上游同批缺席 **16 个文件 / 约 2,700 行**；本仓目录 14 件 vs 上游 32 件；`hot_switch` 命中 **0** | 与 `custom_model` 缺口**合并为一项**，子系统级前向移植，见 §10.10.1 |
> | — | 第 10 项 构建配置化 | 上游 `config/workspace.defaults.json` 的 schema 是 **Windows 桌面多平台工作区**（ResConv.exe / EndfieldUnpacker / wwiser / dobby / Inno Setup / `resource_update` 指向 1.5.3 Windows 游戏输入）；本仓 `config/` 不存在、`app/build.gradle.kts` 也不读 `beWorkspace` | **判为「不适用（Android-only 本仓）」**，唯一有价值子集（SDK 位置归一）已由 `android/local.properties` 承担，见 §10.10.2 |
>
> - **第 7 项不是"低风险顺手活"**：它的原生落点 `CustomModelModule::QueueConfiguration` 依赖 `g_update_ready` / `g_update_mutex` / `g_pending_update` / `ApplyPendingConfiguration` / `UpdateSharedReplacement` / `SharedRegistryText` 整套「共享替换事务」，本仓**全无**。第二轮再往下量一层，连**消费方**都不在本仓：`module.cpp:3316 ReloadRegistryAtDelivery()` + `module.cpp:49 g_hot_switch_runtime` + `module.cpp:3258 g_pending_registry_text/g_registry_request_mutex` + **`model_job_runtime.inc`（1129 行，本仓整个文件不存在）**。原判"native 侧依赖 `model_overlay_host.h`，该头在 `_WIN32` 内"**仍是事实，但已不是主要障碍**。唯一好消息：`ModuleApi.h` 的 `configuration_changed` 本仓已有，缝不用动 —— **但本仓的 `ResourceConfigurationChanged` 没有 `[CustomModel]` 分支**（上游 `module.cpp:3887` 才有），照抄 JNI 只会静默无效。
> - **第 10 项判为不适用**：它的依赖是 `scripts/workspace_config.py`（208 行）+ `config/` 11 件，而 `workspace.defaults.json` 声明的是**桌面**工具链与 Windows 资源更新流水线。本仓既无 ResConv / EndfieldUnpacker / wwiser / dobby / iscc，SDK 也不在仓库内（本机 `android/local.properties` = `sdk.dir=C:/Users/Vens_/AppData/Local/Android/Sdk` + `ndk.dir=D:/android-toolchain/android-ndk-r27c`，与上游"SDK 在 `toolchains/android/sdk`"正相反）。只落 `settings.gradle.kts` + `workspace.gradle.kts` 而不落 `config/` 会**立刻断构建**。
> - **第 6 项的关键判据**：本仓 `MmdLibraryInstaller` 与上游 `MmdInstaller` 的公共面**逐项对得上**（`busy` / `status` / `MAX_WORK_BYTES` / `WORKER = newSingleThreadExecutor()` / `start(Context,MmdImportSession,…)` / `remove(Context,generation)`），且索引键同为 `installed_mmd_works` ⇒ 同源分叉，**不是"710 行可直移"**。该库在 §八 D 列为「本仓领先」，整取会覆盖它。
>
> **上游 v3.5.2（14 提交）已分类完毕**（§11）：Android 编译面内 1 件已**完成并真机通过**（PC 鼠标桥）、4 件已按上游同步（`ui/module.cpp`、`android_frame.cpp`、`native_bridge.cpp` 的 pc-mouse 面、`XposedEntry.java` 的挂点）、**0 件"有意不取"**（原判"不可直移"已被 §11.2.2 推翻）、其余为 `custom_model`（最大缺口扩大至 gap 1845）、上游自带测试与不进编译面的 WPF / web / tools。

---

## 七、禁区（不可合并）

1. **`native/shared/android_compat/*`** — 只增不覆。覆盖会静默删掉 `AddVirtualMouseDelta` / `DrainVirtualMouseDelta`，断掉陀螺仪 + look pad 注入链。
2. **`android/app/proguard-rules.pro` 的本仓 75 行** — 名字在 JVM 外被解析，删了 R8 会吃掉入口。
3. **第一人称与输入线程区域** — `native/modules/camera/first_person_*` 与 `native/shared/input/*` 一律保留我方。
4. **`module.cpp`（`native/modules/camera/`）** — 双方重度分叉（+1339/−1322），绝不整体替换。
5. **页面层** — 32 个上游独有类全部是 View/XML，本仓 Compose 层不可合并，只能按功能名重写。
6. **`android/keystore/`** — 上游已删，本仓是本机唯一签名材料，**保留**。
7. **`android/app/src/main/cpp/input_relay.cpp` + `panel_commands.cpp`** — 上游删除是架构重整，本仓这两个文件是 MMD 命令分帧链载体，**不可跟删**。
8. ~~**`NativeCommandBridge` 上的 JNI 面** — 上游新增的 JNI 方法一律不可直移（本仓该类是文件中继）。~~ **本条已于 2026-10-07 作废** —— JNI 面**可以**直移，前提是同时补齐 `core/jni_binding.h` 的 `BindContextLoaderNatives` 与 `RuntimeBootstrap` 的 context classloader 前置。实证与原理见 §三.1 注与 §11.2.2。
9. **本仓主动删除的 22 件**（`ColorWheelView.java`、`ValueSlider.java`、15 个 `res/drawable/bg_*.xml`、3 个 `res/color/*.xml`、2 个 `res/layout/bem_spinner_*.xml`）— Compose 迁移的有意删除，**不要按上游 resurrect**（尤其中 6 件上游同期又改写过，见 §零点五）。

---

## 八、编译面分级与编译器实测

### 8.1 未移植面按「是否进本仓 Android 编译面」分级

| 级                  | 内容                                                                                                                                                                                                                                            | 处置                                                                                                         |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| **A 本仓不编译**        | `native/modules/{camera,custom_model}/overlay/*`（伴生进程 + `.rc`）、`native/shared/host/hook_diagnostics.{h,cpp}`、`tools/HookInlineScan`、`tools/ThirdPartyModules/echo`、桌面 WPF `ui/BetterEndfield.UI`、`tools/CustomModel/CreatorProjectChecks`（C#） | **只记不移**；只取源文件、**不取构建条目**（依赖 minhook / ws2_32 / mfplat / `.rc`）                                            |
| **B 本仓编译、仍是旧实现**   | `custom_model/module.cpp`、`world_resource_adapter.inc`、`camera/module.cpp`、`GameOverlay.java`、`BemInstaller.java` 等（`native_bridge.cpp` 的 pc-mouse 面已于 2026-10-07 按上游同步，见 §11.2）                                                                                                       | 子系统级前向移植，**绝不整文件覆盖**                                                                                       |
| **B′ 曾属 B、已同步**    | `native/modules/ui/module.cpp`（PC UI §10.3；PC 鼠标桥 §11）、`native/shared/android_compat/android_frame.cpp`（PC 鼠标状态机 §11）、`android/app/src/main/cpp/core/runtime.{cpp,h}`（`ReadFieldObject` §10.7）、**`android/app/src/main/cpp/native_bridge.cpp` 的 `runtimeStatus` 面与 `native/modules/camera/module.cpp` 的 `AndroidCameraValuesStatus()`（第 8 项 §10.9 —— 后者的 `camera.global_fov_ready` 一行本仓无对应标志位，**有意少报**）**                                                | **保持与上游一致，勿再退回旧版** —— `runtime.cpp` 的旧简版曾让 PC UI 开关在两平台行为不同、静默失效；`ui/module.cpp` 现仅余本仓两处日志 summary（+16/−2） |
| **C 数据 / 资源 / 版本** | `android/resources` 缺 6 件、`values-en/ja/ko/zh-rTW` 4 套、`combat_stats` 缺 2 头像、`tools/CustomModel` 缺 9 件                                                                                                                                        | 纯增量，风险低                                                                                                    |
| **D 反向：本仓领先**      | 兼容层陀螺仪（`AddVirtualMouseDelta`）、命令通道分帧、第一人称扩展、悬浮窗 Compose、MMD 作品库、CI                                                                                                                                                                           | **合并时不得被覆盖**                                                                                               |


`custom_model/module.cpp` 内新增件**是否进 Android 编译**（实测引入点守卫）：

| 新件                                             | 引入位置                                     | 平台               |
| ---------------------------------------------- | ---------------------------------------- | ---------------- |
| `async_loading.h`                              | module.cpp:8（无守卫）                        | Android 编译       |
| `model_content_identity.h`                     | module.cpp:9（无守卫）                        | Android 编译       |
| `generic_model_matcher.inc`                    | module.cpp:1858（无守卫）                     | Android 编译       |
| `model_asset_cache.inc`                        | module.cpp:3262（无守卫）                     | Android 编译       |
| `model_job_runtime.inc`                        | module.cpp:3589（无守卫）                     | Android 编译       |
| `android_lod_relations.generated.h`            | module.cpp:17，`#if defined(__ANDROID__)` | Android 专有       |
| `model_overlay_host.h` / `runtime_ini_win32.h` | module.cpp:21–22，`#if defined(_WIN32)`   | Windows 专有（不入本仓） |

### 8.2 编译器实测（NDK r27c）

```bash
D=D:/android-toolchain/android-ndk-r27c/toolchains/llvm/prebuilt/windows-x86_64
"$D/bin/aarch64-linux-android24-clang++.cmd" --target=aarch64-linux-android24 -U_WIN32 \
  -fsyntax-only -std=c++20 -I native/shared/include -I native/shared/android_compat … <上游新头>
```

| 上游新头                                                | 实测          | 说明                                                                                                               |
| --------------------------------------------------- | ----------- | ---------------------------------------------------------------------------------------------------------------- |
| `async_loading.h`                                   | **OK**      | Android-clean                                                                                                    |
| `model_content_identity.h`                          | **OK**      | Android-clean                                                                                                    |
| `runtime_ini.h`                                     | **OK**      | Android-clean                                                                                                    |
| `android_lod_relations.generated.h`                 | **OK（需前置）** | 单独编译报 `undeclared identifier 'std'/'GenericMatching'` 是**假阳性**——生成片段须在 `<span>` + `generic_model_matcher.h` 之后展开 |
| `model_overlay_host.h` / `win32_overlay_window.h` 等 | 不测          | 上游已用 `#if defined(_WIN32)` 排除                                                                                    |

> **已排除的坑**：宿主是 Windows 时 `_WIN32` 会被预定义，必须 `--target=aarch64-linux-android24` **且 `-U_WIN32`**，否则替身层 `Windows.h` 的 `#error` 会造成全量假阳性。

---

## 九、文档分工与复现命令

| 问题                   | 看哪份                                      |
| -------------------- | ---------------------------------------- |
| 到底有哪些文件不一样、数量与规模是多少  | **本文档 §零 / §零点五**                        |
| 改一个文件到底会不会进 `.so`    | `ANDROID_FEATURE_DEPENDENCY_TREE.md` §13 |
| 本仓有哪些功能、各自依赖哪些文件     | 同上 **F1–F11**                            |
| 上游有哪些功能是本仓没有的、依赖哪些文件 | 同上 **§14（U1–U9）+ §15**                   |
| 上游领先提交的逐条功能面 + 建议优先级 | `ANDROID_UNPORTED_FEATURES_20261005.md`  |
| 两份清点的口径核对（4 处冲突及定论）  | 同上 **§9**                                |

**复现本文全部数字**（仓库根执行）：

```bash
BASE=$(git merge-base HEAD upstream/main)   # 9b1e895
git -c core.quotepath=false diff --name-only $BASE..upstream/main | grep -vE '^(research|docs)/' | sort > up.txt
git -c core.quotepath=false diff --name-only $BASE..HEAD          | grep -vE '^(research|docs)/' | sort > our.txt
comm -23 up.txt our.txt | wc -l    # 389 纯未同步（内分 122 本仓有旧版 / 267 上游新增）
comm -12 up.txt our.txt | wc -l    # 210 冲突面
comm -13 up.txt our.txt | wc -l    # 162 本仓独有（140 独改 + 22 主动删除）
git diff --numstat $BASE..HEAD > ns_our.txt ; git diff --numstat $BASE..upstream/main > ns_up.txt   # 再 join 得 gap 排名
```

> **gap 的算法**：`gap = (上游 +行 + 上游 −行) − (本仓 +行 + 本仓 −行)`，按**总改动行数**而非净值 ⇒ **本仓整文件删除的条目 gap 会是负数、永不进榜**（§零点五 已注明该盲区）。

**二分「真缺 vs 被替换」**（判 231 个上游新增里哪些是真缺口）：

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

| 键                          | 语义                                            | 三方取值      |
| -------------------------- | --------------------------------------------- | --------- |
| `mobile_ui_enabled`        | 强制**触摸**布局（供桌面客户端伪装手机）                        | 恒 `false` |
| `platform_spoof_enabled`   | 伪装 Android / 云平台身份                            | 恒 `false` |
| `pc_ui_enabled`（v3.5.1 新增） | 强制 **Keyboard / 桌面**布局，**只改 inputType，不伪装平台** | 由用户开关决定   |

⇒ 第 2 项**可以按原优先级推进**，不构成立场反转。唯一真实门槛仍是 §六 第 2 项风险栏所列：IL2CPP 能否解析出 `DeviceInfo/InputType` 的 `Keyboard(=0)` 枚举值，失败则该功能整体不启用（`g_keyboard_input_type = -1` ⇒ `pc_active=false`，安全退化）。

### 10.3 第 2 项已完成（完整 PC UI，2026-10-06）

**改动落点（5 个文件）**

| 文件                             | 改动                                                                                                                                             |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `native/modules/ui/module.cpp` | 三方合并上游 `3510fa7` 全部改动（+115/−56）                                                                                                                |
| `ModuleSettings.java`          | `PC_UI_ENABLED` 常量、`isPcUiEnabled` / `setPcUiEnabled`、配置写出条件加 `pcUi`；**拼串抽成纯函数** `interfaceConfiguration(hideUid, hideHud, pcUi)`（行为不变，为设备外验证） |
| `SettingsState.kt`             | `pcUi` 状态 + 载入 + `updatePcUi` + `loadedModuleIds()` / `interfaceCardStatus` 条件                                                                 |
| `ExperiencePage.kt`            | 界面增强卡片第 3 个 `SwitchRow`                                                                                                                        |
| `res/values/strings.xml`       | `ui_pc` / `ui_pc_hint`（照上游中文文案）                                                                                                                |

**原生侧并入的内容**：`pc_ui_enabled` 解析；`g_keyboard_input_type`（Android 经 IL2CPP 元数据解析 `DeviceInfo/InputType.Keyboard`）；`DetourChangeInputType` 的 PC 分支；`PumpInputType` 250 ms 漂移重检 + `report_status` + 回读校验；`ParseVirtualKey` → `BetterEndfield::Input::ParseKey`；`PumpHudVisibility` → `Input::IsDown`；`InstallHooks` 在 Android 跳过 `device.*`（保留 `device.change_input_type`）/ `app.*` / `cloud_*`；`AndroidUiFrame` + `SetAndroidFrameClient(FrameClient::Ui, …)`；`ConfigurationChanged` / `Shutdown` 的 PC 侧清理与日志。

**冲突处理**：仅 `PumpInputType` 三处冲突（我方第 1 项的修复与上游改动同区域），**全部取上游实现**——上游版已含等价的 `!active && restore < 0` 早退与 restore 一次性清零。合并后与本仓原独有部分的差异**恰为 13/2 行**（即两处日志 summary），已核验保留。

**证据**

| 判据                                      | 结果                                                                                                                                                                              |
| --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aarch64-linux-android24 -fsyntax-only` | 零错（仅既有 `unused function 'Contract'` 告警）                                                                                                                                         |
| `:app:assembleDebug`                    | BUILD SUCCESSFUL（原生 + Kotlin + Java 全链）                                                                                                                                         |
| `.so`                                   | `48716592 → 48790264` 字节；SHA-256 `5433acc9… → 0e727f8f…`；`llvm-nm` 中 `AndroidUiFrame` = 96 字节本地符号                                                                               |
| **设置写入器 JVM 验证**                        | 真实 `ModuleSettings.interfaceConfiguration()` 跑 8 种开关组合：三开关全关时输出**空配置**（= 模块不进游戏进程），任一开启才写出；`pc_ui_enabled` 随标志位正确变化；**写出的每个键都落在原生解析器认得的 8 个键之内**（`schema_version` 除外——版本标记，非开关） |

**未完成 / 未验证**（2026-10-06 当晚已闭合，见 §10.7）：`DeviceInfo/InputType.Keyboard` 的 IL2CPP 解析是否成功**须真机日志**——成功为 `Android PC layout: resolved InputType.Keyboard by metadata.`，失败为 `Android PC layout: Keyboard enum unavailable; leaving game layout unchanged.`。当时设备未接入，该项待 `PJX110`；实测**失败**，根因在宿主封装层而非本项代码。

**上游一致性**：上游**未**在悬浮窗侧（`OverlaySettingsPage` / `FrameworkSettings`）暴露该开关，故不改本仓悬浮窗；悬浮窗暴露属第 4 项（依赖第 3 项的跨进程设置通道）。

### 10.4 第 3+5 项前置核查：两处与 §六 估计不符（2026-10-06，未落地）

开工前的依赖清点推翻了 §六 对这两项"可直移 / 低风险"的判断。**结论：两项都不能按 §六 原样推进**，需先做决定（见 §10.5）。

**（1）第 3 项的 4 个文件可直移，但唯一消费者在第 4 项 ⇒ 单独做是死代码。**

上游调用方清单（`git grep` 全上游树）：

| 上游调用点                                | 说明                                                                           |
| ------------------------------------ | ---------------------------------------------------------------------------- |
| `OverlaySettingsPage.java:67/90/101` | **唯一的 `OverlaySettingsClient.call` 调用方**，是第 4 项（293 行 View/XML，须重写为 Compose） |
| `XposedEntry.java:36`                | `OverlaySettingsClient.initialize(...)`，游戏进程侧                                |
| `XposedEntry.java:77`                | `GlobalFovUpdater.start(...)`                                                |
| `FrameworkSettings.java:26/197-199`  | 授权令牌的初始化与发布（跨进程偏好）                                                           |
| `BemInstaller.java:386`              | `OverlayWritePolicy.apply` 接入 `saveAll`                                      |

即：`OverlaySettingsClient` + `OverlayWriteAuthorization` + `OverlayWritePolicy` + `OverlaySettingsProvider` 这 279 行，**服务对象就是第 4 项那个悬浮窗设置页**。本仓无 `OverlaySettingsPage` ⇒ 单独移植只会在设置 app 里新增一个 `exported=true` 的 Provider 和一堆无人调用的类。**应与第 4 项合成一批。**

**（2）第 5 项的 `NativeCommandBridge.globalFov` 在本仓走不通——两侧 `NativeCommandBridge` 不是同一种实现。**

| 侧  | `NativeCommandBridge` 形态                                                                                                                                                                                                                                                                                                    |
| -- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 上游 | **JNI**（`static native boolean globalFov(boolean, float)`；`native_bridge.cpp` 用 `BindContextLoaderNatives` 绑定到游戏 classloader）                                                                                                                                                                                               |
| 本仓 | **文件中继**（`NativeCommandBridge.java` 注释原文：*"This used to be a JNI bridge, but the runtime library registers under the game's classloader while these classes belong to the LSPosed module classloader — and Android scopes JNI symbol lookup (and .so openings) per classloader, so every call threw UnsatisfiedLinkError"*） |

本仓同源证据：`native_bridge.cpp` 的 `submit` / `status` / `key` / `releaseKeys` JNI 导出**仍在编译进 `.so`，但 Java 侧无对应调用**，是历史遗留（**2026-10-07 更正**：`NativeCommandBridge.java` 已不再是"零 native 声明" —— 第 11 项为 pc-mouse 与 `frame` 声明了 4 个 `static native`，见 §11.2.2）；`AndroidMmdCommand` / `AndroidMmdStatus`（`native/shared/android_compat/android_camera.h`）同样**只有定义、无调用方**——本仓 MMD 实际走 relay 的 `c <payload>` 行 → `command_pump` → `AcquirePanelCommand("mmd")`。

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
- **`XposedServiceHelper` 经 Provider 代理取 Binder，不要求设置 app 被注入、也不要求它在 LSPosed scope 中**：实测设置 app 未在 scope 内，令牌仍成功发布、Provider 仍被游戏进程调用。此前“未注入 ⇒ 不发布”的判断被推翻。

---

### 10.7 第 2 项真机回归失败并修复：宿主封装层漏同步（2026-10-06）

**（1）症状**：设置 app 里打开「PC 界面布局」、完全重启游戏后，游戏内仍是手机版布局。真机日志（`PJX110`）显示配置**完整到达**、卡在枚举解析：

```
#81  [betterendfield.ui] Android PC layout: Keyboard enum unavailable; leaving game layout unchanged.
#94  [betterendfield.ui] UI Configuration applied: enabled=true, mobile_ui_enabled=false,
     pc_ui_enabled=true, pc_ui_effective=false, keyboard_input_type=-1, ... (effective=ACTIVE)
```

其余前置条件全绿：`device.input_type_backing` 字段解析成功、UI hooks **5 of 5 installed**、模块初始化 successful。**唯一卡点 = `keyboard_input_type=-1`。**

**（2）根因（两层，同一个漏同步文件）**：本仓 `android/app/src/main/cpp/core/runtime.cpp` 的 `ReadFieldObject` 是**旧简版**，上游 `upstream/main` 早已是强化版——即"上游已修的 bug，我方合并时漏了这个宿主封装文件"：

| 层 | 现象 | 上游做法 |
|---|---|---|
| 1 | `Keyboard` 枚举读不出（`-1`） | 直读失败后，对 `static(0x10) && literal(0x40)` 走 `System.Enum.Parse` **命名反射回退**（本机 IL2CPP **不装箱枚举字面量**，同机 `actions` 模块 `#171`"10 enum constants read through the System.Enum.Parse fallback"是同一事实的既有实证） |
| 2 | 第一层修好后 `PumpInputType` 停在 `waiting for readable inputType backing field`，**永不推送** | 用 `field_get_flags_` 判 `kStatic(0x10)`，**只在非静态字段时才要求实例对象**；旧简版的 `instance == nullptr` 短路把 `DeviceInfo` 的静态 backing field 读一并挡死（`TryReadInputType` 传的正是 `nullptr`）。Windows 侧 host（`native/shared/host/dynamic_resolver.cpp:343`）**从来没有**这个守卫 ⇒ 同一份模块源码在两平台行为不同 |

**（3）改动**：`native/modules/ui/module.cpp` **相对 HEAD 零差异**（第一层我曾在模块内自建 `ResolveEnumConstant` + `Enum.Parse` 回退，属重复劳动，已**完全回退**）。修复只落在宿主封装层，两个 `ReadFieldObject` 重载与上游**逐字一致**（文件其余部分保留本仓既有分叉 ⇒ `core/runtime.cpp` 的 gap 由 57 降到 **11**，不是 0）：

| 文件 | 改动 |
|---|---|
| `android/app/src/main/cpp/core/runtime.h` | +8：补 `field_get_flags_` / `field_get_parent_` / `field_get_name_` / `class_is_enum_` |
| `android/app/src/main/cpp/core/runtime.cpp` | +40/−6：`Connect()` 解析这四个导出（`field_get_flags` 列入**必需清单**，同上游 fail-fast）；两个 `ReadFieldObject` 换成上游实现 |

**（4）证据**（真机日志，无 `[REJECTED]`、无 `[managed exception]`）：

| 判据 | 结果 |
|---|---|
| 枚举解析 | `#81 Android PC layout: resolved InputType.Keyboard by metadata.` |
| 配置生效 | `#94 … pc_ui_effective=true, keyboard_input_type=0 … (effective=ACTIVE)` |
| 下发 | `#171 UI layout request generation=1, mode=Keyboard` |
| 落盘生效 | `#203 Input type pushed to 0 (was 1, now 0, active=true)` |
| 画面 | 截图确认为**桌面(Keyboard)布局**：PC 式图标栏、右上 F1–F4/Esc/Q 键位提示 |
| 产物 | `.so` 48,837,728 → 48,842,680 B；`assembleDebug` 成功 |

**（5）教训（重要）**：核对"上游改动是否已同步"**不能只看模块文件**。`android/app/src/main/cpp/core/`（IL2CPP 宿主封装）、`native/shared/host/` 这类**宿主层**必须逐文件比对——它们的一个多余守卫就能让整条上层功能**静默失效**，且症状出现在模块里、根因在宿主里，极易误判为模块 bug。本次 §10.3 标注的"唯一真实门槛"实际就是这一个漏同步文件。

**（6）过程坑**：装完新 APK 首次启动曾出现模块**完全没注入**（`native.log` 0 字节、PID 未变、无崩溃迹象），再重启一轮即恢复正常 ⇒ 判"改动把模块弄坏了"之前先重启一轮排除注入偶发失败。

---

### 10.8 锚点前移与文档全量复核（2026-10-06 22:40，**数字已被 §十一 的锚点取代**）

**（1）锚点**：本仓 `5e09128 → 8b8a0b5`（+7 提交：第 3/4 项、宿主同步、文档写回），上游 `3510fa7 → 3521627`（+1 提交）。§零 / §零点五 / §0.3 / §九 的全部数字按新锚点**重算**（非估算），重算口径与上一版完全一致，因此三版数字可直接对比（§0.4）。

**（2）上游这笔提交是什么**（`3521627 feat: add community download site and desktop/android entry points`）：

- 新增 `web/mod-center/**` **21 件**：Vite + TSX 前端（`src/App.tsx`、`styles.css`）、`server/server.mjs`(+269) 与其单测 `server.test.mjs`(+120)、`deploy/`（systemd unit + 装机器 + 反代脚本）、`shared/types.json`。
- 在**两个第三方模块页**各加一个「下载站」入口：`ThirdPartyModulesPage.java`(+7)、`ThirdPartyModulesPage.cs`(+8)，另两份 locale 各 +2。
- **该入口的 URL 硬编码为 `https://146.235.16.65:8443/endfield/`（裸 IP + 非标端口）**。


**（3）对本仓的影响：不跟进。** 21 件全在 Android 编译面**外**（`web/` 归 §0.3(c)）；唯一沾到本仓的是那 +7 行入口按钮，而本仓该页是 Compose 实现、本就要重写。不跟进的三个理由：① 入口指向裸 IP + 非标端口，上游服务器一撤即 404，我方无法保证可达性；② 下载站产物（模型 / 语音资源）的**格式与许可**不在本仓验证链内，接进来等于引入一条未验证的供应链；③ 本仓第三方模块的获取路径尚未定义，此时不应由上游替我们定。

**（4）本轮刷新范围**（本文档，共 10 处）：

| 节 | 刷新内容 |
|---|---|
| 锚点表 / 一句话结论 | 新锚点 `8b8a0b5` / `3521627`；加进度行（1~5 项落地、缺口收敛到 `custom_model`） |
| §0.1 / §0.2 / §0.4 | 领先量 40 / 75、功能面 545 / 361、四分法 113+231+201 / 201+138+22，三版数字并列对比 |
| §0.3 | **补齐此前漏列的 7 个目录**（`web` `scripts` `config` `manifests` `ui/tests` `tools/FirstPersonProfiles` `tools/CombatDataExporter`），并分「进编译面 / 上游测试 / 不进编译面」三类 |
| §零点五 | 32 → **31** 行；逐件复核榜面变动（唯一掉出 = `ui/module.cpp`）；**新增 gap 算法盲区说明** |
| §一 | 类清点 59/59/32/32/27 → **59/64/28/33/31**；A3 表格移出 4 件 Overlay 类、B 桶加 `OverlayModelPage.kt`、C 桶加 4 行 |
| §二 | Manifest 段改为「本仓现有两个 `exported=true` provider」（第 3 项已落地，不再是「若移植需新增」）；标注 `ui_pc` 缺失已补 |
| §三.1 / §三.2 | `core/runtime.{cpp,h}`、`ui/module.cpp` 标为**已同步**；前者另立一行说明其危害 |
| §四 | `build.gradle.kts` 行补记 R8 门禁新增 Provider |
| §六 | 第 1、2 项标**已完成**（含提交号）；表后加进度说明与第 6~10 项的现状 |
| §八 8.1 / §九 | 新增「**B′ 曾属 B、已同步**」级；复现命令数字更新 + gap 算法说明 |

> **§一 类清点的变化明细**（`64 − 33 = 31`、`59 − 28 = 31` ✓）：A 桶 32 → 28（4 件 Overlay 通道类移植入本仓 ⇒ 移入 C 桶）；C 桶 27 → 31（同 4 件）；B 桶 32 → 33（第 4 项的 `OverlayModelPage.kt` 613 行 —— 上游对应物是 View/XML 的 `OverlaySettingsPage.java`，故本件属本仓独有）。

**（5）本轮新增的两处口径修正**：

| 修正 | 说明 |
|---|---|
| §0.3 目录分布**漏列 7 个目录** | 上一版只列了 9 行，漏掉 `web`(28) / `scripts`(28) / `config`(11) / `manifests`(6) / `ui/tests`(4) / `tools/FirstPersonProfiles`(6) / `tools/CombatDataExporter`(4)。本版补齐并分三类，**明确 (c) 类不进本仓 Android 编译面** |
| **gap 算法会让"本仓整文件删除"永不进榜** | `gap` 按总改动行数之差计算，而本仓删除整文件时 `+0/−N` 的 N 往往**大于**上游改写量 ⇒ gap 为负（实测 `MainActivity.java` −436、`SettingRow.java` −111、`SectionCard.java` −65）。判"上游新增 vs 本仓删除"必须另走 `git cat-file -e HEAD:<path>` 二分，不能只看榜 |

**（6）复核结论**：

| 结论 | 依据 |
|---|---|
| §六 第 1~5 项**全部落地**，且 2 / 3+4 / 5 项均有真机运行时证据 | §10.3 / §10.5 / §10.6 / §10.7 |
| 原生侧三块中 `ui/module.cpp` **已与上游同步**，已从 gap 榜掉出 | §零点五 榜面变动 |
| **最大剩余缺口 = `native/modules/custom_model/module.cpp`**（gap 1491，且在 Android 编译面内） | §零点五 #1 |
| 本仓改动方向已反转：`android/app` 本仓改 **133** > 上游 97 | §0.3(a) |
| 以上结论均不含 `web` / `scripts` / `config` / `manifests` / WPF —— 那些不进本仓 Android 编译面 | §0.3(c) |

---

### 10.9 第 8 项已完成：运行时状态串（`b0d7382`，2026-10-07）

**指令**：「接管移植」+「按照依赖分布 从小到大」。⇒ 先对全部剩余项量出**依赖闭包**，再按升序执行。**第 8 项的闭包最小** —— 它不触碰最大缺口、也不动构建基建，所以是起点。

#### 10.9.1 依赖闭包实测（两处与 §六 原估不符）

| 序 | 项 | 新增文件 | 需改文件 | 跨子系统依赖 | 与原估的差 |
|---|---|---|---|---|---|
| **1** | 第 8 项 运行时状态串 | 2 | 3 | 无 | 一致 ⇒ **本轮完成** |
| 2 | 第 7 项 模型热切换 | 2 | 3 | **custom_model（最大缺口内）** | ★ "82 行"是**表象** |
| 3 | 第 10 项 构建配置化 | 2 | 2 | `config/` 整目录 + `scripts/` 解析器 | ★ "87 行顺手活"是**表象** |
| 4 | 第 6 项 MMD 安装器统一 | 4 | 4+ | MMD 链 + 2 个上游独有 Activity | 一致（最大的一件） |
| 5 | `custom_model` 最大缺口 | ~8 | 5+ | 自成体系（gap 1845） | 一致 |

判定两处"表象"的依据（均为只读核查）：

- **第 7 项**：Java 侧确实只有 `BemHotSwitchUpdater`(50) + `BemHotSwitchUpdate`(32)，但其原生落点 `CustomModelModule::QueueConfiguration` 依赖 `g_update_ready` / `g_update_mutex` / `g_pending_update` / `ApplyPendingConfiguration` / `UpdateSharedReplacement` / `SharedRegistryText` 整套「共享替换事务」——本仓 `custom_model_module.cpp` 只有 `instance_` / `replacement_api_` / `InitializeSharedReplacement`，**上述六项一个都没有**。
- **第 10 项**：`settings.gradle.kts` 的新增块会 `resolveWorkspace()` 调 `scripts/workspace_config.py`，后者读 `config/workspace.defaults.json` + `config/documents.json` + `config/game-discovery.json`，而**本仓 `config/` 整个目录都不存在**；且该块 `check(ANDROID_HOME == tools.android_sdk)` 并改写 `java.io.tmpdir` / `android.home` / `projectCacheDir`。本机 SDK 与 NDK 本就分离（`C:/Users/Vens_/AppData/Local/Android/Sdk` vs `D:/android-toolchain`）⇒ **落地前必须先改本机工作区配置**，不是"顺手"。

#### 10.9.2 改动面（8 文件，+289/−1，新增 4 件）

| 文件 | 净变化 | 与上游的关系 |
|---|---|---|
| `android/app/src/main/cpp/core/runtime_status.h` | +22（新增） | **逐字**（blob `3b866215` == 上游 `3b866215`） |
| `android/app/src/main/java/.../RuntimeSnapshot.java` | +46（新增） | **逐字**（blob `610972f8` == 上游 `610972f8`） |
| `android/app/src/main/cpp/native_bridge.cpp` | +50/−0 | 派生：`runtimeStatus` 导出 + 绑定 + **13 处写入**（全部落在本仓自己的模块启动序列上）；**两条既有日志串逐字保留**（`PC mouse and frame JNI natives bound` / `PC mouse bridge: JNI call path live`），新绑定单独一行日志 |
| `android/app/src/main/java/.../NativeCommandBridge.java` | +9 | 新增 `static native String runtimeStatus();`（与上游同名同签名） |
| `android/app/src/main/java/.../GameOverlay.java` | +24/−1 | **本仓独有**：消费点接在本仓 Compose 悬浮窗的**日志头摘要行**（上游是 View 的 `status` TextView） |
| `native/modules/camera/module.cpp` | +15 | `AndroidCameraValuesStatus()`；**比上游少 `camera.global_fov_ready` 一行**（本仓无该标志位，第 5 项的全局 FOV 走了另一条路 ⇒ 宁可少报也不伪造） |
| `android/app/src/testHost/java/.../RuntimeSnapshotHostTest.java` | +92（新增） | **本仓独有**夹具（上游无），不进 APK |
| `android/app/src/testHost/runtime_snapshot_fixture.py` | +31（新增） | **本仓独有** runner，沿用 `pcui_mouse_fixture.py` 的约定 |

#### 10.9.3 关键设计点：消费点与生产者同轮落地

§11.2.3 的教训是「**有生产者、无消费者**」——第 11 项的 `.so` / 符号 / 字符串 / 夹具四项全绿，真机却一行诊断都不出，因为驱动源没接。⇒ 本轮**刻意不重复**：`runtimeStatus` 的读取在同一个提交里接进了悬浮窗日志头（`snapshotSummary()`，绑定失败降级为 `offline()` 而不吞掉整行）。

原生侧的绑定也与 pc-mouse **同法**（`BindContextLoaderNatives` + 独立数组），且**单独绑定**：一次被拒只损失状态行，不会连带打掉鼠标路径。

#### 10.9.4 证据

| 判据 | 结果 |
|---|---|
| 原生编译 | `:app:assembleDebug` **BUILD SUCCESSFUL**（6 条既有 `unused function` 告警，无新增） |
| Java 编译 | `compileDebugJavaWithJavac` 通过 |
| JNI 导出（`llvm-nm`，`T`） | `Java_*` **9 → 10**，新增 `Java_..._NativeCommandBridge_runtimeStatus`；`_ZN14betterendfield25AndroidCameraValuesStatusEv` 为 `T` |
| `.so` | 48,973,792 → **49,047,944**（+74,152；Debug 不 strip，与新增 DWARF 同量级 ⇒ **无"悄悄变大"**） |
| APK | `app-debug.apk` **81,559,821** B，SHA-256 `ab1ea7e2…`（debug 变体，按既定规则**不作为交付物**） |
| 设备外夹具 | `python android/app/src/testHost/runtime_snapshot_fixture.py --output …` ⇒ **23 项断言全过**：跑的是本仓这份真 `RuntimeSnapshot`，喂的是原生**实际会产出**的字节形状 |
| 行尾 | 补丁后复核 `native_bridge.cpp` CRLF 331 → 375 / LF-only 41 → 47（该文件本就混合）；其余三个被改文件仍单一风格；两个新件逐字 LF（`autocrlf=true` ⇒ 与上游 blob 一致） |

**夹具顺带钉死的一条契约**（本轮实测发现，不是缺陷）：`RuntimeSnapshot.number()` **只解析整数**，而生产端 `std::to_string(float)` 产出 `5.000000` ⇒ 浮点键（`camera.speed` / `camera.fov`）**必须用浮点解析器**。上游 `GameOverlay` 正是用 `finiteNumber(...)` 读它们，从不拿 `number()` 读 ⇒ 本仓接线时须遵守同一分工。

#### 10.9.5 遗留

1. **无真机验收**。第 8 项的可观测面是悬浮窗日志头那一行摘要 ⇒ 需装机 + LSPosed + 游戏内看日志。本轮止于构建 + 设备外夹具。
2. **上游第二个消费点未接**：`OverlaySettingsPage.java`（View/XML，本仓是 Compose）会 `settingsPage.runtime(snapshot)`；本仓未接，也不打算接（页面层不跟进，同 §10.8-3 口径）。
3. **`camera.speed` / `camera.fov` 目前无人读取**：本仓 UI 未取（上游用于初始化速度/FOV 滑条）。
4. **`Map.of()` 与 minSdk 29 的隐患（记录，未改）**：`Map.of` 是 API 30 的 API，本仓 `minSdk = 29` 且未开 `coreLibraryDesugaring` ⇒ API 29 设备上会 `NoSuchMethodError`。**上游同样如此**（上游 minSdk 也是 29），且本仓既有产品码已有同类用法（`Set.of` / `List.of`，第 3 项逐字移植件）⇒ 本轮**保持逐字、不擅自偏离**，只记录待你决策。

---

### 10.10 剩余三项的第二轮实测：三项各自的性质与原判都不同（2026-10-07；第 6 项已于 §10.11 落地，本条保留实测口径）

§10.9.1 的排序只量了"新增/需改文件数"，这一轮把三项**逐符号、逐文件**量到底，结论是**三项都不可比**：一项其实是最大缺口的别名，一项的上游 schema 在本仓根本不适用，一项是与上游同源分叉的对账。

#### 10.10.1 第 7 项（模型热切换）≡ `custom_model` 最大缺口

Java 侧确实只有 82 行（`BemHotSwitchUpdater` 50 + `BemHotSwitchUpdate` 28），但热切换的本体在原生，且**消费方不在 Android 包装层**：

| 环节 | 上游落点 | 本仓 |
| --- | --- | --- |
| JNI | `native_bridge.cpp:253` `updateCustomModelConfig` + `:352` 注册 | 无 |
| 帧泵 | `native_bridge.cpp:267` `frame()` 内每帧 `ApplyPendingConfiguration()` | 无（本仓 `frame()` 只有 `DispatchAndroidFrame`） |
| 事务 | `custom_model_module.cpp:266-296`（`QueueConfiguration` / `ApplyPendingConfiguration` / `UpdateSharedReplacement`）+ `h:23-25` | **无**（本仓只有 `InitializeSharedReplacement`） |
| 注册表文本 | `SharedRegistryText()`（含 `/data/local/tmp` 相对路径归一 + 三个开关键） | 无（本仓在 `InitializeSharedReplacement` 内**内联**一份简版，无三个开关键） |
| **消费方** | `module.cpp:3316` `ReloadRegistryAtDelivery()` → `g_pending_registry_text`；`module.cpp:49` `g_hot_switch_runtime`；`module.cpp:3258` `g_last/pending_registry_text` + `g_registry_request_mutex`；`module.cpp:3734` `g_hot_switch_runtime.store(g_registry.hot_switch)` | 本仓 `native/modules/custom_model/` **14 文件 vs 上游 32 文件**；`hot_switch` 命中 = 0 |
| **作业运行时** | `model_job_runtime.inc`（**1129 行，本仓整个文件不存在**）：`PumpInstanceRebind` / `PumpModelJobs` / `ScanSceneInstancesForRebind` / `RestoreDisabledResource` / `RememberModelTarget` / `RefreshModelTargetPriorities` | 无 |

⇒ **第 7 项不是独立项，它就是最大缺口的入口面**。上游同批缺席的 16 个文件合计 **约 2,700 行**（`model_job_runtime.inc` 1129、`async_loading.h` 638、`model_lod_state.inc` 140、`bem_targets.inc` 138、`generic_model_matcher.inc` 118、`explicit_android_shadows.inc` 114、`runtime_ini.h` 111、`model_overlay_host.h` 70、`model_asset_cache.inc` 58、`model_content_identity.h` 48，另 `android_lod_relations.generated.h`、`model_overlay_hotkey.h`、`model_overlay_protocol.h`、`runtime_ini_win32.h`、`overlay/`、`tests/`）。
唯一的好消息：`ModuleApi.h` 的 `configuration_changed` **本仓已有**（`kResourceApi` 第二项 = `ResourceConfigurationChanged`），所以接口缝不用动；但本仓的 `ResourceConfigurationChanged` **没有 `[CustomModel]` 分支**（上游 `module.cpp:3887` 才有），照抄 JNI 会让每次热切换都落到「未知配置行」⇒ 静默无效。

#### 10.10.2 第 10 项（构建配置化）的上游 schema 在本仓不适用

`settings.gradle.kts`（+63 行）会 shell 两趟 `scripts/workspace_config.py resolve`（208 行，bootstrap python → 配置里的 python），然后 `check(ANDROID_HOME/ANDROID_SDK_ROOT/local.properties 三者 == tools.android_sdk)`、改写 `java.io.tmpdir` / `android.home` / `projectCacheDir`；`workspace.gradle.kts`（24 行）再把每个模块的 `buildDirectory` 挪到 `<paths.build>/android/gradle/<segment>`，并给**每个 `Exec` 任务**注入 `BE_WORKSPACE_DOBBY_ROOT`。

问题在 `config/workspace.defaults.json` 的 schema 是**上游 Windows 桌面多平台工作区**，不是 Android 工作区：

```json
"game": {"version": "1.5.3", "platform": "Windows", ...},
"tools": {"cmake":…, "dotnet":…, "iscc":null, "resconv":"toolchains/FkArkEnd/ResConv/bin/Release/net10.0/ResConv.exe",
          "endfield_unpacker_root":…, "wwiser_root":…, "native_asset_reader":"…/NativeAssetReader.exe",
          "archive_backend":"toolchains/bem-archive-backend/7zip",
          "android_sdk":"toolchains/android/sdk", "android_dobby":"toolchains/android/dobby-1.0.5"},
"resource_update": {"input_root":"inputs/1.5.3/Windows/baseline", "outputs":{"avatars":…, "web_combat_dictionary":…}},
"android_signing": {"properties_file":"config/android-signing.local.properties", "certificate_sha256":"6f157402…"}
```

本仓（`config/` **整个目录不存在**、无 `scripts/workspace_config.py`）没有 ResConv / EndfieldUnpacker / wwiser / dobby / Inno Setup / Windows 资源更新输入，且 `app/build.gradle.kts` 也**没有**读 `beWorkspace`（本仓 116 新 / 462 删，是另一份文件）⇒ 只落 `settings.gradle.kts` + `workspace.gradle.kts` 而不落 `config/` 会**立刻断构建**；落 `config/` 则要给本仓凭空发明一套 Android-only 的 schema 并改 `app/build.gradle.kts`。
**实测环境**：`ANDROID_HOME` / `ANDROID_SDK_ROOT` 均未设置（`check` 的第一半可通过），`android/local.properties` 为 `sdk.dir=C:/Users/Vens_/AppData/Local/Android/Sdk` + `ndk.dir=D:/android-toolchain/android-ndk-r27c` —— 与上游"SDK 在仓库内 `toolchains/android/sdk`"的假设**相反**。
⇒ **判为「不适用（Android-only 本仓）」**，除非将来确实要接上游的 Windows 资源更新流水线。第 10 项的**唯一有价值子集**是「把 SDK 位置显式归一」——而本仓已有 `local.properties` 承担同一职责。

#### 10.10.3 第 6 项（MMD 安装器统一）是**同源分叉对账**，不是「710 行可直移」

上游 4 件与本仓既有件的对应关系（`installed_mmd_works` 索引键两边相同，证明同源）：

| 上游 | 行 | 本仓对应 | 行 | 性质 |
| --- | --- | --- | --- | --- |
| `MmdInstaller.java` | 263 | `MmdLibraryInstaller.java` + `MmdLibraryFiles.java` | ~330 | **分叉**：同名的 `busy` / `status` / `MAX_WORK_BYTES` / `WORKER=newSingleThreadExecutor` / `start(Context,MmdImportSession,…)` / `remove(Context,generation)` 全部对得上 |
| `MmdImportArchive.java` | 223 | 内联在 `MmdLibraryFiles` | — | **架构分歧**：上游走 `org.apache.commons:commons-compress:1.28.0` + `org.tukaani:xz:1.10`（zip + 7z），**本仓一个都没引**，是自己解压 |
| `MmdInstalledResources.java` | 85 | `MmdSlotFiles.prepareWorks` 部分覆盖 | — | **真新增**：启动前把工作**原子发布**（`.stage-<gen>` → rename，失败即撤回）并剪除未广告代际 |
| `MmdAudio.java` | 139 | **无** | 0 | **真新增**：`MediaPlayer` + `HandlerThread` 单曲播放，Unity 读快照（游戏内 MMD 配乐） |
| `MmdImportPlan/Session` | 191 / 220 | 本仓 372 / 388 | — | **本仓更大**（本仓自带 set.ini 解析/写回与槽位表）⇒ 只能小侧前向移植 |
| 2 个 Activity | 195 / 423 | `MmdPage.kt`（Compose） | — | §七.5：**页面层不合并**，只按功能名重写 |

⇒ 第 6 项会**替换本仓 MMD 作品库的后端**（该库在 §八 D 列为「本仓领先」），并新增两个 gradle 依赖（`--offline` 下若缓存没有则构建直接失败）。**必须先定架构**：取上游后端 / 保留本仓后端只补 `MmdAudio` + 原子发布 / 全量对账。

#### 10.10.4 第二轮排序结论

| 序 | 项 | 第二轮实测 | 处置建议 |
| --- | --- | --- | --- |
| — | 第 8 项 | 已完成（`b0d7382`） | — |
| ✅ | **第 6 项 MMD 安装器统一** | 同源分叉对账；**2 件真新增**（`MmdAudio` / `MmdInstalledResources`）+ 2 个新 gradle 依赖；页面层不合并 | **已完成**（`db021cd`）：只 `MmdAudio` 是真缺口、`MmdInstalledResources` 本仓已覆盖、两个 gradle 依赖**未引入**（见 §10.11） |
| **2** | 第 7 项 ≡ `custom_model` 最大缺口 | 16 个文件 / ~2,700 行不出现在本仓；`hot_switch` 命中 0 | 与 `custom_model` 合并为一项，子系统级前向移植 |
| **3** | 第 10 项 构建配置化 | 上游 schema 是 Windows 桌面多平台工作区（ResConv/EndfieldUnpacker/wwiser/dobby/iscc + resource_update） | **不适用**；本仓只需 `local.properties`（已有） |

### 10.11 第 6 项已完成：MMD 本地音轨接线（`db021cd`，2026-10-07）

用户口径：**只移两件真新增**（保留本仓 `MmdLibraryInstaller` / `MmdLibraryFiles` / `MmdImportPlan` / Compose `MmdPage.kt` 不动，**不引入** `commons-compress` / `xz`，不碰页面层）。落地时两件的结论**一真一假**：

| 上游件 | 行 | 实测 | 处置 |
| --- | --- | --- | --- |
| `MmdAudio.java` | 139 | **真缺口** | 移植，与上游**同 blob** `4f9cee7230c0fcb83fd88868d3daa461b1b4c49a` |
| `MmdInstalledResources.java` | 85 | **本仓已覆盖** | **不移**。`MmdSlotFiles.prepareWorks` 已是原子发布（`.stage-<gen>` → 失败 `deleteOwned(folder)` → `renameTo(folder)`）+ 按 `[a-f0-9-]{36}` 剪除未广告代际，逐项对得上 |

**这是一条断链，不是一个重构。** `mmd_director_runtime.inc:113 ResolveMusic()` 调 `betterendfield::AndroidLocalMusicApi()`；`local_music_android.cpp:77` 只在 `java_vm` 非空时返回 `&api`，而负责设置它的 `InitializeAndroidMusic` 在本仓**全仓无调用方** ⇒ `ResolveMusic()` 恒空 ⇒ `g.music`（第 369 行）与 `FeatureMusicModule | FeatureMusic`（第 629 行）双双关着：**本仓 MMD 作品从来没有配乐**。而解析侧 `local_music_android.cpp` 与上游**零差异**（`git diff --numstat` 为空）——缺的只是它要解析的那个类。

**四个接线点**（4 个已存在文件合计 `+56/−6`，另有 7 个新文件 / 716 行）：

1. `NativeCommandBridge` 末尾加 4 个**非 native** 静态方法（`audioOpen` / `audioControl` / `audioStatus` / `audioError`；纯 CRLF，198 → 203 行）——`local_music_android.cpp` 已按名解析它们。
2. `native_bridge.cpp` 在 pc-mouse 绑定**之前**声明 `jclass bridge_class = nullptr;`，把它作为 `BindContextLoaderNatives(..., &bridge_class)` 的出参。`jni_binding.h` 成功时 `NewGlobalRef`，且 `PopLocalFrame` 不影响全局引用。
3. 其后接 `InitializeAndroidMusic(vm, environment, bridge_class)`：失败只 `LogError("mmd.music", "Java media API unavailable; body and camera remain usable")` 降级成「丢音乐」，**不静默**，也不影响 file relay / 帧泵 / PC 鼠标面。
4. `proguard-rules.pro` 加 `-keepclassmembers`（4 个方法）、`build.gradle.kts` 的 `jniCallbacks` 加第二个 owner ⇒ `verifyReleaseEntryPoints` 会断言它们没被 R8 改名、且方法名留在 dex。

**证据（全部离线）**：宿主夹具 `android/app/src/testHost/mmd_audio_fixture.py` → **23/23 passed**；`:app:assembleDebug`（带 `-PheadwearCatalogDir`）BUILD SUCCESSFUL，44 任务，APK **79,009,665 B** / SHA-256 `c3766a9b…c873`；`libbetterendfield_android.so` 内 `InitializeAndroidMusic(JavaVM*, JNIEnv*, jclass)` 与 `AndroidLocalMusicApi()` 均为**已定义**符号，6 条接线字面量（`mmd.music` / `Java media API bound` / 不可用串 / 4 个方法名）全部命中；dex 内 `MmdAudio` 与 4 个方法名命中。

**未验收**：听声需要装机 + LSPosed + 游戏内跑一个 MMD 作品；`verifyReleaseEntryPoints` 只能在 release 产物上跑，而本机无 release 密钥库 ⇒ 该门禁由 CI 覆盖。

> 落地过程中配置阶段曾**整体**失败（`NoSuchMethodError: void Settings_gradle.<init>(KotlinScriptHost, PluginDependenciesSpec, Settings)`），根因是 `caches/<ver>/kotlin-dsl/scripts/<内容哈希>` 下被通用 Kotlin 脚本模板编译出的 `Settings_gradle`（丢父类、构造只收 `Settings`，正常应 `extends CompiledKotlinSettingsPluginManagementBlock`）。因为它**按脚本内容哈希命中**，源码怎么改都命中同一份坏类 ⇒ 表现为「HEAD 也编不过」。**与本改动无关**；恢复动作是移走该哈希目录后**再跑一次**。完整判据与三个方法论坑见 `.workbuddy/memory/2026-10-07.md`。

---

## 十一、上游 v3.5.2 分类与第 11 项（PC 布局相对鼠标桥）

> **本节于 2026-10-07 00:25 重写。** 原版（23:25）记的是第一版实现 —— 把上游的 JNI 通道改造成本仓的文件中继；那版从未通过真机，随后按「关于鼠标实现直接按照上游来、删去本仓实现」的指示整体回退。现按最终形态（`6541856` + `cbbca19`）重写，并**完整保留三版演进** —— 因为「铁律被本仓自己的实证推翻」是这一段最有价值的产出。

### 11.1 锚点前移：上游这 14 个提交不是"只加了下载站"

上一轮只看到 `3521627`（社区下载站），本轮 fetch 发现上游又推进 14 个提交并打了 **v3.5.2**（`d514f2f`）。**其中 3 件落在本仓 Android 编译面内，且有一件是本仓刚完工功能的直系后续** —— 因此 §六 原清单的"第 6~10 项"不再是下一步。

| 提交 | 内容 | 对本仓 Android 编译面 |
|---|---|---|
| `0fda1f2` | `fix(android): bridge PC layout mouse capture into game axes` | **命中** —— §11.2 已移植，**真机通过** |
| `b5d167d` | `preserve Android LOD bias for explicit LOD1 resources`（把 141 行从 `module.cpp` 抽出为 `model_lod_state.inc`） | 命中 `custom_model`（最大缺口内） |
| `7b9f918` / `76db3db` / `9a0b882` | custom-model 资产类型守卫、首次启用发现、热切换开销与验收记录 | 命中 `custom_model` |
| `7a64aa4` / `c872e17` / `b488ded` | BEM 1.4 武器/形态资源目标、创作者工作流、研究样本 | `tools/CustomModel` + `custom_model` |
| `790ae1c` | `feat(ui): add Workshop navigation and group Android module tools`（新增 `ThirdPartyModulesActivity.java` +82、`MainActivity.java` +48） | 页面层 —— **本仓是 Compose，不跟进**（同 §10.8-3 口径） |
| `79cb846` / `032268e` / `c728a99` / `d514f2f` | WPF 左对齐/DPI、merge、3.5.2 版本号、mod-center 双语 | 不进编译面 |

**仍未处理**：`custom_model` 那 5 个提交 —— 本仓该文件的最大缺口由 gap **1491 → 1845**（上游该文件 `+1640/−368`）。

### 11.2 第 11 项：PC 布局相对鼠标桥（三版演进）

#### 症状与归属

§10.3 / §10.7 让 PC UI 把游戏的 `DeviceInfo.inputType` 推成 `Keyboard`（真机已验证到"桌面布局"）。但桌面布局下游戏会**隐藏光标并改读 `InputManager.GetAxis("Mouse X")` / `("Mouse Y")`**；Android 的输入路径是绝对触摸（`touch_input_android.cpp` 只是 stand-in，不做转换），**没有任何东西喂这两个轴** ⇒ 视角转到屏幕边缘就停住。上游 issue #23 报的正是这个。

#### 上游的接法：一个读写通道 + 一个每帧驱动

| 方向 | 上游手段 | 本仓最终形态 |
|---|---|---|
| Java → 原生（写位移 / 告知捕获成功） | `NativeCommandBridge` 的 `static native pcMouseMotion(float,float)`、`pcMouseCaptured(boolean)` | **同上游**（JNI） |
| 原生 → Java（读捕获请求 / 宣告可调用） | `static native boolean pcMouseCaptureRequested()` | **同上游**（JNI） |
| **每帧推进** | `nativeRender` 钩子每帧调 `NativeCommandBridge.frame()` → `DispatchAndroidFrame()` → `AndroidPcMouseState::NextFrame()` | **同上游**（JNI） |

#### 三版演进总表

| 版本 | 提交 | 做法 | **该提交之后**的状态（不是当前状态；当前见 §11.3） |
|---|---|---|---|
| ① 中继替换 | `1c65eab` | 按当时"JNI 不可直移"的判断，把三个调用改造成 relay 动词 `p`/`P` + `<status>.pcmouse` 状态文件 | 构建绿、上游 39 项夹具全过，**但从未真机验证** |
| ② 上游 JNI 路径 | `460dd11` | 新增 `core/jni_binding.h`（`BindContextLoaderNatives`）+ `RuntimeBootstrap` 的 context classloader 前置；三个调用改回 `static native`；删去中继实现 | 真机绑定成功（`PC mouse JNI natives bound` → `call path live`），**但轴仍恒 `(0,0)`** —— `frame()` 在 Java 侧那时还不存在，渲染钩子在运行时加载后照旧被摘除。**这一步只换传输层，不解决"能不能动"** |
| ③ 帧泵接线 | `6541856` | `nativeRender` 钩子在运行时加载后**保持挂载**并每帧调 `frame()` | **真机通过**（到这一步鼠标才可用）：滑动连续转向、点击生效 |
| 收尾 | `cbbca19` | 修复 ② 误删的一行中继注释，去掉不再需要的 `<cmath>` | `input_relay.cpp` 回到第 11 项开始前的**逐字节原状** |

> **②与③的分工必须分清**：②是**架构对齐**（按"直接按照上游来"的指示把传输层换成上游形态），**不是修 bug**；③才是**功能修复**。①→②的替换并不改变鼠标能否使用 —— 真正断掉的那一环（帧泵）与传输层无关。**中继版若真机跑，症状会与 `460dd11` 一模一样。**

#### 11.2.1 ① 中继版：当时为什么那样做，以及它埋下的错觉

当时的判断是 §三.1 那条铁律 —— "凡上游在 `NativeCommandBridge` 上新增的 JNI 方法，本仓一律不可直移"，理由是：运行库为挂 IL2CPP 必须 load 进**游戏族 classloader**，而面板类属于 **LSPosed 模块族**，Android 的 JNI 符号查找按 classloader 作用域隔离 ⇒ 名字式查找必然 `UnsatisfiedLinkError`（`NativeCommandBridge.java` 的类注释原文记着这段历史）。

于是替换落点选在 `PcUiMouseBridge.NativeInput` 接口后面：

| 上游 JNI 调用 | 当时的本仓通道 |
|---|---|
| `pcMouseMotion(float,float)`（写） | relay 行 `"p <dx> <dy>"` → `AddAndroidPcMouseMotion` |
| `pcMouseCaptured(boolean)`（写） | relay 行 `"P <0 或 1>"` → `SetAndroidPcMouseCaptured` |
| `pcMouseCaptureRequested()`（**读**，50 ms 轮询） | relay 每 pass（10 ms）把 `<status>.pcmouse` 重写为 `pc_capture=<0 或 1>`，**仅翻转时落盘**；Java 读该文件，缺失即视为"未请求" |

好处是实打实的：上游已把传输抽象在 `NativeInput` 接口后 ⇒ 替换只发生在 `install()` 内那三个方法体上，**`PcUiMouseBridge.java` 326 行得以逐字节照搬**（`git hash-object` = 上游 blob `6e23f9e…`）。构建绿、39 项宿主夹具全过。

**但它制造了一个错觉**：这一切"看起来都对"的证据（构建、夹具、符号、字符串）**全都与鼠标可用性无关**。真机一跑，`Android PC mouse: lock=…` 诊断行**一行都不出** —— 那才是问题所在（见 11.2.3），而它与传输层毫无关系。

#### 11.2.2 ② 派生铁律被证伪（本文最重要的一次更正）

按"直接按照上游来、删去本仓实现"的指示回查上游代码，发现**上游早已解决 classloader 问题**，而且解法就在本仓缺的那几行里：

1. 上游 `core/jni_binding.h` 的 **`BindContextLoaderNatives`** —— 不靠名字查找，而是走**当前线程 context classloader** 的 `loadClass(className)` 拿到 `jclass`，再 `RegisterNatives`；
2. 上游 `RuntimeBootstrap.java` 第 111 行有一句本仓没有的：

```java
current.setContextClassLoader(NativeCommandBridge.class.getClassLoader());
```

在 `loadIntoTargetNamespace` **之前**把当前线程的 context classloader 临时设成**模块族**，`finally` 还原。

**原理**：`RegisterNatives` 是挂在 **Class 对象**上的，不是靠符号名 —— 所以它能越过 classloader 边界。而 context classloader 决定了 `BindContextLoaderNatives` 能 `loadClass` 到谁。

**真机证据**（`native.log` 第 1、3 行）：

```
#1 [runtime] PC mouse and frame JNI natives bound
#3 [runtime] PC mouse bridge: JNI call path live
```

⇒ **§三.1 的原铁律已被本仓自己的实证推翻**，改写为：

> **`NativeCommandBridge` 上的 JNI 方法可以直移，但必须同时补齐两个前置**：① `core/jni_binding.h` 的 `BindContextLoaderNatives`；② `RuntimeBootstrap.load()` 里在 `loadIntoTargetNamespace` 之前把当前线程 context classloader 临时指向模块族、`finally` 还原。
>
> 由此再推一步：**`input_relay.cpp` 不是"JNI 不可行"的产物，而是"当时没找到注册点"的产物。** ⇒ 后续凡以"本仓是文件中继"为由拒绝的直移项，都要用这个新前置**重判一次**。

#### 11.2.3 ③ 帧泵缺链：鼠标无效的真正根因

`460dd11` 之后绑定成功了，但**鼠标依然完全无效**（视角完全不动）。定位过程：

> **「② 会不会其实已经有效？」的代码级反证**（2026-10-07 追问后追加）。两条只读命令即可判定：
>
> - `git diff 1c65eab 460dd11 -- .../XposedEntry.java` ⇒ **输出为空**。② 根本没碰帧泵那段；当时的 `installFrames` 仍是 `complete.set(true)` 之后立刻 `hooks.forEach(HookHandle::unhook)`。
> - `git show 460dd11:.../NativeCommandBridge.java | grep 'static native'` ⇒ 只有 3 条（`pcMouseCaptureRequested` / `pcMouseCaptured` / `pcMouseMotion`），**没有 `frame()`**；那条"每帧驱动"的 Java 入口是 `6541856` 才加上的（该提交给 `NativeCommandBridge.java` +9 行）。
>
> ⇒ ② 与 ① 在"能不能动"这件事上**完全等价**：都没有任何调用方去驱动 `DispatchAndroidFrame()`。`6541856` 的提交信息（当时所写）也记着同一件事：*"The mouse bridge was bound and its capture gate opened, yet the axes stayed at zero: DispatchAndroidFrame() had no caller anywhere in this tree."* ② 拿到的是**绑定成功 + 闸门可开**，拿不到**轴动**。

1. 拉日志：**`Android PC mouse: lock=…` 一行都没有**。而 `PumpAndroidPcMouseDiagnostics()` 的闸门只有 `g_pc_ui_enabled && g_diagnostics_enabled && !suspend`，且同源的 `cursor request` 行**能**打出 ⇒ 两个开关是开的，卡的是 `suspend = g_suspend || !AndroidForeground()`。
2. 查 `g_foreground`：默认 `true`，本仓无人调 `SetAndroidForeground(false)` ⇒ 不该是它。
3. 追到 `AndroidUiFrame()` 的泵，再往上追 `DispatchAndroidFrame()` —— **全仓 `grep -rn` 零命中，本仓从未接线**。
4. 上游接法：`native_bridge.cpp` 的 `JNI_OnLoad` 暴露 `frame`，由 `XposedEntry` 的 `nativeRender` 钩子每帧调用。
5. 本仓对照：`installFrames` 确实挂了 `nativeRender`，但只把它当"等首帧 → 加载运行时"的**闩**，`complete.set(true)` 之后**立刻摘钩子**（`hooks.forEach(HookHandle::unhook)`）。

**后果链**：钩子一摘 ⇒ `frame()` 永不调用 ⇒ `DispatchAndroidFrame()` 永不执行 ⇒ `AndroidPcMouseState::NextFrame()` 永不执行 ⇒ `Read()` 的**逐帧快照永远停在第一帧** ⇒ 游戏读到的轴恒为 `(0,0)` ⇒ 鼠标完全无效。

**修法**（`6541856`）：把钩子的生命周期从"加载完就摘"改成"运行时存活期间保持"，每帧调 `frame()`；一旦调用抛错就 `unhook` + `report`（**不静默**，避免再次出现"看起来正常但没工作"）。同时把 `g_il2cpp_runtime` 改成原子发布，消除新增的跨线程读。

> **这是"挂载 ≠ 接入"的又一例，且是教科书级的一例**：`android_pc_mouse.h` 状态机、`android_frame.cpp` 的 8 个 C 导出、两个 `.inc`、`ui/module.cpp` 的契约与 detour —— 全部到位、符号全在、字符串全命中，**唯独驱动源没接**。四项表层证据（构建 / 符号 / 字符串 / 夹具）**没有一项**能发现它。

#### 文件清单（第 11 项全程净变化，`409ee0f` → `cbbca19`）

| 文件 | 净变化 | 与上游的关系 |
|---|---|---|
| `native/shared/android_compat/android_pc_mouse.h` | +112（新增） | **逐字** |
| `native/shared/android_compat/android_frame.cpp` | +12 | **逐字**（移植前与上游父提交**逐字节相同** ⇒ 零冲突纯增量） |
| `native/modules/ui/android_pc_mouse_diagnostics.inc` | +143（新增） | **逐字** |
| `native/modules/ui/android_pc_mouse_runtime.inc` | +89（新增） | 上游 + 本仓 **+15/−2 最小探针** |
| `native/modules/ui/module.cpp` | +61 | 仅 1 处冲突（落在我方日志 summary 区），取"上游代码 + 我方 summary" |
| `android/.../core/jni_binding.h` | +44（新增） | **逐字** |
| `android/.../native_bridge.cpp` | +103/−1 | 上半（3 个 pcMouse + `frame` 导出 + `JNI_OnLoad` 注册）**逐字**；其余为本仓独有 |
| `android/.../RuntimeBootstrap.java` | +16/−1 | **逐字**（上游 111 行那块） |
| `android/.../NativeCommandBridge.java` | +28 | **上半逐字**（三个 `static native` 声明 + `frame()`） |
| `android/.../PcUiMouseBridge.java` | +326（新增） | **逐字**（blob `6e23f9e…` == 上游） |
| `android/.../XposedEntry.java` | +65/−9 | 本仓版重度分叉；pcMouse 挂点照上游，帧泵为按上游语义重写 |
| `android/app/src/testHost/`（`PcUiMouseBridgeHostTest.java` + `pcui_mouse_fixture.py`） | +171 / +161（新增） | **逐字**（`build.gradle.kts` 未声明 `testHost` 源集 ⇒ 不进 APK） |
| `android/app/src/main/cpp/input_relay.cpp` | **+1/−1 ⇒ 净 0** | ① 加的中继动词与 `<cmath>` 已全部回退，与起点**逐字节一致** |

**合计**：13 个代码文件（新增 6 件），**+1430 / −12**。

### 11.3 真机验收（PJX110 `b992bd53` + 外接鼠标）—— **通过**

| 项 | 结论 |
|---|---|
| 滑动 | **连续转向**，不再到屏幕边缘就停 |
| 点击 | 生效 |
| 菜单开合 | `relative_capture` 在 `requested` ↔ `off` 之间正确翻转（日志 `#383/385/387/394/398/399`） |

验收前提：**必须接鼠标** —— `PcUiMouseBridge.hasMouse()` 检查 `InputDevice.SOURCE_MOUSE`，纯触摸下该桥**自我抑制**（功能不激活、**也不报错**）。本次用蓝牙鼠标（识别为 `ATK Mouse BT Mouse` / event11）。

### 11.4 证据

**真机层**（`native.log`，当前会话 400 行，游戏 `com.hypergryph.endfield`）

| 判据 | 结果 |
|---|---|
| JNI 绑定 | `#1 [runtime] PC mouse and frame JNI natives bound`、`#3 [runtime] PC mouse bridge: JNI call path live` |
| 捕获闸门跟随菜单 | `cursor request: show=false, force=true, relative_capture=requested` ↔ `show=true, …, relative_capture=off` —— 多轮翻转 |
| 原生收到相对位移 | `relative_events` 0 → **86**（109 行 `relative_events>0`） |
| 游戏在持续读轴 | `axis_reads` 0 → **862** |
| 旧轴路径有值 | `legacy_axis_abs_max` 非零 12 行（如 `(3.8374,3.3396)`） |
| 异常 | `UnsatisfiedLinkError` / `ClassNotFoundException` / `NoSuchMethodException` **均为 0** |

**构建与符号层**

| 判据 | 结果 |
|---|---|
| NDK 语法检查 | `native_bridge.cpp`（含 `-I custom_model -I model`）、`input_relay.cpp`、`ui/module.cpp`、`android_frame.cpp` 全部零错（`-Wall -Wextra -Wpedantic`） |
| 上游自带宿主夹具 | `PcUiMouseBridgeHostTest: 39 lifecycle, delta and button transport checks passed` —— 跑的是**本仓这份 `PcUiMouseBridge.java`**，仅传输面用桩 |
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `.so` | 中继版 `48,947,968` → JNI 版 `48,942,920` → 帧泵+探针版 **`48,973,792`**；SHA-256 `c204f9a6…` |
| JNI 导出（`llvm-nm`，`T`） | `Java_..._NativeCommandBridge_{frame, pcMouseCaptureRequested, pcMouseCaptured, pcMouseMotion}` 全在；`Java_*` 总数 **9** |
| 中继面已清 | `input_relay.cpp` 的 `pcMouse` / `pcmouse` / `pc_capture` / `AndroidPcMouse` 命中数 **0**；与 `409ee0f` **逐字节一致** |
| APK | **`81,556,530` 字节**；SHA-256 `85cd3f1c…`（debug 变体，按既定规则**不作为交付物**） |

> **`.so` 增长的解释**：Debug 变体不 strip，`.debug_info` 段单独 11.8 MB；新增生产代码的 DWARF 与代码同量级。⇒ **无"悄悄变大"**。

### 11.5 未验证 / 遗留

1. **上游 `native/tests/android_rebuild/pc_mouse_state_test.cpp` 未跑**。本机 LLVM 22 **能编但无链接器**（`C:/Program Files (x86)/Microsoft Visual Studio/2022` 目录为空、Windows SDK 只有 import lib、无 mingw/zig）⇒ 宿主出不了可执行文件。命令见上游 `run.sh` 的 `pc_mouse_state` 段。
2. **release 包未构**。本机无 release 密钥库 ⇒ 只能走 CI；且本仓 `android-release.yml` 仅 `workflow_dispatch`，推 `codex/*` 不触发任何 CI。
3. **鼠标按钮 / 滚轮转发**（经 `UnityPlayer.injectEvent` 反射）只有夹具证据，真机未单独验（用户确认的"点击"覆盖了主路径）。
4. **悬浮窗侧「模型管理 / 游戏视野」页**（第 4 项遗留）滑条/选择器的实际交互仍须人工确认（§10.6）。
5. **上游 `790ae1c`**（Workshop 导航，新增 `ThirdPartyModulesActivity.java`）**有意不取** —— 页面层，本仓是 Compose。

### 11.6 本轮确认与更正的口径

| 结论 | 依据 |
|---|---|
| **`NativeCommandBridge` 的 JNI 面可以直移**（原铁律作废） | §11.2.2：`BindContextLoaderNatives` + context classloader 前置；真机 `#1`/`#3` 两行 |
| **`input_relay.cpp` 的存在理由被重新解释** | 它不是"JNI 不可行"的产物，是"当时没找到注册点"的产物 ⇒ 后续以"本仓是文件中继"为由的拒绝**都要重判** |
| `PcUiMouseBridge.java` / `jni_binding.h` 逐字可搬 | blob `6e23f9e…` == 上游；`git diff upstream/main HEAD -- core/jni_binding.h` 为空 |
| `android_frame.cpp` 移植前与上游**逐字节相同** | 移植前 `git diff upstream/main:<file> -- <file>` 为空 ⇒ 禁区目录"只增不覆"未受损 |
| `android/app/src/testHost/` 对 APK **无副作用** | `build.gradle.kts` 只声明 `main` 的 assets `srcDir`，未声明 `testHost` 源集 |
| R8 面**无需**新增 keep 规则 | `PcUiMouseBridge` 由 `XposedEntry`（已有 `-keep class … { *; }`）直接引用；反射目标是 `com.unity3d.player.UnityPlayer`，不在编译类路径上 ⇒ R8 无法改名；未新增清单组件，`manifestComponents` 门禁无需追加 |
| **「表面证据全绿」不等于功能可用** | ① 的构建 / 夹具 / 符号 / 字符串四项全过，而真机零诊断行；真正的断点在**驱动源**（`DispatchAndroidFrame` 无调用方） |

---