# 运镜功能 Android 适配方案（2026-09-30）

> 任务：把相机模块的运镜能力（高级运镜预设 / 关键帧 / VMD 镜头）适配到 Android。
> 方法：纯静态排查源仓库（`native/`、`ui/`、`android/`、`docs/CAMERA_*.md`），未改代码、未构建。
> 结论先行：**原生运镜本体已经全量在 Android 包里**，适配工作集中在应用层的三个缺口（配置面、VMD 文件投递、触摸转向），均可沿用既有模式，其中前两个阶段零原生改动。

---

## 0. 一页结论

| 层 | 状态 | 说明 |
|---|---|---|
| 原生运镜运行时 | **已就绪** | `camera/module.cpp` 1.6.0（含 `free_camera_runtime.inc`）被 `android/app/src/main/cpp/CMakeLists.txt` 直接编入 `betterendfield_desktop_features` 静态库，桌面源零改动 |
| 平台替身 | **已就绪** | `native/shared/android_compat`：QPC→steady_clock 纳秒；`GetAsyncKeyState`→虚拟键锁存；`GetForegroundWindow`→恒真；`CreateFileW/ReadFile/GetFileSizeEx/CloseHandle`→POSIX 只读（VMD 装载天然可用） |
| 键位与悬浮窗 | **大部分就绪** | 10 个热键（0x60-0x69）已钉进 ini；悬浮窗已有移动十字键 + 运镜/关键帧按钮；缺 VMD 播放键、FOV ± 应改按住 |
| 配置面 | **缺口 A** | `ModuleSettings.setCameraSettings` 不写 13 个运镜/VMD 键 ⇒ 只有默认 orbit 参数可用，dolly/crane/truck 无法选择 |
| VMD 文件投递 | **缺口 B** | 无「用户 .vmd → 游戏进程可读路径」通道；`vmd_camera_file` 无值可写 |
| 触摸转向 | **缺口 C** | `g_mouse_dx/dy` 在 Android 恒 0 ⇒ 自由相机能移动/滚转/变焦但**不能转向**，运镜构图不可用 |

**分阶段方案**：P1 配置面+面板补齐（零原生改动）→ P2 VMD 投递链（零原生改动）→ P3 触摸转向（原生三处小改）→ P4（可选）运行时热调参。

---

## 1. 现状盘点

### 1.1 Win 端实现（本仓 `native/modules/camera`，1.6.0，2026-09-26 编码完成、未实机验证）

`free_camera_runtime.inc`（约 1000 行，include 于 `module.cpp:1173`）五件套：

1. **自由相机完整位姿**：位置 + yaw/pitch/roll + FOV，QPC 实时钟推进；写入点已迁 `PushState`（写 CameraState 的 RawPosition/RawOrientation、清 correction、设 Lens FOV/Dutch）；Brain 停推时（如 timeScale=0）由 unscaled 心跳 fallback 写 transform（`WriteFreeCameraTransform`，带一次性日志）。
2. **高级运镜 4 预设**（`MotionPreset`）：Orbit（半径/角速度/高度，LookAt 锚点）/ DollyZoom（希区柯克：`tan(fov/2)·距离` 守恒反向调 FOV，<0.3m 或 FOV 越界即停）/ Crane（匀速升降+LookAt）/ Truck（水平横移）。锚点 = 角色位置 + `motion_target_height`（角色不可得时退相机前 5m）。统一 SmoothStep 0.6s 缓入缓出；`motion_duration=0` 为无限。
3. **关键帧**：≤64 个（位置/四元数/FOV），回放 Catmull-Rom 位置 + Slerp 朝向 + FOV 插值；段长 `keyframe_segment_seconds`、循环 `keyframe_loop` 可配。**无存档/读档文件功能**（Win 侧也未做，见 `CAMERA_EIEM_PORT_PLAN_20260926.md` 阶段 1d 备注）。
4. **VMD 镜头**：解析 VMD（两种文件头变体、跳过 bone(111B)/morph(23B) 段、61B 相机记录、30fps、同帧后写胜出）；EIEM 贝塞尔（控制点 0..127，牛顿迭代 12 次）；MMD 欧拉三分量取反按 Y·X·Z 组合、180° 基、`vmd_camera_scale=0.07`、FOV +`vmd_camera_fov_bias`；锚点 = 角色位置+朝向。文件经 `CreateFileW` 读，64MiB 上限。
5. **触发链**：`InputThreadMain` 轮询 5 热键（`VK_NUMPAD8/0/2/4/6` → motion/keyframe_add/play/clear/vmd_play 请求原子量）→ 游戏主线程泵 `PumpFreeCameraRequests` 边沿触发（toggle 语义：同类型再按停、异类型切换）。

Win UI：`ui/BetterEndfield.UI/Models/FreeCameraExtras.cs` 13 个功能键 + 10 热键全量往返 ini；`MainWindow` 有「运镜与镜头导入」区块。

配置键全表（`module.cpp:1871-1891` 解析、2206-2229 应用到原子量；范围夹取在 1909-1915）：

| 键 | 默认 | 夹取范围 |
|---|---|---|
| `motion_preset` | 0 (orbit) | 0-3（字符串 orbit/dolly_zoom/crane/truck） |
| `motion_speed` | 1.0 | ±20 |
| `orbit_speed` | 20.0 | ±180 |
| `motion_duration` | 0.0（无限） | 0-600 |
| `motion_target_height` | 1.2 | ±5 |
| `keyframe_segment_seconds` | 3.0 | 0.2-60 |
| `keyframe_loop` | false | — |
| `vmd_camera_file` | 空 | 路径字符串 |
| `vmd_camera_scale` | 0.07 | 0.001-10 |
| `vmd_camera_fov_bias` | 5.0 | ±60 |
| `vmd_camera_loop` | false | — |

### 1.2 Android 已就绪部分（逐项核实）

| 项 | 位置 | 状态 |
|---|---|---|
| 编译 | `CMakeLists.txt:48-53` `betterendfield_desktop_features` 静态库收 `camera/module.cpp` | ✅ 整个 1.6.0 已在 APK |
| 时钟 | `android_win32.h` `QueryPerformanceCounter`→纳秒（`NowSeconds` 可用） | ✅ |
| 键位 | `android_win32.cpp` `SetVirtualKey/VirtualKeyDown`（Press 按住 / Pulse 180ms / Release），`GetAsyncKeyState`→锁存 | ✅ |
| 焦点 | `GetForegroundWindow`→恒真（`GameWindowHasFocus` 恒 true，方向键不失效） | ✅ |
| 文件 | `CreateFileW`(GENERIC_READ+OPEN_EXISTING)→`open(O_RDONLY)`；`MultiByteToWideChar`→UTF-8 严格校验 | ✅ VMD 装载可用 |
| 鼠标钩子 | `free_camera_runtime.inc:240` `#if defined(_WIN32)` 整段跳过 | ✅ 编译安全（副作用：增量恒 0，见缺口 C） |
| 热键钉死 | `ModuleSettings.java:286-295` 写 10 热键名 + `free_camera_mouse_look=false` | ✅ |
| 悬浮窗 | `OverlayPanel.kt:81-84`：`MovementPad`（6 向按住）+ `MotionControls`（播放/停止、回正、广角/长焦、滚转按住、关键帧记录/回放/清除） | ✅ 大部分（缺 VMD 按钮） |
| 键位中继 | `input_relay.cpp` 行协议 `"<vk> <action>\n"` / `"c <payload>\n"` / `"r\n"`，10ms 轮询，游戏 filesDir | ✅ |
| SAF 先例 | `GameOverlay.saveJournalToFile`：`ACTION_CREATE_DOCUMENT` + `XposedEntry.activityResultRelayReady()` + listener + 分享降级 | ✅ 可复用于读方向 |
| 面板显隐 | `OverlayFeatures.read`（游戏进程读远程 SharedPreferences） | ✅ 加字段即扩 |
| 配置投递 | `RuntimeBootstrap`（游戏进程）`Os.setenv("BETTER_ENDFIELD_CAMERA_CONFIG", configs.camera())` → `DesktopModule::Start` 一次读入 → `configuration_changed` | ✅ 通道在（boot-only，见决策 1） |

### 1.3 缺口明细

- **A 配置面**：`ModuleSettings.setCameraSettings`（`ModuleSettings.java:253-296`）只写开关/速度/FOV/热键/`free_camera_mouse_look=false`/`free_camera_smoothing=0.3`/两个 loop 硬编码 false。13 个运镜/VMD 键全部缺席 ⇒ 原生只能用默认值跑 orbit；`dolly_zoom/crane/truck` 无入口；体验页（`ExperiencePage.kt` CameraCard）无任何运镜 UI。
- **B VMD 投递**：无导入 UI、无「文件进入游戏 filesDir」链路、`vmd_camera_file` 无值；悬浮窗无 `VMD_PLAY`（0x66）按钮。
- **C 触摸转向**：`StepFreeCamera` 的 yaw/pitch 只来自 `g_mouse_dx/dy`（`module.cpp:248-249` 匿名命名空间原子量，Windows 钩子喂入，Android 恒 0）。移动/滚转/FOV 都有键，唯独**转向**没有任何输入源。dolly 需要瞄锚点、关键帧取景、VMD 预览对位都依赖转向。
- **D 面板细节**：广角/长焦是 180ms Pulse（≈3.6°/tap），语义应为按住（`HoldControl` 先例已存在）。

---

## 2. 适配方案（P1 → P2 → P3，可选 P4）

### P1 配置面 + 面板补齐（零原生改动）

**改动文件**：`ModuleSettings.java`、`SettingsState.kt`、`SettingsPages.kt`/`ExperiencePage.kt`、`OverlayControls.kt`、`OverlayFeatures.java`、strings 资源。

1. `setCameraSettings` 扩参（建议聚合为一个 `CameraMotionOptions` record，避免参数继续膨胀）：
   - 写全 13 键：`motion_preset`（字符串四选一，原生 `ParseMotionPreset` 已认）+ 4 数值 + `keyframe_segment_seconds/loop` + `vmd_camera_file`（P2 前写空或占位符）+ `vmd_camera_scale/fov_bias/loop`；
   - SharedPreferences 对应新键持久化，`republishConfigurations()` 同步带上（升级用户免逐页重开）。
2. 体验页 CameraCard 自由镜头组下加「运镜与镜头」**子页**（沿用 `SubPageRow` + `SettingsPage` 子页码先例，FIRST_PERSON=11 之后顺延）：预设四选一（分段控件）、速度/环绕角速度/时长（0=无限，注明）/锚点高度、关键帧段长+循环开关、VMD 参数组（缩放/FOV 偏置/循环）。
3. `OverlayControls.MotionControls`：广角/长焦改 `HoldControl`；新增「VMD 镜头 播放/停止」行（`callbacks.pulse(Hotkeys.VMD_PLAY, …)`），显隐条件 = `OverlayFeatures` 新增 `vmdCamera` 字段（读新 pref 键，语义同 `worldPause`）。
4. 约束：颜色只用 `UiTokens.Be`（工业黄黑白）；状态入口一律 `updateXxx`；**不碰** 19 个 Java 数据层/桥接文件。

**生效语义**：boot-only——改配置后强停重启游戏（与第一人称先例一致，CHANGELOG 已有明示惯例）。

**验收判据**（面板日志）：
- 切 orbit 播放 → `Free camera motion started: orbit`；时长到点 → `motion finished`；
- 切 dolly_zoom（相机距锚点 <0.5m 时）→ `dolly zoom needs the camera at least 0.5 m from the target`；
- 关键帧 <2 个回放 → `record at least two keyframes before playing`；记录 → `Free camera keyframe N recorded`。

### P2 VMD 投递链（零原生改动）

**改动文件**：设置 app（`CameraMotionPage`/`SettingsState`/`FrameworkSettings`）、`CameraVmdFile.java`（新）、`RuntimeBootstrap.java`、`ModuleConfigurations.java`、`XposedEntry.java`、`OverlayFeatures.java`/`OverlayControls.kt`/`OverlayPanel.kt`、strings。

> **原方案的一处错误（2026-09-30 实施时更正）**：本节最初写的是「存 module 自己的 filesDir → RuntimeBootstrap 从 module filesDir 拷到 game filesDir」。这在 Android 上不成立：设置应用与游戏是两个 UID，`/data/user/<uid>/<pkg>` 是 0700，游戏进程读不到模块应用的数据目录，`Context.getFilesDir()` 拿到的路径只是字符串，打不开。真正可用的通道是**框架（LSPosed 服务）的远程文件空间**——`getRemotePreferences` 相邻的 `openRemoteFile` / `listRemoteFiles` / `deleteRemoteFile`，BEM 包管理器已在用（`FrameworkSettings.publishBem`）。P2 按该通道实施，见第 7 节。

1. **导入入口放在设置 app**（不是悬浮窗）：运镜子页加导入行。`ActivityResultContracts.OpenDocument()` 先例已在 `BemInstallScreen.kt:50`。选完先落缓存临时文件并流式校验：大小 >0 且 ≤64 MiB（与原生 `kMaxVmdFileBytes` 一致）、前 30 字节为 `Vocaloid Motion Data 0002` 或 `Vocaloid Motion Data file`；通过后经 `FrameworkSettings.publishVmd` 写进远程文件空间，名称 `vmd.current`。文件名/大小/导入时间进 SharedPreferences 供页面展示。
   - 选设置 app 而非悬浮窗的理由：完整 Activity Result 能力（`activityResultRelay` 依赖 hook 中继、有降级分支）、可独立回归（HLK-AL00 无游戏也能测导入）、不在游戏运行时打扰游戏。
2. **投递 = RuntimeBootstrap 物化**（`BemInstalledResources.prepare` 同模式，游戏进程内、module→game 两个 Context 已就绪）：把远程空间里的 `vmd.current` 拷到 `game filesDir/betterendfield/camera/current.vmd`（目标已存在且长度等于登记值则跳过，记日志；写入走 `.tmp` → `Os.rename`）。
3. **路径进配置用占位符**：`ModuleSettings` 在已导入时写 `vmd_camera_file=%files%/betterendfield/camera/current.vmd`；`RuntimeBootstrap` 在 `setenv(BETTER_ENDFIELD_CAMERA_CONFIG)` 前把 `%files%` 替换为 `context.getFilesDir()` 绝对路径。占位符方案优于设置 app 硬拼 `/data/user/<uid>/<pkg>/...`（多用户/工作资料机不脆），且 RuntimeBootstrap 是唯一同时知道游戏 filesDir 与配置串的点。路径必须是绝对路径：替身层把 `CreateFileW` 实现为 `open(path, O_RDONLY)`，相对路径会按进程工作目录解析。
4. 未导入时：该键写空值（不是占位符），原生走既有 `no VMD camera file is configured` 分支——比让原生去打开一个必然不存在的文件更可读，也无需 Java 侧重复实现完整解析器。

**验收判据**：
- 面板按「VMD 镜头」→ `VMD camera playback started: N keyframes, X s.`（首次按下时若自由视角未开，会自动 `EnterFreeCamera`——原生已处理）；
- 失败分支日志逐条可复现（`no VMD camera file is configured` / `could not be opened` / `not a VMD motion` / `no camera keyframes` / `camera keyframes are all invalid`）；
- VMD 朝向/缩放/FOV 在 Endfield 场景的标定（0.07 / +5°）预期需实机微调——参数已进 P1 的 UI，闭环内可调。

### P3 触摸转向（原生三处小改 + 一个面板组件）

**改动文件**：`android_win32.h/.cpp`、`input_relay.cpp`、`module.cpp`（平台分支）、`OverlayControls.kt`、`NativeCommandBridge.java`、`GameOverlay.java`（callbacks 加一个方法）。

1. **替身层**（`android_win32`）：新增 `AddVirtualMouseDelta(int dx, int dy)`——两个原子累计器，与 `SetVirtualKey` 同级同风格（`android_virtual_keys.h` 旁）。
2. **中继协议**（`input_relay.cpp` `HandleLine`）：新增 `"m <dx> <dy>\n"` 行 → `AddVirtualMouseDelta`。文件头注释同步协议表。
3. **模块侧**（`module.cpp`）：`#if !defined(_WIN32)` 分支把替身累计器 drain 进 `g_mouse_dx/dy`（放 `StepFreeCamera` 消费处之前；`ClearMouseInput()` 已是统一清零点，顺带清替身累计器）。先例：`free_camera_runtime.inc:240` 已有 `_WIN32` 分支，桌面源共享原则未被破坏。
4. **面板**：`LookPad` 拖拽组件（`pointerInput` + `detectDragGestures`，先例 `HoldControl` 的手势骨架）——按住拖动即转向，增量换算复用 `mouse_sensitivity`（P1 顺带把灵敏度滑杆暴露到设置页，0.02-0.5）与 `mouse_invert_y`。
5. 备选已排除：虚拟键模拟转向（离散、无法构图）；JNI 直调模块符号（类加载器边界，项目已明令禁止——`input_relay.cpp` 头注释）；`runtime command` 泵（单槽，丢增量，不适合连续输入——`android_virtual_keys.h` 注释已论证）。

**验收判据**：LookPad 拖动 → yaw/pitch 连续变化；冻结（timeScale=0）中仍可转向（心跳 fallback 生效证据：`Cinemachine is not pushing; writing the camera transform` 日志 + 画面确实转动）；滚转/FOV 按住语义复查。

### P4（可选）运行时热调参

相机模块消费 `runtime command`（`command_pump` 已有，custom_model 消费先例）：`motion_preset=N`、`vmd reload` 等免重启。**P1 实测重启成本可接受则不做**；做则需给相机模块加泵排空点（挂 `PumpFreeCameraRequests` 所在泵），并把 `AcknowledgeRuntimeCommand` 状态接进面板日志。

---

## 3. 关键设计决策（记录理由）

1. **重启生效（boot-only）vs 热更新**：跟随第一人称先例（`ModuleConfigurations` 明注「设置 Activity 写、注入侧只读」；CHANGELOG 3.3.21 明示强停重启）。运镜参数属低频调整，P4 视实测体验再立项。
2. **VMD 导入在设置 app、投递在 RuntimeBootstrap**：与 actions 骨骼库「APK 资产 → 游戏进程物化」同构；避免在游戏进程内跑 SAF 选择器（中继依赖 hook、存在降级路径）；HLK-AL00 可脱离游戏回归导入功能。
3. **路径占位符 `%files%`**：RuntimeBootstrap 是唯一同时握有游戏 filesDir 与配置串的组件；避免设置 app 硬拼 `/data/user/<uid>/...` 在多用户/克隆应用场景失效。
4. **转向走「替身原子量 + relay 行」**：同一 `.so` 内原生符号中转（`betterendfield_desktop_features` 静态链接进 `betterendfield_android`），无 JNI、无新权限、无类加载器问题；连续增量语义天然匹配拖拽。
5. **FOV ± 从 Pulse 改 Hold**：行为修正而非新功能，180ms 脉冲（≈3.6°/tap）与桌面按住语义不符。

---

## 4. 风险与未验项

| 风险 | 说明 | 缓解 |
|---|---|---|
| 运镜 1.6.0 **从未实机验证**（Win 侧也未验） | Android 首验即双平台首验；`CAMERA_EIEM_PORT_PLAN` 三条「实机需确认」全部悬空 | 分阶段发布、日志判据逐条列（本文各 P 节）；失败可逐项定位 |
| 冻结中 `PushState` 是否仍被调用 | 未验（Win/Android 共同遗留）。不调用则靠心跳写 transform | 设计已兜底（`BrainIsPushing` 0.05s 判旧 + fallback）；P3 验收项覆盖 |
| VMD 标定值 | `vmd_camera_scale=0.07` / FOV bias +5° 沿 EIEM（MMD 场景标定），Endfield 场景可能偏 | 参数已进 UI；实机调参闭环 |
| 悬浮窗本身未全验 | 展开后交互、触摸透传等仍未实机验证（见 MEMORY 第 8 节） | P1 面板改动小（两处），随运镜验收一并覆盖 |
| R8 / 门禁 | 新增 Kotlin 类无新按名解析面；`NativeCommandBridge` 变更涉及 relay 写入 | 本地 `assembleRelease` 跑 `verifyReleaseEntryPoints` 即可，无新 keep 规则预期 |

---

## 5. 交付节奏与文档同步

- **版本**：P1 → `3.3.22-alpha.5`（已交付）；P2 → `alpha.6`（已交付）；P3 → `alpha.7`。
- **闭环**：push → CI（debug）→ 本地 `assembleRelease` 交付 release 包（release 签名，原位覆盖）→ PJX110 实机 → 用户贴面板日志 → 诊断。
- **文档四件套**：每阶段 `CHANGELOG.md` / `README.md` / `README.en.md` / `android/README.md`；P2 占位符机制与 P3 relay 行协议属机制类，另加 `docs/GAME_INTERFACES.md`。
- **验收基线设备**：HLK-AL00（冷启动回归 + 设置 app 可测导入）；PJX110（游戏内全链路）。

---

## 6. P1 实施结果（2026-09-30，已交付 3.3.22-alpha.5）

**已落地**：`ModuleSettings.CameraMotion` 记录（13 键归一化 + 偏好持久化 + ini 序列化）→ `SettingsState` 运镜状态与读写 → 新子页 `CameraMotionPage.kt`（`SettingsPage.CAMERA_MOTION = 14`，挂在「体验 → 自由镜头」组下）→ `OverlayControls` 广角/长焦改按住 → 字符串与 instrumented 断言。

**与原计划的三处偏离**（都是为了不放出「按了没用」的控件，符合本项目既有原则）：

1. **VMD 面板按钮与 `OverlayFeatures.vmdCamera` 字段推迟到 P2**。原计划 P1 就加按钮 + 一个「已配置 VMD」开关；但 P1 阶段没有任何途径让 `.vmd` 就位，该按钮唯一的效果是打印「未配置文件」，开关也只会让用户开一个注定失败的功能。改为与导入通道同时在 P2 交付；`android/README.md` 的对照表本就是「VMD replay … no panel button」。
2. **`mouse_invert_y` / `mouse_sensitivity` 的 UI 推迟到 P3**。原计划把灵敏度滑杆「顺带」放在 P1；但 Android 的鼠标增量恒为 0，P1 阶段这两项写什么都无效果。记录里仍保留这两个字段并按默认值写入（配置契约因此是完整的），P3 加 LookPad 时再补两行 UI，无需再动 `ModuleSettings`。
3. **补了一个先于本次改动的破损**：`androidTest` 源集自 3.3.22 Compose 重写起引用了已删除的 `ValueSlider`，整份 `CameraSettingsTest` 无法编译（该源集不在 CI 路径上，因此长期未暴露）。顺手移除死调用，新增的运镜断言这才真的可编译。

**实施中发现并修掉的一处契约缺口**：原生 `ParseMotionPreset` 除了四个正式名，还接受 `dolly` 与 `pan` 两个短别名；Java 侧若只认正式名，任何从别处写进偏好的等价预设都会在设置页保存时被静默改写成 `orbit`。`presetName` 现已归一这两个别名。

**验证证据**（本机，未上机）：

| 项 | 结果 |
|---|---|
| release 构建 + `verifyReleaseEntryPoints` | 通过（framework entry、4 manifest 组件、3 JNI 导出、mapping 未改名、12 个游戏进程类未被合并） |
| 产物 dex 检出 13 个 ini 键 + 4 个预设名 | 全部命中 |
| 键名与 `module.cpp` 解析器交叉核对 | 13/13 一致，无拼写差异 |
| 真实 `CameraMotion` 在 JVM 生成 ini | 默认值、越界夹取（±20 / ±180 / 0–600 / ±5 / 0.2–60 / 10 / −60）、非有限回退、预设归一化全部符合 |
| instrumented 断言 | 编译通过，**未执行** |
| 签名身份 | 与已发布 alpha.3 完全一致（`8CD6FDC1…`），可原位覆盖 alpha.2 起任何版本 |

**未上机的原因**：在线设备只有 `HLK-AL00`，其上装的是 3.3.21，签名为旧的临时 debug 身份（`62713BA0…`），与本版不相容；原位安装被系统拒绝，需要先卸载。未擅自卸载用户设备上的应用。运镜本体在 Windows 侧也从未实机验证，因此这次是双平台首次上机验证。

**下一步（P2）**：见第 7 节，已交付 `3.3.22-alpha.6`。

---

## 7. P2 实施结果（2026-09-30，已交付 3.3.22-alpha.6）

**已落地**（零原生改动）：

| 层 | 改动 |
|---|---|
| 契约 | `ModuleSettings` 增 `VMD_FILE_SLOT`（`%files%/betterendfield/camera/current.vmd`）、`VMD_REMOTE_NAME`（`vmd.current`）、`CAMERA_VMD_IMPORTED` / `VMD_BYTES` 等元数据键；`CameraMotion` 增 `vmdImported` 字段并据此写 `vmd_camera_file` |
| 校验 | `ModuleSettings.isVmdMotion(byte[30])` 与 `vmdMaximumBytes()`，逐字节对齐 `LoadVmdCamera`（30 字节视图、两代文件头、64 MiB 上限） |
| 发布 | `FrameworkSettings.publishVmd / removeVmd / remoteAvailable`，写入 LSPosed 远程文件空间 |
| 设置页 | `SettingsState.importVmd(uri)` / `clearVmd()` + 运镜子页导入行（`OpenDocument` + 清缓存临时文件 + 后台线程 + 主线程回写状态） |
| 游戏侧 | 新 `CameraVmdFile.materialize`（远程文件 → 游戏 filesDir，长度比对跳过、`.tmp` → `Os.rename`）；`RuntimeBootstrap` 增 `vmdBytes`/`Source` 参数、启动前物化、`setenv` 前展开 `%files%`；`ModuleConfigurations.needsVmdCameraFile()`；`XposedEntry` 接线 |
| 面板 | `OverlayFeatures.vmdCamera = freeCamera && 已导入`；`MotionControls` 增 VMD 行并接收 features；`OverlayPanel`/`GameOverlay` 传参 |

**与原计划的一处实质性偏离**：投递通道由「module filesDir 中转」改为「框架远程文件空间」。原假设不成立（两个 UID，游戏读不到模块应用的数据目录），已在第 2 节 P2 就地更正并留下记录。同时把「未导入时写占位符、由原生报打开失败」改为「未导入时写空值」：后者走的是原生既有的「未配置文件」分支，比让原生去打开一个必然不存在的路径更可读。

**验证证据**（本机，未上机）：

| 项 | 结果 |
|---|---|
| release 构建 + `verifyReleaseEntryPoints` | 通过 |
| `assembleDebugAndroidTest` | 通过 |
| 产物 dex 检出 8 个新字面量（占位符、`vmd.current`、两个偏好键、两种文件头、目标路径等） | 全部命中 |
| 真实 `ModuleSettings` 在 JVM 生成 ini | 默认（空值）/ 已导入（占位符）/ 越界夹取 / 非有限回退四组输出一致 |
| 真实 `isVmdMotion` 在 JVM 判定 | `0002` ✓、legacy `file` ✓、`0001` ✗、`Vocaloid Motion Data` ✗、`garbage` ✗、29 字节 ✗、`null` ✗ |
| instrumented 断言 | 编译通过，**未执行**（新增占位符、清空、元数据与文件头断言） |

**未验证项（须实机）**：远程文件空间的实际投递与 `%files%` 展开后的 `open()` 成功与否；VMD 朝向/缩放/FOV 在 Endfield 场景的标定（0.07 / +5°，来自 MMD 场景，预期需微调）；面板 VMD 按钮的显隐与按下效果。

**下一步（P3）**：触摸转向——替身层 `AddVirtualMouseDelta` + relay 新行协议 + `module.cpp` 非 Win 分支 + 面板 `LookPad`；同批把 `mouse_sensitivity` / `mouse_invert_y` 的 UI 补上（灵敏度滑杆 0.02–0.5，`LookPad` 的增量换算要读它）。
