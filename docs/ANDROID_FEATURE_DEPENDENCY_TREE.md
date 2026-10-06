# 安卓侧功能 → 依赖 文件树

**锚点**：本仓 `5e09128`（3.4.2 / codec 30402）↔ 上游 `3510fa7`（v3.5.1）。全部边由源码核验，非推断。

**图例**
- `[J]` Java/Kotlin（设置 app 或游戏进程内 Java）
- `[N]` 原生 C/C++（游戏进程内）
- `[ENV]` 环境变量（唯一的跨语言通道）
- `[PREF]` SharedPreferences 键
- `[T:xxx]` 所属 CMake target（决定该文件是否真进 Android 编译面）
- `[?]` 本仓缺失、需从上游补齐

---

## 0. 公共底座：所有功能共用的一条注入链

所有设置页都只是**写 prefs**；配置要生效必须走完这五段。

```
设置 app 进程 (dev.betterendfield.android)                    [J]
├── SettingsShell.kt / SettingsState.kt            页面壳与状态
├── ModuleSettings.java                            ① 写 [PREF] 并拼装配置串
│   ├─ UI_CONFIGURATION   = "ui_configuration"
│   ├─ CAMERA_CONFIGURATION = "camera_configuration"
│   ├─ ACTIONS_CONFIGURATION = "actions_configuration"
│   ├─ MODEL_CONFIGURATION = "model_configuration"
│   ├─ VOICE_RULES        = "voice_language_rules"
│   └─ MMD_SLOT_DIRECTORY / VMD_FILE_SLOT          MMD 与 VMD 路径常量
├── FrameworkSettings.java                         远端快照（RemotePreferences 载体）
├── RuntimeJournalProvider.java                    [provider] 只读日志，authority ${applicationId}.journal
└── AndroidManifest.xml                            exported 面
        │
        │  LSPosed: getRemotePreferences("module_settings")  ← 跨 UID 唯一通道
        ▼
游戏进程 (com.hypergryph.endfield)                            [J]
├── XposedEntry.java                               extends io.github.libxposed.api.XposedModule
│   ├─ onPackageReady()                            ② 装配总入口
│   ├─ RemoteFileProvider 回调                     openRemoteFile / listRemoteFiles（app → 游戏）
│   ├─ installActivityResultRelay() / installVolumeKeyRelay()   宿主回调与音量键中继
│   ├─ startRemoteCommandPoller()                  250 ms 轮询 command.next
│   └─ maybeUpdateThirdParty()
└── RuntimeBootstrap.java                          ③ 解析 → 物化资产 → 注入 → 载库
    ├─ 资产物化（读者在游戏进程，故必须先落游戏目录）
    │   ├─ ActionPoseAssets.materialize()          → actionPoseRoot
    │   ├─ HeadwearAssets.materialize()            → headwearRoot
    │   ├─ MmdSlotFiles / CameraVmdFile            → MMD 与 VMD 槽位
    │   └─ ThirdPartyRuntimeMaterializer.indexPath
    ├─ loadIntoTargetNamespace()                   ④ 把 .so 载入游戏的目标 ClassLoader
    └─ Os.setenv(...)                              [ENV] 逐模块注入
        │
        ▼
libbetterendfield_android.so                                  [N]
└── native_bridge.cpp                              JNI_OnLoad
    ├─ Configured(var) = getenv != nullptr         ⑤ 空串/未设 = 该模块完全不启动
    ├─ BetterEndfield_GetUiModuleApiV1()      → DesktopModule   native/modules/ui/module.cpp
    ├─ BetterEndfield_GetCameraModuleApiV1()  → DesktopModule   native/modules/camera/module.cpp
    ├─ BetterEndfield_GetActionsModuleApiV1() → DesktopModule   native/modules/actions/module.cpp
    ├─ BetterEndfield_GetCustomModelModuleApiV1() → CustomModelModule
    ├─ BetterEndfield_GetModuleApiV1()        → LoginModelModule / CharacterVoiceModule
    └─ third_party_host.cpp                   → 第三方模块宿主
```

**关键机制（`android/app/src/main/cpp/CMakeLists.txt:68-79`）**：三个 desktop 模块在 Windows 各是一个 DLL、导出同名 `BetterEndfield_GetModuleApiV1`；Android 侧用**逐文件宏改名**把三者塞进同一个静态库：

```cmake
set_source_files_properties(.../native/modules/ui/module.cpp
    PROPERTIES COMPILE_DEFINITIONS "BetterEndfield_GetModuleApiV1=BetterEndfield_GetUiModuleApiV1")
```

**注入契约全表**（`RuntimeBootstrap.java:137-180` ↔ 原生读取点）：

| `[ENV]` | 写入方 | 原生读取方 | 启动的模块 |
|---|---|---|---|
| `BETTER_ENDFIELD_UI_CONFIG` | `configs.ui()` | `native_bridge.cpp:130` | ui |
| `BETTER_ENDFIELD_CAMERA_CONFIG` | `configs.camera()` + `resolveFiles()` | `native_bridge.cpp:137` | camera（+ 顺带启 `ConfigureAndroidMeshBuilder` / `ConfigureHeadwearCanary`） |
| `BETTER_ENDFIELD_ACTIONS_CONFIG` | `configs.actions()` | `native_bridge.cpp:147` | actions |
| `BETTER_ENDFIELD_MODEL_CONFIG` | `configs.model()` | `native_bridge.cpp:125` | login_model |
| `BETTER_ENDFIELD_VOICE_RULES` | `configs.voice()` | `native_bridge.cpp:122` | character_voice |
| `BETTER_ENDFIELD_CUSTOM_MODEL_CONFIG` | `customModelConfig()` | `custom_model_module.cpp`、`mesh_layout_probe.cpp`、`native_bridge.cpp` | custom_model |
| `BETTER_ENDFIELD_THIRD_PARTY_INDEX` | `ThirdPartyRuntimeMaterializer.indexPath` | `native_bridge.cpp:65,94` | third_party_host |
| `BETTER_ENDFIELD_ACTIONS_ASSET_ROOT` | `actionPoseRoot` | `actions/pose_overlay.inl` | （actions 的资产根） |
| `BETTER_ENDFIELD_HEADWEAR_DIRECTORY` | `HeadwearAssets.materialize` | `custom_model/android_headwear_canary.cpp`、`camera/first_person_runtime.inc` | （头饰目录） |
| `BETTER_ENDFIELD_MMD_ROOT` | `filesDir/<MMD_SLOT_DIRECTORY>` | `camera/module.cpp:3026`、`camera/mmd_director_runtime.inc` | （MMD 作品根） |
| `BETTER_ENDFIELD_VOICE_CATALOG_ROOT` | `filesDir/betterendfield/catalog` | `character_voice_module.cpp` | （配音目录） |
| `BETTER_ENDFIELD_CUSTOM_MODEL_PROBE` | `debugResourceProbe()` | `native_bridge.cpp:108` | CustomModelResourceProbe（调试） |
| `BETTER_ENDFIELD_FP_LOOK_PROBE` | `lookProbeRequested()` | `native_bridge.cpp:118` | FirstPersonLookProbe（调试） |
| `BETTER_ENDFIELD_DIAGNOSTICS_PATH` | `lookProbeJournalPath()` / `BuildConfig.DEBUG` | `core/log.cpp` 等 | （日志落点，调用方已设则不覆） |
| `BETTER_ENDFIELD_INPUT_FILE` | `filesDir/betterendfield/input.latch` | `input_relay.cpp` | （输入中继） |
| `BETTER_ENDFIELD_STATUS_FILE` | `filesDir/betterendfield/status.txt` | `input_relay.cpp` | （命令序号回读） |
| `BETTER_ENDFIELD_NATIVE_LOG` | `filesDir/betterendfield/native.log` | `input_relay.cpp` | （原生日志） |
| `BETTER_ENDFIELD_RUNTIME_STARTED` | `native_bridge.cpp:217` 自设 | `native_bridge.cpp:201` 防双载 | （单例闸门） |

> `%files%` 占位符：`ModuleSettings.VMD_FILE_SLOT = "%files%/betterendfield/camera/current.vmd"`，由 `RuntimeBootstrap.resolveFiles()`（:298）在游戏进程内替换成真实 `getFilesDir()`。**设置 app 不能预先展开**——两侧 UID 不同。

---

## F1. 开屏模型（登录角色模型替换）

```
F1 开屏模型
├─ [J] UI        HomePage.kt
├─ [PREF]        ModuleSettings.java   model_enabled / model_character / model_action
│                model_final_loop / model_force_loop / model_crossfade
│                model_loop_start / model_loop_end / model_crossfade_duration
│                model_scale / logo_theme_enabled / logo_theme_color
│                → 拼装 model_configuration（:1589）
├─ [ENV]         BETTER_ENDFIELD_MODEL_CONFIG
├─ [N] 装配      native_bridge.cpp:125  → LoginModelModule
├─ [N] 实现      android/app/src/main/cpp/modules/login_model/login_model_module.cpp  [T:betterendfield_android]
│                └─ login_model_module.h
│                └─ 符号 GetModuleApiV1（无改名）
├─ [J] 资源索引  ModelPresetIndex.java / BemParameters.java
├─ [J] 数据      android/resources/character-presets.json / character-names.json
└─ 上游差异      MainActivity.java 页签 1；色轮选色 → ColorWheelView.java(201) + logo_theme_color
```

---

## F2. 第三方模型（BEM 包）

```
F2 第三方模型（BEM）
├─ [J] UI        BemInstallActivity.kt → BemInstallScreen.kt / BemInstallState.kt
├─ [PREF]        BemInstaller.INDEX = "installed_bem_packages"（JSON 数组，存 generation）
├─ [J] 安装引擎  BemInstaller.java
│                ├─ native convertNative / inspectNative / cancelNative  ← System.loadLibrary("betterendfield_installer")
│                ├─ referencedGenerations() / cleanLocalUnused() / applyLocalCopies()
│                └─ checkpoint()  取消点
├─ [J] 资源落地  BemInstalledResources.java / BemOptions.java
├─ [J] 纹理      AstcSupport.java（astc 转换开关）
├─ [N] JNI 实现  android/app/src/main/cpp/installer/install_jni.cpp        [T:betterendfield_installer, SHARED]
│                ├─ installer/texture_install.cpp
│                └─ .so = libbetterendfield_installer.so（独立库，非运行时库）
├─ [ENV]         BETTER_ENDFIELD_CUSTOM_MODEL_CONFIG / BETTER_ENDFIELD_HEADWEAR_DIRECTORY
├─ [N] 运行时    android/app/src/main/cpp/modules/custom_model/            [T:betterendfield_android]
│                ├─ custom_model_module.cpp   → GetCustomModelModuleApiV1（宏改名）
│                ├─ android_mesh_builder.cpp / android_mesh_builder.h
│                ├─ mesh_layout_probe.cpp / resource_probe.cpp
│                ├─ mesh_skin_metadata_adapter.cpp
│                └─ native_mesh_layout_android.cpp  world_resource_adapter.inc
│                └─▶ native/modules/custom_model/module.cpp  ← 共享 PC 翻译单元（宏改名为 GetCustomModelModuleApiV1）
│                     ├─ bem.cpp / bem.h / mod_registry.cpp / resource_policy.h
│                     ├─ texture_binding_policy.h
│                     └─ [?] 上游新增：async_loading.h(638) / model_content_identity.h /
│                         model_job_runtime.inc(1021) / model_asset_cache.inc /
│                         generic_model_matcher.{h,inc} / model_overlay_host.h /
│                         model_overlay_protocol.h / model_overlay_hotkey.h /
│                         android_lod_relations.generated.h(346)
├─ [N] 头饰      android_headwear_canary.cpp（本仓独有）
│                └─▶ native/modules/camera/first_person_runtime.inc 读 HEADWEAR_DIRECTORY
├─ [构建]        -PheadwearCatalogDir → :app:prepareHeadwearAssets → assets → 游戏目录
│                └─ android/app/build/generated/headwearAssets/**（1668 个 .behw，gitignore 忽略）
└─ 上游差异      [?] BemImportRequest.java(68) / BemImportStream.java(67) / BemHotSwitchUpdater.java(50)
                 [?] BemHotSwitchUpdate.java(32) / model_management.xml / view_bem_install.xml
                 [?] OverlaySettingsPage 的「模型管理」tab（见 F7）
```

---

## F3. 角色配音

```
F3 角色配音
├─ [J] UI        SettingsPages.kt（配音页）
├─ [PREF]        ModuleSettings.VOICE_RULES = "voice_language_rules" / voice_catalogs
├─ [ENV]         BETTER_ENDFIELD_VOICE_RULES  +  BETTER_ENDFIELD_VOICE_CATALOG_ROOT
├─ [N] 装配      native_bridge.cpp:122  → CharacterVoiceModule
├─ [N] 实现      android/app/src/main/cpp/modules/character_voice/character_voice_module.cpp  [T:betterendfield_android]
│                └─ character_voice_module.h
├─ [J] 索引      VoiceCatalogIndex.java / VoiceCatalogMaterializer.java
└─ [资源]        android/resources/voice-catalog-index.json
                 [?] 上游新增 voice-index-android-evidence.json / voice-index-merge-evidence.json
                 [?] 上游新增 manifests/voice/voice-event-media-manifest.json（460,561 行）
```

---

## F4. 增强功能 → 界面增强（隐藏 UID / 隐藏 HUD）

```
F4 界面增强
├─ [J] UI        ExperiencePage.kt（2 行开关：隐藏 UID / 隐藏 HUD）
├─ [PREF]        ui_hide_uid / ui_hide_hud  →  拼装 ui_configuration（ModuleSettings:232）
├─ [ENV]         BETTER_ENDFIELD_UI_CONFIG
├─ [N] 装配      native_bridge.cpp:130  → DesktopModule(GetUiModuleApiV1)
├─ [N] 实现      native/modules/ui/module.cpp                     [T:betterendfield_desktop_features, STATIC]
│                └─ 依赖 android_compat：
│                   ├─ native/shared/android_compat/android_win32.cpp
│                   ├─ native/shared/android_compat/android_frame.cpp
│                   ├─ native/shared/android_compat/touch_input_android.cpp
│                   └─ native/shared/android_compat/include/（<Windows.h> 替身，仅本 target 可见）
└─ 上游差异      [?] 第 3 个开关 ui_pc「使用 PC 界面布局」
                 UI: EnhancementSettingsActivity.java:139  addToggle(R.string.ui_pc, …)
                 STR: values/strings.xml:135-136  ui_pc / ui_pc_hint
                 NATIVE: native/modules/ui/module.cpp  +121/−48
                   ├─ config.pc_ui_enabled 解析（:1204）
                   ├─ g_pc_ui_enabled（:67） / g_keyboard_input_type
                   ├─ PumpInputType PC 分支（:985）
                   ├─ DetourChangeInputType 重定向（:898）
                   └─ 生效判据 pc_active = enabled && pc_ui_enabled && keyboard>=0（:1507）
                 ⚠ 本仓 PumpInputType 与基线 9b1e895 逐字相同 ⇒ 仍保留隐式 Keyboard 推送副作用
```

---

## F5. 增强功能 → 相机增强（含运镜关键帧）

```
F5 相机增强
├─ [J] UI        ExperiencePage.kt（8 开关 + 7 滑条） + CameraMotionPage.kt（运镜与关键帧）
├─ [PREF]        camera_disable_dither / camera_free_enabled / camera_pause_enabled
│                camera_first_person_enabled / camera_first_person_hide_head
│                camera_first_person_fill_neck / camera_movement_speed / camera_field_of_view
│                camera_global_fov_enabled / camera_global_fov / camera_free_follow_character
│                camera_first_person_fov / _eye_forward / _eye_height / _near_clip
│                _extend_look_range / _gyro_* (7 项)
│                → 拼装 camera_configuration（:945 / :1325）
├─ [ENV]         BETTER_ENDFIELD_CAMERA_CONFIG（经 resolveFiles 展开 %files%）
├─ [N] 装配      native_bridge.cpp:137  → DesktopModule(GetCameraModuleApiV1)
│                ├─ 同时 ConfigureAndroidMeshBuilder(runtime)
│                └─ 同时 ConfigureHeadwearCanary(runtime)
├─ [N] 实现      native/modules/camera/module.cpp                 [T:betterendfield_desktop_features, STATIC]
│                ├─ 房间自由镜头    free_camera_runtime.inc
│                ├─ 第一人称        first_person_runtime.inc（含 HEADWEAR_DIRECTORY 读取）
│                ├─ MMD 导演        mmd_director_runtime.inc + mmd_library.h + mmd_overlay_protocol.h
│                ├─ EIEM 身体动作   eiem/eiem_body.cpp + eiem_slot0..3.cpp
│                │                  └─ eiem/compat/android_*  ARM64 适配层
│                │                  └─ eiem/upstream/          上游片段
│                │                  [cmake:99] 四个 slot 加 -fno-char8_t
│                ├─ 姿态租约        native/shared/host/pose_lease.cpp → GetPoseLeaseApiV1
│                └─ 本地音乐        core/local_music_android.cpp [T:betterendfield_android]
│                                   ↑ MMD 导演调用 AndroidLocalMusicApi，缺此 TU 会 undefined symbol
├─ [N] 调试探针  android/app/src/main/cpp/modules/camera/first_person_look_probe.{cpp,h}（本仓独有）
│                └─ 经 native_bridge.cpp:118 的 BETTER_ENDFIELD_FP_LOOK_PROBE 门控装配
│                   实现：FirstPersonLookProbe::Start(Il2CppRuntime&)，只做 ReportClass / ReportFieldNames
│                   （反射报告 IL2CPP 类与字段名到 core/log.h），不改运行时状态
│                ⚠ 与 native/research/fp_look_probe/module.cpp **无关**：research/ 不在
│                  android/app/src/main/cpp/CMakeLists.txt 里，根本不进 Android 编译面
└─ 上游差异      [?] camera/module.cpp 双方重度分叉（上游 +1339 / 本仓 −1322）
                 [?] 本仓保留 21 件上游已删的第一人称文件（cap_upload / facing / motion / readback / retract / scale…）
                 [?] 上游新增 3 件：first_person_cap_upload.inc 等
                 ⚠ 第一人称区域一律保留我方
```

---

## F6. 增强功能 → 持续冲刺

```
F6 持续冲刺
├─ [J] UI        ExperiencePage.kt / ToolPages.kt
├─ [PREF]        dash_enabled / dash_liino_clean / dash_character_aglina / dash_character_liino
│                → 拼装 actions_configuration（ModuleSettings:1391）
├─ [J] 资产      ActionPoseAssets.java（物化 bundled 骨姿库）
├─ [ENV]         BETTER_ENDFIELD_ACTIONS_CONFIG  +  BETTER_ENDFIELD_ACTIONS_ASSET_ROOT
├─ [N] 装配      native_bridge.cpp:147  → DesktopModule(GetActionsModuleApiV1)
├─ [N] 实现      native/modules/actions/module.cpp                [T:betterendfield_desktop_features, STATIC]
│                └─ actions/pose_overlay.inl  读 ACTIONS_ASSET_ROOT
└─ 构建          boot 期即物化：ModuleConfigurations.needsActionPoses() ⇒ actions 非空就拷
                 （noCompress += "bin" 使 asset 长度可信，见 app/build.gradle.kts:140）
```

---

## F7. 悬浮窗（游戏内，跑在游戏进程）

```
F7 悬浮窗
├─ [J] 壳        GameOverlay.java
│                ├─ OverlayFeatures.java（record，**本仓 7 字段 / 上游 6 字段**）
│                │   panel       ← prefs overlay_enabled
│                │   hideHud     ← prefs ui_hide_hud
│                │   freeCamera  ← prefs camera_free_enabled
│                │   worldPause  ← freeCamera && camera_pause_enabled   （自由镜头子模式）
│                │   firstPerson ← prefs camera_first_person_enabled
│                │   vmdCamera   ← freeCamera && camera_vmd_imported    ★本仓独有
│                │   mmd         ← prefs "mmd_enabled"（**不受相机门控**：MMD 控件是给导演发命令，不是按键）
│                │   anyControl() = hideHud || freeCamera || firstPerson || mmd
│                ├─ OverlayViewOwners.java（宿主追踪）
│                ├─ Hotkeys.java（HIDE_HUD=0x30 '0' / FREE_CAMERA=0x39 '9' / WORLD_PAUSE=0x38 '8' / FIRST_PERSON=0xBD VK_OEM_MINUS）
│                └─ install(application, loader, …)  ← XposedEntry:287 调用
├─ [K] Compose   FloatingHandle.kt（拖拽把手）
│                OverlayPanel.kt（页签容器，入参 OverlayFeatures）
│                OverlaySurface.kt（窗口与 insets，features 可变状态 + render()）
│                OverlayControls.kt（控件，含 MotionControls(features, callbacks)）
│                OverlayTheme.kt（配色）
├─ [ENV]         BETTER_ENDFIELD_INPUT_FILE / _STATUS_FILE / _NATIVE_LOG
│                ↳ 与原生双向：面板读 status.txt，原生读 input.latch
└─ 上游 v3.5.1 新增（本仓全缺）
   ├─ [?] OverlaySettingsPage.java(293)       页签 5=游戏视野 / 6=模型管理，hasTab() 恒 true
   ├─ [?] OverlaySettingsClient.java(44)      游戏进程侧，单线程 worker，不碰 UI/渲染线程
   ├─ [?] OverlaySettingsProvider.java(92)    [provider] exported=true，只实现 call()
   │      authority = dev.betterendfield.android.overlay.settings
   ├─ [?] OverlayWritePolicy.java(93)         鉴权：owner UID / 分割 UID + 单包名
   ├─ [?] OverlayWriteAuthorization.java(50)  AtomicFile 存 64 位 hex token，仅经 RemotePreferences 发布
   ├─ [?] OverlayGeometry.java(63)            贴边几何（纯数学，可直移）
   ├─ [?] GlobalFovUpdater.java(27)           500 ms 轮询 → NativeCommandBridge.globalFov
   ├─ [?] AndroidManifest.xml                 <provider …/> 新增（本仓现有的是 RuntimeJournalProvider，不同物）
   └─ [?] res/values/overlay_settings.xml     透明度 / 自动贴边
   ⚠ JNI 侧配套：native_bridge.cpp +7（globalFov）
```

---

## F8. MMD 作品库与播放器

MMD 是**唯一跨三进程、且资产与命令分两条链**的功能。

> **关键依赖**：MMD **没有自己的模块，也没有自己的 env**。它是 `camera_configuration` 里的一段：
> `ModuleSettings.java:1294-1295` 把 `motion.toIniLines() + mmd.toIniLines()` 拼进相机串 ⇒ MMD 随
> `BETTER_ENDFIELD_CAMERA_CONFIG` 一起下发，`native/modules/camera/module.cpp` 解析 `mmd_*` 键。
> **推论**：相机模块未加载（`camera_configuration` 为空）时，MMD 一并不存在；而 `OverlayFeatures.mmd`
> 却只读 prefs 的 `mmd_enabled`（:698 单独落了一份布尔）——这两处判据的来源不同，调试时要分开看。

```
F8 MMD
├─ [J] UI        MmdPage.kt（设置 app 内）
├─ [PREF]        ModuleSettings$MmdSettings
│                ├─ mmd_enabled / mmd_loop / mmd_music_enabled / mmd_seek_seconds
│                │  mmd_music_gain / mmd_audio_offset / mmd_work
│                ├─ mmd_body_enabled / mmd_face_enabled / mmd_terrain_enabled
│                │  mmd_motion_scale / mmd_cloth_mode / mmd_motion_loop
│                └─ 派生行：mmd_overlay_enabled=true（Android 无伴随 overlay 进程，仅保门控语义）
│                            vmd_motion_file / mmd_face_file / mmd_music_file（路径带 %files%）
│                            vmd_body_enabled / vmd_eyes_enabled / vmd_face_enabled
│                            vmd_terrain_enabled / vmd_motion_scale / vmd_cloth_mode
├─ [J] 资产落地  MmdLibraryInstaller.java / MmdLibraryFiles.java / MmdSlotFiles.java
│                MmdSlotFiles 写 filesDir/betterendfield/mmd（= MMD_SLOT_DIRECTORY，:758）
│                CameraVmdFile.java（导入 VMD 流式落地 + Os.rename 原子发布）
│                MmdVmdParser.java（VMD 头校验，含 LEGACY "Vocaloid Motion Data file"）
├─ [ENV]         BETTER_ENDFIELD_MMD_ROOT = filesDir/betterendfield/mmd ← 无条件设置（空目录=有效答案）
│                ⚠ 不承载 MMD 开关，只给导演一个扫描根
│                ⚠ 真正的开关在 BETTER_ENDFIELD_CAMERA_CONFIG 的 mmd_enabled= 行里
│
├─ 命令链（遥控器 → 播放器，真播放器在游戏进程内）
│  ├─ ① [J] ModuleCommandRouter.issue()      app 侧
│  │       MAXIMUM_PAYLOAD_BYTES = 4096，generation = nextCommandGeneration()
│  │       写 next.tmp → rename → next.command
│  ├─ ② [ENV] command.next 经 openRemoteFile 过 UID 边界
│  ├─ ③ [J] XposedEntry.startRemoteCommandPoller()   250 ms 轮询，见 :476-589
│  ├─ ④ [J] NativeCommandBridge.submit()     追加到 input.latch，**非 JNI**
│  │       LINE_SEPARATOR = '\u001f'  ← 多行 payload 必须折叠，且不 trim()
│  │       look(): "m <dx> <dy>\n"（像素增量，第一人称陀螺仪走这条）
│  ├─ ⑤ [N] android/app/src/main/cpp/input_relay.cpp   [T:betterendfield_android]
│  │       按行分帧；'c ' 分支还原 \u001f；读 INPUT_FILE / STATUS_FILE / NATIVE_LOG
│  ├─ ⑥ [N] android/app/src/main/cpp/core/command_pump.cpp  [T:betterendfield_android]
│  │       世代号 <= 即拒（防旧命令重放）
│  │       └─ core/command_pump.h
│  ├─ ⑦ [N] android/app/src/main/cpp/core/panel_commands.cpp（本仓独有，13 行薄转发）
│  │       AcquirePanelCommand(cmd, value) → AcquireRuntimeCommand(cmd, value)
│  │       AcknowledgePanelCommand(status) → 回写 status
│  │       ↑ 面板命令与 runtime 命令共用同一条队列，"panel" 只是命名分层
│  └─ ⑧ [N] core/log.cpp（原生日志）
│
├─ 播放链（原生）
│  └─ native/modules/camera/mmd_director_runtime.inc（907 行）
│      ├─ 读 BETTER_ENDFIELD_MMD_ROOT
│      ├─ mmd_library.h    MmdLibrary::Scan / ToUtf8 / FromUtf8
│      ├─ mmd_overlay_protocol.h
│      ├─ camera-path.becam 回退路径 = MMD_ROOT 的父目录（camera/module.cpp:3026）
│      └─▶ core/local_music_android.cpp  作品音频
│
├─ 面板命令契约  命令名 "mmd"，7 动词 play_pause / stop / loop / seek / seek_absolute / camera / work
│                work <folder> 只切不播
├─ [PREF]        mmd_* 键名与上游逐字一致，不得改名
└─ 上游 v3.5.1 新增（本仓全缺）
   ├─ [?] MmdInstaller.java(263)              installed_mmd_works 索引，单线程 worker
   ├─ [?] MmdImportArchive.java(223)          ZIP/7z 解包 + 预算（1 GiB / 512 MiB / 4096 条目 / 深度 16）
   ├─ [?] MmdInstalledResources.java(85)      资源物化
   ├─ [?] MmdAudio.java(139)                  MediaPlayer + HandlerThread，串行 token + seek 队列
   ├─ [?] MmdLibraryActivity.java(423)        作品库页
   ├─ [?] MmdImportActivity.java(195)         导入页
   └─ [?] MmdImportPlan.java / MmdImportSession.java  上游大幅重写（本仓 65-72 行侧改动）
```

---

## F9. 第三方模块

```
F9 第三方模块
├─ [J] UI        ThirdPartyModulesActivity.kt
├─ [J] 存储      ThirdPartyModuleStore.java（selectedRuntimeIndex）
│                ThirdPartyModulePackage.java / ThirdPartyModuleActivity.java
├─ [J] 运行时    ThirdPartyRuntimeMaterializer.java  → indexPath
├─ [ENV]         BETTER_ENDFIELD_THIRD_PARTY_INDEX
├─ [N] 装配      native_bridge.cpp:65,94
│                └─ "third-party.runtime.helper" 任务 + Configured() 门控
├─ [N] 实现      native/shared/third_party_modules/third_party_host.cpp   [T:betterendfield_android]
│                └─ third_party_host.h
└─ 上游差异      [?] ThirdPartyRuntimeUpdater.java(41) / ThirdPartyModulesPage.java(70)
                 ⚠ 上游 WPF 侧 tools/ThirdPartyModules/echo 属 Windows 专有，不取
```

---

## F10. 第一人称陀螺仪（本仓独有增量）

```
F10 陀螺仪第一人称
├─ [J] UI        FirstPersonPage.kt
├─ [PREF]        camera_first_person_gyro_enabled / _horizontal / _vertical
│                _invert_horizontal / _invert_vertical / _deadzone / _smoothing
├─ [J] 传感器    GyroscopeController.java（本仓独有，294 行）
├─ [J] 路由      RuntimeBootstrap.startGyroscope(:216) / refreshGyroscope(:253)
│                XposedEntry:449  重载时刷新
├─ 注入链        GyroscopeController
│                → NativeCommandBridge.look(dx, dy)     "m <dx> <dy>\n"
│                → input.latch（文件，非 JNI）
│                → input_relay.cpp
│                → AddVirtualMouseDelta          ⚠ 本仓重写版才有此符号
│                → g_mouse_delta_x / g_mouse_delta_y
│                → DrainVirtualMouseDelta
│                → FoldPanelLookInput
│                → g_mouse_dx / dy → ApplyFirstPersonLook
│                → CameraManager.OnInput(deltaScreenPercentageX/Y)   ← 屏幕百分比，非像素
├─ [N] 落点      native/shared/android_compat/android_win32.cpp    ⚠ 禁区，只增不覆
│                native/shared/android_compat/android_virtual_keys.h ⚠ 同上
│                native/modules/camera/first_person_runtime.inc     ⚠ 保留我方
└─ 契约          unity.screen.width / height（实时分辨率）
                 PIXELS_PER_RADIAN = 1100（1 弧度 ≈ 拖一屏宽）
                 ⇒ dx_pixels / Screen.width 转百分比后再喂 OnInput
```

---

## F11. 日志与诊断

```
F11 日志与诊断
├─ [J] RuntimeLog.java（本仓独有，环形缓冲）
├─ [J] RuntimeJournalProvider.java（本仓独有）[provider] authority ${applicationId}.journal, exported=true
├─ [J] FrameworkSettings.java  远端快照读写（ModuleSettings 上游新增字段也走这里）
├─ [J] NativeCommandBridge.status() / tailNativeLog()   读 status.txt / native.log
├─ [ENV] BETTER_ENDFIELD_DIAGNOSTICS_PATH / _NATIVE_LOG / _STATUS_FILE
└─ [N] android/app/src/main/cpp/core/log.cpp        [T:betterendfield_android]
       android/app/src/main/cpp/core/log.h
       native/shared/host/hook_broker.cpp            [T:betterendfield_desktop_features 侧]
       [?] 上游新增 native/shared/host/hook_diagnostics.{cpp,h}(197+40)
       [?] 上游新增 android/app/src/main/cpp/core/runtime_status.h
           └─ class RuntimeStatus ⇒ 产出 "BE_RUNTIME_V1\n<id>=<state>\n"
       [?] 上游新增 android/app/src/main/cpp/core/jni_binding.h
           └─ BindContextLoaderNatives（走 currentThread 的 ClassLoader，不做双命名空间兜底）
       [?] 上游新增 RuntimeSnapshot.java(46)  解析上面那条状态串
```

---

## 12. 本仓全缺的上游功能（导航索引）

> **本节只是索引，依赖树在 §14。** F1–F11 是"本仓有的功能及其依赖"；U1–U9 是"上游有、本仓全无的功能及其依赖"。两者合起来才是完整的 diff 视图。

```
U1  游戏内设置双 tab（跑在游戏进程的悬浮窗里）   → §14.1
U2  跨进程设置写入通道（Android 独有）           → §14.2
U3  PC 界面布局开关（安卓侧调 ChangeInputType）  → §14.3
U4  模型覆盖层 / 包热切换                        → §14.4
U5  全局 FOV 运行时热下发                        → §14.5   ⚠ 本仓已有"配置期"路径，缺的只是运行时下发
U6  MMD 安装/导入链重整 + 运行时状态快照          → §14.6
U7  构建配置化（工程，不是功能）                  → §14.7
U8  页面层（上游 View/XML 实现，本仓须按功能名重写）→ §15 表 A2
U9  自绘控件（上游自绘，本仓用 Compose 替代）     → §15 表 A3 · 不移植
```

---

## 13. 编译面归属（决定"改了会不会真进 .so"）

```
android/app/src/main/cpp/CMakeLists.txt
├─ libbetterendfield_installer.so          [SHARED] 独立库，Java 侧 System.loadLibrary
│   └─ installer/install_jni.cpp, installer/texture_install.cpp
│
├─ betterendfield_desktop_features         [STATIC] ⚠ 无引用即被链接器整体丢弃
│   ├─ native/shared/android_compat/android_win32.cpp          ← 我方含 AddVirtualMouseDelta
│   ├─ native/shared/android_compat/android_frame.cpp
│   ├─ native/shared/android_compat/touch_input_android.cpp
│   ├─ native/modules/ui/module.cpp            → GetUiModuleApiV1
│   ├─ native/modules/camera/module.cpp        → GetCameraModuleApiV1
│   ├─ native/modules/actions/module.cpp       → GetActionsModuleApiV1
│   ├─ native/modules/camera/eiem/eiem_body.cpp + eiem_slot0..3.cpp（-fno-char8_t）
│   └─ native/shared/host/pose_lease.cpp
│   include（仅本 target）: android_compat/include, android_compat, shared/include,
│                          eiem, eiem/compat, eiem/upstream, modules/ui
│
└─ libbetterendfield_android.so            [SHARED] 运行时主库
    ├─ native_bridge.cpp          JNI_OnLoad + SubmitRuntimeCommand
    ├─ input_relay.cpp            输入中继 ★本仓独有
    ├─ core/hook_broker.cpp, core/log.cpp, core/runtime.cpp
    ├─ core/command_pump.cpp
    ├─ core/panel_commands.cpp    ★本仓独有（上游已删）
    ├─ core/local_music_android.cpp
    ├─ modules/custom_model/{resource_probe, mesh_layout_probe,
    │                        mesh_skin_metadata_adapter, custom_model_module,
    │                        android_mesh_builder, android_headwear_canary}.cpp
    ├─ native/modules/custom_model/module.cpp  → GetCustomModelModuleApiV1
    ├─ modules/custom_model/native_mesh_layout_android.cpp
    ├─ modules/camera/first_person_look_probe.cpp  ★本仓独有
    ├─ modules/character_voice/character_voice_module.cpp
    ├─ modules/desktop/desktop_module.cpp
    ├─ native/shared/third_party_modules/third_party_host.cpp
    ├─ modules/login_model/login_model_module.cpp
    └─ native/modules/model/module.cpp
```

**四条铁律（由这张图直接推出）**
1. `betterendfield_desktop_features` 是 **STATIC** ⇒ 新增文件若无人引用，链接器整体丢弃：文件在、编译绿、`.so` 逐字节不变。**导入文件与引用方必须同步做**。
2. `.inc` 不进 CMake 列表，靠 `#include` 拽入 ⇒ 改 `.inc` 的验收只能看**装机后的运行日志**，不能靠 `.so` 字符串扫描（链接器会合并相邻字面量，`grep` 单个串会误判）。
3. `android_win32.cpp` 同时被 desktop_features 与 `android_compat/include` 的 `<Windows.h>` 替身层包围 ⇒ **禁止整文件 `checkout` 覆盖**。
4. `libbetterendfield_installer.so` 与 `libbetterendfield_android.so` 是两个独立 `.so` ⇒ 改 BEM 安装链验 `installer`，改运行时验 `android`，别混。

---

## 14. 上游独有功能的依赖树（本仓全缺）

> 图例同 §0。`[J]` Java/Kotlin、`[N]` 原生、`[S]` 字符串资源、`[M]` Manifest、`[ENV]` 环境变量、`[T:x]` CMake target。规模取自 `upstream/main` = `3510fa7`(v3.5.1)；差异数字是 `git diff 5e09128 3510fa7` 的 `+上游/−本仓`。

### 14.1 U1 游戏内设置双 tab（跑在游戏进程的悬浮窗里）

```
U1 游戏内设置双 tab
├─ [J] 宿主   GameOverlay.java（两侧同名，上游 tab 常量 5→7：+GLOBAL_FOV=5、+MODELS=6）
│              rebuild() 的 case GLOBAL_FOV / case MODELS 都 new OverlaySettingsPage(models=…)
│              hasTab() 对这两个**恒 true**（不像其余五项受 OverlayFeatures 门控）
├─ [J] 页面   OverlaySettingsPage.java(293)   ← 上游全新独有
│              models 半：热切换状态行 / 角色筛选弹窗 / 关闭全部 / 每包卡片（启用开关文案按热切换分「下次加载生效|重启后生效」）
│                         + 展开区（外观 choices / 组件 choices / BEM 1.3 形态滑条 / 「作者默认值」复位）
│              FOV 半：全局 FOV 开关 + 5–150 滑条 + 四态标签（正常|重启后生效|运行时未加载|预览）
├─ [J] 传输   OverlaySettingsClient.java(44)  ← 上游全新独有，见 §14.2
├─ [M] 入口   AndroidManifest.xml 新增 <provider .OverlaySettingsProvider
│              android:authorities="dev.betterendfield.android.overlay.settings" exported="true">
├─ [S] 文案   res/values/overlay_settings.xml、res/values/model_management.xml（均新文件，含 values-en/ja/ko/zh-rTW）
└─ ⚠ 本仓必须**新增** provider，不是替换：本仓那个 exported 的 ContentProvider 是 .RuntimeJournalProvider
      （authority "${applicationId}.journal"，只读日志）—— 与上游那个只实现 call() 的写通道不是同一个东西
```

### 14.2 U2 跨进程设置写入通道（Android 独有，Windows 无对应物）

```
U2 跨进程设置写入通道（方向：游戏进程 → 模块 app 进程）
├─ 发  OverlaySettingsClient.send(…)          单线程 worker，回主线程才回调（明确不碰游戏 UI/渲染线程）
├─ 过  ContentResolver.call("content://dev.betterendfield.android.overlay.settings")
│      请求体 = { method, expected(revision), patch }
│      method ∈ { read_models, edit_models, disable_models, read_fov, edit_fov }
│      patch 白名单字段 + ≤16 KiB；disable_models 只允许带 expected
├─ 收  OverlaySettingsProvider.java(92)       只实现 call()；query/insert/update/delete 全抛 SecurityException
├─ 鉴  OverlayWritePolicy + OverlayWriteAuthorization
│      ├─ 放行① 调用方 UID == owner UID
│      ├─ 放行② 分割 UID：uid % 100000 >= 10000 && uid / 100000 == owner / 100000，且该 UID 只映射一个包名
│      ├─ 两条都须携带 owner 的 64 位十六进制 token，MessageDigest.isEqual 常量时间比对
│      └─ token ← FrameworkSettings 写 getRemotePreferences("module_settings") 快照时注入
│              owner 侧用 AtomicFile 落在 files/overlay-write-authorization
│              ⚠ 不进 UI 预置表、不进 provider 响应体
├─ 锁  revision = 状态串 SHA-256（乐观锁），不匹配 → 「设置已被更新，请重新选择」
├─ 写  真实写入前先 Binder.clearCallingIdentity()
└─ 落  BemInstaller（模型） / ModuleSettings.saveGlobalFov（FOV）
       └─ saveGlobalFov(ctx, Boolean, Double)：全量读-改-写 + 失败逐键回滚 + 抛 IOException
```

### 14.3 U3 PC 界面布局开关

```
U3 PC 界面布局（安卓侧主动调游戏 DeviceInfo.ChangeInputType；Windows 端本就是桌面布局，无此路径）
├─ [S] 文案   res/values/strings.xml:135   ui_pc / ui_pc_hint
├─ [J] 开关   EnhancementSettingsActivity.java:139（界面增强卡片第 3 个开关，紧跟 隐藏UID / 隐藏HUD）
├─ [J] 键     ModuleSettings.PC_UI_ENABLED = "pc_ui_enabled" + setPcUiEnabled()
│              setInterfaceSettings() 重写 UI 串时**显式**写 mobile_ui_enabled=false、platform_spoof_enabled=false
├─ [ENV]      BETTER_ENDFIELD_UI_CONFIG  ← RuntimeBootstrap:139 Os.setenv
├─ [T:desktop] android/app/src/main/cpp/modules/desktop/desktop_module.cpp → 共享 UI ConfigurationChanged
│              └─ g_desired_generation++ ⇒ UI 线程下一次 PumpInputType()
└─ [N] 生效   native/modules/ui/module.cpp（上游 +121/−48，**在安卓编译面内**）
               ├─ g_pc_ui_enabled / g_keyboard_input_type（Keyboard=0 由 IL2CPP 元数据解析，解析失败整个 PC 模式不启用）
               ├─ pc_active = config.enabled && config.pc_ui_enabled && g_keyboard_input_type >= 0
               ├─ PumpInputType() PC 分支 + 250 ms 漂移重检
               └─ DetourChangeInputType（挂游戏自身方法，PC 模式期间反向重定向；关闭时恢复进入前的 Touch/Controller）
     ⚠ 本仓现状（见 F4）：PC 分支与开关都缺，只剩旧的「隐式推送」——
        active = g_mobile_ui_enabled（本仓 Java 恒写 false）⇒ target = restore >= 0 ? restore : 0 = Keyboard(0)
        ⇒ 手机在 Touch(1) 时会被推成 PC/桌面布局。这是已知缺陷，不是功能。
```

### 14.4 U4 模型覆盖层 / 包热切换

```
U4 模型管理 / 热切换（游戏内模型覆盖层）
├─ [J] 开关面  OverlaySettingsPage 的 models 半（见 §14.1）
├─ [J] 引擎   BemHotSwitchUpdate.java / BemHotSwitchUpdater.java(50)     ← 两个文件本仓都没有
│              BemImportRequest.java(68) / BemImportStream.java(67) / ThirdPartyRuntimeUpdater.java(41)
├─ [N] 主体   native/modules/custom_model/module.cpp（上游 +1451/−262）★在安卓编译面内
│    │  平台守卫（module.cpp:14–24，逐字核过）：
│    │    #if defined(__ANDROID__) → android_mesh_builder.h + android_lod_relations.generated.h
│    │    #if defined(_WIN32)      → <Windows.h> + model_overlay_host.h + runtime_ini_win32.h
│    │    #else                    → platform_compat.h
│    ├─ async_loading.h(638)                  异步/低峰值加载      ★Android 面内 · 可直取
│    ├─ model_content_identity.h              内容同一性          ★Android 面内 · 可直取
│    ├─ runtime_ini.h(111)                    INI 读写（跨平台）   ★Android 面内 · 可直取
│    ├─ generic_model_matcher.inc(63)         通用匹配 .inc 侧      ★Android 面内
│    │    └─ generic_model_matcher.h 两侧**都有**（上游 +57 行）⇒ 补 .inc + 回填头部那 57 行
│    ├─ model_asset_cache.inc(58)             弱观测缓存（无空闲保留）
│    ├─ model_job_runtime.inc(1021)           作业泵（module.cpp:3589 #include）
│    └─ android_lod_relations.generated.h(346) Android LOD 关系表（module.cpp:17 include，需前置 <span>+matcher）
└─ [N] Windows 专有（只记不移；依赖 minhook / Win32 / .rc；module.cpp:21–23 那两支守卫已把它们挡在外面）
     ├─ model_overlay_host.h(70) / model_overlay_hotkey.h / model_overlay_protocol.h（整文件裹 #if defined(_WIN32)）
     ├─ runtime_ini_win32.h(73)
     ├─ custom_model/overlay/{main.cpp(294), model_library.h(169), win32_overlay_window.h(144), *.manifest, *.rc}
     └─ camera/overlay/{main.cpp(801), mmd_overlay.manifest, mmd_overlay.rc}（上游 MMD 独立悬浮窗宿主，Win32）
```

**本仓 `native/modules/custom_model/` 已经有的**（不用重取）：`generic_model_matcher.h`、`native_mesh_layout.{h,cpp}`、`resource_policy.h`、`texture_binding_policy.h`、`mod_registry.{h,cpp}`、`bem.{h,cpp}`、`bem_rewrite.h`、`platform_compat.h`。⇒ U4 实际要补的是上表 `★` 那 7 个文件 + `module.cpp` 的集成点。

### 14.5 U5 全局 FOV 运行时热下发

这一步要特别说明：**本仓并不缺「全局 FOV」，缺的是「免重启」**。两侧同名符号几乎都在，差别在生效时机。

```
U5 全局 FOV 运行时热下发
├─ 本仓**已有**（配置期路径，走相机配置串）
│   ├─ [J] ExperiencePage.kt:89–100      开关 + 滑条 → prefs camera_global_fov_enabled / camera_global_fov
│   ├─ [N] camera/module.cpp:121–122      config 字段 global_fov_enabled / global_fov
│   ├─ [N] camera/module.cpp:265–266      g_global_fov_enabled / g_global_fov（两个 atomic）
│   ├─ [N] camera/module.cpp:2533–2534    解析 global_fov_enabled / global_fov 键（来自 BETTER_ENDFIELD_CAMERA_CONFIG）
│   ├─ [N] camera/module.cpp:2615–2616    clamp 5.0..150.0（非有限值回落 60）
│   └─ [N] free_camera_runtime.inc:767+   ScopedGlobalFovState（Cinemachine 钩子内应用）
├─ 上游 v3.5.1 **新增**（运行时路径，免重启）
│   ├─ [J] GlobalFovUpdater.java          游戏进程内独立线程 "BetterEndfield-GlobalFov"，500 ms 轮询权威 prefs
│   │                                     仅在 enabled+value 串变化时下发（避免重复 JNI）
│   ├─ [J] NativeCommandBridge.globalFov(enabled, value)   新增 JNI 声明
│   ├─ [N] android/app/src/main/cpp/native_bridge.cpp     :34 前向声明 betterendfield::AndroidGlobalFov(bool,float)
│   │                                                     :300–301 JNI 实现   :337 BE_NATIVE(globalFov, "(ZF)Z")
│   ├─ [N] native/modules/camera/module.cpp:3147          AndroidGlobalFov()（#if defined(__ANDROID__)）
│   │       只校验 g_android_global_fov_ready + isfinite + 5..150，然后直接 store 两个 atomic
│   │       注释原文：Cinemachine hook consumes atomics on Unity thread; JNI does not touch Unity state.
│   ├─ [N] native/modules/camera/module.cpp:290           g_android_global_fov_ready（新增就绪标志）
│   └─ [J] ModuleSettings.saveGlobalFov + OverlaySettingsProvider 的 read_fov / edit_fov（见 §14.1、§14.2）
└─ ⚠ 门控差异：必须**同时**改，否则加了 JNI 也无效
      本仓 module.cpp:2965  g_global_fov_enabled = config.enabled && config.global_fov_enabled
                                                 && g_push_state_hook_ready && g_state_layout.ready
                                                 && projection->resolved && brain_object->resolved
      上游 AndroidGlobalFov 走的是 g_android_global_fov_ready，**绕过**上面这串
      ⇒ 移植时必须新增该标志并在钩子就绪时置位，否则「配置期门控」与「运行时下发」两套判据会打架
```

### 14.6 U6 MMD 安装/导入链重整 + 运行时状态快照

```
U6 MMD 安装/导入链重整 + 运行时状态快照
├─ [J] 引擎   MmdInstaller.java(263) / MmdImportArchive.java(223) / MmdInstalledResources.java(85) / MmdAudio.java(139)
├─ [J] 页面   MmdImportActivity.java(195) / MmdLibraryActivity.java(423)   ← View/XML，本仓须按功能名重写
├─ [J] 快照   RuntimeSnapshot.java(46)   解析 "BE_RUNTIME_V1\n<id>=<state>\n"，上限 8192 字节
│              上游的模块状态徽章、「模块将加载|不会加载」文案都由它驱动
├─ [N] 新增   android/app/src/main/cpp/core/runtime_status.h    产出上面那条串
├─ [N] 新增   android/app/src/main/cpp/core/jni_binding.h       BindContextLoaderNatives()
│              走 currentThread 的 ClassLoader；**明确不做**双命名空间兜底
│              native_bridge.cpp:2–3 include、:344 调用
└─ ⚠ 上游**删除** android/app/src/main/cpp/core/panel_commands.cpp 与 input_relay.cpp
      本仓这两个文件是 MMD 命令分帧链（U+001F 折叠/还原）的载体 ⇒ **不可跟删**（见 F8）
```

### 14.7 U7 构建配置化（工程，不是功能）

```
U7 构建配置化
├─ android/workspace.gradle.kts(24)      工作区根与工具链路径
├─ android/settings.gradle.kts(+63)      产物改到 build/android/gradle/… 与 releases/android/app-<version>/
├─ android/app/build.gradle.kts(+140)    signingConfigs.persistentRelease（pin 3.5.0 已发布证书）
│                                         + buildStagingDirectory + noCompress += "bin"
├─ android/tools/CheckOverlayHost.ps1(35)  悬浮窗宿主自检（Win32，不移植）
└─ ⚠ 上游删了 android/keystore/{bem-debug.keystore, debug.properties}
      本仓那是**本机唯一签名材料**（本机无 release 库，release 只能走 CI）⇒ 保留
      另：android/gradlew 两侧只是权限位差异（100755 → 100644）
```

---

## 15. 差异文件归属总表（双向）

口径：`git diff 5e09128 3510fa7`。`android/app/src/main/java/dev/betterendfield/android/` 两侧**各 59 个类** ⇒ 上游独有 **32**、本仓独有 **32**、同名 **27**。

### 表 A — 上游独有 32 个 Java 类 → 归属功能与处置

| 档 | 类（行数） | 归属 | 处置 |
|---|---|---|---|
| **A1 引擎/逻辑**（无 View 依赖） | `OverlaySettingsClient`(44) `OverlaySettingsProvider`(92) `OverlayWritePolicy` `OverlayWriteAuthorization` | U2 | 可近乎直移 |
| | `GlobalFovUpdater` | U5 | 可近乎直移 |
| | `OverlayGeometry` | F7 悬浮窗 | 可近乎直移 |
| | `BemHotSwitchUpdate` `BemHotSwitchUpdater`(50) `BemImportRequest`(68) `BemImportStream`(67) | U4 | 可近乎直移 |
| | `ThirdPartyRuntimeUpdater`(41) | F9 第三方模块 | 可近乎直移 |
| | `MmdInstaller`(263) `MmdImportArchive`(223) `MmdInstalledResources`(85) `MmdAudio`(139) | U6 | 可近乎直移 |
| | `RuntimeSnapshot`(46) | U6 | 可近乎直移 |
| **A2 页面/Activity** | `MainActivity`(814) `BemInstallPage`(503) `MmdLibraryActivity`(423) `EnhancementSettingsActivity`(346) `OverlaySettingsPage`(293) `MmdImportActivity`(195) `AboutPage`(180) `BemInstallActivity`(44) `FrameworkServiceWait`(30) | U1/U3/U4/U6/U8 | **必须重写**（上游 View/XML，本仓 Compose） |
| **A3 自绘控件** | `ColorWheelView` `ValueSlider` `SponsorDialog` `SettingRow` `ControlIcon` `SectionCard` `ThirdPartyModulesPage` | U8 | **不移植**，视觉并入本仓 `UiComponents.kt` |

### 表 B — 上游独有 21 个 native 文件 → 是否进安卓编译面

| 归属 | 文件 | 结论 |
|---|---|---|
| **安卓编译面内（7）★需移植** | `custom_model/async_loading.h`(638) `model_content_identity.h` `runtime_ini.h`(111) `generic_model_matcher.inc`(63) `model_asset_cache.inc`(58) `model_job_runtime.inc`(1021) `android_lod_relations.generated.h`(346) | U4 的核心缺口 |
| **Windows 专有（14）只记不移** | `shared/host/hook_diagnostics.{cpp,h}`、`custom_model/{model_overlay_host.h, model_overlay_hotkey.h, model_overlay_protocol.h, runtime_ini_win32.h}`、`custom_model/overlay/{main.cpp, model_library.h, win32_overlay_window.h, model_overlay.manifest, model_overlay.rc}`、`camera/overlay/{main.cpp, mmd_overlay.manifest, mmd_overlay.rc}` | 依赖 `<Windows.h>` / minhook / `.rc` |

**`hook_diagnostics` 这条尤其别碰**：`hook_diagnostics.h` 头两行就是 `#include <Windows.h>`，`.cpp` 又拉 minhook 的 `hde64.h`；它只挂在 `native/CMakeLists.txt`（该文件第 5 行 `if(NOT WIN32)` 直接挡掉）的 `BetterEndfield.Host` 目标里。更关键的是上游 `native/shared/host/hook_broker.h:5` **无守卫**地 `#include "hook_diagnostics.h"` ⇒ **上游那版 `hook_broker.h` 本身就不能用于 Android**。本仓的 `hook_broker.h` 已剥掉这行（本仓该文件 include 只剩 `BetterEndfield/ModuleApi.h` + 标准库），这是**必要的分叉，不要去「补齐」上游那版**。

### 表 C — 同名 27 类：9 个逐字相同，6 个重度分叉

逐字相同（零工作量）：`ActionPoseAssets` `AstcSupport` `BemParameters` `ModelPresetIndex` `ModuleApplication` `ThirdPartyModuleActivity` `ThirdPartyModulePackage` `VoiceCatalogIndex` `VoiceCatalogMaterializer`

重度分叉（**判页面层不可合并**）：`ModuleSettings` `GameOverlay` `XposedEntry` `FrameworkSettings` `RuntimeBootstrap` `NativeCommandBridge`
完整 27 项差异规模见 `docs/ANDROID_DELTA_VS_UPSTREAM_3510FA7.md`。

---

## 16. 三份文档的分工

| 问题 | 看哪份 |
|---|---|
| 本仓有哪些功能、各自依赖哪些文件 | **本文档 F1–F11** |
| 上游有哪些功能是本仓没有的、各自依赖哪些文件 | **本文档 §14（U1–U7）+ §15** |
| 到底有哪些文件不一样、数量与规模是多少 | `docs/ANDROID_DELTA_VS_UPSTREAM_3510FA7.md` |
| 改一个文件到底会不会进 `.so` | **本文档 §13** |
| 上游领先提交的逐条功能面清单 + 量化 + 建议优先级 | `docs/ANDROID_UNPORTED_FEATURES_20261005.md` |
| 两份未移植清点的口径核对（4 处结论冲突及定论） | `ANDROID_UNPORTED_FEATURES_20261005.md` **§9** |

> ⚠ 本文档 F1–F11 是「本仓有什么」，§14–§15 是「上游有什么而本仓没有」；再叠加 `ANDROID_UNPORTED_FEATURES_20261005.md` 的逐提交与量化视角，三者合起来才是完整未移植视图。**单看任一份都会漏**（本文档漏 `_TickStatePerformInterrupt`/Discord/下载镜像/发布体系；该文档漏 PC 界面开关/`PumpInputType` 隐式推送缺陷/运行时状态快照子系统）。
