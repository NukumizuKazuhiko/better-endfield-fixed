# 第一人称实现路线图（2026-09-28）

> 目标：参照 RenoDX Endfield Enhancer（作者 `ItsTheSewerRat` / ItsaRat，MIT）新版第一人称实现，逐项补齐本项目的功能缺口。分析依据见 `CAMERA_EE_ADDON_DIFF_20260928.md`，署名见 `THIRD_PARTY_NOTICES.md`。

当前执行合同与验收记录见 [CAMERA_FIRST_PERSON_EXECUTION.md](CAMERA_FIRST_PERSON_EXECUTION.md)。表内“已落地”只表示实现状态，不能替代设备验收。

用户已反馈 Android 3.3.21 除“外部模型缩头兼容”未测外，其余可操作第一人称项目实机通过。这是用户侧整体验收反馈，尚无逐项截图/新日志供独立复核；下表原有的 S4/S5 接口阻断与 S8 未测门槛保持有效。

## 0. 总览

| 阶段 | 主题 | 状态 | 依赖 | 验收门槛 |
| --- | --- | --- | --- | --- |
| S0 | 数学层 + 眼位/近裁剪 + 角色判定 + body_skin | ✅ 已落地 | — | 离线单测通过 |
| S1 | UI 配置项接入 | 🟡 双端已实现；Android 设备配置仪器测试通过 | S0 | 两侧 UI 真实交互/游戏应用待验 |
| S2 | 朝向跟随（R5） | 🟡 核心与 adapter 已实现、离线测试通过 | S0（数学层已就位） | 实机验证 5 条待验 |
| S3 | 动画跟随（R10） | 🟡 核心与 adapter 已实现、离线测试通过 | S2 | 实机验证待验 |
| S4 | 同步 GPU 回读（R4） | 🟠 有界路径已编码，但当前 Windows 客户端缺必需 managed 方法 | — | 取得可验证的完整顶点/索引读取合同及 GPU 实测 |
| S5 | 顶点流重写（R7） | 🟠 新顶点与上传核心离线通过，当前运行路径受 S4 阻断 | S4 | S4 闭合后实机封口；BlendShape 网格保守拒绝扩点 |
| S6 | 战斗/对话状态机（R8） | 🟡 战斗/对话 adapter 与策略核心已接线，离线声明已核对 | — | 真实对话/战斗及运行时解析待验 |
| S7 | 平滑过渡（R9） | 🟡 状态核心与相机接线已实现、离线测试通过 | — | 实机 Cinemachine 过渡待验 |
| S8 | EFMI/XXMI 兼容（R6） | 🟡 显式缩头策略已实现、离线测试通过 | — | EFMI/XXMI 真实组合待验 |

优先级建议：**S1 → S2 → S3**（源码最完整、收益最高），随后 **S4 → S5**（结构性），最后 **S6/S7/S8**（体验增强）。

## 1. S0 —— 已完成（2026-09-28）

已按「可照搬就照搬、版权单列」落地，本机 MSVC `/W4` 单测 + clang 严格语法检查全过：

| 项 | 落地位置 | 说明 |
| --- | --- | --- |
| 数学层 | 新增 `native/modules/camera/first_person_math.h` | 照搬 `camera_math.hpp`：`ExpandLookPitch` / `LateralFacingYaw` / `BlendRotation` / `FacingRotation` / `AxisAngle` / `Unit` / `Finite` |
| 眼位公式 | `module.cpp` · `ApplyFirstPersonState` | 上游 `planar` 水平前向 + 世界竖直高度，低头不再下沉 |
| 配置化 | `module.cpp` · 配置结构 / 解析 / 夹取 / 下发 | 新增 `first_person_eye_forward`(0.03) / `first_person_eye_height`(0.05) / `first_person_near_clip`(0.03) / `first_person_extend_look_range`（默认关，倍率 1.10/1.50，±89° 夹取） |
| 角色名判定 | `first_person_mesh.h` · `IsDedicatedHeadMesh` / `IsBodyMesh` | `s_actor_` + `_lod` + 部件 token + `shadowproxy` 排除；原名字子串判定保留 |
| body_skin | `first_person_mesh.h` · `Build` + `module.cpp` 探针 | 身体网格「三顶点 head/neck 权重过半」隐藏，补颈部开口外圈 |
| 版本 | `android/app/build.gradle.kts` | versionCode 30320 → 30321，versionName 3.3.20 → 3.3.21 |

## 2. S1 —— UI 配置项接入

S0 原先只加了 ini 键；本阶段已在两侧 UI 暴露并持久化：

- `android/app/src/main/java/dev/betterendfield/android/ModuleSettings.java`：新增 `first_person_eye_forward` / `first_person_eye_height` / `first_person_near_clip` / `first_person_extend_look_range` 的读写与 ini 生成。
- `ui/BetterEndfield.UI/Models/ModConfiguration.cs`（桌面端）：同步以上键的模型字段与默认值。

验收：两端设置页可编辑四键，读取、保存及重新打开后值一致，修改其他相机项不丢失四键。Android 沿用启动快照合同，保存后强停并重新启动游戏生效；桌面覆盖模型、完整 INI、即时保存与迁移调用。两端真实 UI 和游戏效果须另行验证。

2026-09-28 已在 PJX110（Android 16、ARM64）运行 `testCameraSettingsRoundTrip`，runner 返回 `PASS BEM installation tests`，命令退出码 0；测试使用隔离偏好设置，覆盖默认值、边界夹取、往返、禁用后保留与滑块精度。真实设置页操作、LSPosed 快照和游戏视角仍未验收，详见执行合同。

## 3. S2 —— 朝向跟随（R5）

> 本阶段必须实机验证：会写角色骨架根（`animator` 下 `Root`）的世界旋转，节点/契约名错误会破坏角色渲染，离线单测覆盖不到。其他涉及游戏行为的阶段同样需要各自的实机证据。

完整施工图（契约表 18 条、每帧算法 7 步、依赖顺序、实机验证清单 5 条）见 `CAMERA_EE_ADDON_DIFF_20260928.md` §8。

关键常量（照搬自 `camera_movement.hpp` / `camera_locomotion.hpp`）：

- `side_look_limit` 默认 60°，clamp 0–90。
- 横移转身：`LateralFacingYaw` ±45°，纯后退不转身。
- 移动中转身：`turn_time` 上限 **0.35s**，每帧增量夹取 **0.1f**，收敛项 `-expm1(-16 * elapsed)`。
- 站定转身：`held_yaw = view_yaw - clamp(remainder(view_yaw - held_yaw, 360), ±side_look_limit)`。
- yaw = `atan2(view.x, view.z) * 57.295779513f`。

当前 Windows 客户端没有上游的 `get_rawMoveAxis`；S2 接线已按元数据改为零参 `get_moveAxis`（返回 Vector2）。横移、后退和输入锁定场景仍须实机确认其语义，详见[元数据复核](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)。
- 转身动画打断（可选）：3 个循环组 hash `0xab63503b` / `0x7b6c66f0` / `0xcf45fcba`，`Animator.HasState` + `CrossFade`(0.15f)。

执行顺序：先补契约解析（18 条）→ 每帧算法 → 离线能测的部分（`LateralFacingYaw` / 插值）加单测 → 实机验证。

## 4. S3 —— 动画跟随（R10）

读 R5 的 `held_yaw` / `attached` / `interaction_alignment`，用 `Rotate` / `BlendRotation` / `FacingRotation`（已在 `first_person_math.h`）把 Body / Head / Realistic（含 head roll）三种模式叠加到相机朝向。依赖 S2 先行。

当前 adapter 经宿主元数据解析采样骨骼、bindpose 和动画状态，未把游戏对象布局写成固定偏移。接口能否在当前客户端完整解析、Bone/Head/Realistic 的实际画面仍待实机验收。

## 5. S4 —— 同步 GPU 回读（R4）

目标：消除 `SupportsAsyncGPUReadback=false` 时静默跳过的回读可用性依赖。

已核验的公开 Unity 2021.3 接口使用 `Mesh::GetVertexBufferImpl` → staging `GraphicsBuffer(Target.CopyDestination)` → `Graphics::CopyBuffer` → `GraphicsBuffer::InternalGetData`（同步）。上游新版符号 `CopyBufferOffsetImpl` 没有足够的公开 ABI 证据，因此未猜测移植；细节见[上游源码复核](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)。adapter 按完整 `count × stride` 复制，设 128 MiB 上界并管理待释放缓冲区。

当前接线按名称解析所需 Unity 接口和元数据。最新本机客户端核验发现 `InternalGetData`、`GetIndexBuffer`、indexBufferTarget getter/setter 缺失，当前 S4 初始化将保守停用；不能把离线构建当作功能可用。`UnityPlayer.dll` 中的同名字符串不足以证明 icall ABI。详见[上游源码复核 §6](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)。

本机 `UnityPlayer.dll` 标为 `2021.3.34f5`；官方邻近 `2021.3.34f1` 绑定源码与上游 addon 的索引 buffer、同步读回调用形状相符。本机元数据又提供五参 `Graphics.CopyBuffer`，因此原生候选可把托管复制保留下来，仅针对缺失的索引 buffer/target 与回读 icall 做独立 canary 验证。静态名称表不含可确认的函数入口，且尚无真实运行时验证；S4/S5 状态不提升。

当前增加了只解析候选 icall 的运行时日志门禁；不调用候选函数，也不启用 S4/S5。实机先取得解析与地址归属日志，再决定是否可安全构造独立 canary buffer/mesh 验证 ABI。

验收：回读走新链路，`SupportsAsyncGPUReadback` 为 false 的设备也能完成整块隐藏判定。

## 6. S5 —— 顶点流重写（R7）

依赖 S4。当前几何核心为颈部开口**新增独立顶点**，复制原始所有 vertex stream 后修正法线/切线，将源网格与封口 Bounds 合并，并通过私有 mesh clone 上传；上传后回读核对源与副本。带 BlendShape 的网格因未实现形变帧扩点而保守拒绝此路径。当前 Windows 客户端的 S4 读取合同未闭合，因此这条 S5 网格路径尚不能运行；真实 GPU 上传与游戏内接缝也未验收。

## 7. S6–S8 —— 战斗/对话（R8）、平滑过渡（R9）、EFMI（R6）

公开源码中已有对话 getter 与网格相关头文件；此前“均无公开源码”是采样遗漏，详见 [上游源码复核](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)。本机客户端元数据经 92 字节类型记录步长核验，取得战斗 getter 的声明、静态性、参数和返回类型证据；S6 战斗采样现已接线，并在解析、类型或调用失败时保守让出相机。两端设置默认关闭，启用后战斗期间交还游戏视角、结束后按策略延迟返回。对话、战斗及 S7/S8 的运行表现仍待实机验收。

## 8. 收口纪律

每完成一个阶段：

1. 先补离线失败用例 → 改核心/入口 → 通过。
2. 同步四份文档：`CHANGELOG.md` / `README.md` / `README.en.md` / `android/README.md`。
3. 新增复用的上游代码，同步记入 `THIRD_PARTY_NOTICES.md`。
4. S2 及以后涉及实机的阶段，必须过 `CAMERA_EE_ADDON_DIFF_20260928.md` §8.5 的验证清单才能视为完成。
