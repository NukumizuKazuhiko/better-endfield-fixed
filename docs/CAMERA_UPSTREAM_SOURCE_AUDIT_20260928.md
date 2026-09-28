# 第一人称上游源码补齐审计（2026-09-28）

受众：内部实现与验收。本文记录取证纠正和可实施合同；其中 S6 方法声明已由本机客户端元数据核对，运行时调用和实机行为仍待验。当前路线图与执行合同应引用本文；阶段状态仍由执行合同统一维护。

## 1. 来源、身份和许可

本轮经代理实时查询作者公开分支 `ItsTheSewerRat/renodx:endfield-enhancer`，HEAD 为 `7b6ff8f19c4f233a3bf5054ae6ebf109d760eaf7`，提交时间 `2026-09-13T19:07:37Z`。下载均固定到该提交。

- [固定源码目录](https://github.com/ItsTheSewerRat/renodx/tree/7b6ff8f19c4f233a3bf5054ae6ebf109d760eaf7/src/games/endfield-enhancer)
- 仓外目录：`D:\codexdata\bem-first-person\upstream`。
- 身份清单：同目录 `source-manifest.json`，记录提交、文件长度及 SHA256。后追加的 `Unity-REFERENCE-LICENSE.md` 不在首次清单内。
- 作者 fork 根 `LICENSE` 为 MIT，版权行为 `Copyright (c) 2025 Carlos Lopez Jr.`。复用 RenoDX 代码时保留许可并更新本仓 `THIRD_PARTY_NOTICES.md`。
- UnityCsReference 是 Reference Only License；本轮只将其作为接口签名与语义依据，不复制实现代码。

Unity 官方参考固定提交：

| 分支 | 提交 |
| --- | --- |
| 2021.3 | `b052b1fe38b45d5aefa609a115c626ed2e74f3a6` |
| 2022.3 | `a322ce5f78a82cf7ea211857a45338136ed7a22f` |
| master | `88ce7b60434ba7a8ca0218590a4cb509971788ad` |

## 2. 对此前取证的纠正

此前仓外暂存只有九个文件，未取齐 include 依赖。现补齐全部十三个 `camera*.hpp`。因此不能继续使用“公开分支 InternalGetData / 顶点流重写均零命中”“对话无公开源码”的结论。

| 内容 | 已核实源码 | 正确边界 |
| --- | --- | --- |
| 同步数据读取 | `camera_mesh_runtime.hpp:208` 解析 `InternalGetData`，`:138` 调用 | 旧实现使用 `ReadAlias` 原生描述符伪装，不是新版 staging CopyBufferOffset 链路 |
| 顶点流重写 | `camera_mesh_copy.hpp:12-13,198-202` | 重建声明并提交全部原顶点；没有增加顶点数 |
| 颈部封口 | `camera_mesh_copy.hpp:137-157` | 将已有顶点构成的 cap 索引插入子网格；新版新增顶点封口仍无源码证据 |
| 对话 | `camera_dialogue.hpp:49-84,106-237,239-366` | 含普通对话资格、注视、进出视角保存恢复完整逻辑 |
| 特效跟随 | `camera_effects.hpp:34-41,118-164` | 含恢复与元数据解析，可作为后续动画接线依据 |

`ReadAlias` 复制 0x58 字节原生描述符、修改 0x1c 标志，并依赖 Windows 指令与 UnityPlayer build 校验。不得将这些偏移、原生别名和指令检查移植到 Android，也不应作为本项目同步回读默认方案。

## 3. S4：有依据的 managed 同步回读合同

以下为 Unity 2021.3 官方真实签名；2022.3 仍兼容这些 managed 形状，master 的 `InternalGetData` 已改用 `Span<byte>`。必须按完整参数类型解析，不能只匹配方法名。

程序集均为 `UnityEngine.CoreModule.dll`，命名空间均为 `UnityEngine`。

| 类 | 方法 | 参数 | 返回 |
| --- | --- | --- | --- |
| GraphicsBuffer | .ctor | GraphicsBuffer.Target, Int32, Int32 | Void |
| GraphicsBuffer | get_count / get_stride | 无 | Int32 |
| GraphicsBuffer | get_target | 无 | GraphicsBuffer.Target |
| GraphicsBuffer | InternalGetData | System.Array, Int32, Int32, Int32, Int32 | Void |
| GraphicsBuffer | Dispose | 无 | Void |
| Graphics | CopyBuffer / CopyBufferImpl | GraphicsBuffer, GraphicsBuffer | Void |

依据：[GraphicsBuffer 2021.3](https://github.com/Unity-Technologies/UnityCsReference/blob/b052b1fe38b45d5aefa609a115c626ed2e74f3a6/Runtime/Export/Graphics/GraphicsBuffer.bindings.cs#L109)、[Graphics.CopyBuffer](https://github.com/Unity-Technologies/UnityCsReference/blob/b052b1fe38b45d5aefa609a115c626ed2e74f3a6/Runtime/Export/Graphics/Graphics.cs#L388)。

推荐通过 host `object_new` 创建 staging，再用 `runtime_invoke` 调构造、复制、读取和 Dispose，以 managed exception 作为失败证据，不猜 native ABI。

约束：

1. 源 `count * stride` 必须防溢出且受预算上界控制。`CopySource=1<<2`，`CopyDestination=1<<3`；检查标志位包含关系，不能要求 target 恰好等于 CopySource。
2. 官方 CopyBuffer 要求源和目标的总字节数相等。staging 必须按完整源大小分配，不可仅按待取 `expected` 前缀大小分配；可复制后只取所需前缀。
3. staging 选 CopyDestination，stride 2 或 4；长度对齐与 count 范围先验校验。通过 managed Byte[] 和 `InternalGetData(array,0,0,expected,1)` 取字节。
4. Byte[] 用 `il2cpp_array_new` 创建并 GC root；通过已有 array header / byte length 导出核对内容，不能硬编码 `+0x20`。
5. 所有失败均保留原 mesh。源 buffer 与 staging 均明确 Dispose；构造失败也必须有清理路径。
6. `CopyBufferOffsetImpl` / `get_offset` 在上述三个官方版本均未发现，不能凭新版二进制字符串假定签名。若游戏只有该扩展，需另取客户端元数据。
7. 源无 CopySource、官方复制 API 被裁剪、managed 类型不匹配时，应报告该同步路径不可用，不能伪造原生描述符来绕过。

仓内历史证据记录 Windows 引擎为 2021.3.34f5。Android 的裁剪和 HG 定制仍须实际解析与实机确认；官方参考可支撑实现方案，不能证明目标设备可用。

## 4. S5：已有提交 owner 与旧源码范围

本仓 Windows `native/modules/custom_model/module.cpp:90-96` 已有 `SetVertexBufferParamsFromPtr` 和 `InternalSetVertexBufferData` 的真实函数类型，`:732-752` 包装，`:1250-1271` 提交。Android 使用 `android/app/src/main/cpp/modules/custom_model/android_mesh_builder.cpp:230-247` 的 managed MeshData 契约以及 `AndroidSubmitMesh`，不得据 Windows ABI 推定 Android 可用。

上游旧版重写所有流，但始终沿用原 `vertices`。S5 新增顶点必须同步位置、法线/切线、UV、蒙皮、全部流、索引、子网格范围和 bounds；新增部分不能只补 position。需要独立几何合同与离线验证，不能把旧版 cap 索引逻辑宣称为已完成新增顶点封口。

## 5. S6：对话确证接口

来源：[camera_dialogue.hpp 固定提交](https://github.com/ItsTheSewerRat/renodx/blob/7b6ff8f19c4f233a3bf5054ae6ebf109d760eaf7/src/games/endfield-enhancer/camera_dialogue.hpp#L268)。下表游戏类型属于 `Gameplay.Beyond.dll`。

| 类型 | 成员 | 确证合同 |
| --- | --- | --- |
| Beyond.Gameplay.Core.GameWorld | dialogManager | static DialogManager 字段 |
| Beyond.Gameplay.Core.DialogManager | m_dialogType | enum 字段，动态解析 Normal 常量 |
| 同上 | m_mainEntity | Entity 字段 |
| 同上 | get_isPlaying / get_isPreparing | 无参 -> Boolean |
| 同上 | get_playingTimeline | 无参对象返回；非空时拒绝普通对话资格；源码未校验具体返回类型 |
| 同上 | get_interactNpcCamController | 无参对象返回；与当前 controller 比较；源码未校验具体返回类型 |
| 同上 | get_interactNpc | 无参 -> Beyond.Gameplay.Core.Entity |
| Beyond.Gameplay.Core.Entity | get_iModelCom | 无参 -> Beyond.Gameplay.View.IModelComponent |
| Beyond.Gameplay.View.IModelComponent | GetModelGo | 无参 -> UnityEngine.GameObject，按实际对象解析虚方法 |
| Beyond.Gameplay.View.LevelCameraController | get_param | 无参；源码没有校验返回类型 |
| 同上 | get_config | 无参 -> Beyond.Gameplay.View.CameraControlConfigRuntime |
| 同上 | get_levelVirtualCamera | 无参 -> Beyond.Gameplay.View.LevelVirtualCamera |
| Beyond.Gameplay.View.CameraControlConfigRuntime | pitchToVerticalValue | UnityEngine.AnimationCurve 字段 |
| Beyond.Gameplay.View.CameraControlParam | get_currHorizontalAngle / get_currVerticalValue | 无参 -> Single |
| 同上 | SetHorizontalAngle / SetVerticalValue | Single, Boolean；源码校验参数，未校验返回类型 |
| Beyond.Gameplay.View.LevelVirtualCamera | m_transitionRemainingTime / m_playerInputDisableTime | Single 字段 |

资格条件：controller 类型为 `InteractNpcCameraController`，已有第一人称视角快照，dialogType=Normal，playing/preparing 至少一个为真，playingTimeline 为空，manager 的 interact controller 为当前 controller。

NPC 注视以头骨为目标，从 saved_view 经 0.65 秒 smoothstep 混合。退出时恢复水平/垂直角度并处理 Cinemachine blend。Cinemachine 相关字段见上游 `:318-349`：`CinemachineBlend.TimeInBlend/Duration/CamB`、`CinemachineBrain.mFrameStack` 及列表项 `blend`。不照抄上游 `0x470/0x88/0x390/0x700/0x730` 等 Windows 偏移；本项目应按元数据解析且校验类型。

战斗 `IsInFight`、`get_inSkill`、`get_castingNormalAttack` 的完整声明未从该公开提交确证；本机客户端元数据的补充核验见下节。

## 6. 本机客户端元数据核验（2026-09-28）

只读检查发现本机 `D:\Games\Hypergryph Launcher\games\Arknights Endfield` 含 `GameAssembly.dll` 和 `Endfield_Data\il2cpp_data\Metadata\global-metadata.dat`。前者 SHA-256 为 `0C5573679BC6DEC2D068A14335466DB7CCF20AF9BAE2B983FB9D45677D80FFCE`，后者为 `90C58E26E87C7227A85DDA3FEDF6CE5ED0B06DC1F76E0ABBE75AB20750ADF97E`；元数据头标称 v29。未修改游戏文件。

用[官方 Il2CppDumper v6.7.46](https://github.com/Perfare/Il2CppDumper) 对这两个文件离线解析，工具报告 `CodeRegistration=18b9217c0`、`MetadataRegistration=0`，随后自定义 PE loader 因缺模块（Win32 126）失败。公开 `Metadata` 类默认按 88 字节解释类型记录，因此出现越界 methodStart、破损类型名和方法重叠；不能使用这一解析结果。

进一步只读核对元数据头与 image 表：`typeDefinitionsSize=5886804`，170 个 image 的 typeCount 合计 `63987`，整除后每条类型记录恰好 **92 字节**。以 92 字节步长及实测字段偏移重新解析，所有非空 methodStart/methodCount 都在 494830 条、每条 32 字节的方法表内；各类型 methodCount 总和也恰为 494830，越界范围为 0。布尔值和 AbilitySystem 的返回类型索引另与类型定义的 byval 索引交叉核对。隔离工具保留在 `D:\codexdata\bem-first-person\il2cpp-audit\metadata-only`，未写入仓库或游戏目录。

| 声明类型（均位于 `Gameplay.Beyond.dll`） | 方法 | 静态/实例 | 参数 | 返回类型 |
| --- | --- | --- | --- | --- |
| `Beyond.Gameplay.GlobalTagUtils` | `IsInFight` | 静态 | 0 | `System.Boolean` |
| `Beyond.Gameplay.GameInstance` | `get_playerController` | 静态 | 0 | `Beyond.Gameplay.Core.PlayerController` |
| `Beyond.Gameplay.Core.PlayerController` | `GetMainCharacter` | 静态 | 0 | `Beyond.Gameplay.Core.Entity` |
| `Beyond.Gameplay.Core.AbilitySystem` | `get_inSkill` | 实例 | 0 | `System.Boolean` |
| `Beyond.Gameplay.Core.PlayerController` | `get_castingNormalAttack` | 实例 | 0 | `System.Boolean` |
| `Beyond.Gameplay.Core.Entity` | `get_abilityCom` | 实例 | 0 | `Beyond.Gameplay.Core.AbilitySystem` |

这构成 S6 战斗采样器的**离线声明证据**。接线仍按完整方法描述符解析，并在运行时检查静态标志与对象类型；任一检查或调用失败时状态标记未知，让出相机。下一步需在真实游戏中验证该客户端的解析/调用、战斗进入、退出、返场延迟及角色切换，离线元数据不能替代该验收。

S2 朝向合同也在同一元数据上复核：当前 `Beyond.Gameplay.Core.PlayerController` 没有上游的 `get_rawMoveAxis`；存在实例 `get_moveAxis()`，返回类型索引 `169547`，与 `UnityEngine.Vector2` 的 byval 索引一致。因此当前 adapter 使用 `get_moveAxis`。另有 `Entity.get_baseController()` 声明返回 `Beyond.Gameplay.Core.BaseController`，不是 `CharacterController`；adapter 按声明解析，并在运行时检查对象确为 `CharacterController` 可赋值类型。`RotatorComponent.SetRotation` 的单一参数索引对应 `UnityEngine.Quaternion`。这些只核实声明，未验证移动轴在锁定/输入延迟场景的语义或实际 controller 派生类型。

S6 对话 getter 也可从当前方法表确认：`DialogManager.get_isPlaying()`、`get_isPreparing()` 均为实例、零参数、返回 `System.Boolean`。按 92 字节类型记录中的 fieldStart/field_count 校核，311628 个字段全部被类型范围覆盖且无越界；`GameWorld.dialogManager` 与 S2 的 `MovementComponent.input` 字段名均存在。其字段类型索引不是已核对的类型 byval 索引，**不能据此声称字段类型已离线验证**；运行时解析必须继续检查完整 descriptor。

S4 的官方 Unity 2021.3 参考合同与**当前 Windows 客户端**有关键差异。客户端 `UnityEngine.GraphicsBuffer` 方法表含 `.ctor(Target,Int32,Int32)`、`get_count/get_stride/get_target`、`Dispose`，`UnityEngine.Graphics` 含 `CopyBuffer(GraphicsBuffer,GraphicsBuffer)`，`Mesh` 含 `GetVertexBuffer(Int32)` 与 vertexBufferTarget getter/setter；但方法表**没有** `GraphicsBuffer.InternalGetData`、`Mesh.GetIndexBuffer`、indexBufferTarget getter/setter。当前同步读回初始化要求全部方法解析，因此会返回不可用，连带 S5 的网格补丁无法使用这条路径。只读检查 `UnityPlayer.dll` 的连续 icall 名称区可见 `UnityEngine.GraphicsBuffer::InternalGetData`、`UnityEngine.Mesh::GetIndexBufferImpl` 和 `get_indexBufferTarget`；当前代码要求的 managed `GetIndexBuffer` 与该 native 名称并不相同，`GameAssembly.dll` 中也未找到这些名称。名称区不能证明 icall 的实际函数地址、签名、可调用性或客户端对象布局，不能据此绕过完整描述符门禁。后续必须取得可信运行时 ABI 或其他可验证的完整顶点/索引读取合同，再实现与验收；不能把官方参考 API 当作该客户端已经提供的方法。

客户端方法表另有 `Mesh.AcquireReadOnlyMeshData(Mesh)`，但[Unity 2021.3 官方 API](https://docs.unity3d.com/2021.3/Documentation/ScriptReference/Mesh.AcquireReadOnlyMeshData.html)明确要求运行时网格 `isReadable=true`，否则会抛 `InvalidOperationException`。它不能作为“网格仅留 GPU 数据、设备不支持 AsyncGPUReadback”情形的完整替代合同；不能据此缩小 S4 目标或把可读网格的 CPU 路径宣称为同步 GPU 读回。

补充离线 PE 检查：`UnityPlayer.dll` 的 `.text` 共 30074368 字节，以 x64 指令解码扫描 7793039 条指令，未找到直接 RIP 相对引用到上述三个 icall 名称附近区域；这**不证明**不存在间接注册表或运行时解析，只说明这次只读扫描未能从名称区定位函数地址。隔离脚本为 `D:\codexdata\bem-first-person\il2cpp-audit\scan_icall_refs.py`；本轮未调用游戏 icall，不能据此填写 ABI。

继续按本机 PE 节区映射检查，四个候选名称在 `.data` 都有唯一 64 位地址引用（`InternalGetData`：文件偏移 `0x20d5948`；`GetIndexBufferImpl`：`0x20d67f8`；index target getter/setter：`0x20d6810/818`）。相邻槽解析后仍是**名称指针序列**，不是名称与函数入口的配对表。对该 `.data` 范围再扫描 `.text` 的直接 RIP 相对引用，结果为 0；不能从这张表推出本机函数指针。静态脚本保存在 `D:\codexdata\bem-first-person\il2cpp-audit\scan_icall_tables.py`、`inspect_icall_table.py`、`scan_icall_table_refs.py`。下一项确证仍是运行时 `il2cpp_resolve_icall` 及独立 canary 调用，而非继续猜测地址。

再对照用户提供的新版 RenoDX Enhancer addon（`E:\Downloads\_ee_mod_extract\终末地EE\终末地EE.addon64`）：其自有解析槽 `0x180385818/820/828/830/838/840/848/850` 分别标注 `GetVertexBufferImpl`、`GetIndexBufferImpl`、GraphicsBuffer `get_count/get_target/get_stride/get_offset`、`CopyBufferOffsetImpl`、`InternalGetData`。同步读取函数 `0x1800fb5c0` 在 `0x1800fb845–854` 以寄存器 RCX/RDX/R8/R9 和一个栈参数调用 copy 槽，在 `0x1800fb8a6–8be` 以四个寄存器和两个栈参数调用 read 槽；这是**上游 addon 对其目标客户端的调用形状证据**，比单独字串强。本机 `UnityPlayer.dll` 的名称区逐项含这些同名 icall（包括 `GetIndexBufferImpl` 与 `get_offset`），但尚未将名称解析到本机函数地址，也未验证本机引擎版本、对象布局及调用约定是否与上游一致。故可把上游路径作为唯一候选原生 adapter 设计输入，不能直接宣称本机 S4 ABI 已闭合。

针对版本的补充核验：本机 `UnityPlayer.dll` 的 ProductVersion 为 `2021.3.34f5 (0)`，SHA-256 为 `B47728BA10F09C46E8A107B4C7055E48CFE402D3D8C88A4529074981F9672AA2`。Unity 官方 `UnityCsReference` 可定位到邻近标签 [`2021.3.34f1`](https://github.com/Unity-Technologies/UnityCsReference/tree/2021.3.34f1)：[Mesh.bindings.cs](https://github.com/Unity-Technologies/UnityCsReference/blob/2021.3.34f1/Runtime/Export/Graphics/Mesh.bindings.cs) 声明 `GetIndexBufferImpl()` 返回 `GraphicsBuffer`，且 `indexBufferTarget` 有 getter/setter；[GraphicsBuffer.bindings.cs](https://github.com/Unity-Technologies/UnityCsReference/blob/2021.3.34f1/Runtime/Export/Graphics/GraphicsBuffer.bindings.cs) 声明 `InternalGetData(System.Array,int,int,int,int)` 为实例原生绑定。其五个显式参数加实例指针，与上游 addon 六参调用形状相符。官方邻近标签不是定制 `f5` 的二进制 ABI 证明；当前元数据也确实没有这些托管声明，故仍不可从官方声明直接调用本机 icall。

本机元数据另确认 `Graphics.CopyBuffer(GraphicsBuffer,GraphicsBuffer)` 与五参 `CopyBuffer(GraphicsBuffer,int,GraphicsBuffer,int,int)` 都存在。因此候选 adapter 可以优先沿现有托管 `CopyBuffer` 合同复制，把直接 icall 的最小待验证面收敛到索引 buffer 获取、索引 target 读写与 `InternalGetData`；顶点 buffer 获取和其 target 读写已有托管合同。接线前须在**本机运行时**通过 `il2cpp_resolve_icall` 逐项取得非空地址，检查地址归属与签名；在独立可释放的 canary buffer/mesh 上验证写入、GPU 复制、完整字节回读、索引读取及所有资源 Dispose/Destroy，再允许处理玩家网格。任一步异常、字节不一致或释放失败即停用 S4/S5。Windows 真实进程和 Android 对应库仍分别缺实测，这一候选不改变阶段验收状态。
