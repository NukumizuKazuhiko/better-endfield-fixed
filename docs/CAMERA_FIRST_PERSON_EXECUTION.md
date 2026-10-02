# 第一人称 S0–S8 执行合同

## 2026-10-02：Android 3.4.0 正式版与 CI 资源来源

用户确认将已验收的 3.3.22-alpha.21 合成包递进为 Android 正式版 3.4.0（versionCode 30400），并允许公开 834 份头饰资源。完整 `catalog-bundled`（834 份 `.behw` 加 `coverage.json`）已作为 `headwear-catalog-v3-834.zip` 附在固定保留的 `v3.3.22-alpha.21` Release；归档 SHA-256 为 `BEAF2135063C962D382129098B65A3779D18ADF515EBDAC1FBD292E7B4644A78`。Android debug/release CI 与 CodeQL Java/Kotlin 编译共用 `tools/Camera/fetch_android_headwear_catalog.py` 下载、验摘要和限定解压，再将完整目录传给 Gradle `headwearCatalogDir`；Gradle 原有 834 份 coverage、摘要与已接受萤石 profile 校验继续是打包门禁。运行时仍只读 APK 内置资源，不访问 Release。用户明确后续 Release 保留；此前删除旧 Release 是为了移除未修复版本，不是通用清理规则。上游桌面版的本地同名 `v3.4.0` tag 已删除，以便本仓库 Android 正式版占用该 tag；上游仓库自身 tag 不在本次操作范围。

本轮本地复核：从公开 Release 重新下载归档、SHA-256 校验与 835/835 文件比对通过；已有打包器生成资源 ID `1fb67e1585ec27b6013437d008a989c48fe18555dd3d6c1c02efa79b37471640`。以下载目录运行 Android `:app:assembleRelease :app:verifyReleaseEntryPoints` 与 `:app:assembleDebug` 均成功。Release APK 读回 `versionName=3.4.0`、`versionCode=30400`，834 份资源加 manifest 与 alpha.21 已验收 APK 835/835 逐字节一致，签名证书仍为 `8CD6FDC15038530E101668AB4B3CCD0030AE88AE37153E6D66AA45930C7B8EFD`。本机尚未对新 versionName/versionCode 组合进行设备安装和运行验证。Gradle 的 `ndk.dir` 废弃提示、Kotlin `srcDir` 废弃提示、既有 C++/Java 警告仍按非本轮资源/版本链路债务记录。

远端 `android-apk` Debug 构建 36970134290、`android-release` 正式发布构建 36970297467 与修复资源输入后的 CodeQL 六语言扫描 36970296090 均成功。`v3.4.0` 是正式 Release，tag 指向 `0a2d3c3a67aaf1de9247a19ef19b100faa6f424a`，保留 `v3.3.22-alpha.21` 供 CI 读取资源。重新下载发布 APK 后读回 `3.4.0/30400`、原 release 签名证书和同一资源 ID，835/835 包内资源与已验收 alpha.21 APK 一致；附件 49,163,703 字节、SHA-256 `B4E69560969DD509CBD79DD0B67D58127AD59798828AB1BE188D537EADF952D2`。该远端 APK 尚未独立实机复测。

## 2026-10-02：陀螺仪与内置头饰资源合成包（本地主线）

将隔离工作树 `D:/CodexData/bem-headwear-canary` 的头饰网格实现、834 份资源的生成/打包合同和游戏私有目录自动部署接入本地 `main`，同时保留 `2d87f57` 的陀螺仪实现、第一人称 look 探针和 `3.3.22-alpha.21` 版本。接线冲突只涉及 Android native include、`RuntimeBootstrap` 的加载参数和本文新增章节；合成后的 `load()` 同时接收头饰目录与陀螺仪设置快照。第三方 `.bem` 外观包仍走用户导入链路，不能与本节头饰 `.behw` 资源混称。

本轮从已验收目录 `D:/CodexData/headwear-audit/all-characters/catalog-bundled` 构建 release APK，内含 `assets/headwear-v3/` 下 834 份 `.behw` 和 `manifest.tsv`。资源 ID 为 `1fb67e1585ec27b6013437d008a989c48fe18555dd3d6c1c02efa79b37471640`；与先前实机通过的内置资源 APK 逐份 SHA-256 比较，834/834 一致。直接从新 APK 运行部署 owner 的首次解压、缓存复用和 834 份字节比对通过。20 项 Python 测试、33 项 Java 部署检查、Windows Camera/HeadwearFixtureTests Release 构建及后者运行、Android `:app:assembleRelease :app:verifyReleaseEntryPoints --offline --no-daemon`、APK v2 签名验证均通过。完整 lint 的既有错误仍按下节记录，未将其写成通过。

交付包为 `D:/CodexData/headwear-audit/betterendfield-3.3.22-alpha.21-gyro-headwear-bundled.apk`，49,164,659 字节，SHA-256 `0404ED0B049389ED5D2A784F6FD65A1644892B9DB3458F5B278EA1EC7671F7E4`；签名证书 SHA-256 `8CD6FDC15038530E101668AB4B3CCD0030AE88AE37153E6D66AA45930C7B8EFD`。2026-10-02 用户反馈该合成包“验收通过”，按本次操作场景记为用户侧实机通过；本轮未收到设备日志或逐角色画面，不能独立复核冷启动自动部署、头饰/服装/阴影与退出恢复、陀螺仪低速转动和触摸共存的各子路径，也不扩大为全部角色及故障分支均通过。萤石小三角边按用户已接受状态保留；噗切娜、大潘暂缓；卡缪、利诺、伊冯仍未逐项实机验收。

## 2026-10-02：陀螺仪分支并入本地主线

`gyroscope` 从 `1897ffe` 分出，主线另有依赖修复。合并无文本冲突；Windows 定向构建发现 Android 专用 `ApplyFirstPersonLook()` 的调用缺少平台条件，已将调用限制在非 Windows 编译路径。相机仍由共享 `native/modules/camera/module.cpp` 负责；Android 传感器在游戏进程采样，经现有输入中继送到自由相机或第一人称。第一人称把像素增量除以实时屏幕宽高后调用 `CameraManager.OnInput`。历史 `CAMERA_FIRST_PERSON_GYRO_PLAN_20261001.md` 的 `SnapshotCameraController.RotateCamera*` 路线已被设备证据推翻，不作为当前合同。

本轮离线门禁：VS CMake Release 的 `BetterEndfield.Camera`、`BetterEndfield.FirstPersonFacingTests` 构建成功；后者运行通过。Android `:app:assembleDebug :app:verifyReleaseEntryPoints --offline --no-daemon` 成功。Gradle 仍报既有的 `srcDir`、`ndk.dir` 废弃提示以及其他原生模块警告，本轮未扩大到这些模块。未运行 Android 游戏或连接设备验证本次合并包；陀螺仪低速连续转动、触摸共存、自由相机切换和关闭后停止仍需以本次构建 APK 的设备画面与日志验收，不能把旧 alpha 设备日志当作合并包通过。

## 2026-10-02：资源内置 APK 与自动部署（当前）

用户选择目前采用打包方式。当前主线为内置 APK，不增加 GitHub 下载运行面。本节替代历史“APK 与外置目录配套手动部署”的交付方式；此前接受的网格数据与裁剪规则不变。

### Owner 与执行路径

- `build_android_headwear_fixture.py` 继续拥有资源分类，coverage 新增每份 fixture_sha256。
- `package_android_headwear_assets.py` 校验完整 834 份目录、全部 coverage 摘要以及萤石三份接受摘要，生成 APK assets/headwear-v3。固定版本 50；834 文件上界、单文件 16 MiB、总文件 96 MiB、manifest 256 KiB。Gradle preBuild 强制执行，输入通过 headwearCatalogDir 显式下传；源或摘要缺失会拒绝构建。
- `HeadwearAssetStore` 是部署 owner；`HeadwearAssets` 只映射 Android AssetManager、游戏版本/私有 files 与日志，`RuntimeBootstrap` 在现有后台准备线程调用，然后把成功目录交给 native loader，Unity frame 内不解压。语音准备失败不阻断头饰准备。
- manifest.tsv 用资源清单 SHA256 标识内容版本，逐文件包含名称、长度、SHA256；部署至游戏私有 `betterendfield/headwear-assets/<digest>`。先写临时目录并验证全部文件，再同文件系统原子 rename。缓存逐文件校验后复用；损坏自有目录重建；失败不启用旧版本，不写原源 Mesh。
- 清理限定自有目录、拒绝符号链接和越界，目录树/缓存数量有上界；更新成功后清理旧 digest 目录，旧手动外部 headwear-v3 不读不删。失败原因带 stage；不伪造成功路径，native 环境变量在失败时清除。

### 验收与未闭合边界

- Python 生成/打包 20 项测试通过，包括 APK asset staging 逐字节一致以及缺失/损坏拒绝、旧输出不动；Java 17 独立部署测试 33 checks 通过（符号链接检查实际执行），含缓存复用、同长度损坏、manifest/资源缺失、路径/版本/尺寸拒绝、更新失败保留旧资源、旧外部目录不动。
- 新增类没有 lint 告警；完整 lintRelease 仍因本轮未改动代码中的 5 个错误、35 个 warning、28 个 hint 失败。错误为 BemInstaller 两处 NewApi、NativeCommandBridge 一处 NewApi、ToolPages 两处 LocalContextGetResourceValueCall。这些记录为非本轮旧债务，未改全局 lint 规则或生成 baseline 掩盖。
- APK 构建、入口与签名及全部资源/原生库核对通过。构建输入目录 `D:/CodexData/headwear-audit/all-characters/catalog-bundled`，资源内容 SHA manifest ID `1fb67e1585ec27b6013437d008a989c48fe18555dd3d6c1c02efa79b37471640`，834 份原始资源共 77859062 字节。
- 直接用真实 APK 的 ZIP streams 运行部署 owner，首次解压和第二次缓存复用通过；834 份部署文件逐字节等于此前接受目录，三份 native 库也与 fix2 完全相同。该闭环发现并修正 Windows Path 默认大小写折叠排序与 Java ASCII 顺序不一致的问题；现在 manifest 按 filename 的明确 ASCII 顺序生成，并有真实资源排序回归。
- 单文件交付 `D:/CodexData/headwear-audit/betterendfield-headwear-bundled-20261002.apk`，实际大小 49138907 字节（49.1 MB / 46.9 MiB）；SHA256 与最终构建产物摘要见 `headwear-bundled-verification.json`。安装步骤与新路径实机验收入口见 `headwear-bundled-README.md`。
- 2026-10-02 用户对交付的内置资源 APK 实机反馈“通过”，新增 Android AssetManager/游戏后台自动部署链路按该次用户操作场景记为验收通过。对应 APK SHA256 `485644310EFBFF64A9459EF6CE13468DC7986FBE91903424C7A1C72985BD97CE`，文件未重新构建或变更。本轮未采集新的设备日志，反馈未逐项列出角色、二次启动或强制损坏缓存场景，不扩写为全角色/所有故障分支逐项实机通过。
- 使用现有实验 alpha.9 工作树，未合入 F:/bem、未提交/发布；旧 CI 尚未提供资源输入时会显式失败，不声称 CI 发布已验收。

## 萤石生成器整合与产物一致性（历史，手动资源交付已由上节替代）

用户要求“整合进生成器，确保当前编译产物体验与我实际相同”。本轮已将接受的 boundary 集合整合到隔离工作树生成器；下方“未吸收生成器、需覆盖补丁”的记录为历史状态，已由本节替代。未合并至 `F:/bem` 主工作树，未提交或发布。

### Owner 与合同

- `tools/Camera/build_android_headwear_fixture.py` 是离线资源分类/编码 owner；`android_headwear_asset_profiles.json` 是已接受资产分类的登记源。Android runtime 的上传/绑定/阴影与恢复逻辑不变。
- 只有 registry 中三份 bounda cloth01 LOD 启用 `head_boundary_all_vertices`：任一顶点 Head 权重超过 32767，或三个顶点 Head 权重均非零，则退化该三角。对应用户已经接受的 453/203/44 额外低权重面，保持独立中央颈块在本次接受集合中的原样处理，不擅自换成审计提出的更保守集合。
- 每份 profile 同时约束 Mesh ID、原索引 SHA256、三个源 stream SHA256、接受的隐藏数和最终 fixture SHA256；不是全角色阈值调整。原顶点流与 draw、skin、bounds 合同沿用原路径。
- 全目录生成强制三份登记源唯一存在，并在创建/写入/删除任何输出前完整预编译。已知资产身份/布局/骨板/文件/字节漂移导致 ProfileRejected，不能被普通 skipped 分支吞掉，保持旧产物。
- coverage 中 pure_head/mixed/pure_body 仍描述源分类；hidden_triangles 是实际隐藏总数，profile_hidden_triangles 是额外隐藏数，retained_triangles 是实际保留数，不把边界面冒称纯头。

### 本轮验收

1. 增加真实源三 LOD 与手机接受 SHA 的回归，先运行看到三个 LOD 均失败（原 fix2 输出），整合后通过。
2. `python -m unittest discover -s tools/Camera -p test_android_headwear_fixture.py -v`，配置当前 Android raw/database，18 项通过；包含权重字节漂移以及 catalog 身份/布局/缺流/缺源四种失败，确保旧输出字节和目录项不动。
3. 当前生成器 `--catalog` 全量生成 included=834、skipped=413；三份萤石 SHA 与 `fluorite-probe-boundary` 和此前手机校验记录完全一致，另外 831 份与 fix2 catalog 完全一致，无文件增删。
4. `:app:assembleRelease :app:verifyReleaseEntryPoints --offline --no-daemon` 成功；APK 签名验证通过。APK SHA256 `B4BF7C149492C31BE22FA0F51045D6D7287E9B687B677DF5EC758FA81C272074`，与用户实测 fix2 APK 逐字节相同。
5. 独立只读审查发现早期 profile 错误被 catalog 跳过的问题，已闭合并二次复核无当前目标阻断。`git diff --check` 通过，换行转换提示为既有属性行为。
6. 当前 ADB 无设备，未重新安装/重做视觉验收。本机独立 C++ 测试尝试因链接器不可执行未运行；不声明新增 native parser 验收。对应夹具此前已有手机 ARM64 校验，本轮精确字节复现它们，Android native 构建通过。

### 交付与剩余边界

完整配套包 `D:/CodexData/headwear-audit/headwear-all-generator-integrated.zip`，SHA256 `133FF3C2BA33580495124CCF5668AC660F74779AA6E5D61BCEBF004BC1922268`；包含 APK、重新生成的 headwear-v3 全目录、摘要、测试/构建与接受日志。APK 单独安装不会更新外部资源目录；部署时必须同时安装 APK 和 headwear-v3。

当前生成命令：

```powershell
python tools/Camera/build_android_headwear_fixture.py --catalog --raw D:/CodexData/headwear-audit/all-characters/world-raw --database D:/CodexData/headwear-audit/all-characters/database.json --output D:/CodexData/headwear-audit/all-characters/catalog-integrated
```

不再需要萤石后置覆盖补丁。残留小三角边原样保留、角度限制未实现；噗切娜和大潘修复难度大暂缓；卡缪、利诺、伊冯仍未实机验收。真实字节一致性确保本轮生成/打包的代码与资源组合相同，不替代不同游戏设置/客户端版本下的新视觉验收。

## 2026-10-01：萤石残留定位与实验资源接受（历史，已被上节整合替代）

**当前结论覆盖本节的待验状态：** 用户对 boundary 资源反馈“基本上修复 残留一些小三角边 日后限制角度就看不到 故不改动”。遵照该指示冻结手机当前三份资源，不继续裁剪剩余三角，不实现角度限制。结论为当前场景基本接受、保留已知小边，不声明完全无残留；本轮衣物/阴影/退出恢复未获得逐项独立确认。下方是本轮诊断过程。

当前原样归档为 `D:/CodexData/headwear-audit/all-characters/fluorite-accepted-20261001.zip`，含三份覆盖文件、三份测试前恢复文件、反馈/摘要及运行日志。生成生产目录的规则未改，此资产特定实验分类尚未集成生产生成器；重新生成目录会覆盖它，需重新应用覆盖包。不能把该规则推广至其他角色。噗切娜和大潘暂缓，卡缪/利诺/伊冯未实机验收的状态不变。本轮不提交/合并源码。

- 用户授权截图后已保存 `D:/CodexData/headwear-audit/all-characters/fluorite-before.png`，画面存在较大灰色布料遮挡。`fluorite-live.log` 确认角色资源为 `bounda`，body01、cloth01、clothshadowless01 的三个 LOD 共九份裁剪 Mesh 均成功绑定，无绑定失败；不能把遮挡归因于目录未加载。
- 离线审计 `fluorite-geometry-analysis.md` 证明 cloth02/04/05 不含帽顶部候选，cloth01 的纯头顶部已裁剪，但 Head/Neck 混合面仍按当前保守合同保留。clothshadowless01 三个 LOD 全部为纯头几何且已裁剪。当前证据尚未区分混合面残留、代理绘制或模拟几何路径，不确认根因。
- 仅在审计目录准备了可重复生成的诊断资源：`compile_fluorite_probe.py mixed` 多裁 cloth01 的 123/84/27 个边界面；`all` 用于必要时隔离该 Renderer 的可见贡献。这两组均标记 diagnostic_only，顶点流与源完全一致、阴影仍沿用源 Mesh，手机 ARM64 fixture 校验退出码 0。未修改生产生成器、未部署至游戏资源目录。
- 用户已明确允许临时混合面对照测试。已从手机备份 cloth01 的三份旧资源至 `fluorite-before-fixtures`，退出游戏后仅替换这三份资源；手机 SHA256 与 `fluorite-probe-mixed` 的三份文件一致。游戏已重启，限时 300 秒采集 `fluorite-mixed-live.log`，等待用户进入同一残留视角再截图。混合面诊断可能损伤局部领口，它不构成正式修复或服饰验收通过；目录 `coverage.json` 仍是正式 fix2 元数据，对照文件仅为暂时诊断覆盖。
- 混合面对照用户反馈“还存在，但是少了一些”。日志证实 cloth01 三个 LOD 的 hidden_triangles 为 1747/764/199，与诊断编译器预期一致；已保存 `fluorite-mixed.png`，相机朝地面，画面两侧仍存在较大灰色布料。两次截图视角不完全一致，不按像素面积声称定量减少；用户反馈支持混合面参与遮挡，但不证明全部残留同源。
- 用户已明确允许仅针对萤石 cloth01 的全可见网格隔离测试（上衣会暂时隐藏，阴影保留、退出恢复）。`fluorite-probe-all` 已编译并通过 ARM64 fixture 校验；仅覆盖手机 cloth01 三个 LOD，设备 SHA256 与本地三份诊断资源一致，游戏已重启并限时采集 `fluorite-all-live.log`。等待用户进入残留视角。生产资源分类、runtime 与其他角色不变；帽底与上衣的明确边界仍待证据确认。
- 整体隐藏用户反馈“大面积人物与衣物消失”；`fluorite-all.png` 中两侧灰色布料不再出现，`fluorite-all-live.log` 确认 cloth01 三个 LOD 隐藏 20433/9570/3397 三角。该对照定位其可见贡献，不作为修复。随后先恢复测试前文件，三个设备 SHA256 与备份一致，重启游戏。
- 更窄的 `boundary` 诊断已生成并通过手机 ARM64 fixture 校验，继承 mixed 集合，仅额外加入三个顶点都有非零 Head 权重的剩余面（453/203/44）；合计隐藏 2200/967/243 三角。顶点流不变、源 Mesh 保留阴影、三个顶点完全无 Head 权重的面不受影响。该规则仅在审计目录中的萤石诊断编译器，不能推广为生产全角色规则；微小 Head 权重与帽/上衣边界仍需核对。已部署并重启，限时采集 `fluorite-boundary-live.log`，等待视觉对照。

## 2026-10-01：用户复核范围修订（当前）

用户复核反馈萤石帽子仍有残留，本轮唯一修复目标为该残留，须以对应角色实际网格/日志和画面复测闭合。噗切娜与大潘用户反馈支持不好，并指出毛绒外表及非标准体型；两者标记为“已知支持问题，修复难度大，暂不修复”，不纳入本轮修复，也不算实机验收通过。具体技术原因未做本轮定位，不将用户提供的外观特征替代网格层面的根因证据。

卡缪、利诺、伊冯用户明确无法测试，保持离线资产覆盖、实机未验收。此前“成功”仅对应当时操作场景，不能覆盖本次复核发现；全Characters资源目录覆盖不等于全角色视觉通过。当前手机为fix2，萤石新日志采集fluorite-live.log；修复与最终包尚待此轮定位和验收。

## 2026-10-01：全角色回归定位与子网格元数据修复（当前，fix2本轮场景通过）

fix2用户反馈“成功”，对应已请求的头饰/服饰/阴影/退出恢复检查。接受快照`D:/CodexData/headwear-audit/all-characters/fix2-accepted-device.log`记录35份不同Mesh的draw校验、GPU上传和绑定：aglina6、ardelia12、lizhiyan8、typhoea9，跨LOD1–3；binding failed与metadata format failure均0，且有切角色/退出逐字段恢复与副本销毁记录。镜像journal重复消息不重复计入35份Mesh；用户没有逐角色分别反馈，视觉通过限定本次操作场景，不扩为全部36模型通过。ardelia12份驻留LOD补丁也验证16租约能容纳当前审计最大12份需求。

正式本轮交付修复包`D:/CodexData/headwear-audit/headwear-all-fix2-20261001.zip`，47,565,797字节，SHA-256 `3ECDF73DFBE4F168B1E3B24D2FA9734FCF55B23BA575C28083BC35C574F0758C`；APK仍为`B4BF7C149492C31BE22FA0F51045D6D7287E9B687B677DF5EC758FA81C272074`，手机已装，无需重复安装。包内有834夹具、逐模型覆盖、当前README、接受日志/JSON、构建与测试记录，zip完整性通过。初版zip及fix1是已失效历史证据，不再推荐；诊断版不作为交付。未提交、合并或发布。

后续仍须确认UInt32双槽fur、rigid单骨衣物与几何全头网格的实机上传与画面，优先男管理员、tangtang及用户拥有的aurora/karin/purrche/bounda；没有的角色保留未验收。其余未操作角色、长期稳定性、外部同名Mod与正式主线集成仍未完成。

fix1现场再次失败，记录fix1-live.log。随后临时诊断明确：typhoea source MeshData保留完整draw（如body_lod1 start0/count1956/topology0/base0），但GetIndexDataSize返回0；当前Mesh.get_indexFormat managed签名存在且engine_code=yes，MeshData.GetIndexFormat未保留。不能用源CPU缓冲长度校验GPU索引宽度。最终fix2仅读取MeshData draw描述，同时通过Mesh.get_indexFormat读取真实2/4字节格式；未知枚举值拒绝，源和Upload后副本分别校验。目标buffer的完整字节回读仍由AndroidSubmitMesh负责，未读取源CPU缓冲内容。该事实覆盖下文fix1“总字节数共同确认宽度”的中间判断。

fix2新增回归覆盖无源CPU字节条件、未知格式与错误格式拒绝，最终ARM64门禁含全部834夹具再次实机执行返回0，Android assembleRelease/verifyReleaseEntryPoints及签名核验通过。DEBUG-hwmeta临时探针已全部从生产源码移除。只读复核确认元数据槽校验仍完整、没有源缓冲读取。APK `D:/CodexData/headwear-audit/betterendfield-headwear-all-fix2.apk` SHA-256 `B4BF7C149492C31BE22FA0F51045D6D7287E9B687B677DF5EC758FA81C272074`，adb install -r成功，lastUpdateTime 2026-10-01 18:24:08。新运行PID31389，启动日志versioned headwear catalog available；`all-characters/fix2-live.log`限时采集，实际绑定/恢复及用户画面反馈仍待取得。资源沿用原835文件已校验目录，不再重复安装。fix1与诊断APK仅为失败定位记录，不能当作修复成功交付。

用户反馈全角色头饰仍在，包括三角色已验收范围。重新连接PJX110并采集后，庄方仪第一人称日志`D:/CodexData/headwear-audit/all-characters/failure-live.log`显示hide_head=true、头骨绑定正常、目录adapter确实执行，但每次绑定在`Mesh.GetIndexStart unavailable`退出，源网格保留。初版v3增加了未在当前Android客户端验证过的便捷managed Mesh getter，因此单槽已验收角色也被同一前置校验阻断。日志80条同名错误包含native journal镜像；这不是80份独立网格。初版全角色APK `E57D5CC6...E979858`已确认不能用于此设备头饰验收，不再推荐该包。

修复统一复用已有AndroidMeshBuilder的`MeshDataScope::Init(mesh,true)`，仅读取GetSubMeshCount/GetSubMesh_Injected/GetIndexDataSize元数据；不读源vertex/index指针、不克隆不可读Mesh、不关闭draw校验。新增AndroidReadMeshSubmeshes是平台private adapter；共享MatchesDrawMetadata逐槽核对topology/start/count/base与完整索引字节数。fixture合同要求draw连续覆盖全部索引，故完整数量与总字节数共同校验2/4字节索引宽度。源及上传后副本使用同一入口校验，替代不可解析的Mesh.GetIndexStart/GetTopology/get_indexFormat便利接口。保留first_vertex/vertex_count/bounds由底层构建器重建的既有语义，不宣称源GPU索引CRC核验。

修复前trace断言`assert failure_log.count('Mesh.GetIndexStart unavailable') == 0`实际失败；新增单槽/双槽metadata门禁测试覆盖索引宽度错误、start/base/topology错误及槽位交换。最终ARM64测试含全部834夹具在PJX110执行返回0；Android assembleRelease/verifyReleaseEntryPoints离线构建及签名核验通过。独立只读审查未发现阻断，无源CPU缓冲读取或公共ABI扩展。

修复APK `D:/CodexData/headwear-audit/betterendfield-headwear-all-fix1.apk`，SHA-256 `6FF89FE97EE647FAC509767BC0FBF08561F40C8F97027FA633DCDCDA1EEFB75A`。已adb install -r返回Success，模块lastUpdateTime 2026-10-01 18:14:12，重启游戏PID20343；启动日志确认versioned headwear catalog available与hide_head=true。资源仍用已校验headwear-v3，无需再次推送。运行记录`all-characters/fix1-live.log`限时采集5分钟；新source MeshData draw PASS、绑定/恢复及用户头饰服饰阴影反馈尚待取得。未提交、合并或发布。

## 2026-10-01：全 Characters v3 初始覆盖与门禁（历史，接口回归已确认）

安装补充：用户随后明确要求“安装”。PJX110 b992bd53重新在线，游戏读回1.5.3/versionCode50；完全退出游戏后，`adb install -r`返回Success，模块读回3.3.22-alpha.9/versionCode30322、lastUpdateTime 2026-10-01 17:46:03。headwear-v3的834份夹具及coverage.json共835文件全部推送，设备端sha256sum逐项比对本地，835项一致、mismatches为空；证据`D:/CodexData/headwear-audit/all-characters/installation-verification.json`。安装后游戏无运行PID，尚未重新启动或取得新版本游戏内证据。下文“尚未部署”为初始交付时状态，本次补充覆盖该状态；全角色视觉验收仍待用户操作。

用户在三角色通过后要求所有角色覆盖。当前 Android manifest `2954fa80-23c1-1579-2b22-4ecfd6d70418` 有70个世界模型prefab资产，其中Characters目录36、Npc目录34。本轮仅将Characters目录作为玩家第一人称资源范围，包含独立大招形态；36个模型根不等同于36名已实装可玩角色。目标闭包1104个bundle，全部manifest资产闭包1138个bundle共466,479,247字节，缓存复用后新增只读提取402,140,058字节。1555个Renderer、1247份唯一world Mesh已导出原始流，missing_raw和missing_references均为0。Reader有一个非Mesh未知ClassID对象，原始证据保留于all-characters/raw.json，不能将其抹去后宣称Reader无告警。

唯一owner与恢复路径沿用三角色方案。生成器和运行时仅接纳模型直属Mesh_all子树，排除Shadow_Proxy与嵌套召唤骨架；Renderer与Mesh同名或Mesh只额外带一个纯数字尾段。共享同一Mesh的主Renderer实例只有骨板头部分类一致才可编译。真实实例说明原先“字符串同名、唯一Renderer”不足：大潘LOD4阴影代理同名，fur有数字后缀，坡葛兰尼包含12份嵌套召唤网格。源Mesh不克隆、不修改。已有匹配、dedicated_head或head_attached继续由原ShadowsOnly路径拥有，目录副本仍保留完整源Mesh作为shadowProxyMesh；退出、切角色/LOD按字段独立恢复。

v3是当前单一协议，拒绝v2；Java只下传游戏versionCode 50的headwear-v3目录。固定84字节`<8s19I>`头，其后依次为顶点声明、12字节draw描述(firstIndex/indexCount/baseVertex)、mesh_name、三个流、索引。支持UInt16/UInt32和1..8个原材质槽次序的连续Triangles draw，仅baseVertex=0；源及上传后副本的indexFormat/subMeshCount/GetIndexStart/GetIndexCount/GetBaseVertex/GetTopology均精确核对。weighted布局stride[16,16或8,12]；rigid布局去掉weight声明、stride[16,16或8,4]，后3个骨索引字节必须0，源m_BonesPerVertex必须1。保留源bindposes、skin metadata、顶点流、材质槽；纯头三角退化，头身边界三角保守保留。几何全头但骨板有身体骨的副本继承源bounds，完整源Mesh继续投影。

上界为262144顶点（UInt16最多65535）、1200000索引、256骨、16MiB单夹具、16份活跃租约和64MiB累计夹具。审计当前主角色最多12份待补丁Mesh横跨驻留LOD，原8份租约上限会漏掉部分LOD，所以仅增加租约数，总字节预算保留。当前最大真实夹具1,471,551字节、30773顶点。预算拒绝或恢复重试耗尽不能作为稳态通过；恢复失败最多3次并保留可能仍绑定的副本。

审计1247份Mesh的路径：252份标准目录、590份已有整体阴影隐藏、384份无纯头三角保持、4份混合fur、4份实际几何全头、1份刚性衣物、12份嵌套召唤排除。9处新增缺口为aurora/karin/purrchena/tangtang的4份UInt32双draw fur、男管理员cloth_04_lod3、bounda clothshadowless的3个LOD及purrchena fur_03_lod1_11。261份目录必需Mesh均已生成，无缺失。最终目录834份v3夹具，共77,859,062字节，其中包含已由整体隐藏路径覆盖的冗余候选；夹具数量不等于实际绑定数量。purrchena混合fur保留605个头身边界三角，残留遮挡必须视觉验收。

实际门禁：`python -m unittest discover -s tools/Camera -p test_android_headwear_fixture.py -v`在配置全量真实Android资产的环境中16项通过，覆盖9处特殊布局、三角色9份衣物、原始流/保留面/材质槽及scope。PJX110执行ARM64解析器异常输入与全834夹具返回0；随后收紧UInt16顶点界限并重新编译，所有真实夹具顶点数低于界限，手机断开后未重跑最终二进制。`:app:assembleRelease :app:verifyReleaseEntryPoints --offline --no-daemon`与apksigner核验通过；构建的Gradle提示沿用既有configuration-cache建议，不是本轮新增源码告警。新managed子网格接口、rigid及全头副本仍未在游戏内调用验证，不能将解析成功写成渲染通过。

交付`D:/CodexData/headwear-audit/headwear-all-20261001.zip`（47,547,220字节），SHA-256 `9F57832497DA5B8F71A2C010E772D4BD2BC451ADD53815BE0977D0070537C359`。APK SHA-256 `E57D5CC6C7F2AE76344E95499D7A46FD76937744C91A731FA8F84E4F0E979858`，证书与此前实验包一致。包内包含headwear-v3、README手机Termux/电脑ADB安装与采集命令、逐模型coverage-summary、完整审计矩阵、构建和Python测试记录。设备在安装前断开，尚未部署；不重置旧headwear-v2。用户需优先验收特殊角色与大潘，随后验收其拥有的其他角色及三角色回归，逐项反馈头饰/服饰/阴影/退出恢复/闪退，未拥有的角色保持未验收。

隔离worktree D:/CodexData/bem-headwear-canary、codex/headwear-android-assets分支；仍基于alpha.9，未吸收F:/bem alpha.16的陀螺仪等无关dirty改动。未提交、合并、发布；所有角色实机通过、UI/NPC模型、外部同名Mod和正式主线集成均未完成。本节覆盖此前v2实现合同；下列v2段落保留为历史验收证据。

## 2026-10-01：三角色 v2 目录实机验收通过（历史）

用户对三角色验收问题反馈“通过”。设备日志 headwear-catalog-device.log 记录庄方仪6、安洁莉娜6、佩丽卡4个不同混合Mesh的GPU上传校验与绑定，总16项，并有切角色/退出的逐项source/shadow/offscreen恢复和副本销毁。25个目录夹具均经过native解析；face/hair若属于独立头部继续交已有ShadowsOnly，因此目录数量不等于局部补丁数量。新增角色本次画面与运行记录相符；先前“待验收”段落为实现过程历史，不是当前状态。

验收记录 D:/CodexData/headwear-audit/headwear-catalog-acceptance.md，日志快照 headwear-catalog-accepted-device.log、退出记录 headwear-catalog-accepted-exit-info.txt。本次通过限定为庄方仪、安洁莉娜、佩丽卡及用户操作的世界模型场景；其他角色、UI模型、外部同名Mod和长期稳定性仍未验证。复用路径已无角色名称硬编码，新角色仍须生成其当前Android资产夹具并经过相同门禁。保持隔离分支，未合入F:/bem当前alpha.16主线。
## 2026-10-01：三角色 v2 资源目录复用（历史实现过程）

Camera 仍为第一人称隐藏与恢复的唯一 owner，Android adapter 仅构建与提交本目录里的独立网格。移除庄方仪名称、顶点数量和单副本的特殊分支；按实际 source Mesh 名在游戏外部目录 headwear-v2 匹配，只有 game versionCode 50 开启。RuntimeBootstrap 只下传目录位置，目录不遍历加载；单个夹具最多8 MiB，至多8份活跃Renderer租约。名字仅允许ASCII字母、数字、下划线，最多160字节。多个Renderer分别保留source/copy/prior_shadow/prior_offscreen与GC roots，切角色、LOD替换及退出按实际持有值逐字段恢复；失败最多3次并保留仍可能绑定的副本，预算耗尽不再发布新副本。现有独立头部ShadowsOnly路径保留owner，catalog只用于混合头身网格，其他pipeline跳过本adapter已拥有的Renderer。源网格不克隆、不改流。

v2为单一格式BEHWMESH/version2，76字节header + 5或6个16字节顶点声明 + 精确mesh_name + 3流与16位索引。限制顶点1..65535、索引6..600000且整三角面、骨骼1..256、头部面>0且小于总面数；流/总长度、CRC、几何索引和骨骼索引均校验。支持六属性stride[16,16,12]和实际安洁莉娜的五属性stride[16,8,12]；旧v1夹具不兼容。运行时源与夹具的name/counts/layout/stride/骨板数量匹配，禁止有BlendShape或多submesh网格；上传后校验bindpose矩阵与bounds再发布。源码名称/结构门禁不等于GPU原流摘要验证，外部同名同结构Mod不在验收覆盖。

离线生成器 tools/Camera/build_android_headwear_fixture.py 按真实Android prefab骨板编译；mixed三角面保守保留并报告，全头网格交给原ShadowsOnly路径。raw导出源码 tools/Camera/export_android_headwear_raw.cs。三角色目录 D:/CodexData/headwear-audit/headwear-catalog-v2 共25夹具，4,242,242字节（庄方仪6、安洁莉娜12、佩丽卡7），每项在coverage.json登记资产ID、路径、角色、布局、纯头/身体/混合面。face/hair条目若运行时属于独立头部，不启用局部补丁。89个世界候选跳过64项：33无头骨、17全头、12布局不符、2多submesh或BlendShape。世界LOD0未出现在这些角色的资源中，UI模型不进入本路线。新增安洁莉娜、佩丽卡Android目标闭包约42.5MB/43.4MB，共享已有包不重复下载。

实际验收：11个Python生成器测试通过，包括三角色7个真实衣物网格原始字节回归及目录所有权；ARM64解析器的畸形输入/两布局及全部25实际夹具，在PJX110执行返回0。Android assembleRelease/verifyReleaseEntryPoints通过，apksigner证书匹配。测试APK SHA-256 5D731DBB88CCF8FE77CEE94B547C83D85D7B26C2ED80E32B0CB7ABFAF107DA13，已adb install -r成功；资源已推送到游戏外部headwear-v2，游戏已重启，日志采集限时5分钟。用户测试问题已发出，新增角色画面结果未到，不能宣称三角色视觉或所有角色通过。

后续门禁：庄方仪回归、安洁莉娜和佩丽卡头饰/衣物/阴影、反复进出与切角色/LOD恢复。若失败以对应mesh日志定位；不扩大整cloth ShadowsOnly，不再次克隆不可读源，不进入未知布局。F:/bem当前alpha.16主线未修改，隔离alpha.9实验未提交/合并/发布。
## 2026-10-01：庄方仪 LOD1 上传版验收通过

用户在本轮明确反馈“都正常”，对应已请求的服饰完整、头饰消失、头部阴影保留、退出恢复和无闪退五项。设备日志 `D:/CodexData/headwear-audit/headwear-upload-accepted-device.log` 同时记录 16:50:53.800、16:51:55.801 的 `UploadMeshData(false) returned; post-upload skin/counts PASS` 与 `LOD1 patch assigned`，16:51:16.625 记录源 Mesh、阴影代理和 offscreen 状态恢复以及副本销毁。可见重新进入第一人称后重新绑定，用户反馈与运行日志相符。

本轮验证支持：独立 Mesh 完成构建后，在发布前显式 GPU 上传可解决此次庄方仪 LOD1 服饰不可见；禁止克隆不可读源 Mesh，仍保留来源版本、夹具和结构门禁。设备日志 bounds 为 center=(0,0.361516,0.938786)，extents=(0.534921,0.522247,0.938335)，后上传骨骼矩阵/布局门禁通过。本次退出记录未出现新的游戏原生崩溃，但不据此推论长时间稳定性。

验收范围只有游戏 1.5.3、庄方仪世界模型 LOD1。所有角色、其他 LOD、游戏更新及新版主线整合仍未完成。源码保留在隔离工作树；未修改 F:/bem 当前主线，未提交、合并或发布。用户自行换回正式版，本轮不执行回退安装。原“画面待验收”段落为历史过程，以上状态为当前结论。
## 2026-10-01：独立 Mesh 上传实验（历史过程，现已通过上述范围验收）

源码对照发现头饰 canary 缺少通用 CustomModel builder 发布前的 `UploadMeshData(false)`。本轮在隔离工作树移除 `Internal_CloneSingle`，恢复独立 Mesh 构造和提交前蒙皮字段初始化，新增 managed GPU 上传；上传后验证蒙皮字段、HasBoneWeights、顶点/索引数量、顶点声明、步长、207 个 bindpose 的全部矩阵内容和 bounds，再绑定 Renderer。没有在源 Mesh 上补 CPU 数据或改写源流。

离线运行 `:app:assembleRelease :app:verifyReleaseEntryPoints --offline --no-daemon` 通过；签名核验通过。APK SHA-256 `0693B0BAAC58B572EE231134C9AFEBE3A85ADBE171E4C11D44F67E8A8A3F0F40`。主工作树 F:/bem 已到 alpha.16，手机测试前为 alpha.15；用户明确允许暂装隔离 alpha.9 实验并自行换回正式版。先从设备备份 alpha.15（SHA-256 `0150DFF9894BCFD0D238526A68D09C4522958926A2A7E08CB35CFFC4438557E5`），`adb install -r` 返回 Success，重启游戏并开始日志 `D:/CodexData/headwear-audit/headwear-upload-device.log`。尚未取得第一人称视觉结果，不宣称服饰恢复或功能完成。

包 `D:/CodexData/headwear-audit/headwear-upload-experiment-20261001.zip`，SHA-256 `3513C30493295FFF56369D6412431EB29AF1E29195C04AE14DAA1B16CE48E872`。若完整 upload PASS 后服饰仍缺失，该假设不足以解释问题，下一步按源/副本 bounds 与 shadowProxy 派生状态区分，不能再次直接克隆不可读源网格。仅庄方仪 LOD1，不覆盖全部角色。原 F:/bem 未修改；未提交、推送或合并。
## 2026-10-01 10:48：克隆实验实机崩溃，停止使用

用户提供的 `E:/Downloads/seek.zip` 已捕获 UnityMain 原生 SIGSEGV。模块 Build ID 与本地 Release ELF 一致，首个模块栈帧准确落在 `android_headwear_canary.cpp:207` 的 `clone(source)`，尚未提交裁剪数据或绑定 Renderer。Unity 故障指令 `ldr h2, [x1]` 的 x1=0xA9720（694048），与原网格蒙皮 stream2 偏移一致，强烈指向克隆时读取缺失 CPU 数据。该克隆实验实机失败，下面的“尚未安装或运行”仅记录此前交付时状态，不能当作当前状态。

恢复使用测试前备份 `D:/CodexData/headwear-audit/headwear-clone-experiment-20261001/rollback-original-installed.apk`；SHA-256 `FBD2C4B42F885E504365366BE4EFB35FC9D0C5A8CCA87F989B2ABC2B916D4723` 已重新核对。也可先重命名游戏外部目录中的 `headwear-zhuangfy-lod1.behw`，完全结束并重启游戏，停用夹具实验。原新建 Mesh 方案服饰不可见仍未定位；两个失败实验均不能宣称组合头饰目标完成。本轮未修改源码或生成新包，设备恢复待用户操作验证。

详细证据、符号化命令和后续边界：`D:/CodexData/headwear-audit/seek-crash-20261001/analysis.md`。
## 2026-10-01：庄方仪 Android LOD1 资源夹具探针

本轮在隔离工作树 `D:\CodexData\bem-headwear-canary` 验证资源预处理路线。夹具生成器 `tools/Camera/build_android_headwear_fixture.py` 只接受游戏 1.5.3 的 `S_actor_zhuangfy_cloth_01_lod1` 精确网格布局，保留全部三个原始顶点流，把 3300 个纯头部三角形的索引退化，19060 个纯身体三角形保持原样；混合头身三角形数量为零。运行时加载上限为 2 MiB，并检查版本、网格名称、顶点/索引/骨骼数量、顶点声明、流步长和 CRC。外部文件路径由游戏外部文件目录取得，仅在游戏 versionCode 50 时传入原生侧。

PJX110 探针已验证独立 Mesh 创建、数据提交与回读。随后可视绑定版在 2026-10-01 的用户验收中显示：头饰消失、头部阴影保留、退出后恢复，但 `cloth_01` 服饰也消失，仅有其他 Renderer 的丝带可见，因此该版本失败，不能交付为修复。设备日志记录 `LOD1 patch assigned` 和 `source mesh, shadow proxy and offscreen state restored`，证明生命周期调用成功，但不能证明克隆体的渲染属性完整。

下一轮实验改为从游戏原始 Mesh 克隆独立对象，再提交裁剪后的三个流和索引；保持源 Mesh 作为阴影代理，退出时只恢复自己持有的引用。克隆前后验证原生蒙皮信息和回读数据，任何失败都不绑定 Renderer。这针对“新建 Mesh 丢失服饰渲染所需原生属性”的假设，尚需设备画面证伪。此专用夹具不能推论为“所有角色”已覆盖；其他角色必须先以相同资源和运行时证据建立夹具或形成通用且有上界的生成合同。

克隆实验交付包为 `D:\CodexData\headwear-audit\headwear-clone-experiment-20261001.zip`，SHA-256 `2693E7B5BC4163127ECDA44F5C616198391AF26BDA0FA3DDBD0B5150F7EB1FD8`，内含 APK、夹具、测试前从设备备份的回退 APK 和验收说明。该包通过 Android Release 构建、入口检查与签名校验；设备此时已从 ADB 断开，克隆实验尚未安装或运行。先前失败绑定版仍可能留在 PJX110，重启游戏前应安装新实验包或回退 APK。

离线门禁：Android `:app:assembleDebug`、`:app:assembleRelease` 和 `:app:verifyReleaseEntryPoints` 曾通过；PJX110 上运行了 ARM64 夹具格式测试，包括实际 `.behw` 文件解析。原始新建 Mesh 的可视门禁明确失败，克隆实验另行构建和验收。构建现有 Kotlin 冗余 cast、Gradle `srcDir` 和 NDK `ndk.dir` 弃用告警属于基线，本轮新增的原生文件未出现编译告警。
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
