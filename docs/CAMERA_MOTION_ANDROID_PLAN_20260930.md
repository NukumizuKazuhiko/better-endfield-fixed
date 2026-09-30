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
| 键位与悬浮窗 | **已就绪** | 10 个热键（0x60-0x69）已钉进 ini；悬浮窗已有移动十字键 + 运镜/关键帧按钮 + VMD 播放键；FOV ± 已改按住 |
| 配置面 | ~~缺口 A~~ **已补齐（alpha.5）** | `ModuleSettings.CameraMotion` 写全 13 个运镜/VMD 键，并按原生 `std::clamp` 的同一组上下限夹取 |
| VMD 文件投递 | ~~缺口 B~~ **已补齐（alpha.6，已实机通过）** | 设置应用 SAF 导入并校验 → 框架远程文件空间 `vmd.current` → 游戏进程物化到 `files/betterendfield/camera/current.vmd` → 配置里的 `%files%` 展开为绝对路径 |
| 播放按键时机 | ~~取景问题~~ **已补齐（alpha.7）** | 三个播放键按下后先收起面板、延迟 1 秒再发键；这一秒里 handle 一起隐藏 |
| 隐藏期的出口与归位 | ~~缺口~~ **已补齐（alpha.7）** | 音量键=打断（丢待发键 / 停正在跑的运镜）；handle 自动归位两条路：原生 stopped 行、或按键发出后 3 秒无 started 行（兜住「原生没东西可播」的静默返回） |
| 触摸转向 | ~~缺口 C~~ **已补齐（alpha.8）** | 面板 `LookPad` 拖拽 → 中继 `m dx dy` → 替身层 `AddVirtualMouseDelta` 累计 → 相机模块每 tick 折进 `g_mouse_dx/dy`；共享桌面源一行未改，灵敏度/反转页内可调 |
| 参数生效方式 | ~~boot-only~~ **已补齐（alpha.9）** | 相机整段配置经命令泵重放，改完即生效，不必重启；首次启用相机功能例外（模块是否载入由启动配置决定） |

**分阶段方案**：P1 配置面+面板补齐（零原生改动）→ P2 VMD 投递链（零原生改动）→ P3 触摸转向（原生三处小改）→ P4 运行时热调参（原生排空点 + 通道修正）。

**进度**：P1 / P2 / 播放倒计时 + 隐藏期音量键出口 + 播完自动回 handle / P3 触摸转向 / P4 运行时热调参均已交付（`3.3.22-alpha.5` … `alpha.9`，alpha.7 ~ alpha.9 尚未验收）；P2 的 VMD 链路已由用户实机验收通过（导入 → 远程投递 → 物化 → 播放全通）。**三个缺口与 P4 全部完成。**

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
| 鼠标钩子 | `free_camera_runtime.inc:240` `#if defined(_WIN32)` 整段跳过 | ✅ 编译安全（**缺口 C 已由 P3 补齐**：面板拖动经由替身层累计器折进同一组增量，见 §9） |
| 热键钉死 | `ModuleSettings.java:286-295` 写 10 热键名 + `free_camera_mouse_look=false` | ✅ |
| 悬浮窗 | `OverlayPanel.kt:81-84`：`MovementPad`（6 向按住）+ `MotionControls`（播放/停止、回正、广角/长焦、滚转按住、关键帧记录/回放/清除） | ✅（后续补齐 VMD 按钮 alpha.6、`LookPad` 与三个播放键延迟 alpha.7/alpha.8） |
| 键位中继 | `input_relay.cpp` 行协议 `"<vk> <action>\n"` / `"c <payload>\n"` / `"r\n"`（alpha.8 增 `"m <dx> <dy>\n"`），10ms 轮询，游戏 filesDir | ✅ |
| SAF 先例 | `GameOverlay.saveJournalToFile`：`ACTION_CREATE_DOCUMENT` + `XposedEntry.activityResultRelayReady()` + listener + 分享降级 | ✅ 可复用于读方向 |
| 面板显隐 | `OverlayFeatures.read`（游戏进程读远程 SharedPreferences） | ✅ 加字段即扩 |
| 配置投递 | `RuntimeBootstrap`（游戏进程）`Os.setenv("BETTER_ENDFIELD_CAMERA_CONFIG", configs.camera())` → `DesktopModule::Start` 一次读入 → `configuration_changed` | ✅ 通道在（boot-only，见决策 1） |

### 1.3 缺口明细

> 本节记录的是**改造前**的现场（2026-09-30 方案立项时）。四个缺口此后全部补齐：A → P1 / `alpha.5`（§6），B → P2 / `alpha.6`（§7），C → P3 / `alpha.8`（§9），D → P1。下面的文字保留原样，作为「当时缺什么」的记录。

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

### P4 运行时热调参（已立项并实施，2026-09-30）

**立项理由**：P1 交付后实测的结论与立项时的假设相反——不是「重启成本可接受」，而是**重启不该出现在参数调整的闭环里**。运镜参数（预设、速度、角速度、时长、灵敏度、Y 轴反转、两个 FOV、眼位、VMD 标定值）全都是「对着画面调」的量：调一次要强停游戏、等加载、再进自由视角，一轮几十秒，而一次取景往往要试三五轮。P1/P3 之后参数面还变大了（灵敏度、反转、13 个运镜键），成本只会更显眼。

**做法：整段配置重放，而不是逐键命令**

原方案写的是「消费 runtime command：`motion_preset=N`、`vmd reload`」。实施改成**把整段配置文本交回原生，走它启动时那个入口重放**。理由是逐键命令有三件必须自己维护的事，而重放一件都不用：

1. **键的完备性**：13 个运镜键 + 2 个灵敏度键 + 第一人称的 8 个进阶键，任何一条命令漏掉一个键，那个键就停在启动时的值上——而且不会报错。
2. **两个不是赋值的转换**：`free_camera_enabled`、`first_person_camera_enabled` 由 true 变 false 时，原生要做的是**退出**（`g_force_exit_request` / `g_first_person_exit_request`），不是把布尔置 false。`ConfigurationChanged` 里已经写好这两个分支；照抄一份就是照抄一个会过时的副本。
3. **原子性**：`ConfigurationChanged` 在自己的 mutex 下把整块键一次写完；逐键命令会在键与键之间留下可观测的半套配置。

于是模块侧只剩一件事：**在 tick 里排空命令，把文本交回 `ConfigurationChanged`**。

**链路**（沿用既有通道，不新增跨进程机制）：

```
设置页改动 → ModuleSettings.setCameraSettings（文本变了才发）
           → ModuleCommandRouter.issue("camera_config", 整段配置)
           → 远程文件空间 command.next            （唯一跨进程通道，见 §7）
游戏进程   → XposedEntry 的 poller（250 ms）读取，并先物化 VMD 槽
           → NativeCommandBridge.submit → 单槽命令泵
原生       → PumpFromEngineTick 顶部 AcquirePanelCommand("camera_config")
           → ConfigurationChanged(文本) → AcknowledgePanelCommand("applied")
```

**改动清单**

| 层 | 文件 | 改动 |
|---|---|---|
| 原生核心 | `core/command_pump.h/.cpp` | `ConsumeRuntimeCommand` → `AcquireRuntimeCommand(command, value)`：**按命令名取用**，别人的命令留在槽里 |
| 原生替身层 | `android_panel_commands.h`（新）、`core/panel_commands.cpp`（新）、`android_win32.h` | 桌面模块经 `<Windows.h>` 链拿到 `AcquirePanelCommand` / `AcknowledgePanelCommand` |
| 原生相机模块 | `native/modules/camera/module.cpp` | `#if !defined(_WIN32)` 的 `DrainConfigurationReload()`，挂在 `PumpFromEngineTick` 顶部；`free_camera_runtime.inc` 一行未改 |
| 既有消费点 | `custom_model_module.cpp`、`resource_probe.cpp` | 改用按名取用 |
| Java 设置 app | `ModuleCommandRouter.java` | value 上限从 512 字符改为「整段 payload ≤ 4096 字节」（原生泵的真实判据） |
| Java 设置 app | `FrameworkSettings.java` | `writeRemoteCommand` 补 `truncate(0)` |
| Java 设置 app | `ModuleSettings.java` | `setCameraSettings` 返回 `CameraWrite(changed, firstEnable, delivered)`；文本变化才投递；三个 scalar 按原生范围夹取 |
| Java 游戏进程 | `XposedEntry.java` | 转发前物化 VMD 槽；物化不成功则**扣下**这条配置并记日志 |
| Kotlin | `SettingsState.kt`、`strings.xml` | 相机改动不再计入「需要重启的更改」；提示语区分「已投递 / 未送达 / 首次启用」 |

**四条必修的边角（都是实施时才发现的，不是设计时想到的）**

1. **单槽命令泵必须按名取用。** 泵只有一个槽，而一个进程里可能同时有自定义模型模块与相机模块：谁先跑，谁就把对方的命令取走并 Ack 成 `unsupported`。此前只有一个消费者，所以这个缺陷从未被触发。`AcquireRuntimeCommand` 改成「名字不匹配就不动槽」。
2. **远程文件必须截断。** `openRemoteFile` 是读写打开、不截断；旧实现写完不截断，短 payload 后面会留下上一段的尾巴。对普通命令无害（解析只看前三个换行），但**对配置是致命的**：残留的 `key=value` 行会被原生解析器当成新配置的一部分，而且因为排在后面，旧值会赢过新值。
3. **VMD 槽要跟着重载一起物化。** 配置里引用的是游戏自己 filesDir 里的 `current.vmd`，那份副本本来只在启动时拷一次。运行中导入一个 .vmd 时配置会立刻引用它，而文件还不存在——原生只会说 `could not be opened`，用户以为功能坏了。所以游戏进程在转发前按远程快照的字节数先物化；物化不了就**不发**，保留上一份能用的配置。
4. **投递出去的文本必须已经在原生范围内。** `movement_speed` / `field_of_view` / `first_person_fov` 三个 scalar 此前是直接拼进文本的（`CameraMotion` 与 `FirstPersonAdvanced` 里的键都有夹取，这三个没有）。这段文本现在每改一次就实时进游戏，越界值等于在原生侧被静默改写；`NaN` 更糟——`%.4f` 把它渲染成字面量 `NaN`，原生解析后落进驱动相机位置的 float。已按原生 clamp 夹取（0.5–100 / 20–120）。

**边界（必须写进用户文档，不能让人以为「什么都能热更新」）**

- **首次启用相机功能仍需重启一次**：本次启动是否加载相机模块，由**启动时的配置**决定（`native_bridge.cpp` 的 `AnyModuleRequested`）。模块没被载入，就没有人接这条命令。此后同一会话内所有相机参数改动都即时生效，包括「关掉再打开」。
- **不在本期范围**：UI 模块（隐藏 UID/HUD）、sustained dash、配音 catalog、模型替换仍按启动时配置生效——它们各有各的资源物化路径。
- **VMD 的标定值**（缩放 / 视野偏置 / 循环）可热调；**VMD 文件本身**在运行中导入也会同步（见边角 3）。

**验收判据（面板日志）**

| 事件 | 期望日志 |
|---|---|
| 游戏在跑时改任一相机参数 | `Camera configuration reloaded from the settings app: N bytes, applied without restarting the game.`，紧跟原生自己的 `Camera configuration applied: enabled=true, ...` 与 `Free camera extras: ...` |
| 关掉自由视角 | 上面几行之后出现 `Free camera disabled`（重放走的是同一个退出分支） |
| 运行中导入 .vmd | `VMD camera motion ready: <path> (N bytes)`，然后是重载那两行 |
| .vmd 尚未就绪 | `configuration reload held back: the imported .vmd is not materialized yet (declared N bytes)`，旧配置继续有效 |
| 游戏没在跑 | 设置页提示「没有送达游戏进程」；`command.next` 留在远程空间，下次启动读到就重放（幂等） |

**代价**：`ConfigurationChanged` 会打两行日志，每次改动都进面板日志。这是有意的——热重载最需要能看见的就是「它到底发生了没有」。

**验证**：见 §10。

---

## 3. 关键设计决策（记录理由）

1. **重启生效（boot-only）→ 热重载（P4 已实施）**：立项时跟随第一人称先例（`ModuleConfigurations` 明注「设置 Activity 写、注入侧只读」；CHANGELOG 3.3.21 明示强停重启）。P4 改为热重载，但**保留了 boot-only 决定的那一件事**：模块是否载入进程仍由启动配置决定，所以「首次启用」依旧是重启语义。热重载只覆盖已经载入的模块的参数。
2. **VMD 导入在设置 app、投递在 RuntimeBootstrap**：与 actions 骨骼库「APK 资产 → 游戏进程物化」同构；避免在游戏进程内跑 SAF 选择器（中继依赖 hook、存在降级路径）；HLK-AL00 可脱离游戏回归导入功能。
3. **路径占位符 `%files%`**：RuntimeBootstrap 是唯一同时握有游戏 filesDir 与配置串的组件；避免设置 app 硬拼 `/data/user/<uid>/...` 在多用户/克隆应用场景失效。
4. **转向走「替身原子量 + relay 行」**：同一 `.so` 内原生符号中转（`betterendfield_desktop_features` 静态链接进 `betterendfield_android`），无 JNI、无新权限、无类加载器问题；连续增量语义天然匹配拖拽。
5. **FOV ± 从 Pulse 改 Hold**：行为修正而非新功能，180ms 脉冲（≈3.6°/tap）与桌面按住语义不符。

---

## 4. 风险与未验项

> 状态更新（2026-09-30，P3 交付后）：下表是立项时的清单，逐行标注了此后的事实。

| 风险 | 说明 | 状态 |
|---|---|---|
| 运镜 1.6.0 **从未实机验证**（Win 侧也未验） | Android 首验即双平台首验；`CAMERA_EIEM_PORT_PLAN` 三条「实机需确认」全部悬空 | **已关闭**：`alpha.6` 的 VMD 投递链由用户实机端到端验收通过（Win 侧仍是未验状态，本仓库不负责桌面端） |
| 冻结中 `PushState` 是否仍被调用 | 未验（Win/Android 共同遗留）。不调用则靠心跳写 transform | **仍未验**；设计已兜底（`BrainIsPushing` 0.05s 判旧 + fallback），已写进 P3 验收判据 |
| VMD 标定值 | `vmd_camera_scale=0.07` / FOV bias +5° 沿 EIEM（MMD 场景标定），Endfield 场景可能偏 | **仍待实机调参**；参数已在 UI 里可调 |
| 悬浮窗本身未全验 | 展开后交互、触摸透传等仍未实机验证（见 MEMORY 第 8 节） | **部分关闭**：handle 显隐链路已在 alpha.7 相关需求中被实际使用；面板展开交互仍无系统性验收记录 |
| alpha.7 / alpha.8 的交互时序（延迟播放、音量键、拖动转向手感） | 只有静态与产物级证据，没有实机证据 | **待验**：判据见 §8 / §9，日志行已设计成可核对 |
| R8 / 门禁 | 新增 Kotlin 类无新按名解析面；`NativeCommandBridge` 变更涉及 relay 写入 | **已处置**：本地 `assembleRelease` 跑 `verifyReleaseEntryPoints` 五类断言通过，无新增 keep 规则 |

---

## 5. 交付节奏与文档同步

- **版本**：P1 → `3.3.22-alpha.5`（已交付）；P2 → `alpha.6`（已交付）；播放倒计时 → `alpha.7`（已交付）；P3 → `alpha.8`（已交付）；P4 → `alpha.9`（已交付）。
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

**实机验收（2026-09-30，用户执行，通过）**：远程文件空间的实际投递、`%files%` 展开后原生 `open()` 的成功、面板 VMD 按钮的显隐与按下，整条链走通了——此前列出的三项未验证项全部落地，包括当时最可疑的 `open()` 那一环。仍未单独评估的是标定的画面观感：`0.07` / `+5°` 取自 MMD 场景，用户未对动作幅度或朝向提出异议，但也没有明确确认它符合预期。

**下一步（P3）**：触摸转向——替身层 `AddVirtualMouseDelta` + relay 新行协议 + `module.cpp` 非 Win 分支 + 面板 `LookPad`；同批把 `mouse_sensitivity` / `mouse_invert_y` 的 UI 补上（灵敏度滑杆 0.02–0.5，`LookPad` 的增量换算要读它）。

---

## 8. 播放倒计时 + 隐藏期的音量键出口 + 播完自动回 handle（2026-09-30，已交付 3.3.22-alpha.7）

**需求**（用户提出，三次追加）：① 点击「预设运镜播放」「回放关键帧」「VMD 镜头播放」这三个按键时，先关闭悬浮窗，等待 1 秒再播放；② **handle 也要隐藏**，但按音量键可以中断播放并唤回 handle；③ **运镜播完 handle 自动回来**。

**这个需求的来由**：这三个键的意义全在相机接下来怎么动，而面板占掉约三分之一画面、点击又发生在手指尚未离开屏幕之时——按键在点击瞬间发出，运镜就会带着控件一起开拍。handle 是浮在画面上的 50 dp 方块，同理要一起收掉。其余控件（移动 / 升降 / 滚转 / 变焦 / 记录关键帧 / 清除 / 视角回正）仍是按下即生效：那些是需要看着画面实时微调的调整项，延迟只会让它们难用。**只有这三个键被延迟是有意为之，不是漏改。**

**实现**（零原生改动）：

| 文件 | 改动 |
|---|---|
| `OverlaySurface.kt` | `Callbacks` 增 `delayedPulse(key, description)` |
| `OverlayControls.kt` | `MotionControls` 中三个播放键由 `pulse` 改为 `delayedPulse` |
| `GameOverlay.java` | `deferPlayback` / `runDeferredPlayback` / `cancelDeferredPlayback`、`standDown` / `endStandDown`、`onTakeoffCheck`、`onVolumeKey`、`observeJournalLine` / `applyPlaybackState`；常量 `DEFERRED_PLAYBACK_DELAY_MS = 1000`、`STOP_EDGE_GAP_MS = 120`、`TAKEOFF_GRACE_MS = 3000`；状态 `deferredKey/deferredDescription`、`firedDescription`、`runningKey/runningDescription`、`stoodDown` |
| `RuntimeLog.java` | 增 `Observer` 接口 + `observe/stopObserving`，`record()` 在锁外回调（悬浮窗据此读原生播放事件） |
| `XposedEntry.java` | 钩 `Activity.dispatchKeyEvent`（基类）+ 宿主 Activity 自己声明的那次覆写，把音量键转发给悬浮窗，**不吞按键** |

**关键设计决策（记录理由）**：

1. **等待挂在控制器的主线程 Handler 上，不挂在面板的 Compose 作用域上。** 面板收起正是「这份组合是否还活着」不再值得依赖的时刻：把等待交给随面板一起被取消的作用域，按键会被无声吞掉，日志里也不会留下任何痕迹。控制器（`GameOverlay`）的生命周期与 Activity 一致，且它本来就有 `mainHandler`（重挂载看门狗在用）。
2. **取消语义**：一秒内再次点击三个播放键中的任意一个会**替换**待发的键（`removeCallbacks` + 重设），而不是排队两次；重新展开面板（`togglePanel` 展开分支）、按音量键、游戏切到后台（`onActivityPaused`）、悬浮窗被拆掉（`remove()`）都会取消。
3. **预览模式直接发键**：预览跑在模块自己的进程里，`collapse()` 在预览下等于 `remove()`（拆掉整个悬浮窗），延迟没有意义；预览也不注册音量键与日志观察者。
4. **延迟从点击那一刻算起**：面板淡出约 100 ms，因此手指离开屏幕后还剩约 0.9 秒。
5. **音量为隐藏期唯一出口**：panel + handle 都不可见时，屏幕上没有可点的东西了。三个播放热键在原生侧本来就是**开关**（`PumpFreeCameraRequests` 里 `g_playback == kind` 即 `StopPlayback("hotkey")`），所以「停止」用的就是当初发出去的那个键；而预设运镜（`motion_duration=0` 表示不限时）、关键帧循环与 VMD 循环都可能永远不结束，没有这个出口只能重启游戏。
6. **「正在播放」照原生日志判定，不猜**：原生记 `Free camera motion started` / `Free camera keyframe playback started` / `VMD camera playback started`，停止（含播完、被切换、被自己的热键停）统一记 `Free camera playback stopped: <原因>`，退出自由相机记 `Free camera disabled`；这些行进的是既有的 native ring → `native.log` → Java 日志环（`RuntimeLog.observe`）。**必须先看到 started 才认为运镜在跑**：按键发出但原生没能开拍（VMD 打不开、不在自由相机里）时音量键不会去「停」一次从未开始的播放，已经播完的运镜也不会被同一个按键重新启动——失败方向是「音量键只唤回 handle」，不是「重播一次用户以为已经结束的运镜」。
7. **停止前必须先释放再等 120 ms**：虚拟键锁存把一次 pulse 保持 180 ms，而模块的输入线程只在**上升沿**上武装热键（`down && !was_down`），紧接着补一次按键会被看成同一次长按，什么都不会切。所以先 `releaseKeys()` 再把停止键延后 `STOP_EDGE_GAP_MS` 发出。
8. **不吞音量键**：手机音量照常变化，转发纯加法，永远不会从游戏手里抢走一个键。代价是「同一次按键被两个钩子各看一次」（宿主 Activity 覆写 `dispatchKeyEvent` 又调了 super）——因此消费端写成幂等：第一遍处理完就清掉状态，第二遍无事可做。
9. **运镜结束后 handle 自动回来**（用户第三次追加的需求，取代原先「不自动回来」的设计）：触发就是原生的 stopped 行（`Free camera playback stopped: <原因>` 或 `Free camera disabled`），它覆盖播完、被切换、被自己的热键停掉、退出自由相机全部路径。**只认已登记为「在跑」的那次运镜**（`runningKey != 0`，而 `runningKey` 只在读到 started 行时才登记），所以别人的 stopped 行不会提前把 handle 拽回来。handle 回来、面板保持收起——展开面板仍是用户自己的一次点击（决策 10 不变）。
9b. **兜底：按键发出后 3 秒内原生没有 started 行 → 当作「它没东西可播」，唤回 handle。** 这不是锦上添花：`g_free_camera_active` 为假时按预设/关键帧键，`PumpFreeCameraRequests` 直接 `return`，一行都不记（`free_camera_runtime.inc` 的 `if (!g_free_camera_active) return;`）；关键帧不足两帧、VMD 装载失败、dolly zoom 距离太近也都只记一行提示就返回。这些情况下若只等 stopped，悬浮窗永远停在隐藏态，而用户没有任何理由去猜「音量键在这种状态下还有用」。3 秒是 native ring → `native.log` → 轮询（`Thread.sleep(250)`）这条通路延迟的十二倍，够宽；代价是万一 VMD 装载真的超过 3 秒，handle 会先回来一次（可见、可再收起，不损坏任何状态）。
9c. **`runningKey` 在 `standDown()` 时清零**：它描述的是「本次隐藏对应的那次运镜」，而不是上一个会话残留的状态。native.log 跨进程重启不会截断，早期行可能被重放，清零让重放的 started 行无法污染新一次隐藏。
10. **音量键只唤回 handle，不展开面板**：用户按下它是为了中止或退出隐藏态，面板要不要摊开由他自己点一下决定。
11. **钩 `dispatchKeyEvent` 要顺着继承链找声明类**：宿主 Activity 往往自己不声明、而由基类覆写（Unity 的基类就是覆写且未必调 super 的那类），只问叶子类会误判成「没有覆写」，只钩框架方法又会漏掉不调 super 的覆写。

**验证**（本机）：release 构建 + `verifyReleaseEntryPoints` 通过；`assembleDebugAndroidTest --rerun-tasks` 通过；原生段未动（纯 Java/Kotlin 改动，`.so` 不变）；dex 里 14 条新字面量全部检出（三条 started 判据、两条 stopped 判据、`no take reported within`、`the runtime had nothing to play`、`no take started`、`take finished: `、`the take ended`、`overlay handle restored`、`the game came back to the foreground`）。**延迟、超时与音量键都是时序行为，没有产物级证据**，能否接受只能实机看；实机可核对的日志行：`playback armed` → `playback fired` → `take running` → 二选一收尾 `take finished: <描述>` + `overlay handle restored: the take ended`，或 `no take reported within 3000 ms` + `overlay handle restored: no take started`，或音量键路径的 `take stopped by volume key`。

**已知取舍**：录制中按下音量键会让手机音量一起变化（有意为之，见决策 8）；**不限时运镜**（`motion_duration=0`）与**循环的关键帧 / VMD** 永远等不到 stopped 行，因此不会自动回 handle——这是「播完才回来」的字面含义，不是缺陷，出口仍是音量键或切后台再回来；万一 VMD 装载超过 3 秒，handle 会先于播放回来一次（决策 9b）；handle 回来时面板仍是收起的，想操作要再点一下 handle。

---

## 9. P3 实施结果（2026-09-30，已交付 3.3.22-alpha.8）

**需求**：继续推进方案 → 补缺口 C：手机上自由相机不能转向，运镜构图事实上没法用。

**改动**（本阶段第一次动原生；**共享的桌面源 `free_camera_runtime.inc` 一行未改**）：

| 文件 | 改动 |
|---|---|
| `android_virtual_keys.h` | 声明 `AddVirtualMouseDelta` / `DrainVirtualMouseDelta`，并把「为什么不是虚拟键、为什么不是命令泵」写进注释 |
| `android_win32.cpp` | 两个 `std::atomic<int>` 累计器 + `exchange` 取走 |
| `input_relay.cpp` | `HandleLine` 增 `"m <dx> <dy>"`（±1000 夹取），文件头协议表同步 |
| `modules/camera/module.cpp` | `FoldPanelLookInput()`（`#if !defined(_WIN32)`）在 `PumpFromEngineTick` 顶部把累计器折进 `g_mouse_dx/g_mouse_dy` |
| `NativeCommandBridge.java` | `look(dx, dy)` → `"m dx dy\n"` |
| `GameOverlay.java` | `accumulateLook` / `flushLook` / `dropPendingLook`，常量 `LOOK_FLUSH_INTERVAL_MS = 16` |
| `OverlaySurface.kt` / `OverlayControls.kt` / `OverlayPanel.kt` | `Callbacks.look`；`LookPad`（`detectDragGestures`，62 dp 拖动区）置于自由相机区块最上方 |
| `SettingsState.kt` / `CameraMotionPage.kt` / `strings.xml` | 转向设置组：灵敏度滑杆 0.02–0.5 °/像素（48 档、两位小数）+ Y 轴反转开关；复用 P1 已写好却无入口的 `updateMouseSensitivity` / `updateMouseInvertY` 与 `CameraMotion` 记录的夹取 |

**关键设计决策（记录理由）**：

1. **增量走「累计器 + 每 tick 折叠」，不走命令泵、也不新造虚拟键。** 命令泵是单槽代次队列，连续输入会自己覆盖自己（一次拖拽只剩最后一个采样）；虚拟键是状态量，而视角是增量。替身层两个原子累加器与 `SetVirtualKey` 同级同风格，`DrainVirtualMouseDelta` 取走即清零，语义与 Windows 钩子里的 `fetch_add` 完全一致。
2. **坐标与单位沿用钩子的那一套**（屏幕像素、x 向右、y 向下），而不是另立一套触屏坐标再换算。好处是 `StepFreeCamera` 里 `control.yaw += dx * sensitivity` 那两行一行都不用改，`mouse_invert_y` 在两端同义，且「右滑右转」（桌面习惯）+「上滑抬头」（触屏习惯）同时成立。
3. **在 `PumpFromEngineTick` 顶部无条件折叠**，不只在 `g_free_camera_active` 时：否则相机没开时拖的几下会攒着，开相机瞬间一次性甩镜头。`EnterFreeCamera` 本来就调 `ClearMouseInput()`，再兜一道——因此**不需要**去改 `ClearMouseInput()` 所在的共享 `.inc`，桌面源保持零改动（这条比原方案更保守：原方案打算在 `ClearMouseInput()` 里顺带清替身累计器）。
4. **面板侧合并写**（`LOOK_FLUSH_INTERVAL_MS = 16`）：拖拽按显示刷新率上报（约 120 次/秒），原生每 10 ms 读一次；一行一事件意味着每秒上百次 `open/write/close`。求和后最多每 16 ms 写一行，总量不变、延迟低于一帧。**取整后只减掉整数部分，小数留下继续累**（`pending -= dx`，不是 `= 0`）：写成清零的话，比「每 16 毫秒半像素」更慢的拖拽会被反复四舍五入抹平，按多久都不动——实机上表现为「慢慢拖没反应」，而构建与门禁全绿。（本条是自查时改掉的：初版代码写的是清零，注释却写着「会累起来」，两者相反。）
5. **在飞的增量在面板收起 / 切后台 / 悬浮窗拆除时丢弃**：控件已经不在了还在转镜头，用户无从解释。
6. **灵敏度 UI 的范围比原生夹取窄**（0.02–0.5 对 0.01–2.0）：`CameraMotion` 记录本来就夹在这一段（P1 定的），滑杆与之对齐；0.5 时滑过整个拖动区已超过一整圈，再往上没有可用手感。

**验收判据**：拖动转向区 → 相机连续转向；时间冻结中仍可转向（心跳 fallback 生效的证据：`Cinemachine is not pushing; writing the camera transform` 且画面确实转了）；播放运镜（预设/关键帧/VMD）期间拖动无效；调过灵敏度或 Y 轴反转后**立即生效**（P4 起配置热重载，见 §10——本档交付时还是重启生效）。

**验证**（本机）：release 构建 + `verifyReleaseEntryPoints` 五类断言通过；`assembleDebugAndroidTest` 通过；**本版有原生改动，因此核对到产物**：`libbetterendfield_android.so` 本次重编，APK 内该库含 `AddVirtualMouseDelta` / `DrainVirtualMouseDelta` 符号（`FoldPanelLookInput` 是内部函数，被 strip 属预期，未 strip 的中间产物里在），dex 含 `look deltas rejected (relay not configured)` 与面板文案，资源表含新设置文案。**转向手感是交互行为，没有产物级证据**，只能实机看。

另外把**跨进程行协议的文本契约**逐字核对了一遍（这是唯一没有编译期约束的接缝）：Java 写 `"m " + dx + " " + dy + "\n"`；原生按 `'\n'` 切行并去掉换行后，要求 `line[0]=='m' && line.size()>=3 && line[1]==' '`，两次 `strtol` 之间要求**恰好一个空格**（`*end != ' '` 即丢弃），负号由 `strtol` 处理 ⇒ `"m 3 -4"` 这类行两边一致，格式串打错会表现为「拖了没反应」而不是崩。畸形行（`"m x y"`、`"m 3"`、超范围）一律静默丢弃，与既有的 `"<vk> <action>"` 分支同风格。

**已知取舍**：转向只在自由相机实际开着时生效（增量被消费的地方就是 `StepFreeCamera`），面板上拖动区与移动区同门控，因此「设置里开了自由相机但没按自由视角」时拖动无反应——与移动键一致；灵敏度是「度/像素」，同一段拖动在不同分辨率的机器上转过的角度不同（设置页文案已注明）；播放运镜期间不接管转向，这是可复现性的前提，不是漏改。


---

## 10. P4 实施结果（2026-09-30，已交付 3.3.22-alpha.9）

**设计**：见 §2 的 P4 节（立项理由、链路、四条边角、边界、判据）。本节只记实施后的事实与证据。

**本档第一次改动命令泵本身**：`ConsumeRuntimeCommand` → `AcquireRuntimeCommand`（按命令名取用，别人的命令留在槽里），并新增替身层转发（`android_panel_commands.h` + `core/panel_commands.cpp`，由 `android_win32.h` 带入），让桌面模块经既有的 `<Windows.h>` 链就能拿到它——与 P3 的虚拟键累计器同一手法。`free_camera_runtime.inc` 一行未改；`native/modules/camera/module.cpp` 的改动全在 `#if !defined(_WIN32)` 分支内（Windows 行为不变），`custom_model` 的两个既有消费点改用按名取用。

**设备外证据（纯 JVM，跑的是真生产代码）**

`F:/tmp/p4verify` 把 `ModuleSettings` / `ModuleCommandRouter` / `FrameworkSettings` 三个真文件编到 JVM 上跑（只桩掉 `android.content.Context`、`SharedPreferences`、`android.util.Log`、`ParcelFileDescriptor` 与 libxposed 服务句柄），三组输入各写一次：

| 组 | 输入 | 结果 |
|---|---|---|
| A 全开 + 默认 | — | 53 行、1347 字节 |
| B 全部越界 | 速度 −20、两个 FOV 180、时长 600… | 夹取后 `movement_speed=0.5`、`field_of_view=120`、`first_person_fov=120`；53 行、1347 字节 |
| C NaN + 未知预设 | 6 个 scalar 传 `NaN`、`preset="nonsense"` | 回退默认（`5` / `60` / `75`）、预设回落 `orbit`、`animation_mode` 回落 `0`；53 行、1303 字节 |

- **最长一档 1347 字节**，投递出的整段 payload **1334 字节**（`BE_COMMAND_V1\n<gen>\ncamera_config\n<配置>\n`），对原生 4096 的上限余量充足。`issue` 现在按整段 payload 的 UTF-8 字节数判据，512 字符的天花板已撤（它本来就会拒绝这份配置）。
- **命令内容逐字核对**：`header=BE_COMMAND_V1\n3`、`command=camera_config`、`body=1304 字节`（配置 1303 + 结尾换行）。
- **键名交叉核对**：从真实输出文本里提取 53 个键，与原生 `ParseConfiguration` 认得的 54 个键取差集 ⇒「写了但原生不认」为**空**。反向差集两项都是有意为之：`first_person_enabled`（旧键名，Java 写的是 `first_person_camera_enabled`）与 `first_person_neck_plug_scale`（未暴露的设置项，用原生默认 1.0）。
- **重复写不投递**：同参数再写一次 ⇒ `changed=false, delivered=false`，不产生多余命令。

**产物证据**：release 构建 + `verifyReleaseEntryPoints` 五类断言通过；`assembleDebugAndroidTest --rerun-tasks` 通过；本档有原生改动，逐个核对到产物——APK 内 `libbetterendfield_android.so` 含 `AcquirePanelCommand` / `AcknowledgePanelCommand` / `AcquireRuntimeCommand` 符号（`DrainConfigurationReload` 是内部函数，strip 后不在，未 strip 的中间产物里有），dex 含 `camera_config` 与新增提示文案，`resources.arsc` 含新字符串。

**未验项（实机）**

- 热重载本身完全没验过：判据见 §2 的表，全是日志行，可逐条核对。
- 唯一有实质不确定性的地方是**tick 是否覆盖「相机功能全关」的会话**：`DrainConfigurationReload` 挂在 `PumpFromEngineTick`（render loop / unscaled delta 两个钩子），这两个钩子只要 Initialize 成功就装着，与功能开关无关——按代码路径应当成立，但没有设备级证据。
- VMD 槽物化的耗时：运行中导入一个较大的 .vmd 时，物化发生在命令 poller 线程里（同步），期间该线程不轮询；对几十 KB 的典型相机轨道可以忽略，对接近 64 MiB 上限的文件会让配置晚几秒生效。没有实测。

---

## 11. 实机验收步骤（3.3.22-alpha.9，照着做）

产物：`betterendfield-3.3.22-alpha.9-30322.apk`（8,702,192 B，SHA-256 `854DE97A…28E98EF3`，`CN=BEM Release` / 证书 `8CD6FDC1…8EFD`）。α.9 的验收重点是 **P4 热重载**，同时把 alpha.7、alpha.8 两档一并回归（它们此前都未过实机）。全程**不需要重启游戏**，唯一例外是第 1 步的「首次启用」。

### 步骤 0 · 前置（一次性）

1. 另一台手机需 **Android 9+**（`minSdk 29`）且已 root、装好 **LSPosed**。
2. 安装本 APK。**全新手机直接装即可**；只有装过 `3.3.20` / `3.3.21` / `3.3.22-alpha.1` 这些旧临时身份的机器才需要先卸载（签名不相容）。alpha.2 及以后的原位覆盖，从 alpha.8 升上来同样不用卸载。
3. 在 LSPosed 里**启用本模块**，作用域勾选**游戏**（终末地）；若要用 BEM 包管理器，把它也勾上。设置 app 本身**不需要**在作用域里。
4. 强制停止游戏（或重启手机）让 LSPosed 生效。

### 步骤 1 · 首次启用（唯一需要重启的一次）

5. 打开设置 app →「体验 → 自由视角 / 相机」，把相机功能打开。此时提示应写明**首次启用需重启**。
6. **完全退出游戏**（从最近任务里划掉，不是切后台），再重新进入并进到可活动场景。

> 判据：这一步之后，本次游戏进程已经载入了相机模块；后续任何参数改动都不再需要重启。

### 步骤 2 · P4 主验收：运行中改参数，当场生效

7. 游戏内呼出悬浮窗 → 进入**自由视角**。
8. **切后台**到设置 app（不要退出游戏）→「体验 → 运镜与镜头」。
9. 挑一个**肉眼立刻能分辨**的参数改：`运镜速度`（拉到最大或最小）或 `视野 FOV`。保存。
10. 切回游戏 → 在自由视角里移动/拖动一次，确认手感或画幅**已经变了**。

> 面板日志判据（这是 P4 是否成立的唯一硬证据）：
> - `Camera configuration reloaded from the settings app: N bytes, applied without restarting the game.`
> - 紧随其后的原生行 `Camera configuration applied: enabled=true, …` 与 `Free camera extras: …`
>
> 三条提示语分别对应三种状态，别混淆：**游戏在跑** → 已投递生效；**游戏没在跑** → 「没有送达游戏进程」（配置留在远程空间，下次启动重放）；**首次启用** → 需重启。

### 步骤 3 · 三个边界（最容易误判的地方）

11. **边界 A：首次启用之后关掉再打开相机，不应再要求重启。** 在同一会话里把相机功能关掉再打开，观察是否即时生效（重放走的是同一个退出/进入分支）。
12. **边界 B：VMD 运行中导入。** 游戏在跑时，在设置页导入一个 `.vmd` → 切回游戏 → 应即时生效，日志应出现 `VMD camera motion ready: <path> (N bytes)` 与重载那两行。可再拿一个较大的 `.vmd`（几百 KB~MB）观察延迟——物化在游戏进程的轮询线程里同步做，大文件会让配置晚几秒生效。
13. **边界 C：未送达不清空。** 游戏**没在跑**时改一个参数 → 设置页提示未送达 → 启动游戏 → 该配置应被重放（幂等，不会重复生效）。

### 步骤 4 · 回归（alpha.7 + alpha.8，一并验）

14. **alpha.7 播放时序**：按面板上的三个播放键之一（预设运镜 / 回放关键帧 / VMD 播放）→ 面板应**先收起**、handle 一起消失，约 **1 秒后**才开始播放；播放中按**音量键**应能停掉并唤回 handle（手机音量本身照常变化，音量键不被吞）；一次**会自然播完**的运镜结束后 handle 应**自动回来**、面板保持收起。
    - 注意：**不限时 / 循环**的运镜永远不会发结束信号，这类只能靠音量键或切后台再回来。
15. **alpha.8 触摸转向**：面板「镜头转向（拖动）」区按住拖动 → 右滑右转、上滑抬头；时间冻结状态下仍可转向；**播放运镜期间拖动应无效**。灵敏度与 Y 轴反转在设置页可调，且（P4 之后）改完即时生效。

### 步骤 5 · 取证

16. 任一步不符预期，把**面板日志整段**贴回来即可（它同时含 Java ring 与 `[native]` 尾行，是判定链路走到哪一环的主要依据）。
17. 需要更底层时（需 root）：游戏进程日志文件在
    `/data/data/<游戏包名>/files/betterendfield/native.log`
    → `adb shell su -c 'cat /data/data/<游戏包名>/files/betterendfield/native.log'`。

### 一句话预期

「改完就生效」应成立，且全程只做一次「重启」——就是步骤 1 那次首次启用。若在步骤 2 里发现必须重启才生效，则本档的核心功能不成立，按第 16 步取证。
