# 与 RenoDX Endfield Enhancer 新版第一人称实现的差异清单（2026-09-28）

状态：静态逆向结论，未实机验证，未改动本项目代码。
参照物：`E:\Downloads\终末地EE..zip` → `终末地EE/终末地EE.addon64`（3,760,128 字节）。

## 0. 参照物身份（已确证）

| 项 | 值 |
| --- | --- |
| 格式 | PE32+ x64 DLL（ReShade addon） |
| OriginalFilename | `renodx-endfield-enhancer.addon64` |
| FileVersion | `0.2026.924.2314` |
| PE TimeDateStamp | 1790291655 → 2026-09-24T23:14:15Z（北京时间 2026-09-25 07:14） |
| 内嵌 PDB 路径 | `C:\Users\Rats\Documents\CodexWorkspace\RenoDX\build\Release\renodx-endfield-enhancer.pdb` |

结论：**与本项目 `docs/CAMERA_FIRST_PERSON_CINEMACHINE_20260915.md` 记载的移植来源是同一个上游项目**，只是新约 10 天的构建（改名打包为「终末地EE」）。因此本次 diff 等价于「上游 09-15 → 09-25 对第一人称的重构」。

### 0.1 上游源码出处（2026-09-28 补：公开源码已定位）

此前只知「PDB 指向 RenoDX」，未确认是否有公开仓库。补查结论：**有，而且是可读的 C++ 源码；但它不在 RenoDX 官方主仓，而在作者的公开 fork 分支上。**

| 层 | 仓库 | 位置 | 许可 |
| --- | --- | --- | --- |
| 基座 HDR addon（`renodx-endfield.addon64` / `renodx-endfield-dx11.addon64`） | `clshortfuse/renodx`（RenoDX 官方主仓） | `src/games/endfield/`（main）；维护者 Spiwar & Forge；讨论帖 #490 | MIT（© 2025 Carlos Lopez Jr.） |
| **Enhancer——本文档逆向的那个** | **`ItsTheSewerRat/renodx`（作者 fork，public）** | **分支 `endfield-enhancer` → `src/games/endfield-enhancer/`** | 同 fork 根 `LICENSE` = MIT |

作者身份链：内嵌 PDB 的 `C:\Users\Rats\...` → addon 内嵌捐赠链接 `https://ko-fi.com/itsarat` → GitHub `ItsTheSewerRat`（显示名 **ItsaRat**）= Nexus 页面 credits 中的「Rat」。同 fork 另有 `endfield` / `endfield-dx11` / `endfield-fg` 分支（HDR 侧开发分支；其 PR #607「update for version 1.4.4」已 close 未合并）。作者另有一个 MIT 公开仓 `ItsTheSewerRat/ArknightsEnhancer`（明日方舟 PC 端 QoL 插件），是同一「Enhancer」思路的姊妹项目，**不含终末地相机代码**（src 仅窗口/快捷键/输入处理）。

**对应关系已逐项校验（非推测）**：§3 的 7 个相机配置键全部在源码中命中；`camera_mesh.hpp:17-20` 的头部 token 表与本文档 §3 第 11 项从 UTF-16 字符串表还原出的表**逐字相同**（含 `s_actor_` / `_lod` / `shadowproxy` 前置条件与 `_face_ _hair_ _brow_ _eyebrow_ _iris_ _eyeshadow_ _hairshadow_`）；骨骼名 `bip_head` / `j_head` / `head_m` 见 `camera.hpp:166`。

源码同时直接解释了此前只能推测的三处：

1. **眼位公式（对应 R2）** `camera.hpp:287-289`、`:321`：
   `position = eyes + Vec3{-correction.x, eye_height - correction.y, -correction.z} + planar * eye_forward`，
   其中 `planar` 是相机前向去掉 Y 分量后的单位向量（`length = hypot(forward.x, forward.z)`）——这就是「低头不下沉」的实现方式。同一处 `Write(copy.data(), 0x28, 0.03f)` 把 NearClip 写为 **0.03**。
2. **视角放宽倍率（对应 R3）** `camera.hpp:518`：`extend_look_range` 开启时上/下视角范围倍率 = **1.10f / 1.50f**；`side_look_limit` 默认 **60**、clamp 0–90；`animation_motion` 默认 **35**(%)。
3. **转身动画（对应 R5）** `camera_movement.hpp:20-22,177-199`：转身用定时插值，`turn_time` 上限 **.35f** 秒；每帧时间增量 clamp 到 **0.1f**；视向 yaw 由 `atan2(view.x, view.z) * 57.295779513f` 得出；`movement::Update(..., forward, side_look_limit)` 是「站定偏转超限才转身 + 横移小幅跟随」的入口。横移时 `conversation` 分支另有 `dialogue::Focus(controller, position + correction, GetTickCount64() * .001)` 做对话注视点。

**必须注意版本差（公开源码 ≠ 本机二进制）**：

| 项 | 公开分支 `endfield-enhancer` | 本机二进制 |
| --- | --- | --- |
| 版本 | HEAD `7b6ff8f1`，2026-09-13T19:07 | FileVersion `0.2026.924.2314`，2026-09-24T23:14Z |

公开分支**缺失**本机二进制中已确证存在的符号（均 0 命中）：`CameraSmoothPerspectiveTransition`、`CameraThirdPersonInCombat`、`_NeedAutoYaw`、`_UpdateLookatParameter`、`SetVertexBufferParamsFromPtr`、`InternalSetVertexBufferData`、`CopyBufferOffsetImpl`、`InternalGetData`。即**公开源码恰好停在本项目移植参照的那一代（09-13/09-15）**，其后 11 天的增量（平滑过渡、战斗第三人称、hook 游戏相机方法、顶点流重写 + 同步回读）只存在于 Discord 分发包。

**影响**：§4 的 **R1/R2/R3（P0）与 R5 现在可直接对照源码实现**，不再仅凭反汇编推断；R9/R8/R7 仍须沿用反汇编结论。

取源命令（本机需 `HTTPS_PROXY=http://127.0.0.1:7897`）：

```bash
curl -O https://raw.githubusercontent.com/ItsTheSewerRat/renodx/endfield-enhancer/src/games/endfield-enhancer/camera.hpp
# 或仅取子目录：
git clone --filter=blob:none --sparse -b endfield-enhancer https://github.com/ItsTheSewerRat/renodx
```

本次已取 9 个文件到仓外暂存目录 `E:\Downloads\_ee_mod_extract\upstream_src\`（勿入库）。

**许可提醒**：RenoDX 与该作者 fork 均为 **MIT**，但 `camera_*.hpp` 等**无逐文件版权头**，只靠仓根 `LICENSE`。本项目为 AGPL-3.0-only，若逐行复用需在 `THIRD_PARTY_NOTICES.md` 记入并保留 MIT 声明；**建议按逻辑重写、只引用数值与流程**，与本项目既有做法保持一致。

## 1. 取证方法与产物

IDA Free 9.4（`D:\software\IDAFree`）**不含 IDAPython、无 `idat` 批处理入口**，`plugins/hexcx64.dll` 为云反编译客户端（交互式可用，无法脚本化）；仓库内 `tools/ida_*.py` 依赖 `ida_hexrays`，需完整版 IDA 才能复用。故本次改用自建分析器：

- `E:\Downloads\_ee_mod_extract\re_tool.py`：解析 PE 节表、`.pdata`（x64 异常表，给出**精确函数边界**）、导入表，用 Capstone 线性反汇编，并支持按字符串地址查交叉引用。命令示例：`re_tool.py f:0x1800d5680`、`o:0x30e075`、`x:0x18030f075`。
- 产物：`rdata_strings.txt`（46,075 条带偏移字符串）、`utf16_all.txt`（645 条 UTF-16 字符串）、`strings_ascii.txt`、`skel_d5680.txt`。
- 注意：**上游大量运行时字符串是 UTF-16**（il2cpp managed string），只查 ASCII 会漏掉整张部件名 token 表。这是本次最容易踩的坑。

## 2. 上游第一人称的架构（逆向结论）

上游已从「改 CameraState + 克隆网格」演进为一套 **9 个 Detours hook + 单一 per-frame 协调器 + 多子解析器** 的结构：

### 2.1 解析器（全部返回 bool，失败即降级）

| 地址 | 体积 | 职责 |
| --- | --- | --- |
| `0x18015f480` | 5774 B | 相机总解析器 + hook 注册（校验 IL2CPP 导出、程序集、CameraState/LensSettings 布局、9 个 hook 目标签名） |
| `0x180160fa0` | 3747 B | 对话 / 首人称角度 API 解析（`CameraControlConfigRuntime`、`DialogManager`、Cinemachine 混合栈） |
| `0x180161e50` | 4367 B | 移动 / 转身 / 战斗 API 解析 |
| `0x180162f60` | 1720 B | 动画黑板 / 相机运动元数据解析 |
| `0x180163660` | 746 B | 自由相机输入解析 |
| `0x1800d5680` | 10621 B | **per-frame 协调器**（读 15 项运行期配置，驱动下述所有子模块） |

### 2.2 注册的 hook 目标（Detours，非 MinHook；`.detourc/.detourd` 节 + `DetourTransactionBegin/Commit`）

`CinemachineBrain::PushStateToUnityCamera`、`CameraControlParam::get_maxZoom`、`Dynamic3rdPersonFollow::get_ZoomScaleMax`、`CameraManager::TailLateTick`、`CameraManager::Tick`、`CameraControlAutoYaw::_NeedAutoYaw`、`LevelFreeLookCameraController::_NeedAutoYaw`、`CharacterAnimationBlackboard::_UpdateLookatParameter`。

关键点：**上游直接 hook 了游戏自己的相机控制方法**（自动 yaw、look-at 参数），而不只是被动覆写最终 CameraState。

### 2.3 第一人称骨架（按协调器调用序）

```
0x1800d5680 (per-frame)
 ├─ 0x1800e8de0  角色/头部发现（含 500 单位时间闸、512 renderer 上限）
 ├─ 0x1800eb6b0  捕获阶段 → 0x1800f0890 校验/重建/克隆/绑定（34896 B 单体）
 ├─ 0x1800efe90  头骨解析（Transform.Find 全路径 + get_bones 名字回退）
 ├─ 0x1800dbce0  移动表现层（il2cpp_runtime_invoke 调用 managed 方法）
 ├─ 0x1800dd6b0  头部相机更新（读 12 项配置槽后 invoke）
 ├─ 0x1800ee3e0  对话退出角度保存/恢复
 ├─ 0x1800eee70  动画挂点刷新
 ├─ 0x1800edae0  每帧绑定 / 退役陈旧副本
 └─ 0x1800e2fc0  单 renderer 恢复
```

### 2.4 上游硬编码的 CameraState 布局（已校验，可作为本项目的交叉验证期望值）

- `Cinemachine.CameraState` 类尺寸必须 **`0x120`**；`Lens = 0x30`、`RawPosition = 0x90`、`RawOrientation = 0x9c`、`PositionCorrection = 0xbc`、`OrientationCorrection = 0xc8`。
- `Cinemachine.LensSettings`：`FieldOfView = 0x10`、`NearClipPlane = 0x18`、`Dutch = 0x20`。
- `<isFirstPerson>k__BackingField` / `<controller>k__BackingField` 的偏移必须落在 `[0x400, 0x410)` 窗口内。

### 2.5 上游配置项与默认值（ini 键 / 默认值）

| 键 | 默认 | 语义（上游原文） |
| --- | --- | --- |
| `CameraFirstPerson` | 1 | 第一人称总开关 |
| `CameraThirdPersonInCombat` | 1 | 战斗中切第三人称 |
| `CameraCombatReturnDelayEnabled` / `CameraCombatReturnDelay` | 1 / — | 战斗结束后延迟回第一人称 |
| `CameraCombatOnAttack` | — | 攻击与技能期间也切第三人称 |
| `CameraSmoothPerspectiveTransition` | 1 | 进出第一人称时「飞入/飞出眼睛」 |
| `CameraMeshHeadHiding` | 1 | 隐藏头部与头饰 |
| `CameraEFMICompatibility` | 1 | EFMI / XXMI 皮肤 mod 兼容（实验性） |
| `CameraFillNeckHole` | 1 | 封颈部开口 |
| `CameraExtendLookRange` | 1 | 放宽上下视角范围 |
| `CameraFirstPersonFOVOverride` / `CameraFirstPersonFOV` | 1 / **60.0f** | 第一人称独立 FOV |
| `CameraFirstPersonMovement` | 1 | 角色保持朝前，横移时轻微转身 |
| `CameraFirstPersonDialogue` | 1 | NPC 对话期间保持第一人称 |
| `CameraAnimationFacing` | 1 | 枚举 `Body` / `Head` / `Realistic`（`Realistic` 含 head roll） |
| `CameraAnimationMotion` | **35.0f**（%） | 动画对视角的影响量 |
| `CameraSideLookLimit` | **60.0f**（度） | 站定时侧看超过该角度角色转身 |
| `CameraEyeHeight` | **0.05f** | 相对头骨的眼高微调 |
| `CameraEyeForward` | **0.03f** | 眼位前移（低头时不下沉） |

## 3. 功能级 diff

| # | 能力 | 上游（09-25） | 本项目当前 | 差距性质 |
| --- | --- | --- | --- | --- |
| 1 | 相机接入 | 9 个 Detours hook，含游戏自身相机控制方法 | 1 个主 hook（`cinemachine.push_state`）+ 3 个辅助泵 | **架构级**：上游主动介入游戏相机控制 |
| 2 | CameraState 布局 | 硬编码 + 严格校验（size 0x120，偏移固定） | 按元数据动态解析、扣 boxed header | 本项目更健壮；上游数值可作期望值交叉校验 |
| 3 | 眼位偏移 | `EyeHeight` 0.05 / `EyeForward` 0.03，**可配置 + UI**，含「低头不下沉」处理 | 硬编码 `forward 0.10 / up 0.06`，不可配置，无低头处理 | **可直接复用的配置化 + 语义修正** |
| 4 | FOV | 独立引用 `60.0f` | 默认 75.0f（20–120 可配） | 数值调优 |
| 5 | 近裁剪面 | 写 `LensSettings.NearClipPlane`（未见配置项） | 硬编码 `0.05f` | 本项目文档已建议改 0.1 |
| 6 | 俯仰 / 水平范围 | `CameraExtendLookRange` + 走**游戏自身 API**（`pitchToVerticalValue`、`get_currHorizontalAngle`、`get_currVerticalValue`、`SetHorizontalAngle`、`SetVerticalValue`、`AnimationCurve::Evaluate`） | **无任何限制**（直接沿用第三人称朝向） | 对应本项目已知问题「视角可 360° 旋转」 |
| 7 | 朝向跟随移动 | `CameraFirstPersonMovement` + `CameraSideLookLimit`(60°)，写 `RotatorComponent::set_rotation`，读 `get_isRotating`/`get_lockToCamera`/`PlayerController::get_rawMoveAxis`/`MovementComponent::get_velocity` | **无** | 对应「身体不跟随、移动方向与画面不一致」 |
| 8 | 动画跟随 | `CameraAnimationFacing`(Body/Head/Realistic) + `CameraAnimationMotion`(35%) + hook `CharacterAnimationBlackboard::_UpdateLookatParameter`（读 `cameraForward`/`entityForward`/`entityRight`）+ `EffectManager.m_followTarget/m_followTargetRot/ManualUpdateFollow` | **无** | 体验增强 |
| 9 | 平滑过渡 | `CameraSmoothPerspectiveTransition` + 读 CinemachineBrain 活动混合（`get_ActiveBlend`/`TimeInBlend`/`Duration`/`CamB`/`mFrameStack`）+ 写 `LevelVirtualCamera.m_transitionRemainingTime` / `m_playerInputDisableTime` | **无**（瞬时切换） | 体验增强 |
| 10 | 战斗 / 对话状态 | `IsInFight`、`AbilitySystem::get_inSkill`、`get_castingNormalAttack`；`DialogManager`（`get_isPlaying`/`get_isPreparing`/`get_playingTimeline`）+ `InteractNpcCameraController` + `m_dialogType`/`m_mainEntity`；对话进出角度保存/恢复 | 仅「退出游戏自带拍照第一人称」 | 体验增强 |
| 11 | 头部部件判定 token | `head` / `bip001 head` / `bip001_head` / `bip_head` / `j_head` / `head_m`；分隔符 `_:.|/` 切分后做**段匹配**；部件 token `_face_` `_hair_` `_brow_` `_eyebrow_` `_iris_` `_eyeshadow_` `_hairshadow_` `_body_` `_lod` `s_actor_`；另有 `neck` `spine` `chest` `shadowproxy` | `kHeadPartTokens` = head/face/hair/brow/eyelid/eyes/iris/mouth/horn，**裸子串匹配** | 覆盖率差异不大（裸子串反而更宽松，`hair` 也能命中 `_hairshadow_`）；真实差异在**匹配方式**与**判据组合** |
| 12 | 头部判定主依据 | **CPU 侧骨骼层级**（`Transform::IsChildOf` + `get_bones` 骨板）为主，名字仅作辅助（含去尾部数字的归一化） | 名字命中即整块隐藏；未命中则依赖**骨骼权重**——而权重必须靠 GPU 回读才能得到 | **关键差异**：上游整块隐藏不需要 GPU 回读，本项目需要 |
| 13 | 网格管线 | **重写顶点流**（`SetVertexBufferParamsFromPtr` + `InternalSetVertexBufferData`）+ 重算 `bounds` + 仍用 `Internal_CloneSingle`；封口段有独立几何代码（`0x1800f3877–0x1800f6600`，含 ±1.5 / 9.0 / 1.25 / 0.64 / 1e-6 等常量），**新增顶点** | 仅重建索引缓冲与子网格范围，**不新增顶点**，复用自己的耳切实现 | 上游能新增顶点 → 可做真正的局部隐藏与开口封口 |
| 14 | GPU 回读 | **完全不用 `AsyncGPUReadback`**：`Mesh::GetVertexBufferImpl` → 建 staging `GraphicsBuffer`（`Target.CopyDestination`，元素宽 2/4）→ `Graphics::CopyBufferOffsetImpl` → `GraphicsBuffer::InternalGetData` → `memcpy`。**纯同步**，无回调 | `AsyncGPUReadback` 同步等待；`SystemInfo.SupportsAsyncGPUReadback` 为 false 时**静默跳过** | **直接对应「头发挡视线」的一个未证实成因**；上游方案不依赖 AsyncGPUReadback 可用性 |
| 15 | 回读安全校验 | `target == CopySource`、`1 ≤ count*stride ≤ 0x800000`、字节数必须偶数、`offset ≥ 0`、防溢出 | 检查完成状态/错误/实际字节数 | 可借鉴 |
| 16 | EFMI / XXMI 兼容 | 用 `Transform::set_localScale` 缩放头骨（不换网格），代价是「影子会缺头」 | **无** | 解决外部改模与本项目网格克隆冲突 |
| 17 | 节流 | 500 单位时间闸；单次发现上限 512 个 renderer；跨帧延迟恢复状态机（`waiting for previous mesh restoration`） | 30 帧节流；每帧处理 1 个 renderer；每 renderer 2 次重试预算 | 上游把「等待」集中到发现阶段 |
| 18 | 降级语义 | 硬失败（IL2CPP 导出/程序集/布局/9 个签名/移动与相机运动解析）→ 整组拒绝；软失败（`IsInFight`、对话 API、EFMI scale、自由相机输入）→ 只关对应子特性 | 网格管线各步失败回落阴影兜底；相机侧失败停用特性 | 上游分级更细 |

## 4. 可复用清单（按性价比排序）

> 2026-09-28 补：**R1 / R2 / R3 / R5 的公开源码已定位**（`ItsTheSewerRat/renodx` @ `endfield-enhancer`，见 §0.1），可直接对照实现；R7 / R8 / R9 仍须沿用反汇编结论。

### P0 —— 直接解决本项目两个已知问题

**R1. 「整块隐藏」的判据改为 CPU 侧骨骼层级（不依赖 GPU 回读）**
把 `module.cpp` 的 `MatchesHeadPartToken`（`954-965`）与 `first_person_runtime.inc` 的隐藏判定拆成两级：
1. **主判据（新增，纯 CPU）**：读 `SkinnedMeshRenderer.get_bones`（本项目已有契约 `unity.skinned_mesh_renderer.bones.get`），若某部件骨板中的骨骼全部 `IsChildOf(headBone)`（等价于本项目 `FpPalette` 里 kind==1 的判定，只是不需要回读顶点），则整块按 `ShadowsOnly` 或整块隐藏处理。新增契约：`unity.transform.is_child_of`。
2. **辅助判据（保留并收紧）**：名字 token 匹配，但改为按分隔符集合 `_:.|/` 切分后做**段匹配**（降低误命中），并补齐 token：`head_m`、`bip001 head`/`bip001_head`/`bip_head`/`j_head`，以及 `_body_` / `spine` / `chest`（用于识别「身体」以免误隐藏）。

收益：**直接消除「读回失败则静默跳过」这条成因**——上游证明整块隐藏判定可以在纯 CPU 侧完成，GPU 回读只服务于「局部切除」这类真正需要顶点数据的场景。
成本：中低，`first_person_retry.h` 同级可测，可加单测。
证据：`0x1800e8de0`（发现，`IsChildOf` @`0x1800e9742`、`get_bones` @`0x1800e9782`）；UTF-16 token 表 `0x30e3e6–0x30ef82`、分隔符 `0x30e630`。

> 说明：R1 的价值不在「token 表更全」，而在**把判据从 GPU 依赖搬到 CPU**。本项目现有 token 表覆盖率与上游大体相当，单靠补 token 不足以解释「头发仍可见」。

**R2. 眼位与近裁剪面配置化 + 修正「低头下沉」**
新增 `first_person_eye_forward`（默认对齐上游 0.03）、`first_person_eye_height`（默认 0.05）、`first_person_near_clip`（默认 0.10），替换硬编码 `kFirstPersonEyeForward=0.10 / kFirstPersonEyeUp=0.06 / kFirstPersonNearClip=0.05`（`module.cpp:217-219`）。
「低头不下沉」= 前向偏移只用水平分量（把相机前向投影到水平面后再偏移），而不是直接用含俯仰的 forward。
证据：`CameraEyeForward` 说明文本 `0x2fbaa3`、默认值槽 `0x180372b8c`。

**R3. 俯仰 / 水平视角限制**
在 `ApplyFirstPersonState`（`module.cpp:1240-1280`）里对 `RawOrientation` 做 pitch 钳位，或在拿到游戏自身水平/俯仰 API 后用 `SetHorizontalAngle` / `SetVerticalValue` 回写。
更稳的中间路线：先把**显示用朝向**做 pitch 钳位（不动移动方向），并提供 `first_person_extend_look_range` 开关放宽范围——这正是上游的 `CameraExtendLookRange` 语义。
证据：`0x180160fa0` 内 `0x18016146a/52b/54f/573/597`（`pitchToVerticalValue` / `get_currHorizontalAngle` / `get_currVerticalValue` / `SetHorizontalAngle` / `SetVerticalValue`）。

### P1 —— 结构性改进

**R4. 同步 GPU 回读（替换 AsyncGPUReadback）**
用 `GetVertexBufferImpl` → staging `GraphicsBuffer(CopyDestination)` → `Graphics::CopyBufferOffsetImpl` → `GraphicsBuffer::InternalGetData` 替换 `FpReadBuffer`（`first_person_runtime.inc:175-187`）。
收益：不再依赖 `SupportsAsyncGPUReadback`，消除「静默跳过」这条未证实成因；上游证明此路径在该游戏可跑通。
需新增 icall：`UnityEngine.GraphicsBuffer::get_target/get_count/get_stride/get_offset`、`UnityEngine.Graphics::CopyBufferOffsetImpl`、`UnityEngine.GraphicsBuffer::InternalGetData`、`GraphicsBuffer..ctor`（经元数据解析 + `object_new`）。
本项目宿主已具备 `resolve_class` / `object_new` / `runtime_invoke` / `gchandle_*`，无需扩 ABI。
证据：`0x1800fb5c0`（980 B 同步回读），校验常量 `0x800000` / 偶数 / `offset ≥ 0`。

**R5. 朝向跟随移动（R3 的彻底版）**
新增契约：`Beyond.Gameplay.Core::Entity::get_rotateCom`、`RotatorComponent::set_rotation` / `get_isRotating` / `get_lockToCamera`、`PlayerController::get_rawMoveAxis`、`MovementComponent::get_velocity`。
逻辑：站定且水平偏离超过 `first_person_side_look_limit`（默认 60°）→ 转身；横移时按小角度跟随。
证据：`0x180161e50` 的解析点 `0x18016230e–0x1623dd`；`0x1800dc3b0` 的「有限 + 单位量级」四元数校验（0x1800dc5b7-c656）。

**R6. EFMI / XXMI 兼容分支**
当检测到外部改模（或网格校验失败）时，回退到「缩放头骨 `Transform::set_localScale`」，而不是放弃隐藏。需新增契约 `unity.transform.local_scale.get/set`。
证据：`0x1801609be` 解析 `get/set_localScale_Injected`；`0x180310032`「EFMI head hiding unavailable; transform scale API missing」。

**R7. 顶点流重写（顺序上放在 R4 之后）**
上游用 `SetVertexBufferParamsFromPtr` + `InternalSetVertexBufferData` + `set_bounds_Injected`，因此能**新增顶点**。这使「只隐藏头部三角形而保留颈部无缺口」与「新增封口顶点」成为可能，比本项目当前的「索引塌缩」更干净。
代价最高，且封口的三角化算法在反汇编里无法确证（见 §5），建议在 R1–R6 落地并实机验证后再评估。

### P2 —— 体验增强（可选）

- **R8** 战斗 / 对话状态机（`IsInFight`、`inSkill`、`castingNormalAttack`、`DialogManager`）。
- **R9** 平滑进出过渡（读 CinemachineBrain `ActiveBlend`，写 `LevelVirtualCamera.m_transitionRemainingTime`）。
- **R10** 动画跟随 Body / Head / Realistic（需 hook `CharacterAnimationBlackboard::_UpdateLookatParameter`）。

## 5. 未确证项与风险

1. ~~`0x1800d5680` 中 `RawPosition` / `RawOrientation` 的**具体赋值表达式与顺序**未逐条追完~~ → **已由公开源码解答**（见 §0.1 第 1 条：`camera.hpp:287-289`/`:321`）；`RawPosition`/`RawOrientation` 之外的其余字段仍以源码为准。
2. 封口三角化**算法本身无法判定**（该段无符号、无字符串，12 KB 手写向量化代码）；只能给出常量集（`1e-06` / `0.0001` / `±1.5` / `9.0` / `1.25` / `0.64`）与调用图。**因此 R7 不能照抄，只能自行设计。**
3. ~~「slight turn」的具体角度、`CameraSideLookLimit` 的判定细节未定位~~ → **已由公开源码解答**（见 §0.1 第 3 条：`camera_movement.hpp:20-22`/`177-199`，`turn_time` ≤ .35s、增量 clamp .1f）。
4. `CameraSmoothPerspectiveTransition` 的过渡时长与曲线未定位。
5. 上游 hook 的**回调函数体**（数据指针形式交给 `DetourAttach`，无 `call` 引用）未定位，因此 `_NeedAutoYaw` / `_UpdateLookatParameter` 回调具体做什么仍是黑盒。
6. 「Movement presentation」被 invoke 的 managed 方法名**未能确证**（间接调用，采样范围内无字面量）。
7. 上游硬编码 CameraState 偏移（`0x90/0x9c/0xbc/0xc8`）是 **Windows PC 版**的值；本项目面向 Android，**不可直接硬编码**，只能当作校验期望值。本项目现有的「按元数据动态解析」应保留。
8. 本次全部为静态结论，**未实机验证**。R1–R6 落地后仍需按 `docs/CAMERA_FIRST_PERSON_CINEMACHINE_20260915.md` 的实机清单复核。

> 📌 执行视图另见独立路线图 `CAMERA_FIRST_PERSON_ROADMAP_20260928.md`（按 S0–S8 分阶段，含每阶段依赖与验收门槛）。本节保留原始分析排序。

## 6. 建议的推进顺序

1. R1（整块隐藏判据改到 CPU 侧骨骼层级）—— 直接命中「头发挡视线」；先加失败用例再改。
2. R2 + R3（眼位/近裁剪配置化 + 俯仰限制）—— 直接命中「360° 旋转」，纯 CPU。
3. R4（同步 GPU 回读）—— 消除回读可用性依赖，是 R6/R7 的前置。
4. R5（朝向跟随）—— 需要新增契约，工作量中等。
5. R6（EFMI 兼容）→ 视实机结果决定 R7 是否值得做。
6. P2 三项按需。

每一步都应：先补离线单测 → 改核心 → 同步 `CHANGELOG.md` / `README.md` / `README.en.md` / `android/README.md` 四份文档 → 实机验证。

## 7. 实施状态（2026-09-28 更新）

按「可照搬就照搬」落地的一批（源码出处见 §0.1，署名见 `THIRD_PARTY_NOTICES.md`）：

| 项 | 落地位置 | 说明 |
| --- | --- | --- |
| R2 眼位公式 | `native/modules/camera/module.cpp` · `ApplyFirstPersonState` | 改为上游的 `planar` 水平前向 + 世界竖直高度，低头不再把眼睛一起拽下去 |
| R2 配置化 | 同上 · `CameraConfiguration` / 解析 / 夹取 / 下发 | 新增 `first_person_eye_forward`(0.03) / `first_person_eye_height`(0.05) / `first_person_near_clip`(0.03)，替换原硬编码 0.10 / 0.06 / 0.05 |
| R3 俯仰扩展 | 同上 | `first_person_extend_look_range`（默认关）→ 倍率 1.10 / 1.50、±89° 夹取；关闭时朝向修正仍为单位四元数 |
| R1 角色名判定 | `first_person_mesh.h` · `IsDedicatedHeadMesh` / `IsBodyMesh` | 照搬 `s_actor_` + `_lod` + 部件 token + `shadowproxy` 排除；原按名字子串匹配的判定保留，覆盖范围只增不减 |
| R1 body_skin | `first_person_mesh.h` · `Build` + `module.cpp` 探针 | 新增身体网格「三顶点 head/neck 权重过半」隐藏，补上颈部开口外圈那层身体几何 |
| 数学层（R5 前置） | 新增 `first_person_math.h` | 照搬 `camera_math.hpp`：`ExpandLookPitch` / `LateralFacingYaw` / `BlendRotation` / `FacingRotation` 等 |

离线验证：`native/tests/first_person_mesh_tests.cpp` 新增角色判定、body_skin、相机数学断言，本机 MSVC `/W4` 编译并运行通过（`module.cpp` 亦通过 clang 严格语法检查，仅余改动前既有的两条警告）。

仍未做：R4（同步回读）、R5（朝向跟随）、R6（EFMI）、R7（顶点流重写）、R8（战斗/对话）、R9（平滑过渡）、R10（动画跟随）。其中 **R5 与 R10 在上游公开分支上有完整源码**（`camera_movement.hpp` / `camera_locomotion.hpp` / `camera_motion.hpp`），照搬条件已具备；R4 / R7 / R8 / R9 仍只能依赖 §2 的逆向结论。

## 8. R5 / R10 施工图（源码已就位，待实机铺开）

R5（朝向跟随）与 R10（动画跟随）在上游公开分支上有完整源码，照搬条件已具备。之所以不随本批一起提交代码，只有一条理由：**这两项会写角色骨架节点的旋转**（R5 写 `animator` 下 `Root` 节点的世界旋转，R10 再把偏移叠加到相机朝向），节点选错或契约名不符会破坏角色渲染，而这只能靠实机反馈收敛——本轮的离线验证覆盖不到。先把施工图固化在这里。

### 8.1 接入点

本项目已有与上游同构的三个每帧驱动点，无需新增：

| 驱动点 | 本项目位置 | 上游对应 |
| --- | --- | --- |
| Cinemachine 推状态（每帧、拿得到最终朝向） | `DetourPushState` → `ApplyFirstPersonState` | `HookedPush` → `movement::Update(...)` |
| 帧尾 | `DetourTailLateTick` → `PumpFirstPerson` | `HookedTailTick` → `motion::Apply(...)` |
| 相机 tick | `DetourCameraTick` | `HookedInputTick`（`RestoreControlView` + `RestoreSimulationPose`） |

R5 的 `Update(active, view, look_limit)` 应放在 `ApplyFirstPersonState` 末尾（此处 `orientation` 已知，`view = orientation * rotation_correction`）；传入的 `active` 需同时满足「第一人称生效」且「非拍照模式」，与上游 `first_person_active && v.first_person_movement && !conversation && object_class != photo_class` 对齐。

### 8.2 需要新增的契约（全部依赖已有宿主接口，无需扩 ABI）

本表记录上游版本的目标符号；当前 Windows 客户端元数据复核发现 `PlayerController.get_rawMoveAxis` 已不存在，现有零参 `get_moveAxis` 返回 `UnityEngine.Vector2`；`Entity.get_baseController` 的声明返回 `BaseController`，不是表内的 `CharacterController`。本项目 S2 adapter 改用当前声明，并在运行时检查 controller 的派生类型。移动轴语义和实际派生类型仍需实机验证，不能再按表中旧签名直接接线。

| 契约 key | 程序集 | 命名空间 . 类 | 方法 | 形参 / 返回 |
| --- | --- | --- | --- | --- |
| `game_instance.player_controller.get` | Gameplay.Beyond.dll | `Beyond.Gameplay` . `GameInstance` | `get_playerController` | — / `PlayerController` |
| `player_controller.raw_move_axis.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `PlayerController` | `get_rawMoveAxis` | — / `UnityEngine.Vector2` |
| `entity.rotate_com.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `Entity` | `get_rotateCom` | — / `RotatorComponent` |
| `entity.rotation.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `Entity` | `get_rotation` | — / `Quaternion` |
| `rotator.set_rotation` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `RotatorComponent` | `SetRotation` | `Quaternion` / `Void` |
| `entity.mark_started.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `Entity` | `get_markStarted` | — / `Boolean` |
| `entity.mark_released.get` | 同上 | `Entity` | `get_markReleased` | — / `Boolean` |
| `entity.is_paused.get` | 同上 | `Entity` | `get_isPaused` | — / `Boolean` |
| `entity.in_cinematic.get` | 同上 | `Entity` | `get_inCinematic` | — / `Boolean` |
| `entity.movement_component.get` | 同上 | `Entity` | `get_movementComponent` | — / `MovementComponent` |
| `entity.base_controller.get` | 同上 | `Entity` | `get_baseController` | — / `CharacterController` |
| `entity.animator_com.get` | 同上 | `Entity` | `get_animatorCom` | — / `ComplexAnimatorComponent` |
| `complex_animator.animator.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.View` . `ComplexAnimatorComponent` | `get_animator` | — / `Animator` |
| `move_input.has_animated_move.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `MoveInput` | `get_hasAnimatedMove` | — / `Boolean` |
| `movement.block_grounded_move.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `MovementComponent` | `get_blockGroundedMove` | — / `Boolean` |
| `character_controller.is_script_controlled.get` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `CharacterController` | `get_isScriptControlled` | — / `Boolean` |
| `unity.transform.local_rotation.get` | UnityEngine.CoreModule.dll | `UnityEngine` . `Transform` | `get_localRotation` | — / `Quaternion` |
| `unity.transform.local_rotation.set` | 同上 | `Transform` | `set_localRotation` | `Quaternion` / `Void` |
| 字段 `MovementComponent.input` | Gameplay.Beyond.dll | `Beyond.Gameplay.Core` . `MovementComponent` | `input` | 类型须解析为 `MoveInput`；偏移量落在 `[0x10, 0x400]` 之外即判失败 |

`unity.transform.rotation.get` / `unity.transform.rotation.set` 本项目**已有**（`DetourPushState` 与自由相机在用）。上游另用 `component.get_transform`、`transform.get_parent`、`transform.Find(string)`、`Object.op_Implicit`，这些本项目的契约表里也都在。

### 8.3 每帧算法（照搬要点）

1. 取 `PlayerController`；校验 `markStarted` 为真、`markReleased` / `isPaused` / `inCinematic` 为假，且 `view.xz` 长度 > 0.001，否则释放并返回。
2. 经 `baseController` / `movementComponent` / `MovementComponent.input` 取输入；校验 `baseController` 属于 `CharacterController`（`il2cpp_class_is_assignable_from`）；`isScriptControlled` 或 `blockGroundedMove` 或 `hasAnimatedMove` 为真时释放并返回（脚本/动画接管时本功能让路）。
3. 经 `animatorCom` → `animator` → `get_transform` → `Find("Root")` 取骨架根；要求它与 animator 的 transform 不同、且其父正是 animator 的 transform，否则释放并返回。
4. 目标 yaw = `atan2(view.x, view.z) * 57.295779513`；横移增量用 `FpMath::LateralFacingYaw(move, {0,0,1})`（已在 `first_person_math.h`）。
5. 移动中：`turn_time` 上限 **0.35s**，前 0.35s 用 smoothstep + 起始速度项插值（原文式见 `camera_movement.hpp:198-209`），之后用 `lateral_yaw += (target - lateral_yaw) * (-expm1(-16 * elapsed))` 收敛；每帧 `elapsed` 夹取到 **0.1f**。随后把 `held_yaw = view_yaw + lateral_yaw` 写成骨架的**世界旋转**（`set_rotation`）。
6. 站定：`held_yaw = view_yaw - clamp(remainder(view_yaw - held_yaw, 360), ±side_look_limit)`，即走动时保留的水平偏移在站定后**被拉回**，超过 `side_look_limit`（默认 60°）才转身；转身经 `RotatorComponent.SetRotation`（`AxisAngle({0,1,0}, delta) * entity_rotation`），先判 `Transform.set_localRotation` 是否被本模块改过、是则还原。
7. 退出/失效：把骨架的局部旋转还原为进入前的 `original_visual`，并清空 `lateral_yaw` / `turn_time` / `velocity` / `held_yaw` 等状态。

### 8.4 依赖与顺序

- **R5 必须先于 R10**：R10（`camera_motion.hpp`）读 R5 的 `movement::held_yaw`、`movement::attached`、`movement::interaction_alignment`，并调用 `Rotate` / `BlendRotation` / `FacingRotation`（已在本项目 `first_person_math.h`）。
- R5 的「转身动画打断」在 `camera_locomotion.hpp`（103 行）：把 15 个动画状态 hash 归成 3 个循环组（`0xab63503b` / `0x7b6c66f0` / `0xcf45fcba`），用 `Animator.HasState` + `CrossFade`（时长 `.15f`，从当前 `normalizedTime` 起）打断转身动画。这是**可选**项：不做只会让转身动作播放得不够顺，不影响朝向正确性。
- R10 另需 `GameObject.GetComponentsInChildren`、`SkinnedMeshRenderer.get_bones`、`Mesh.get_bindposes` 与 `il2cpp_class_get_type` / `il2cpp_type_get_object` / `il2cpp_array_length`；本项目契约表已有前两个，后三个需经宿主 `resolve_class`（返回 `type_object`）与新增两个导出替代。

### 8.5 实机验证清单

1. 确认上述每条契约在 Android 客户端解析成功（日志逐条打印，任何一条失败即整块禁用）。
2. 确认写的是骨架根（`animator/*/Root`）而非模型根：进第一人称后原地左右转视角，角色不应发生模型扭曲或抖动。
3. 站定侧看：超过 `side_look_limit` 前身体不应转动；超过后转身应一次性到位、不来回抽动。
4. 横移：`LateralFacingYaw` 应为 ±45°，且纯后退（`move=(0,0,-1)`）不转身。
5. 退出第一人称后：骨架局部旋转必须回到进入前值（对照进入时打印的四元数）。

## 附：上游字符串证据位置（文件偏移）

| 偏移 | 内容 |
| --- | --- |
| `0x30e3e6`–`0x30e440` | 头骨名候选（UTF-16）：`head` / `bip001 head` / `bip001_head` / `bip_head` / `j_head` / `head_m` |
| `0x30e630` | 分词分隔符集合 `_:.|/` |
| `0x30e63c` / `0x30e646` | `neck` / `shadowproxy` |
| `0x30eee8`–`0x30ef82` | 部件 token：`s_actor_` / `_lod` / `_face_` / `_hair_` / `_brow_` / `_eyebrow_` / `_iris_` / `_eyeshadow_` / `_hairshadow_` / `_body_` |
| `0x30f8d8` / `0x30f8e4` | `spine` / `chest` |
| `0x30f73e` | `staged GPU buffer copy/read failed; original mesh retained` |
| `0x30e0e7` | `Renderer bone palette changed; stale copy retired` |
| `0x30e26e` / `0x30e288` | `Movement skeleton expired` / `Movement skeleton rotation invalid` |
| `0x310934` | `Endfield camera motion: animation blackboard metadata missing or ambiguous` |
| `0x3100af` / `0x31010b` | `automatic yaw method signature mismatch` / `look-at method signature mismatch` |
| `0x2fb812` / `0x2fb9a3` / `0x2fba4e` / `0x2fba90` | `CameraFirstPersonMovement` / `CameraSideLookLimit` / `CameraEyeHeight` / `CameraEyeForward` |
