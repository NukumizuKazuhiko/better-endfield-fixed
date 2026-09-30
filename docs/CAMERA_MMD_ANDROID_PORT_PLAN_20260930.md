# MMD 身体动作 Android 移植方案（2026-09-30）

> 目标：把参考源 v3.4.0（`2c0cc12`）的 MMD 播放 / EIEM DirectVmd 能力**移植到我们的 Android 端**。
> 已定范围（2026-09-30 用户决策）：**单人身体动作最小闭环**，**含 Android 音频通路**；4 人同台、衣物物理全模式、MMD 作品库、BEM 1.1/1.2 本档不做。
> 前置清点见 `UPSTREAM_SYNC_BACKLOG_20260930.md`。

## 0. 决策摘要

| 决策点 | 结论 |
| --- | --- |
| 编排层 | **不移植**上游的「独立悬浮窗 exe + 共享内存协议」，改用我们已有的「Compose 游戏内面板 + 单槽命名命令泵 + LSPosed 远程文件空间」 |
| 钩子后端 | **零改动可用**。上游已把 MinHook 调用点改成 Host 的 `create_hook`（`BE-PATCH(hook-broker)`），而我们 Android 侧 `create_hook` 早已在用（camera/actions/model/custom_model/combat_stats 五个模块） |
| IL2CPP 访问 | **保留 EIEM 的直连 C API 写法**。我们替身层已把 `GetModuleHandleW` 映射到 `libil2cpp.so` 映像、`GetProcAddress` 映射到符号解析 ⇒ `Resolve()` 无需改写（但须由 Phase 0 验证符号导出） |
| 编译单元 | **先只编 1 个 slot**（`eiem_slot0`），不编 4 份；确认可用后再扩 |
| 音频 | 新增 `MmdAudioPlayer`（Java，跑在游戏进程）经 JNI 由原生驱动；**导演时钟仍是主时钟**，音频为从属 |
| 文件投递 | **复用 P2 的 VMD 投递链**（SAF 导入 → 远程文件空间 → `%files%` 物化） |

## 1. 目标与验收判据

**目标定义**：在真机上，导入一个含身体动作的 VMD（可选 face VMD、可选音频），通过游戏内面板开始播放后，**游戏角色按 VMD 演出身体动作，表情随之变化，音频与动作对齐**；面板可暂停 / 停止 / 跳转；播完自动回到 handle。

**验收判据（缺一不可）**：

1. `:app:assembleRelease` 通过，产物 dex + `.so` 均含新键（`vmd_body_enabled`、`mmd_play` 等，用 dex 字符串池与未 strip 的 `.so` 分别核）。
2. PJX110 真机：面板点「播放」→ 日志出现 `Free camera …` 同级的原生判定行（沿用 P2 的「照原生日志判定」原则，不靠 UI 自报）。
3. 角色身体实际运动（**用户肉眼确认，日志不能替代**）。
4. 音频与动作对齐误差在可接受范围内，且时间偏移可调。
5. 停止 / 退出后角色恢复原生姿态，**无残留骨骼写入**（离开后动画正常）。
6. 崩溃率：连续 3 次播放 / 停止循环无闪退。

> 本机无显卡 ⇒ Win 侧不可验收；所有验收都在 Android 真机（PJX110）完成。

## 2. 能力对照表：上游实现 ↔ Android 对等物

| 上游（Windows） | Android 对等物 | 状态 |
| --- | --- | --- |
| Host `create_hook`（MinHook 后端） | Host `create_hook`（Dobby 内联钩子） | **已有**，5 个模块在用 |
| `GetModuleHandleW("GameAssembly.dll")` + `GetProcAddress` 取 `il2cpp_*` | 替身层映射到 `libil2cpp.so` 映像 + `Symbol()` | **已有**（`android_win32.h:202-207`） |
| 独立 MMD 悬浮窗 exe + `mmd_overlay_protocol.h` 共享内存 | Compose 游戏内面板 + 单槽命名命令泵（P4） + `NativeCommandBridge` | **已有**，协议层直接弃用 |
| 设置 app → 游戏进程文件投递 | LSPosed 远程文件空间 + `%files%` 展开 | **已有**（P2 链） |
| 设置热调参（整段 ini 幂等重放） | `camera_config` 命令 + `ConfigurationChanged` | **已有**（P4 链） |
| 连续输入（鼠标） | 原子累加器 + 每 tick `exchange`（P3） | **已有** |
| Media Foundation（`mfplat`/`mfreadwrite`）本地音轨 | MediaPlayer/MediaCodec + JNI（`MmdAudioPlayer`） | **需新建**，全仓无先例 |
| WinUI 设置页（`FreeCameraExtras.cs`） | Compose 设置页 + `ModuleSettings` 归一化 | **已有**，加键即可；功能面清单见 §2.1 |
| `native/tests/camera_playback/**`（宿主回归测试） | 可用同套源码在宿主跑（平台无关） | **可选**，见 Phase 2 |

结论：**需要新建的只有「音频通路」一项**，其余全部落在已有基础设施上。这是本方案可行的核心原因。

### 2.1 上游「MMD 播放与作品库」设置区块（Android 侧重建对照）

上游改动的 WinUI 设置页 = `MainWindow.xaml`（+169/−67）+ `MainWindow.xaml.cs`（+344/−81）+ 新增 `Services/MmdLibraryService.cs`（294 行）。它是一层**薄前端 + 磁盘管家**：本身不执行任何动作，只做三件事 —— 写 ini 配置、管理作品库文件、提供热键清单。真正的播放/骨骼写入全在原生（`camera` 模块 + EIEM）。

**配置键（实测自 `FreeCameraExtras.cs` 抽取）**：

| 分组 | 键 | 备注 |
| --- | --- | --- |
| 总开关 | `mmd_enabled` | 启用 MMD 播放 |
| 身体动作 | `vmd_body_enabled`、`vmd_motion_file`、`vmd_motion_scale`（位移幅度 %）、`vmd_motion_weight`、`vmd_motion_loop` | Phase 3 目标 |
| 表情 | `vmd_face_enabled`、`vmd_eyes_enabled` | Phase 4 目标 |
| 衣物物理 | `vmd_cloth_mode` = `stable`（稳定，推荐）/ `game`（游戏原样）/ `freeze`（冻结） | 本档不做（§7） |
| 地形贴合 | `vmd_terrain_enabled` | 上游标注「实验」 |
| 时间轴 | `mmd_seek_seconds`（快进/快退秒数）、`mmd_loop` | |
| 音乐 | `mmd_music_enabled`、`mmd_music_gain`（音量 %）、`audio_offset` | Phase 4 目标 |
| 悬浮窗 | `mmd_overlay_enabled`、`mmd_overlay_visible`（进入游戏时显示） | 我方由 Compose 面板替代 |
| 当前作品 | `mmd_work` | 本档不做（§7） |
| 运镜 / 关键帧 | `keyframe_file`、`keyframe_{add,clear,load,save,play}_hotkey`、`vmd_camera_file`、`view_reset_hotkey`、`motion_hotkey`、`roll_left/right_hotkey`、`fov_wide/narrow_hotkey` | 与 P1/P3/P4 已有面部分重合 |
| 热键布局 | `hotkey_layout`（上游 =2，第二套布局把全部 MMD 动作放小键盘） | |

**作品库（`MmdLibraryService`）**：一件作品 = `<install>/mmd/<文件夹>/set.ini` + 其文件，记录 `Name / Motion / Camera / Face / Music / AudioOffset / ExtraMotions(motion2–4) / ExtraFaces(face2–4)`（后两组即 4 人同台）。提供 列表 / 导入 / 删除 / 改偏移 / 设默认。**`camera` 模块与悬浮窗读同一目录**。校验：VMD 文件头必须为 `Vocaloid Motion Data`（≤64 MB）；音乐白名单 WAV/MP3/M4A/AAC/FLAC/WMA（≤1 GB，Android 侧收窄为 5 种，见 §3.5）。

**小键盘热键（不需 NumLock）**：`Enter` 播放/暂停、`+` 停止、`4`/`6` 后退/前进、`5` 切镜头（VMD / 自由 / 游戏）、`-` 显隐悬浮窗。

**Android 侧对照**：整块界面用 Compose 设置页重做（「体验」页加 MMD 区块，见 Phase 5）；上表键名可分批复用 —— Phase 3 取「身体动作」组，Phase 4 取「表情 + 音乐」组；`mmd_work` / `set.ini` / 作品库按 §7 不在本档，改为直接选单个 VMD 文件。

## 3. 六个硬缺口（实测）

### 3.1 SEH（`__try`/`__except`）——最大工作量

Android 无 SEH。**在实际编译链上共 254 处**：

| 文件 | 处数 |
| --- | --- |
| `upstream/ghost_rig.h` | 112 |
| `upstream/smc_face.h` | 58 |
| `upstream/animation.h` | 38 |
| `upstream/cloth.h` | 18 |
| `eiem_slot.inc` | 14 |
| `upstream/il2cpp_api.h` | 6 |
| `upstream/globals.h` | 4 |
| `eiem_body.cpp` / `compat/cloth_be.h` | 2 / 2 |

（`init.h` 38 处、`trojan.h` 72 处不计——两者是参考件，不参与编译。）

**对策（两段式，不要一步到位）**：

- 第一段：`#define __try if (true)` / `#define __except(x) else if (false)`，让代码先跑起来。**必须诚实记录代价**：SEH 在 EIEM 里是防止读到失效托管指针的**安全网**；去掉后，一个坏读就是游戏进程 SIGSEGV 直接闪退，而不是优雅降级。
- 第二段：把最危险的 6 处换成信号守卫（`sigsetjmp`/`siglongjmp` 包一层 `BeGuardedCall(fn)`）——即 `il2cpp_api.h` 的 `ReadStr`/`ReadStrUtf8`/`Invoke` 与 `globals.h` 的日志写入。这 6 处是真正会读托管内存的。

> 判据：这两段先后顺序不能颠倒。先做宏是为了尽快拿到「能不能跑通」的信号，而不是把它当成最终形态。

### 3.2 Windows 专有头与 API（数量少，但有一处隐蔽阻断）

| 缺口 | 位置 | 对策 |
| --- | --- | --- |
| **`#include <windows.h>` 小写** | `eiem_slot.inc:28` 预包含列表 | **CI 在 `ubuntu-latest`（大小写敏感）⇒ 找不到我们的 `include/Windows.h`**。必须补一个小写的 `windows.h`（内容只 `#include "android_win32.h"`），否则 CI 必炸而本地不炸——**这是最容易漏的一条** |
| `<bcrypt.h>` / `<commdlg.h>` | 只在 `init.h`（不编译）用到 `BCrypt*` / `GetOpenFileNameA` | 从预包含列表移除 |
| `<mmsystem.h>` | `eiem_slot.inc:556` 用 `timeBeginPeriod(1)` | 补替身：`timeBeginPeriod` 返回 `TIMERR_NOERROR` 的 no-op（Android 无需提升计时精度） |
| `<intrin.h>` | 实测编译链中无 `__rdtsc`/`__cpuid` 使用 | 移除；若后续编译报错再按需补 |
| `"MinHook.h"` | `eiem_slot.inc:52`、`upstream/il2cpp_api.h` | 直接从预包含列表移除（`Hook()` 已改走 Host `create_hook`） |
| `CreateThread` | `eiem_slot.inc:568`（`StartWorker`） | 改 `std::thread` |
| `_wfopen_s` | `vmd_parser.h`、`direct_vmd_source_pmx.h` | 改 `fopen`。Android 文件路径本身就是 UTF-8 字节流，**上游为 CJK 路径打的 UTF-8 补丁在 Android 上天然成立，反而更简单** |
| `WideCharToMultiByte` | 3 个文件 | 补替身层实现（IL2CPP 字符串在 Android 同样是 UTF-16，可如实实现） |
| `GetLastError` | 视编译报错 | 如需要补 no-op |

替身层**已提供**（无需处理）：`GetModuleHandleW`、`GetProcAddress`、`HMODULE`、`GetTickCount`、`GetCurrentThreadId`、`Sleep`、`CloseHandle`、`MultiByteToWideChar`。

### 3.3 MethodInfo 布局假设

`upstream/il2cpp_api.h` 的 `struct MInfo { void *mp; };` 把 `MethodInfo*` 当「首个字段就是函数指针」来读——这是**写死的结构体布局假设**，与上游「不含写死偏移」的自我描述相矛盾，且在 Android 上无保证。

**对策**：改用 Host `resolve_method` 返回的 `BE_ResolvedMethodV1.method_pointer`（我们的 camera/actions 模块都是这么做的），彻底去掉这个假设。这一步同时让「不用 `__try` 包住解析」成为可能。

### 3.4 游戏接口可达性（真正的门禁，未知）

EIEM 安装 **9 个钩子**（全部按名解析 managed 方法后挂 `method_pointer`）：

```
SkeletalMorphCore.Update
DoEvaluateMorphToBoneJob
DoEvaluateSpecialMorphToBoneJob
SolverManager.LateUpdate
BipedIK.UpdateSolver
IKSolverTrigonometric.OnUpdate
GrounderBipedIK.OnSolverUpdate
GrounderBipedIK.OnPostSolverUpdate
BeyondBoneCloth.SetClothSimulateWeight
```

**本项目已有先例：第一人称 S4/S5 曾被接口门禁阻断**（managed 方法表缺 `GraphicsBuffer.InternalGetData` / `Mesh.GetIndexBuffer` 等）。所以这 9 个 + 所需字段（`MovementComponent.m_bipedIK`、`currentFloor`、`FindFloorResult` 布局、`Entity.get_movementComponent`）**必须在动代码之前先探**。

额外风险：`DoEvaluateMorphToBoneJob` 是 **Unity Job 结构体方法**——Android 上 Burst/IL2CPP 可能不生成可由 metadata 到达的函数指针，或按泛型实例共享。这是 9 个钩子里最可能失败的一个。

⇒ **Phase 0 是必做前置，不是可选项。**

### 3.5 音频：全仓零先例

`MediaPlayer` / `AudioTrack` / `MediaCodec` 在 `android/` 下**出现 0 次**。需要新建：

- `MmdAudioPlayer.java`（模块 dex，跑在游戏进程）：内部起一个带 Looper 的 `HandlerThread`，对外 `load/play/pause/stop/seekTo/positionMs/durationMs/setGain/setOffset`。
- 原生侧缓存 `JavaVM*` 与类全局引用，工作线程经 `AttachCurrentThread` 后调用。

**两个已知陷阱**：

1. **原生线程里 `FindClass` 看不到模块的类**——必须缓存 Xposed 入口拿到的 `ClassLoader`，走 `Class.forName(name, true, cachedLoader)`，不能直接 `FindClass`。
2. 格式面：WAV/MP3/M4A/AAC/FLAC 可用，**WMA 在 Android 上无解**（`MediaExtractor` 不支持）⇒ 上游 6 种格式收窄为 5 种，文档要写清。

**时钟设计**：导演时钟保持主时钟，音频做从属（seek/play/pause 时下发对应命令并应用 `audio_offset`）。**不要把音频当主时钟**——`MediaPlayer.getCurrentPosition()` 粒度粗，做主时钟会让动作抖动。

### 3.6 许可与署名

EIEM 是 **AGPL-3.0**（上游 commit `1bc9baa`）。我们仓库同为 AGPL-3.0-only，兼容。必须随移植带入：`eiem/LICENSE.EIEM`、`eiem/UPSTREAM.md`、`THIRD_PARTY_NOTICES.md` 条目、以及源码内 "Portions copyright Sasye and EIEM contributors" 头。

## 4. 分阶段计划

### Phase 0 — 接口门禁探针（必做前置）

| 项 | 内容 |
| --- | --- |
| 动作 | 写一个最小原生探针（可挂在现有 camera 模块的 `Initialize` 里，或独立调试入口），用 `host->resolve_method` / `resolve_field` 逐个解析 §3.4 的 9 个方法 + 全部所需字段，结果写 `native.log`；同时用 `dlopen("libil2cpp.so", RTLD_NOLOAD)` + `dlsym` 核对 `il2cpp_*` 符号是否导出（这套 `RTLD_NOLOAD` 检查 `native_bridge.cpp:176` 已有先例可抄） |
| 产出 | 一份「可用 / 缺失」清单，逐项标注缺失项 |
| 验收 | PJX110 上跑一次，日志给出 9 个钩子目标 + 字段的逐项判定 |
| 回滚 | 探针只读不写，无副作用 |
| **决策点** | 若 `BipedIK.UpdateSolver` / `GrounderBipedIK.*` / Job 方法缺失 ⇒ **停止并按 §6 的替代路线重新评估**，不要硬上 |

### Phase 1 — 基础设施搬入（平台无关，零风险）

| 项 | 内容 |
| --- | --- |
| 动作 | 搬入 `native/shared/motion/{vmd.h, character_pose.h, character_mapping.h, pose_lease_registry.h}`、`camera_path.h`、`camera_file_worker.h`；这 6 个已实测在 Android 目标下干净编译 |
| 产出 | 可被 camera 模块引用的共享 VMD 解析与异步文件 worker |
| 验收 | Android release 构建通过，现有运镜功能无回归 |
| 说明 | `pose_lease`（姿态租约）本档**先不接**——它是为「多人/多写者争抢同一角色」服务的，单人闭环用不到；留到扩 4 人时再上 |
| 回滚 | 纯新增文件，`git revert` 即可 |

### Phase 2 — EIEM 本体 Android 化编译（目标：**编过**，不追功能）

| 项 | 内容 |
| --- | --- |
| 动作 | ① 搬入 `eiem/**`（27 文件），**只编 `eiem_slot0.cpp` + `eiem_body.cpp`**；② 按 §3.1–3.3 做平台修补（SEH 宏、Windows 头/API、`MInfo`→Host `method_pointer`）；③ 在 `android/app/src/main/cpp/CMakeLists.txt` 新增一个 OBJECT 库 `betterendfield_camera_eiem`，只把 `eiem_body.h` 暴露给 `camera/module.cpp`（照抄上游的隔离手法）；④ **绝不把上游 `native/CMakeLists.txt` 的 `CameraEiem`/`MmdOverlay` 条目带进来** |
| 产出 | `.so` 里含 EIEM 符号；`Initialize` 能进入并输出解析报告 |
| 验收 | ① `:app:assembleRelease` 通过（含 ubuntu CI）；② 设备上 `Initialize` 不崩，日志输出 9 个钩子的安装结果 |
| 注意 | `ghost_rig.h` 近万行，`-O2` 下编译时间与内存需实测；先只编 1 个 slot 正是为此 |
| 回滚 | 新 OBJECT 库可整体摘除，不影响既有模块 |

### Phase 3 — 单人身体动作闭环

| 项 | 内容 |
| --- | --- |
| 动作 | ① 配置面：新增 `vmd_body_*` 键到 `ModuleSettings`（归一化 + 夹取，遵循 P4 纪律：**任何写进 ini 的标量都要按原生 clamp 夹取**）；② 面板新增播放/暂停/停止/跳转按钮，走**按名取用的单槽命令泵**（新增 `mmd_*` 命令名，避免与 `camera_config` 互吞）；③ 装载路径复用 P2 投递链（VMD 经 SAF 导入 → 远程文件空间 → `%files%` 物化）；④ 目标角色取「当前受控角色」，实体/Animator/MovementComponent 由 camera 模块供给（照上游 `eiem_body.h` 的 `SetTarget` 契约） |
| 产出 | 可播放 / 暂停 / 停止 / 跳转的单人身体动作 |
| 验收 | §1 的判据 1–3、5、6 |
| 回滚 | 用一个总开关键（如 `vmd_body_enabled`）隔离，关闭即完全不挂钩子 |

### Phase 4 — 表情 + 音频通路

| 项 | 内容 |
| --- | --- |
| 动作 | ① face VMD（morph）驱动面部，含上游的 MMD 口型别名（「ワ」「口横広げ」「にやり」）；② 新建 `MmdAudioPlayer.java` + JNI 桥（缓存 ClassLoader，避开 `FindClass` 陷阱）；③ 时间偏移 `audio_offset` 与增益；④ 音频随导演时钟 seek/pause/play，绝不反向驱动 |
| 产出 | 表情 + 本地音轨与动作同轴 |
| 验收 | §1 的判据 4；格式面按 §3.5 收窄为 5 种并写入文档 |
| 回滚 | 音频为独立模块，可单独关闭（`mmd_music_enabled`） |

### Phase 5 — 收尾

| 项 | 内容 |
| --- | --- |
| 动作 | ① 「体验」设置页加 MMD 区块（复用 P1 的归一化 + 夹取写法）；② **四件套同步**：`CHANGELOG.md` / `README.md` / `README.en.md` / `android/README.md`，机制类补 `docs/GAME_INTERFACES.md`；③ 改完 **grep 旧版本号确认无残留**（本项目历史最易漏项）；④ 动过 `androidTest` 就补跑 `:app:assembleDebugAndroidTest`；⑤ 出 **release** 包（含体积 + SHA-256）交付并归档 `D:/CodexData/better-endfield-apk/` |
| 产出 | 可交付 APK + 文档 |
| 验收 | 全部判据 + 用户实机确认 |

## 5. 建议节奏

- Phase 0 与 Phase 1 **可以并行**（Phase 1 无风险且不依赖探针结果）。
- **Phase 0 的结论是 Go/No-Go 闸门**：接口缺失就停，不要把 3 万行带进一个跑不通的分支。
- Phase 2 结束（编过 + 钩子装上）是第二个检查点——此时应能看到「钩子安装成功但角色不动」，说明代码链路通、只差驱动逻辑。
- Phase 3 是唯一需要大量设备迭代的阶段（对齐、退出恢复、崩溃），预留最多时间。

## 6. 风险登记

| # | 风险 | 概率 | 影响 | 对策 |
| --- | --- | --- | --- | --- |
| R1 | 9 个钩子目标或所需字段在 Android metadata 缺失（含 Job 方法不可达） | 中高 | 致命 | **Phase 0 先探**；若仅 FinalIK 相关缺失，退化为「不挂钩子、自驱骨骼」方案：仅用 `Animator`/`Transform` 写入，放弃 IK/地面贴合与衣物服务，动作质量下降但可播 |
| R2 | 去掉 SEH 后坏读直接闪退 | 中 | 高 | 两段式（§3.1）；6 个高危点换信号守卫；先用安全带包住 `ReadStr`/`Invoke` |
| R3 | CI（ubuntu）因 `<windows.h>` 大小写失败，本地不复现 | 高 | 中 | 补小写 `windows.h`；**改完在容器/CI 之外先本地跑一次全量构建不够，必须让 CI 跑一遍** |
| R4 | 4 份 slot 导致编译时间/内存爆炸 | 中 | 中 | 先 1 份；扩展前实测编译耗时 |
| R5 | 音频与动作对不齐 / 抖动 | 中 | 中 | 导演时钟为主、音频从属；提供 `audio_offset` 手动对齐；不做音频主时钟 |
| R6 | 与已有相机功能互相干扰（共享 `free_camera_runtime.inc` 与 tick 泵） | 中 | 中 | 总开关隔离；body 写入与自由相机写入分属不同阶段（照上游 `mmd_director_runtime.inc` 的仲裁思路）；单人档不接 pose lease |
| R7 | 上游这批代码**未经其自身实机验证** | 高 | 中 | 不假设「上游能跑」；所有行为以我们设备日志为准 |
| R8 | 许可/署名遗漏 | 低 | 中 | 随代码带入 `LICENSE.EIEM` + `UPSTREAM.md` + `THIRD_PARTY_NOTICES` 条目 |

## 7. 本档明确不做

- 4 人同台（`motion2`–`motion4`）、`SquadManager` 小队访问 → 依赖 `pose_lease`，留到扩人数时
- 衣物物理三模式（本档固定一种；`BeyondBoneCloth.SetClothSimulateWeight` 钩子随 R1 结论决定）
- MMD 作品库 / `set.ini` / 作品管理器（本档直接选单个 VMD 文件）
- 上游的伴生悬浮窗 exe 与共享内存协议（由我们的 Compose 面板替代）
- BEM 1.1/1.2 外观格式与「角色外观」页折叠（与本功能无关，属另一条线）
- 桌面侧（Win）任何验收

## 8. 附：Phase 0 探针清单

**需要的 9 个钩子目标**（按名解析 → 取 `method_pointer`）：

```
SkeletalMorphCore.Update
DoEvaluateMorphToBoneJob
DoEvaluateSpecialMorphToBoneJob
SolverManager.LateUpdate
BipedIK.UpdateSolver
IKSolverTrigonometric.OnUpdate
GrounderBipedIK.OnSolverUpdate
GrounderBipedIK.OnPostSolverUpdate
BeyondBoneCloth.SetClothSimulateWeight
```

**需要的字段/方法**：

```
MovementComponent.m_bipedIK
MovementComponent.currentFloor
FindFloorResult 字段布局（bool@0, bool@1, float@8, Vector3@0x10, Vector3@0x1C）
Entity.get_movementComponent
（4 人档才需要）GameInstance.get_player → GamePlayer.squadManager → SquadManager.GetMemberBySlot
```

**符号层**：`dlopen("libil2cpp.so", RTLD_NOLOAD|RTLD_NOW)` 成功，且 `dlsym` 可取到 `il2cpp_domain_get` / `il2cpp_thread_attach` / `il2cpp_runtime_invoke` / `il2cpp_class_from_name` 等（`il2cpp_api.h` 的 `R(n)` 全表）。

**判定**：任一必需项缺失 → 记录缺失名与所属程序集/命名空间，按 R1 的替代路线重新评估后再动 Phase 2。
