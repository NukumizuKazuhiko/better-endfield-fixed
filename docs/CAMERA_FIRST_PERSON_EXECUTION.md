# 第一人称 S0–S8 执行合同

## 2026-10-02：陀螺仪分支并入本地主线

`gyroscope` 从 `1897ffe` 分出，主线另有依赖修复。合并无文本冲突；Windows 定向构建发现 Android 专用 `ApplyFirstPersonLook()` 的调用缺少平台条件，已将调用限制在非 Windows 编译路径。相机仍由共享 `native/modules/camera/module.cpp` 负责；Android 传感器在游戏进程采样，经现有输入中继送到自由相机或第一人称。第一人称把像素增量除以实时屏幕宽高后调用 `CameraManager.OnInput`。历史 `CAMERA_FIRST_PERSON_GYRO_PLAN_20261001.md` 的 `SnapshotCameraController.RotateCamera*` 路线已被设备证据推翻，不作为当前合同。

本轮离线门禁：VS CMake Release 的 `BetterEndfield.Camera`、`BetterEndfield.FirstPersonFacingTests` 构建成功；后者运行通过。Android `:app:assembleDebug :app:verifyReleaseEntryPoints --offline --no-daemon` 成功。Gradle 仍报既有的 `srcDir`、`ndk.dir` 废弃提示以及其他原生模块警告，本轮未扩大到这些模块。未运行 Android 游戏或连接设备验证本次合并包；陀螺仪低速连续转动、触摸共存、自由相机切换和关闭后停止仍需以本次构建 APK 的设备画面与日志验收，不能把旧 alpha 设备日志当作合并包通过。

## 2026-09-30：第一人称自动隐藏头部配件（设备部分验证，组合件仍失败）

范围为所有角色模型中附着于头骨的配件。现有 `first_person_hide_head` 默认开启；进入第一人称时，Camera owner 继续按已有名称处理头发与面部部件，并对名称未命中的 Renderer 增加纯 CPU 头骨附着判定：蒙皮 Renderer 的骨骼必须全部位于当前角色头骨或其子层级，非蒙皮 Renderer 的 Transform 必须位于该层级。空骨板、混合头部/躯干骨板、读取失败及超过层级深度上界均不按整块头饰隐藏，防止误隐藏身体。命中后沿用 `ShadowsOnly` 租约保留阴影，退出第一人称或游戏接管相机时沿用现有读回与有界恢复流程；LOD 和角色重建仍由原有 30 帧重扫覆盖。GPU 网格读取不可用不阻断此判定。没有新增设置或 UI 状态，陀螺仪仍不在本轮。

离线门禁：`BetterEndfield.FirstPersonHeadAttachmentTests` 覆盖头骨子层级、混合骨板、空骨板和循环层级；Windows `BetterEndfield.Camera` Release 构建、Android `:app:assembleRelease` 和 `:app:verifyReleaseEntryPoints` 通过。实机门禁：至少在两个不同角色进入第一人称，检查独立头饰/发饰不遮挡视线且身体装备仍正常；退出第一人称、触发终结技/角色界面收回、切角色与 LOD 后检查配件重新显示或再次隐藏，并核对 `First person mesh: ... set to shadow-only rendering` 与恢复画面。角色模型若把头饰绑定到躯干或放在模型扫描范围之外，当前判据会保守跳过；需以具体部件名、骨板或画面证据决定是否扩展，不能仅凭离线测试宣称覆盖所有造型。

本地验收包：`D:\CodexData\bem-camera-retract-build\betterendfield-3.3.22-alpha.9-head-accessories-release.apk`，8,706,612 字节，SHA-256 `80007E91E52C5F39E657EA7988FD8808D028081F3A27F93E59F6C0E5412C34F8`；`apksigner verify --print-certs` 通过，证书 SHA-256 `8CD6FDC15038530E101668AB4B3CCD0030AE88AE37153E6D66AA45930C7B8EFD`。该包包含工作树现有其他 alpha.9 修改，未提交或发布，不是单功能正式版本。

2026-10-01 实机复核：PJX110 `b992bd53` 已安装的模块 APK 从设备拉取后 SHA-256 与上列验收包相同。启动游戏前开启 `adb logcat -v time -s BetterEndfield.Runtime:I '*:S'`，日志保存到 `D:\CodexData\headwear-audit\device-first-person-20261001.log`。用户进入庄方仪第一人称并退出，反馈“仍有遮挡，退出后恢复正常”。日志记录头骨 `Bip001_Head`、第一人称启用、239 个相机补丁帧和退出；`vfxpart_01/02/03_lod1` 与面部、头发等 Renderer 成功设置为 `ShadowsOnly`。同一部件树里的 `S_actor_zhuangfy_cloth_01_lod1` 没有隐藏记录。此日志无法仅凭名称断定剩余遮挡的每一个三角面归属，也未独立证明角色切换及所有角色恢复。

本机 VFS 离线解包见 `D:\CodexData\headwear-audit\README.md`：庄方仪 `cloth_01_lod0` 是单个子网格，既有头部骨骼主导的顶点/三角面，也有大量身体面，不能整 Renderer 隐藏。实机初始化报告 `Mesh::get_vertexBufferCount`、`GetVertexAttributeFormat/Dimension/Stream/Offset`、`GetVertexBufferStride`、`GetSubMesh_Injected`、`SetSubMesh_Injected`、`SetIndexBufferParams`、`InternalSetIndexBufferData` 等绑定缺失，最终为 `named GPU readback/clone bindings unavailable`。因此现有局部网格补丁未运行，组合头饰目标未通过；下一步必须先取得完整、可验证的 Android 顶点/索引读取与上传合同，再在真实角色和 LOD 中验证局部隐藏及阴影，不能扩大整块 `ShadowsOnly` 判定以掩盖失败。

进一步读取庄方仪 `cloth_01_lod0` 序列化标志：`m_IsReadable=False`、`m_KeepVertices=False`、`m_KeepIndices=False`。该网格的 52,106 个三角面按头部骨骼权重分成 6,548 个纯头部面与 45,558 个纯身体面，边界没有混合面；这为局部裁剪提供了离线 fixture，但不能证明运行时 GPU 读取、所有 LOD 或其他角色。Unity 2021.3 的 `Mesh.AcquireReadOnlyMeshData` 要求可读网格，不能替代 GPU 路径。用户明确要求保留头部阴影，故不将 `first_person_external_head_scale` 自动启用，也不把缩头作为该目标的完成证明。

用户提供的 `E:\Downloads\终末地EE9.28.zip` 内 Windows `.addon64` 已有定点静态逆向报告 `D:\CodexData\headwear-audit\EE-20260928-reverse-report.md`（样本 SHA-256 `3B552B839FE578DD6F1DF2ADE57FAE5086938B978C3C896C6DEAD1AAA4E2D22A`）。报告和关键指令复核显示：它通过 GPU buffer staging 读取不可读网格，在独立克隆的索引中退化选中的三角面，原网格留作 `shadowProxyMesh`，并对退出恢复做所有权检查。这与当前 Camera 的局部裁剪/阴影设计同向；报告没有 Android ARM64 的 icall 地址、ABI 或设备读回结果，不能仅凭 Windows RVA 改写 Android 生产网格。

同日从 PJX110 已安装的游戏 1.5.3 拉取 `libunity.so`（SHA-256 `46D5658A71BD35580C9F5D91D41C39B5653201CF142F34542FDD8DA856A6B4C8`）做只读静态接口核对，详见 `D:\CodexData\headwear-audit\PJX110-android-mesh-api-gate.md`。明确登记了 `Mesh::GetVertexBufferImpl`，但未在明文或 256 种单字节 XOR 名称搜索中找到 EE 路径所需的 `Mesh::GetIndexBufferImpl` 与 `GraphicsBuffer::InternalGetData`；现有 Android 相机日志也报网格补丁绑定缺失。该证据不排除所有替代渲染方案，但足以继续阻止把 Windows 的同步 GPU 读回和索引写回直接移植到当前 Android 生产路径。

同日找到可继续验证的 Android 资源路径，取证记录见 `D:\CodexData\headwear-audit\PJX110-android-asset-mesh-route.md`。设备 VFS 的 Android manifest 可解析，庄方仪世界/界面模型依赖闭包为 89/89 包、52,882,609 字节；只读提取后 `NativeAssetReader` 解析 49 个 Mesh 和 52 个 SkinnedMeshRenderer，后端错误为 0。与实机部件树同名的 `S_actor_zhuangfy_cloth_01_lod1` 可导出 21,689 顶点、67,080 索引、207 骨骼的源数据；按头骨子树权重大于 0.5 分类，22,360 个三角面中 3,300 个纯头部、19,060 个纯身体、0 个跨界，且只有一个子网格、无 BlendShape。这为“从当前 Android 资源离线生成独立可见网格，保留运行时原网格作阴影代理”的路线提供了样本证据；尚未实现原始顶点流/骨骼/材质的运行时一致性门禁、全角色与所有 LOD 覆盖或实机画面，因此组合头饰目标仍未通过。

## 2026-09-30：终结技与角色界面视角收回（用户侧验收通过）

范围为手机 LSPosed 模块的第一人称自动让出与恢复。用户确认终结技动画和角色界面期间交还游戏原生视角，结束后自动恢复且不关闭第一人称开关；陀螺仪与头饰隐藏不在本轮。Android 与 Windows 继续编译同一份 Camera owner。

本轮只扩展 `first_person_policy.h` 的临时抑制输入与 `first_person_retract_runtime.inc` 的游戏状态采样；Compose 面板和配置不另建状态机。当前 Windows 客户端元数据已核对：`CameraUtils.get_cameraManager`、`CameraManager.get_curActiveController/GetMainLevelCameraController`，以及 `Entity.get_inCinematic`、`AbilitySystem.get_inSkill/get_curSkill/get_curUltimateSkill`、`Skill.get_skillId` 的所属类、静态性、零参数与返回类型。运行时仍按完整描述符和方法静态性验证，缺少可选技能合同只停用对应采样并记录原因。主关卡控制器退场、终结技施放或角色进入剧情状态时策略交还原生视角；恢复后保留第一人称请求。用户侧已反馈本轮约定的终结技与角色界面收回、结束后自动恢复行为验证通过；具体运行时信号及控制器切换路径尚无日志可独立复核。

离线门禁：`cmake --build D:\CodexData\bem-first-person\native-vs18 --config Release --target BetterEndfield.Camera BetterEndfield.FirstPersonPolicyTests` 成功；执行 `BetterEndfield.FirstPersonPolicyTests.exe` 成功；Android `:app:assembleDebug`、`:app:assembleRelease` 与 `verifyReleaseEntryPoints` 成功。供设备验收的本地 release APK 为 `D:\CodexData\bem-camera-retract-build\betterendfield-3.3.22-alpha.9-camera-retract-release.apk`，8,705,248 字节，SHA-256 `33667FF534968AE77B9BEAD5B8BF72CAE82A601BE74EDDDEA7CC69077A0C3336`；签名证书 SHA-256 `8CD6FDC15038530E101668AB4B3CCD0030AE88AE37153E6D66AA45930C7B8EFD`，与现有 alpha.9 release 身份一致。另有 debug APK `D:\CodexData\bem-camera-retract-build\betterendfield-3.3.22-alpha.9-camera-retract-debug.apk`，SHA-256 `95AC797480593C5AE78C48C30DDCD60B12B26315E4703563504FD88FE983CA97`。两包 `apksigner verify` 均通过，arm64 `.so` 均含新策略字符串。工作树已有未提交的 alpha.9 改动，本轮未提交、未安装、未发布；两包均为合成工作树验收包，不能当作单功能正式发布。Android 构建另有既存的 `ndk.dir` 废弃提示、Compose Kotlin 冗余转换，以及其它模块的 C++ 警告；本轮 Camera 编译未报新警告。设备验收路径：确认 APK 哈希和 LSPosed 作用域，进入第一人称后分别触发终结技、打开/关闭角色界面，拍摄交还与恢复画面，导出含 `First person perspective reason=` 的运行日志；重复三轮并确认退出/切角色后头部显示恢复。2026-09-30 用户回复“验证通过”，据本轮约定范围记为用户侧实机验收通过；未收到设备序列号、所装 APK 哈希、运行日志、截图或重复轮次记录，因此不标记独立复核通过，也不扩大为 S0–S8 全部通过。

状态：执行中。入口为 [路线图](CAMERA_FIRST_PERSON_ROADMAP_20260928.md)，参考证据为 [上游差异分析](CAMERA_EE_ADDON_DIFF_20260928.md)。本文件面向内部实现与验收，不替代用户使用说明。

## 基线与边界

2026-09-28 接管基线为 `a38d0a6` 及用户已有 S0 未提交改动。保留原改动，不提交、不发布。Windows 与 Android 共用 Camera 原生 owner，不移植上游固定内存偏移，不新增独立相机运行时。每个阶段区分实现、离线验证、设备验证；缺少设备证据不能标记整阶段完成。

Android 当前通过启动快照传入配置，S1 保持设置保存后强停并重启游戏生效。路线图此前的“即时生效”不是当前实现事实，也不作为本阶段新增热更新系统的授权范围。桌面保存不得丢失未编辑的第一人称键。

## 唯一 owner 与阶段计划

| 阶段 | 目的与交付 | owner / 接线边界 | 验收与停止条件 |
| --- | --- | --- | --- |
| S0 | 核验数学、眼位、网格基线 | Camera 数学/网格核心 | 重跑现有 FirstPersonMeshTests；设备效果单列 |
| S1 | 四参数可读、可改、可保存 | native 语义；Android ModuleSettings 与桌面 ConfigurationService 仅映射 | 默认/边界/非默认往返，修改其他选项不丢值；双端构建和真实 UI/游戏验证 |
| S2 | 站定侧看阈值与横移朝向 | 独立朝向状态核心；managed adapter 只读输入、验证节点及应用/恢复 | 阈值、±180°、横移/后退、时间上界、无效输入；节点失效/切角/退场恢复；五条实机清单 |
| S3 | Body/Head/Realistic 动画跟随 | 相机运动核心消费 S2 状态；骨骼采样只读 | 四元数有效性、权重端点、模式切换；游戏内头部翻滚与动画接管 |
| S4 | 有界同步读取 GPU 数据 | 网格资源 adapter；签名和能力按元数据验证 | 实际同步复制/读取；资源所有出口释放；不支持 AsyncGPUReadback 的设备验证 |
| S5 | 独立封口顶点与 Bounds | 网格构建核心输出几何，adapter 负责提交 | 接缝/骨权重/索引宽度/容量/Bounds/失败原网格保持；实机封口 |
| S6 | 战斗/对话让出相机 | 第一人称会话状态机；adapter 采样真实游戏状态 | 进入/退出/延迟/角色切换/未知状态，真实对话与战斗 |
| S7 | 进出第一人称平滑过渡 | 相机会话中的过渡状态，消费游戏当前姿态 | 起终点、时间上界、打断/反向/切场；实际 Cinemachine 混合无抢占 |
| S8 | 外部改模下可恢复隐藏 | 隐藏策略 owner 管理缩放租约 | 仅在确证适用时降级；原缩放保存/条件恢复/换角/退出/卸载；真实 EFMI/XXMI 组合 |

S2、S3 参考公开源文件，不复制上游全局运行面、裸异常、硬编码游戏布局。S4–S8 对每个需要新增的契约先核对实际源码或元数据；差异文档中的符号名不能单独证明调用 ABI。能力缺失仅停用对应能力，并输出原因，不伪造成功。

## 新能力配置与证据修正

完整补源纠正见 [上游源码复核](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)：旧采样漏掉 dialogue、mesh_runtime、mesh_copy，不能沿用“全部无源码”的结论。

新增能力默认关闭，先允许显式验证：`first_person_movement=false`、`first_person_side_look_limit=60`（0–90）、`first_person_animation_mode=0`（0关闭/1身体/2头部/3真实）、`first_person_animation_strength=0.35`（0–1）、`first_person_yield_dialogue=false`、`first_person_third_person_in_combat=false`、`first_person_transition_seconds=0`（0–1）、`first_person_external_head_scale=false`。缩头是显式外部改模兼容模式，会影响头部阴影，不伪造自动识别 EFMI/XXMI 的能力。S6 战斗方法声明已由本机客户端元数据交叉核对，实际运行仍待验。

## 测试边界与资源纪律

- 配置通过真实 owner 的读写接口测试，测试路径隔离于用户实际配置。
- 朝向、会话、过渡和网格通过纯核心的输入/输出测试；先记录失败，再实现最小闭环。
- 游戏对象由会话持有 GC 引用；更新、退出、失效和卸载须有恢复路径。仅恢复仍属于本模块的写入，避免覆盖游戏新姿态。
- 每帧时间、枚举数量、GPU 数据尺寸、重试与日志均有上界。复用现有预算，不能用永久重试掩盖不可用。
- 验收产物默认写 `D:\codexdata\bem-first-person`。现有构建命令优先，不修改生成物。

## 执行记录

- [x] 审计 Git、路线图及 S1 双端保存链。
- [x] 确认 Android 配置为启动快照；桌面存在保存整节丢四键风险。
- [x] S0 当前版本离线基线：MSVC C++20 `/W4 /WX`，FirstPersonMeshTests 通过。
- [x] S1 实现与离线验证：桌面真实 INI 往返测试及 WinUI Release 构建通过；Android 主程序/测试 Java、ARM64 native、debug APK/test APK 构建及签名检查通过。仪器测试未运行。
- [x] S2–S8 核心与接线已进入离线构建；S6 战斗 getter 的声明类型、静态性、参数和返回类型已由本机客户端元数据核对。十个 FirstPerson 原生测试全部通过；双端 S6 设置接线与配置检查已完成。实机行为仍待验。
- [ ] 双端真实渲染、交互与游戏验收。

初始设备检查 `adb devices -l` 没有连接设备。此事实只限制设备验收，不阻止可独立完成的设计、实现和离线验证。

本机 Windows《终末地》的 `GameAssembly.dll` 与 `global-metadata.dat` 已做只读核验；经类型记录步长校正取得 S6 战斗 getter 的离线声明证据，详见[上游源码复核 §6](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)。游戏存在本机并不等于已完成真实窗口、渲染、交互或战斗验收。

S2 朝向核心、S3 动画模式核心、S4 同步读回预算核心、S6 策略核心、S7 过渡核心均已执行红绿测试，不能替代对应 managed 调用与设备验证。S2/S3 adapter 和 S4 同步读取已进入 Windows DLL / Android ARM64 首轮构建；后续修改仍须复验。

此前离线验证：VS 2026 在 `D:\codexdata\bem-first-person\native-vs18` 完整 Release 构建成功；当时九个 `BetterEndfield.FirstPerson*Tests.exe` 逐个执行成功。SDK 9.0.314 的桌面配置往返检查通过，WinUI Release 构建零警告/零错误。Gradle 9.3.1、NDK 27c 的 `:app:assembleDebug :app:assembleDebugAndroidTest` 成功；删除当轮未使用函数后重建 Android 仍成功。Android 仪器测试没有设备，尚未执行。产物与依赖均在 D 盘隔离目录。完整 Windows 构建报告第三方 MinHook C4701，当轮未修改该第三方源码。

S5 扩点路径只接受 `blendShapeCount==0` 的源及克隆网格，并在提交后复查；否则保守跳过该封口补丁。此门禁阻止无法同步扩展 BlendShape 帧的顶点数更改，不等于已经在 Unity/游戏里验证封口。S4 同步 GPU 读取、S5 实际上传、S2/S3 动画与朝向、S6 战斗/对话、S7 Cinemachine 混合、S8 EFMI/XXMI 同用，都仍需要游戏内证据。

卸载路径复核补齐 S4 的待释放 GPU 缓冲区/捕获网格三次以内重试，以及 S8 缩头租约的恢复重试与失败日志；重试仍失败时释放 GC 引用，避免宿主 API 失效后留有不可释放的句柄。此项只证明有界收口与可诊断失败，不证明 Unity 原生资源或头部缩放在失败场景必然恢复。改动后 Windows Camera Release 与 Android `assembleDebug`/`assembleDebugAndroidTest` 再次构建通过；真实卸载仍待游戏验收。

S8 恢复审查补齐写后读回：原先 `set_localScale` 返回成功就释放租约，遇到静默无效 setter 会把仍被缩小的头骨遗留在场景中。现在只有读回与原始缩放一致才算恢复，读失败或数值不一致保留租约并占用有界重试预算。`FirstPersonScaleTests` 覆盖静默无效、读回失败及成功重试；真实 EFMI/XXMI 同用、卸载与角色切换仍待窗口证据。

S2 恢复条件复核发现，原四元数点积阈值会把小角度的后续动画写入也判成本模块的 pose。已先加入 0.2° 外部旋转失败用例，再改为逐分量精度比较（兼容四元数正负号）；朝向测试通过。卸载时朝向恢复另加有界重试及无法确认恢复的日志。Windows Camera Release 和 Android APK/test APK 重建通过，仍未取得角色真实动画/卸载画面证据。

S5 阴影兜底原先在退出时无条件写回旧模式，失败后也丢弃记录。现由独立 shadow lease 管理：只有当前仍为本模块写入的 `ShadowsOnly` 才恢复；若观察到其他第三方模式则本会话让出所有权；若回到原始模式则允许按旧有 LOD 重建语义重申隐藏。读写失败保留 GC 引用并最多重试三次，卸载再给有界恢复机会且记录耗尽原因。FirstPersonMeshTests 覆盖外部所有权、回到原值、读失败、重试预算和销毁；Windows Camera Release 与 Android APK/test APK 构建通过。真实 LOD/EFMI 同用行为仍待游戏验收。

S5 上传审查发现扩点后曾把整个网格 Bounds 改成封口 Bounds；颈部范围小于身体时会错误裁剪原网格。已改为源网格与封口的并集，拒绝无效/反向/溢出范围，子网格和网格共享扩展后的安全范围。CapUploadTests 覆盖源范围更大、封口越界与无效输入；Windows Camera Release、Android APK/test APK 构建通过。该测试证明 CPU 合同，实际 GPU/游戏裁剪待验。

S6 战斗接口复核发现本机元数据类型记录比公开解析器默认布局多 4 字节。以 92 字节记录重新核对全部 63987 个类型与 494830 个方法后，确认 `IsInFight`、`get_playerController`、`GetMainCharacter`、`get_abilityCom`、`get_inSkill`、`get_castingNormalAttack` 的声明、静态性、零参数和返回类型（见[元数据证据](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)）。战斗采样器现使用完整方法描述符及运行时类型/静态性检查；失败时策略将状态视为未知并交回游戏相机。Android 与桌面端均增加默认关闭的开关；策略测试覆盖进入、退出、冷却、未知状态与会话重置。Windows 全量 Release、9 个原生测试、桌面配置检查及 WinUI Release、Android debug APK/test APK 构建通过。`adb devices -l` 仍无设备；桌面真实窗口、战斗进出、角色切换、对话和卸载效果均未验收，S6 及 S1–S8 的实机门禁保持未完成。

Android CMake 的 `betterendfield_desktop_features` 明确编译同一份 `native/modules/camera/module.cpp`，再链接进 `betterendfield_android`；已只读检查 debug APK 内的 `lib/arm64-v8a/libbetterendfield_android.so`，其中包含 S6 的 `combat.main_character`、`get_castingNormalAttack` 和 `IsInFight` 描述符。这证明 S6 原生接线进入 Android 包，仍不证明 Android 客户端存在相同 IL2CPP 声明或运行时行为可用；当前元数据取自 Windows 客户端。

对 S2 朝向 adapter 的游戏方法做离线声明核对，发现当前 Windows 客户端的 `PlayerController` **没有**上游的 `get_rawMoveAxis`；若保留它，S2 初始化会在方法解析时停用。当前类提供实例 `get_moveAxis()`，返回 `UnityEngine.Vector2`（方法索引的返回类型与 Vector2 byval 索引交叉核对）；接线已改用此当前客户端声明。还发现 `Entity.get_baseController` 声明返回 `BaseController`，原 descriptor 错写成 `CharacterController`；现按声明解析，之后继续验证实际对象可赋值给 `CharacterController`。它们都只是解除已证实的签名阻断；移动轴在锁定、延迟消费时与上游 raw 输入的差异、实际 controller 派生类型，以及 Android 客户端声明仍需实机验证。

对 S6 对话元数据追加只读核验：`get_isPlaying`、`get_isPreparing` 的实例/零参数/布尔返回声明与当前 adapter 一致；`GameWorld.dialogManager` 字段名存在，字段表 311628 条均在类型范围内，但仅凭离线 byval 映射尚不能证明该字段的类型签名。保持运行时完整字段解析门禁，不将此项记作实机验收。

S4 客户端合同复核纠正了先前的可用性判断：官方 Unity 2021.3 参考中有 `GraphicsBuffer.InternalGetData`，但当前 Windows 客户端托管方法表无此方法，也无 `Mesh.GetIndexBuffer` 与 indexBufferTarget getter/setter。`FpSynchronousReadback.Init()` 要求全组完整 descriptor，因此当前客户端会停用同步读取；S5 的 GPU 网格补丁依赖它，同样不能宣称运行可用。`UnityPlayer.dll` 的名称区虽含 `GraphicsBuffer::InternalGetData`、`Mesh::GetIndexBufferImpl` 等 icall 名称，但没有已核实的原生函数地址和 ABI，且 `GetIndexBufferImpl` 并非当前代码要求的 managed `GetIndexBuffer`；不能用名称直接替换。S4/S5 的核心与构建测试仍有效，验收状态已降回“合同阻断”；需先取得可信的完整顶点/索引读取合同，再做设备验证。现有仅投射阴影兜底可独立尝试，但不等于 S4/S5 完成。

替代入口审查：当前客户端方法表含 `Mesh.AcquireReadOnlyMeshData`，但[Unity 2021.3 官方说明](https://docs.unity3d.com/2021.3/Documentation/ScriptReference/Mesh.AcquireReadOnlyMeshData.html)要求运行时网格 `isReadable=true`；它不能覆盖仅有 GPU 数据的 S4 目标。S7 过渡核心未新增游戏 getter；S8 的 `Transform.get_localScale/set_localScale` 声明在当前 Windows 元数据中存在，仍缺外部改模真实组合与恢复画面。上述离线取证不改变 S4/S5 阻断及 S7/S8 实机待验状态。

S4 下一步只沿一条可证伪主线推进：取得与本机 `UnityPlayer.dll` 精确版本匹配的 icall 函数地址及参数/返回 ABI 证据；先在真实客户端只读解析 `InternalGetData`、`GetIndexBufferImpl`、index target 的存在性和模块范围，再用独立且可释放的临时 buffer/mesh 验证完整顶点、索引字节数与内容。任一签名、源 CopySource、读取内容、Dispose 或 Android 对应能力不可信，立即停止该平台的 S4/S5 路径并保留原 mesh/阴影兜底；不从 UnityPlayer 名称字符串猜函数指针或调用约定。Windows 实机检查需先取得真窗授权，Android 检查需连接目标设备。

S4 第一层运行时诊断现已接线：当完整 managed 方法表缺失时，`FpSynchronousReadback.Init()` 只通过本机 `il2cpp_resolve_icall` 查询四个候选名称；Windows 还检查返回地址是否位于已知可执行模块映射。日志只记录可用性与地址归属，**不调用**候选函数、不改变 S4/S5 能力门禁，也不输出原始地址。等待真实客户端日志后再决定是否进入独立 canary 的调用验证；离线构建不能替代该日志。

上游新版 addon 的 `0x1800fb5c0` 同步读取函数及已标注的解析槽给出 `CopyBufferOffsetImpl` 五参、`InternalGetData` 六参的 x64 调用形状；本机 `UnityPlayer.dll` 含同名 icall。它将 S4 候选收敛为**原生 icall 路径**，但没有证明本机函数指针或版本 ABI。当前代码仍按 managed 完整 descriptor 保守停用；在本机运行时与可释放的独立测试对象完成验证前，不把候选路径接入生产网格。

版本锚点已补：本机 `UnityPlayer.dll` 为 `2021.3.34f5 (0)`；Unity 官方邻近标签 `2021.3.34f1` 的源码声明与 addon 对 `GetIndexBufferImpl`、`InternalGetData` 的调用形状相符。本机元数据另有五参 `Graphics.CopyBuffer`，可让候选 adapter 保留托管复制调用，只针对缺失的方法校核直接 icall。版本差异和本机地址/实测仍未闭合，详见[上游源码复核 §6](CAMERA_UPSTREAM_SOURCE_AUDIT_20260928.md)。

S5 上传 adapter 使用的 `SetVertexBufferParamsFromPtr`、`InternalSetVertexBufferData`、`get/set_bounds_Injected`、`get_blendShapeCount` 名称也在本机 `UnityPlayer.dll` 中逐项存在；这只排除了名称缺失，签名与实际 GPU 上传仍须运行时校验。S4 的读取门禁优先于 S5，不能跳过。

S5 失败回滚审查补上两个资源边界：克隆创建后若 GC 根获取失败或克隆的 BlendShape 门禁拒绝，显式销毁未绑定的克隆；恢复 renderer 时若 `updateWhenOffscreen` setter 缺失或写入后读回仍未恢复，则保留补丁供有限重试，不提前释放克隆。`FirstPersonRestoreTests` 覆盖 setter 缺失、静默无效与成功重试；Windows 和 Android 构建仅验证离线接线，实际 Unity 对象销毁及退出恢复仍待实机验收。

S5/S8 退出重试复核：过渡仍显示时保留头部隐藏；过渡结束或立即退出后，心跳对尚未恢复的 S8 缩放租约及 S5 网格补丁继续有限重试。S5 逐补丁最多三次，卸载再给三次独立机会；仍失败则记录 renderer 状态未经恢复验证，并在宿主 API 消失前释放 GC 句柄。已绑定的克隆不会被盲目 Destroy；这条失败分支可能留下 Unity 原生状态，不能算成功恢复。`FirstPersonRestoreTests` 覆盖重试上限、生命周期重试与卸载句柄收口，真实退出/卸载仍待游戏证据。

S7 会话边界复核：主相机缺失或待提交的相机姿态无效时，同步撤销隐藏并清空旧过渡、相机根及退出截止时间，避免场景切换后沿用上一台相机的插值端点。离线构建只能验证接线；切场、摄像机替换和 Cinemachine 混合仍须真实画面验收。

## Windows 实机验收预检（尚未启动游戏）

2026-09-28 用户确认本机无可用于启动《终末地》的显卡，因此本机 Windows 游戏验收不可执行；此前的 Windows 包体和注入器检查仅为离线预检，不应继续尝试启动。用户将提供 ADB 设备，后续实机主线改为 Android。设备连接前 `adb devices -l` 为空，Android 游戏、LSPosed 作用域、运行时 icall 与 S1–S8 画面均尚无实机证据。连接后先运行只读 `android/Test-FirstPersonPreflight.ps1 -Serial <实际序列号> -Adb <adb.exe 路径>`，将设备系统、ABI、实际游戏包名与进程、已安装模块版本记录到 D 盘；再单独核查 root/LSPosed 状态和现有设置，决定安装与启动步骤。脚本不安装、不强停、不启动应用；无设备时已验证其报错且不落盘。不得直接运行会强停游戏、启用 resource probe 的 `android/Test-ResourceProbe.ps1`。Android debug APK 位于隔离构建目录 `D:\CodexData\bem-s1-android\build\app\outputs\apk\debug\app-debug.apk`，其版本由 `aapt dump badging` 核实为 `dev.betterendfield.android` 3.3.21（30321）。设备验收需保留启动前状态、确切产物哈希、模块/游戏日志、截图和退出恢复证据；S4/S5 仍先以运行时合同门禁为准。

Android 设备已连接：`b992bd53` / PJX110，SDK 36、ARM64，游戏 `com.hypergryph.endfield` 1.5.3 已安装且预检时未运行；原模块 3.3.20。预检证据在 `D:\codexdata\bem-first-person\acceptance\android-device-preflight-20260928-214055.json`。原模块 APK 已拉取到同目录下的 `android-b992bd53-20260928\module-3.3.20-base.apk`；其签名与本轮 debug APK 不同。用户授权必要时删除重装，但设备已启用 CorePatch，普通 `adb install -r` 原位升级 3.3.21 成功，因此未卸载、未清除应用数据。新 APK SHA-256 为 `FEA3E408AC20C4A1AF3FDD7BAD5F276B0D076AE8071443FA5408A336E6F97EFC`。SukiSU 4.1.3 管理器存在；尚未创建或读回受限 Shell App Profile，也未调用 `su`。SukiSU/KernelSU 文档确认可约束 `su` 后的 UID/GID、组、capabilities、SELinux、挂载命名空间及 `NO_NEW_PRIVS`，但本设备具体策略仍需应用并核验，不能把文档能力写成已生效控制。

设备侧 S1 配置仪器测试：`adb -s b992bd53 shell am instrument -w -e method testCameraSettingsRoundTrip dev.betterendfield.android.test/dev.betterendfield.android.BemInstallerTest` 退出码 0，runner 输出 `PASS BEM installation tests`。目标测试使用 `camera-settings-test-` 前缀的隔离 SharedPreferences 并在 `finally` 清空；证据在 `D:\codexdata\bem-first-person\acceptance\android-b992bd53-20260928\camera-settings-instrumentation.txt`。它验证 Android 配置合同，不证明真实 UI 点击、LSPosed 注入、游戏应用或 S2–S8 行为。

本机完整原生 Release stage 位于 `D:\codexdata\bem-first-person\native-vs18\stage\Release`，已由 `cmake --build` 验证；其 Host、Camera、Injector 和其余模块的相对布局与 README 的内置注入器合同一致。`D:\codexdata\bem-first-person\acceptance\windows-stage-preflight-20260928.json` 记录本次 stage 的 14 个 DLL/EXE SHA-256、Git HEAD 和 dirty 条目数，供日后日志对应准确构建。该 manifest 是预检快照，不证明已经加载进游戏。

## Android 第一人称失效：日志复现与修复待验

用户提供的 `E:\Downloads\betterendfield-log-20260928-215520..txt` 中，21:54:37 的热键已启用第一人称，随后记录 `dialogue manager missing or unrooted` 与 `perspective reason=2`（`DialogueUnavailable`）；21:54:39 关闭时 `patched frames=0`。因此当时没有任何第一人称相机帧写入，不能把“热键已启用”当成功。日志没有区分静态字段值为空与 GC 根失败。

当前客户端的 `GameWorld.dialogManager` 已通过运行时静态字段标志检查。同仓动作模块已有静态字段通过 `field_get_value_object` 取值失败、改用 `il2cpp_field_static_get_value` 的记录；审计的上游对同一个 `dialogManager` 也直接使用静态字段 getter，并将管理器为空视为非对话。对话采样 adapter 现改为静态读取：真实空管理器返回“已知非对话”；导出缺失、元数据不符、GC 根失败、对话 getter 失败仍返回未知，保持策略让出游戏镜头。独立测试覆盖静态值为空、存在与 getter 缺失，现有策略测试覆盖未知让出。

Windows Camera Release、`FirstPersonDialogueTests`、`FirstPersonPolicyTests` 与 Android `:app:assembleDebug` 在本轮通过。Android APK SHA-256：`DFE926C90622587FF9AF499953F5B8AC15D9FC2044FDA9E047D864403396ED1B`。设备 `b992bd53` 重新连接后执行 `adb install -r` 返回 `Success`；`dumpsys package` 读回 `dev.betterendfield.android` 3.3.21、更新时间 2026-09-28 22:06:52。尚未得到 `patched frames>0`、真实对话进出、画面或卸载恢复证据；第一人称与 S6 实机验收仍待游戏启动后验证。不得将安装成功记为功能完成。未使用 `su`。

安装后，用户反馈“除头部改模未测试之外全部通过”。按用户侧 Android 实机操作结果记录基础第一人称及已操作的高级设置通过，不推断具体测试步骤、角色、场景或日志数值。外部模型缩头兼容（S8）明确未测；S4 同步 GPU 读取及依赖它的 S5 网格封口仍有当前已记录的接口阻断，不因一般画面表现正常而解除。此前日志中的 `patched frames=0` 故障已由用户反馈为已修复，仍缺新版日志供独立复核；Windows 真实窗口验收亦未发生。

发布预检：本机 Android `:app:assembleRelease` 的 54 项 Gradle task 成功，包含 `lintVitalRelease`；产物 `app-release.apk` 的包名 `dev.betterendfield.android`、versionName 3.3.21、versionCode 30321、ABI arm64-v8a，APK Signature Scheme v2 验证通过。原生 Camera Release 与 11 个 `BetterEndfield.FirstPerson*Tests.exe` 通过。Android 原生 Release 编译有未触及的 UI/CustomModel 等模块告警，列为非本轮第一人称功能债务；桌面配置检查因本机当前找不到 `global.json` 指定的 .NET SDK 9.0.314 未能重跑，不把旧检查当本轮结果。GitHub Actions 的独立构建与公开发布结果以远端执行记录为准。

发布后核验：工作流 `android-release` run 36435841446 成功，公开 `v3.3.21` tag 指向 `904dac997e3fc0f770fb1dfa34ecf328099124c8`，附件 `betterendfield-3.3.21-30321.apk` SHA-256 为 `DFF4B49E56AA2B120DF8E96F7CCB60FA852C7706C774A75FB131A02CC8945DF0`。`v3.3.20` 附件签名证书 SHA-256 为 `2a9a0886a0e7fabdda516811a8ca88fde6b4b24b8796607869d86f8e0e6ea019`，`v3.3.21` 为 `6253aa93a0a87be95e5577c40cf726043dd9e7b13c7f91dee10502697dbc02a5`；两者不同，普通设备不能原位覆盖升级。发行包是 CI 重建产物，与用户确认功能的本地 debug APK 不同，尚无发行附件本身的游戏内复测。

内置注入器从自身上两级目录加载 `runtime/BetterEndfield.Host.dll`，通过 `--game <Endfield.exe>` 启动；不会为测试自动安装 XInput 代理。本机当前 `%LocalAppData%\BetterEndfield\BetterEndfield.ini`、`ui-settings.json` 和日志均不存在。实际运行前须重新核对这些路径，若测试需在系统要求的 C: 配置路径创建文件，先记录原状并在结束后只清理本次创建的内容；不得覆盖后来出现的用户配置。验收日志需标记 stage 的 Camera SHA-256、游戏 `UnityPlayer.dll`/元数据指纹、启动参数、配置快照和时间，再分别记录 S1 双端设置、S2/S3 姿态、S4 icall 解析与 canary、S5 接缝及原网格不变、S6 战斗/对话、S7 进出与切场、S8 外部改模/卸载恢复。S4 解析日志不是 ABI 验收，任何阶段缺真实画面或设备证据均继续标为未完成。
