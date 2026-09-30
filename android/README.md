# Better Endfield Android

Android ARM64 and LSPosed workspace for Better Endfield. The Android port keeps
the desktop project's module boundary: a small runtime owns IL2CPP access and
independent feature modules implement game behavior.

The packaged Android release is an LSPosed module and requires a working
LSPosed/LSP framework. Installing the APK alone does not inject it into the game.
Version 3.3.22-alpha.8 is an experimental prerelease: the in-game Compose handle
appeared on a PJX110 cold launch, while gameplay controls and touch pass-through
still need device acceptance. The camera module's motion presets, keyframes and
VMD parameters are configurable from the app, and a `.vmd` can be imported there
and played back: the user's acceptance run of 3.3.22-alpha.6 passed end to end, so
the motion stack's first device run on either platform - on Android, ahead of
Windows - was a successful one. Since then the panel's three playback keys arm a
take instead of firing on the tap, the overlay stands down completely while it
runs, and the free camera gained the steering input it never had on the phone
(see "Interface, camera and sustained dash" below). None of that has been through device acceptance yet,
and the alpha.7 and alpha.8 builds are still sitting on the same `versionCode`,
so the only way to tell them apart on a device is the `构建 3.3.22-alpha.N` line
at the top of the runtime journal.

The first feature module is `voice.character`. It combines two desktop routes:
resident `BEVCAT01` Media-ID replacement through Wwise `CSharp_SetMedia`, and
external-source replacement through `AudioAdapter.PostEventExternal`. External
paths such as `voice/chinese/.../chr_0013_aglina/...wem` are changed to the
selected character's Chinese, English, Japanese, or Korean path after mounting
the corresponding PCK. Unselected characters continue using the game's global
voice language.

## Runtime lifecycle

Loading the native runtime during `Application.attach()` is too early for this
client: `libil2cpp.so` may be mapped while its domain is still unsafe to enter.
The Xposed entry therefore hooks `UnityPlayer.nativeRender()` and loads the
runtime only after the first successful Unity frame. The one-shot render hook is
removed immediately after loading.

On the tested Android 1.4.3 client, all native targets are resolved from IL2CPP
by assembly, namespace, class, method name, and parameter count. Dobby then
patches the resolved ARM64 entry addresses; no game-version offsets are stored.
Aglina and Liino Japanese catalogs generated from the device PCK passed Media
validation and Wwise accepted all 436 routes (188 + 248). Device verification
also confirmed Japanese PCK mounting and successful external-path replacement
for both `chr_0013_aglina` and `chr_0035_liino` while the global language stayed
Chinese. The per-character duration route is active as well: device logs showed
`chr_0013_aglina_sim_talk_lv01_01` changing from 7.939479 seconds (Chinese) to
9.828813 seconds (Japanese), and `chr_0035_liino_sim_talk_lv01_01` from
11.639063 to 12.834063 seconds.

Version 3.0.1 mirrors all 15 desktop voice Hook points. In addition to the core
Media, external-source, package, duration, language, and lip-track hooks, it
includes the four context routes `VoicePlayer._PlayVoice`,
`VoiceSpeakChannelProcessor._PlayVoice`, `VoicePlayer._PlayEvent`, and
`VoiceManager._SpeakNarrative`. `VoiceContext.voiceData` and
`RuntimeVoiceData.speakerChannel` are resolved through IL2CPP metadata rather
than fixed offsets. Per request, this parity update is build-verified but was
not launched for another device test.

Version 3.1.1 keeps the Android voice module unchanged and only aligns the
version with desktop. The desktop 3.1.1 duration fix (hooking
`VoiceUtils._GetVoDurationFromVoData` instead of the `TryGetVoiceDuration`
entry) addresses an x64 IL2CPP delegate-invoke inlining path that the ARM64
build does not exhibit; the `TryGetVoiceDuration` entry hook above is still
observed working on device.

## Login model module parity

Version 3.0.1 also ports `betterendfield.model`. Android does not maintain a
second rewritten implementation: CMake compiles the desktop source file
`native/modules/model/module.cpp` directly into the Android ARM64 library. An
Android Host adapter supplies exact IL2CPP method/field/class resolution,
managed invocation and object helpers, GC handles, Dobby hooks, configuration,
and logging. Model method resolution checks assembly, namespace, class, method
name, parameter types, return type, and parameter count; no fixed game offsets
are introduced.

The complete desktop Hook set is therefore present in the Android binary:

| Group | Managed Hook point |
| --- | --- |
| Model lifecycle | `LoginSceneRoot.OnBindToManager` |
| Resource lifecycle | `StringPathHashBinary.InitMain` |
| Resource lifecycle | `StringPathHashBinary.InitInit` |
| Animation | `LoginSceneAnimCtrl.Tick` |
| Animation | `LoginSceneAnimCtrl.OnRelease` |
| Animation | `LoginSceneAnimCtrl._ChangeToState` |
| Animation | `LoginSceneAnimCtrl._ResetToA1` |
| Animation | `LoginSceneAnimCtrl._PlayA1sAndTriggerNext` |
| Animation | `LoginSceneAnimCtrl._PlayA1A2Impl` |
| Actor capture | `UnityEngine.Object.Internal_CloneSingleWithParent` |
| Logo | `LoginDecorateUI.Tick` |
| Logo | `LoginDecorateUI.OnRelease` |
| Login band | `LoginEnterGamePanel.OnValueChanged` |
| Login band | `UIMaterialAnimation.LateTick` |
| Login band | `CanvasUpdateRegistry.PerformUpdate` |

This is 10 model/animation Hooks, 2 Logo Hooks, and 3 login-band Hooks. The
local Android metadata snapshot contains every declaring class and method name.
The same-source native code and its Android Host adapter compile successfully.
Device testing then confirmed that all three contracts are ready, the original
14 Hooks install, and their runtime paths execute. The `PerformUpdate` prefix
and the neutralized sprite/texture copies (see `docs/GAME_INTERFACES.md`) were
added later for the baked-yellow login assets and are verified on desktop.

On the tested client, `chr_0013_aglina_postmodel(Clone)` loaded and replaced
`SK_actor_female(Clone)`. All four configured animation clips became resident,
the replacement PlayableGraph reached the final looping action, and the
original 11 renderers were hidden while the replacement was active. The Logo
route applied its theme to 9 Graphics, and the login-band route captured 28
Graphics and themed all 27 intended targets without a material-remap failure.
The game process remained alive without a fatal signal, and the replacement
was visually confirmed on the device.

Version 3.0.2 compiles the current desktop model source instead of the 2.3.1
baseline that 3.0.1 shipped. The desktop 3.0.1 "Main path hash recovery" in
`LoadConfiguredAssets` was removed on both platforms after an Android A/B test
on the same game client: with the recovery present, the prefab was loaded
before `InitMainPathHash`, and the clone's Animator reported `avatar=null`,
`human=false`; with the original gate restored, the replacement was verified
working again. The 3.0.1 improvements that remain (Animator enumeration, avatar
copy fallback, full rollback on failure, no actor capture during login scene
release) were verified in the same run.

## Interface, camera and sustained dash

Version 3.3.0 ports `BetterEndfield.UI`, `BetterEndfield.Camera` and
`BetterEndfield.Actions` the same way the login-model module was ported: CMake
compiles the desktop sources
(`native/modules/{ui,camera,actions}/module.cpp`) straight into the Android
ARM64 library. There is no second Android implementation of any of them, and no
game offsets are introduced - every method is still resolved by assembly,
namespace, class, name, parameter types, return type and parameter count.

`native/shared/android_compat` holds everything platform specific:

- `include/Windows.h` -> `android_win32.h`, the Win32 surface those three
  sources use. Most of it is a direct POSIX equivalent (`GetTickCount64`,
  `GetCurrentThreadId`, `GetModuleHandleW`/`GetProcAddress` over
  `libil2cpp.so`, `QueryPerformanceCounter`). `GetForegroundWindow` reports this
  process, because the library is only ever mapped inside the running game, and
  `CaptureStackBackTrace` reports no frames rather than inventing addresses that
  would be read as real GameAssembly offsets.
- `android_virtual_keys.h`, the latch behind `GetAsyncKeyState`. The desktop UI
  and camera modules decide what to do by polling virtual keys; rather than fork
  those code paths for a device with no keyboard, the in-game panel presses the
  keys. `Pulse` auto-releases after 180 ms so one tap is exactly one rising
  edge; `Press`/`Release` back the free-camera movement pad.
- `touch_input_android.cpp`, the stand-in for the desktop mouse-to-touch
  injector. An Android client already has a real Touchscreen device.

Three desktop-only facilities could not be represented honestly and carry an
explicit `#if defined(_WIN32)` at their call site instead: structured exception
handling (`__try`/`__except`, which clang/AArch64 has no equivalent for, so
those calls run unguarded here), the module's own DLL directory (Android reads
the bone-pose directory from the configuration), and the v11 AssetBundle trace
file. The desktop build of all three modules was rebuilt and is unaffected.

`DesktopModule` (`modules/desktop/desktop_module.*`) is the shared
`BE_HostApiV1` adapter all three use, so the login-model adapter's boilerplate
is not copied per module.

### What is wired where

Anything that needs a keypress on desktop is on the in-game panel; everything
else is on the settings screen.

| Feature | Desktop hotkey | Android |
| --- | --- | --- |
| Hide UID watermark | none | Enhancements page |
| Hide all HUD | `0` | switch on the page, button on the panel |
| Remove near-camera dither | none | Enhancements page |
| Free camera | `9` | switch on the page, button on the panel |
| Time freeze | `8` | switch on the page, button on the panel |
| First person | `-` | switch on the page, button on the panel |
| Free-camera movement | arrows, PageUp/PageDown | press-and-hold pad on the panel |
| Camera steering (look) | the mouse | look pad on the panel: drag to aim, with sensitivity and Y-invert on the page |
| Camera roll / FOV in-out / view reset | `Numpad7`, `Numpad9`, `Numpad1`, `Numpad3`, `Numpad5` | roll and FOV are press-and-hold, view reset is a tap |
| Motion preset play/stop | `Numpad8` | button on the panel, deferred (see below) |
| Keyframe record/play/clear | `Numpad0`, `Numpad2`, `Numpad4` | record and clear are taps, play is deferred (see below) |
| VMD replay | `Numpad6` | button on the panel, shown only once a `.vmd` has been imported, deferred (see below) |
| Escape from the hidden overlay / stop a running take | none | volume up / down / mute (relayed, never consumed) |
| Runtime journal | none | read-only list on the panel, plus "save log to file" |
| Movement speed, both FOVs, head/neck options, motion presets, keyframe/VMD parameters, look sensitivity and Y-invert, and the `.vmd` import | ini values | sliders, switches and a document picker on the page and its sub-pages |
| Sustained special dash | none | Enhancements page only |

The panel only offers a control whose module was actually configured to load. A
button that presses a key nothing reads is worse than a button that is not
there.

Each module has its own configuration string and its own environment variable
(`BETTER_ENDFIELD_UI_CONFIG`, `BETTER_ENDFIELD_CAMERA_CONFIG`,
`BETTER_ENDFIELD_ACTIONS_CONFIG`). An empty string keeps that module out of the
game process entirely, which is also what the diagnostics page reports.

The Android-only `betterendfield.enhancement` module that 3.0.2 through 3.2.2
shipped is gone: its two switches (hide UID, disable dither) are now served by
the shared desktop sources, and keeping both would have installed two hooks on
`GameObject.SetActive` from two different brokers. The old preference keys are
read once on upgrade so the user's choice carries over.

### Panel input relay and runtime journal (3.3.20)

The panel no longer reaches the native runtime over JNI. `Runtime.nativeLoad`
registers the module library under the game's classloader, while the panel's
bridge classes belong to the LSPosed module classloader, and Android refuses to
open the same `.so` path twice under different classloaders - so unresolved JNI
symbols made the Java side load a second copy of the library, which would have
installed every hook twice. That copy now returns early behind the
`BETTER_ENDFIELD_RUNTIME_STARTED` environment guard and serves JNI symbols only.
Key presses, runtime commands and status travel as plain lines in files under
the game's own files directory instead, polled by `input_relay.cpp` (10 ms for
input, 500 ms for status) and fed into the same virtual-key latch the ported
modules already poll. The status file is rewritten only when the command status
changes, and a shrinking file restarts the read offset, which is how a fresh
session truncates the stream.

The same process now journals its own load pipeline. `RuntimeLog` keeps a
150-line ring buffer and mirrors it into the remote preference `runtime_log`;
the panel displays it in-process, so a broken transport cannot lose it, and the
diagnostics page reads the same store. If the journal section is missing from
the panel entirely, the game is still running an older module build.
"保存日志到文件" writes the journal through `ACTION_CREATE_DOCUMENT` (no storage
permission) and falls back to an `ACTION_SEND` plain-text share; the result is
routed back through hooked `Activity.onActivityResult` relays, because overlay
code never receives it directly.

Two silent failures in the ported modules were closed as well. Enum constants
are read through `System.Enum.Parse` instead of the boxing path that some
clients refuse, and static fields are read with `il2cpp_field_static_get_value`
and judged per read, since a shared success flag used to veto values that had in
fact been recovered. First-person hiding gained a second path for parts the GPU
mesh patch cannot reach (non-skinned renderers, or exhausted patch attempts):
they switch to `ShadowCastingMode.ShadowsOnly` - nothing drawn in cameras,
shadows kept - read and written through the
`unity.renderer.shadow_casting_mode.get` / `.set` contracts, because Android's
raw icall table is partial. A failed mesh-patch `Init()` no longer returns
early, so the fallback covers every matched part, and the part tree is logged
once per session to diagnose renderer names the tokens miss.

### Sustained dash bone-pose banks

The sustained dash always drives its looping segment from the bone-pose banks,
not the native-only hold: `external_loop` is written as `true` whenever the
module is configured. `native/modules/actions/assets/pose_*.bin` - the same
files the desktop module reads from beside its DLL - are packaged into the APK
uncompressed and copied into the game's own files directory on first launch,
and the native side is pointed at them through
`BETTER_ENDFIELD_ACTIONS_ASSET_ROOT`.

Character names, the clean-exhaust option and the camera/interface labels use
the desktop UI's wording (洁尔佩塔, 梨诺, 隐藏机甲与光效, 启用时间冻结功能,
视野（FOV）) so the two platforms describe the same switch the same way.

### Contract evidence

Every distinctive contract these three modules need was checked against the
1.5.3 client's own `global-metadata.dat`, pulled from the installed APK:
`UIStyleByState.UpdateStyle`, `CameraUtils.get_cameraManager`,
`CameraManager.AddUICamCullingMaskConfig` / `RemoveUICamCullingMaskConfig`,
`CameraMono._ProcessDitherByPitch` / `ForceClearDither`,
`CameraManager.TailLateTick`, `PlayerController.GetMainCharacter`,
`Entity.get_modelCom`, `BaseModelComponent.GetModelGo`,
`CinemachineBrain.PushStateToUnityCamera`,
`SnapshotCameraController.SetFirstPerson` / `_ShowChar`,
`Animator.GetBoneTransform`, `CharacterAnimationComponent.StartSpDash` /
`PreLateTick` / `InterruptSpDashPerform` / `ForceStopSpDashPerform` and
`CharacterSpecialDashBrain.ShouldInterruptSpDash` are all present.

Note that `CinemachineBrain.PushStateToUnityCamera` is **absent** from the
1.4.3 snapshot under `android/research/device-1.4.3` and present in 1.5.3. Free
camera and first person rewrite the camera pose there, so those two features
need a 1.5-series client; the module reports the contract as unavailable rather
than pretending on an older one.

The world pause needs one tick the game will not always provide. Hotkey requests
are drained on the game main thread, and freezing the world stops the game's own
camera update, so the request that would thaw it stayed latched (3.3.22-alpha.3
adds the tick that cannot be silenced: `RenderPipelineManager.DoRenderLoop_Internal`,
which the engine calls for every rendered frame it hands to the Scriptable Render
Pipeline). It is an optional contract - a build without an SRP keeps the previous
ticks - and both the input thread and the pump now log what happened: a request
that goes undrained for 1.5 s is reported, and each drained request names its
pump.

## Android settings UI

The settings screen is a tree of four tabs — 首页 (overview), 体验 (interface,
camera, actions), 角色 (appearance, login display, voice) and 工具 (in-game
panel, diagnostics, journal, about) — plus four sub-pages reached from a card:
第一人称, 角色外观, 运行日志 and 关于. Both kinds of page are addressed by one
integer because the shell switches on one value; `SettingsPage.parentOf` is what
keeps a sub-page's parent tab highlighted, so "which tab am I in" stays
answerable two levels down.

It is written in Kotlin with Jetpack Compose, in an industrial palette: a
near-black ground, one white text ramp (`#F2F2EE` / `#A8A8A8` / `#777777`) and a
single yellow accent (`#F4E900`). There is no second hue anywhere — a green or
teal "success" colour would break the palette's discipline even when it is only
used once. Layers step by fill rather than by outline: page `#0A0A0A`, panel
`#121212`, row `#191919`, field `#1D1D1D`. The palette sheet names three greys;
the fourth (the row) exists because a row sits *on* a panel and the panel cannot
serve as its own row fill without flattening the card. The mix is held at roughly
76% black/grey, 18% white/grey text and 6% yellow, so the accent is spent only on
the primary action, the current selection and key state. The palette has exactly
one theme; there is no light variant, because the panel is read over a dark game
frame and next to a dark launcher. Colour tokens live in `UiTokens.kt`, and
`colors.xml` keeps only the two values the window theme needs, so there is one
source of truth rather than two copies to keep in step.

| File | Role |
|---|---|
| `UiTokens.kt`, `UiTheme.kt` | palette, spacing, radii, and the Material colour/typography/shape mapping |
| `UiComponents.kt` | the shared vocabulary: section cards, switch rows, sliders, pickers, buttons, tabs, swatches, HSV wheel |
| `SettingsState.kt` | every setting as Compose state, plus the configuration strings the native modules parse, the page tree, and the journal body |
| `SettingsShell.kt` | header, tabs, responsive shell, and the back-and-title row a sub-page gets |
| `HomePage.kt` | overview: modules the next launch will load, pending changes, current appearance and login display, shortcuts |
| `ExperiencePage.kt` | interface, camera (general / free / first-person entry), sustained dash |
| `FirstPersonPage.kt` | the first-person sub-page: basic, display, control, animation, scene behaviour |
| `SettingsPages.kt` | the characters tab: appearance entry, login display, per-character voice, and the appearance sub-page |
| `ToolPages.kt` | the tools tab: in-game panel, diagnostics, journal sub-page, about sub-page |
| `MainActivity.kt` | the settings Activity (Kotlin, edge-to-edge) |
| `BemInstallState.kt`, `BemInstallScreen.kt`, `BemInstallActivity.kt` | the BEM package manager |
| `GameOverlay.java` | Activity lifecycle, attachment, hotkey relay and journal export controller |
| `OverlaySurface.kt`, `FloatingHandle.kt`, `OverlayPanel.kt`, `OverlayControls.kt` | experimental in-game Compose handle and panel; device acceptance is pending |

The camera card is the one place the page tree does not map one-to-one onto the
preference store. "Default FOV" and the free camera's FOV are the same stored
key: the desktop module reads `field_of_view` as the free camera's baseline and
as the target its "reset view" hotkey returns to. It is therefore offered once,
under the general camera group, and the free camera group points at it. Two
sliders bound to one value would drift apart as soon as one of them was dragged.

The overview page deliberately does not claim that a change has taken effect. It
counts the writes made since the screen was opened and says they will apply on
the next launch; the published snapshot carries a generation number, but nothing
on the settings side can see which generation the running game process read, so a
comparison there would be a guess presented as a fact.

Stock Material controls (switch, slider, dropdown) are reused but their colours
are overridden, so Material cannot reintroduce its own tonal surfaces into the
palette. Press feedback is a fill step rather than a ripple, because a ripple
reads as a second accent on a surface that is allowed exactly one.

Bottom navigation on phones and a navigation rail at 720 dp and above are two
arrangements of one composition, so a setting cannot exist on one layout and be
missing from the other. The model page reads the generated Android
`character-presets.json` and `character-names.json` resources and currently
exposes 32 replacement models and 4,210 final actions, plus final-action looping,
model scale, and the desktop Logo/login-band theme switch. Saving a preset
serializes the same schema-5 model configuration consumed by the desktop module.
The voice page retains the per-character language table and Android catalog
materializer workflow.

The model page exposes the desktop loop modes: native LoopTime, forced looping,
and dual-Playable crossfade with editable loop start, loop end, and blend
duration. Logo and login-band colours can be selected from swatches, from the HSV
ring, or entered as an exact `#RRGGBB` value. A precision slider keeps the exact
stored number in its readout until the slider is actually dragged, so saving an
unrelated setting cannot silently round a first-person eye offset.

## Character rules and embedded comparison table

The Android settings page exposes every character present in the desktop
short-voice table, plus the desktop-style default rule. Each row supports
Chinese, English, Japanese, Korean, or Follow Global. The generated files under
`android/resources` are copied into the APK at build time. Model bundle hashes
come from the Android manifest, while voice route IDs may be shared with the
desktop table only after the current `AudioDialog` and device PCK indexes agree.
The current table contains 32 model presets and 132 character/language catalog
entries.

## Android catalog materialization

The desktop app already generates `BEVCAT01` files automatically when its
configuration is saved. Android now has a separate on-device materializer with
the same catalog format, route deduplication, PCK header/media parsing, VFS
decryption, target-Media validation, atomic output, and cache validation. It
runs in the target game process before the native runtime is loaded.

The route pairs are stored in the validated Android
`voice-catalog-index.json`. Only the payload lookup differs: Windows validates
the exact desktop package descriptor, while Android extracts the language VFS
partition from that descriptor, scans the target app's downloaded CHKs, and
selects the current device package that contains every required target Media
ID. This is necessary because Windows and Android PCK filenames, sizes, hashes,
and WEM payloads are not interchangeable.

Generated catalogs are private to the game at
`files/betterendfield/catalog`. They contain only the selected routes and are
rebuilt when the embedded table or device PCK identity changes. The APK does
not contain PCK, BNK, or WEM payloads. A selected language must first be
downloaded through the game.

For offline research, the existing build script still accepts
`--package-path` for an explicitly copied Android CHK:

```powershell
py -3 .\scripts\BuildVoiceCatalog.py `
  --game-path .\android\research\device-1.4.3\vfs `
  --package-path .\android\research\device-1.4.3\vfs\japanese-main.chk `
  --language Japanese `
  --character-id chr_0013_aglina `
  --output .\android\research\device-1.4.3\catalog\voice.japanese.chr_0013_aglina.becat
```

Research catalogs and source PCK/CHK files stay under ignored
`android/research` paths. They must not be embedded in the APK or distributed.

## Current limitations

- Narrative lip-sync routing is ported through `_PlayLipSyncTrack`,
  `GetLipSyncTrackPath`, and `TryLoadTrack`, including a global-language
  fallback when the selected-language track is unavailable. The Android 1.4.3 contracts
  resolve and hook successfully, but a suitable narrative scene has not yet
  been available for behavioral verification.
- Media routes are reasserted after later game `SetMedia` and `UnsetMedia`
  calls, and the active global PCK is preserved while mounting the auxiliary
  Japanese package.
- Rule changes require force-stopping and restarting the game.
- The in-game panel's controls are wired: hide-HUD, free camera, time freeze,
  first person, the free-camera movement pad and the roll / FOV / view-reset /
  motion-preset / keyframe group all press the virtual keys the ported desktop
  modules poll; VMD replay is one of them, and its button appears once a `.vmd`
  has been imported. The three keys that start a shot are deferred rather than
  sent on the tap; see "Deferred playback" below. BEM hot switching is still not
  connected.
- The three ported modules are build-verified for ARM64 and their settings and
  panel were exercised on a local emulator. The emulator has no LSPosed, so
  their in-game behaviour has not been run against the injected client; the
  contract evidence above is a metadata check, not a device test.
- The first launch after selecting a new character/language waits for its
  device-local catalog preparation before arming the native hooks. Missing or
  stale language packages are reported in LSPosed logs; external-source routing
  is still allowed to start when resident catalog generation fails.
- The current build is ARM64-only and runs in user 0.
- The target package is not hard-coded. The module attaches to whatever the
  LSPosed scope names, as long as it is that app's own main process, and then
  requires evidence before doing anything: `UnityPlayer.nativeRender` must
  exist, and every native hook is resolved by name through `libil2cpp.so`'s
  exports rather than by offset. So 官服 (`com.hypergryph.endfield`), 国际服
  (`com.gryphline.endfield.gp`) and channel builds such as the bilibili one are
  all supported without a per-variant build, and a client update does not
  invalidate the hooks unless the managed type or method names themselves
  change. Only the first two are declared in `xposed_scope`, because the
  bilibili package name has no authoritative source; tick it by hand.
- Model, animation, Logo, and login-band behavior is verified on the connected
  Android client with `chr_0013_aglina` and its default final action. Other
  character/action combinations remain data-driven but have not each been
  exercised individually.

## Requirements

- JDK 21 (the version CI pins)
- Android SDK platform 37 (`platforms;android-37.0`, matching `compileSdk = 37`) and build-tools 36.0.0
- Android NDK 27.2.12479018
- CMake 3.22.1

The settings app and the BEM package manager already use Kotlin + Jetpack Compose.
This experimental branch also composes the in-game handle and panel inside the
hooked game process. The earlier 3.3.21 attempt crashed on game entry; this
version uses an Activity-scoped Java controller, explicit Compose view owners and
an eager first-composition check, but game-process acceptance remains pending.
AGP 9 compiles Kotlin itself,
so `org.jetbrains.kotlin.android` must **not** be applied - doing so is a build
error, not a warning. Only the Compose compiler plugin
(`org.jetbrains.kotlin.plugin.compose`) is applied, and the Kotlin Gradle plugin
is raised to the same version in the root build file (2.4.20) because the Compose
compiler refuses to run against a different Kotlin compiler. The Compose BOM is
`2026.09.00`.

The repository-local toolchain is under `tools/android-toolchain`. Build without
network access from the repository root:

```powershell
.\android\gradlew.bat -p android :app:assembleDebug --offline --no-daemon
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

The release build runs R8 (`isMinifyEnabled = true`). Compose's synthetic classes
and `kotlin.Metadata` cost roughly 21 MB of dex when they survive into the APK:
the unminified Compose build measured 27.95 MB, against 6.61 MB for the
pre-Compose 3.3.20. R8 shrink + obfuscate brings the release APK to 8.60 MB.

The optimiser stage is deliberately **off** (`-dontoptimize` in
`proguard-rules.pro`). R8 merges classes that are never instantiated into one
shared holder, and that holder's `<clinit>` pools the initialisers of everything
folded into it. Measured here: `NativeCommandBridge` - a static-only class whose
`status()` is polled from inside the game process - was folded into `Lr4;`, whose
`<clinit>` instantiates `androidx.compose.ui.BiasAbsoluteAlignment` and friends.
The game process calls `status()` on its normal path, so it ran that initialiser
and crashed with no Java stack at all. Keeping the class name cannot prevent this:
the class survives, its side effects are moved elsewhere. Turning the optimiser
off costs about 1.5 MB (about 7.0 MB to 8.60 MB) and is the only reliable fix.
The optimiser remains off for this experiment so it cannot merge unrelated
game-process classes into shared Compose holders. This does not establish that
the new Compose panel can run inside the game process.

Names that are read from outside the Java type system are pinned in
`android/app/proguard-rules.pro`:

- the libxposed entry class, which the framework instantiates from
  `META-INF/xposed/java_init.list` (no root in the APK references it, so R8
  would otherwise delete it rather than rename it);
- `dev.betterendfield.android.BemInstaller`, whose name and method names *are*
  the JNI symbols exported by `libbetterendfield_installer.so`, plus the
  `conversionProgress` callback that `install_jni.cpp` resolves with
  `GetStaticMethodID`.

`:app:verifyReleaseEntryPoints` re-checks all of that against the packaged APK -
entry class, JNI symbols, the `conversionProgress` descriptor, the four manifest
components, and, as a fifth check, that the listed classes on the game path still
exist as their own classes rather than folded holders. It is finalized onto
`packageRelease`, so `:app:assembleRelease` fails instead of producing an APK
whose entry points were renamed or whose game-process classes were merged. That
fifth check distinguishes a *folded* class (mapped members, no class header in
`mapping.txt`) from a *shrunk* one (no mapping lines at all, e.g. `Hotkeys`,
whose constants javac inlined), so it does not flag a class that simply has no
code on the game path. Debug builds stay unminified and do not run it.

The `android-apk` workflow (`.github/workflows/android-build.yml`) builds that
debug APK on GitHub Actions for every push to `main` or the fix branch and every
pull request touching `android/**`, `native/**` or the workflow itself, and
uploads it as the `better-endfield-debug-apk` artifact. It pins JDK 21,
`platforms;android-37.0`, `build-tools;36.0.0`, NDK `27.2.12479018` and CMake
`3.22.1`, then fetches the Dobby v1.0.5 source into the gitignored
`tools/android-toolchain/dobby-1.0.5` and drops its `example/` subdirectory,
which needs the `DobbyInstrument` / `DobbySymbolResolver` entry points Better
Endfield disables.

Version 3.3.0 dropped the legacy API 82 build variant. libxposed API 102 is the
only framework entry point, so there are no longer two flavors and `minSdk` is
29, the version that service requires.

The settings page uses bottom navigation on phones and a navigation rail at
720 dp and above, with a bounded content width and system-bar insets. The BEM
entry is separate from login-model settings. The enhancement page owns the
overlay switch and preview; the BEM page only manages packages. The framework
entry attaches a collapsed BE icon directly to the scoped Unity application's
Activity. The host remains a plain `FrameLayout`; two bounded `ComposeView`
children draw the handle and panel. Blank host space has no touch listener, so
the game retains it. The host and Compose children share explicit lifecycle, saved-state
and view-model owners, with resume/pause/destroy driven by the existing Activity
callbacks. Tapping the icon expands the panel, dragging repositions it, and the panel
survives pause/resume/destroy: game SDKs can re-call `setContentView`, which
either strips our host from the content view or leaves it attached but buried
under the freshly added game view, so the panel re-attaches the host to the
current content view and raises it when the z-order is the only thing wrong,
re-checking once shortly after `onActivityResumed`. It does not require
SYSTEM_ALERT_WINDOW permission or a foreground service. The panel footer
renders this process's runtime journal in place, with a "save log to file"
button that writes through the system file picker. After first enabling
the option, restart the scoped game. The Handle has been observed over the
game's startup screen on PJX110 after a direct cold launch; panel interaction
and gameplay remain to be verified.
The experimental scope and remaining gates are recorded in
[`docs/ANDROID_OVERLAY_COMPOSE_EXPERIMENT_20260929.md`](../docs/ANDROID_OVERLAY_COMPOSE_EXPERIMENT_20260929.md).

After installing or updating the APK, disable and re-enable the module once in
LSPosed. This makes LSPosed register the module's protected shared-preference
store. Set the scope to the Endfield build you actually play — the two
confirmed ids are pre-selected, any other channel build has to be ticked
manually — enable the desired character rules in the module app, force-stop the
game, and launch it again. Scoping the module to unrelated apps is harmless but
pointless: they fail the Unity check and are left alone.

The debug build writes a short native diagnostic log to
`/data/user/0/<game package>/cache/betterendfield-diagnostics.log`.
The native library is linked with 16 KiB ELF LOAD-segment alignment and the APK
is also zip-aligned for Android 16 page-size compatibility.

## Signing

The Android build carries two signing identities, held at deliberately
different secrecy.

|  | debug | release |
|---|---|---|
| Keystore | `keystore/bem-debug.keystore` — tracked | `keystore/bem-release.keystore` — gitignored |
| Credentials | `keystore/debug.properties` — tracked | `keystore/release.properties` — gitignored |
| Alias | `bemdebug` | `bemrelease` |
| Certificate SHA-256 | `4E:DD:10:B9:8A:C4:A2:39:A7:8B:64:93:15:09:A4:3F:A0:F3:28:FA:77:EF:B8:D4:3D:37:E5:73:58:FA:82:6F` | `8C:D6:FD:C1:50:38:53:0E:10:16:68:AB:4B:3C:CD:00:30:AE:88:AE:37:15:3E:6D:66:AA:45:93:0C:7B:8E:FD` |

Both are PKCS12 with an RSA 2048 key under SHA256withRSA, valid until
2054-02-14. PKCS12 cannot hold a key password separate from the store password,
so one value per keystore covers both — that is a property of the format, not a
choice.

**debug.** Tracked together with its password, because neither protects
anything: a debug certificate is not a trust boundary, and having both in the
repository is what lets a fresh clone and every CI runner produce a debug APK
under one stable identity. That is the point of it — a debug APK built today
installs over the one built yesterday. Wiring is done by overriding AGP's
built-in `debug` signing config rather than adding a second one, so
`debugAndroidTest` and any future test-only build type inherit it without
further wiring.

**release.** The mirror image. This repository is public, and anyone holding
`bem-release.keystore` can sign an APK that Android accepts as an in-place
upgrade of the installed app, so the keystore stays out of it. CI never sees a
keystore secret per build; `android-release.yml` materialises both the keystore
and `keystore/release.properties` from three repository secrets before it
starts, from `base64 -w0 keystore/bem-release.keystore`:

| Secret | Value |
|---|---|
| `ANDROID_RELEASE_KEYSTORE_BASE64` | base64 of `keystore/bem-release.keystore`, unwrapped |
| `ANDROID_RELEASE_KEYSTORE_PASSWORD` | `storePassword` from `keystore/release.properties` |
| `ANDROID_RELEASE_KEY_ALIAS` | `bemrelease` |

A missing one aborts the job with the secret named, rather than producing an
unsigned APK. After the build, the workflow compares the APK's signer against
`RELEASE_CERT_SHA256` — the pinned digest above — so "the pipeline signed with
the release identity" is checked rather than asserted.

**Locally.** `:app:signingReport` prints both certificates and their stores.
`assembleDebug` needs nothing configured: `keystore/debug.properties` and its
keystore are tracked. `assembleRelease` needs the release keystore and
`keystore/release.properties` present; without them it stops at
`:app:checkReleaseSigning`, which `preReleaseBuild` depends on and which names
the missing fields and both ways of supplying them. The check exists so the
failure is a sentence rather than a keystore exception from inside packaging.

**Upgrade behaviour.** `v3.3.20`, `v3.3.21` and `v3.3.22-alpha.1` were each
signed by a different throwaway key — the CI runners regenerated AGP's debug
keystore every run. The first release cut after this change is signed by
`bemrelease`, which differs from all three, so installing over any of them still
requires an uninstall and a backup of app data. From that release onwards
upgrades install in place. Rotating the release keystore later reintroduces the
same one-time uninstall, and the pinned digest in the workflow has to move with
it.

## LSPosed scope troubleshooting

Third-party BEM packages are managed from the `角色外观` sub-page under the
characters tab.
Import validates every appearance and preserves the original package bytes;
texture conversion is optional. If textures look wrong in game, choose
`转换手机纹理` on that package's management card. A successful conversion
publishes a new generation while preserving its enabled state and selected
appearance. Failure or cancellation leaves the active package intact. Packages
without verified normal-map encoding can still be imported, but conversion
requires that metadata. Restart the game after changing packages or appearances.

If Endfield is missing from every module's scope list, open the scope page's
overflow menu, choose `Hide`, and turn off the `Games` filter. LSPosed applies
that filter globally and Android classifies Endfield as a game.

The module declares its recommended scope in `META-INF/xposed/scope.list`.

## First-person camera

The first-person camera is shared desktop source: Android compiles
`native/modules/camera/module.cpp` directly into `betterendfield_desktop_features`,
so both platforms run the same behaviour with no second implementation. The eye
anchor follows the head bone resolved from the player model. Its forward offset
is applied along the horizontal projection of the view direction while the
height offset stays world-vertical, so looking down moves the eye towards the
face instead of dragging it downwards.

`first_person_hide_head` hides the head parts of the player model. A name-token
hit (`head`, `face`, `hair`, `brow`, `eyelid`, `eyes`, `iris`, `mouth`, `horn`)
or an upstream-style role name (`s_actor_..._lodN` carrying a `_face_`, `_hair_`,
`_brow_`, `_eyebrow_`, `_iris_`, `_eyeshadow_` or `_hairshadow_` segment, and not
`shadowproxy`) collapses the whole part. A body mesh
(`s_actor_..._body_..._lodN`) is kept and only loses the triangles whose three
vertices are dominated by head/neck skin — the ring of body geometry around the
neck opening. `first_person_fill_neck_hole` requests a cap for the remaining
opening when the GPU mesh path passes its runtime contract checks.

Where the GPU mesh patch cannot run (non-skinned renderers, unavailable readback
contracts, or parts whose patch retries are exhausted), matched parts fall back to
`ShadowCastingMode.ShadowsOnly`: not drawn by the camera, shadows kept. That
property is read and written through runtime-invoke contracts, because the
Android icall table implements only part of the engine surface and the direct
icall does not exist there.
The Android client's managed readback contracts still require device verification;
the Windows client's missing methods do not establish Android availability.

Release APKs are signed by the dedicated release identity described under
[Signing](#signing). `v3.3.20`, `v3.3.21` and `v3.3.22-alpha.1` predate it and
were each signed by a throwaway CI key, so a standard Android installation still
cannot upgrade in place across those versions; back up app data before
uninstalling the old APK. The user's in-game report was made with a local debug
build; the published APK passed CI build and signature verification but has not
been retested in-game.

Optional keys, with their defaults: `first_person_eye_forward=0.03`,
`first_person_eye_height=0.05`, `first_person_near_clip=0.03`,
`first_person_extend_look_range=false` (widens the vertical look range past the
game's own pitch clamp: 1.10x up, 1.50x down, clamped to ±89 degrees),
`first_person_neck_plug_scale=1.0`. The module app writes the keys it exposes;
any key it does not write falls back to the default listed here.

The 3.3.22 app also exposes `first_person_movement=false`,
`first_person_side_look_limit=60` (0–90 degrees),
`first_person_animation_mode=0` (0 off, 1 body, 2 head, 3 realistic),
`first_person_animation_strength=0.35` (0–1),
`first_person_yield_dialogue=false`, `first_person_third_person_in_combat=false`,
`first_person_transition_seconds=0`
(0–1), and `first_person_external_head_scale=false`. Fully stop and restart
the game after saving; the native module reads a startup snapshot. The
external head-scale option also removes the head shadow. The combat option
hands camera control back to the game during combat and restores first person
after combat. The user reports the operable first-person settings passed on
Android 3.3.21 except external head scale, which was not tested. The GPU mesh
readback and cap path still requires separate contract and runtime evidence.

Since 3.3.22-alpha.5 the same app also writes the free camera's motion, keyframe
and VMD parameters: `motion_preset=orbit` (`orbit` | `dolly_zoom` | `crane` |
`truck`, and `ParseMotionPreset` also takes the short aliases `dolly` and `pan`,
both of which this screen normalises to the long form), `motion_speed=1`
(-20 to 20), `orbit_speed=20` (-180 to 180 deg/s), `motion_duration=0`
(0 means unlimited; 0 to 600 s), `motion_target_height=1.2` (-5 to 5, the
anchor's height above the controlled character), `keyframe_segment_seconds=3`
(0.2 to 60), `keyframe_loop=false`, `vmd_camera_scale=0.07` (0.001 to 10),
`vmd_camera_fov_bias=5` (-60 to 60 deg) and `vmd_camera_loop=false`. Those
ranges are exactly the ones the native module clamps to, so a value the settings
screen accepts is never rewritten on the way into the game; non-finite input
falls back to the defaults above.

Since 3.3.22-alpha.6 the import row on that page also accepts a `.vmd`. The file is
validated before anything is published - the loader's own 64 MiB ceiling and its
two header generations, `Vocaloid Motion Data 0002` and `Vocaloid Motion Data
file` - then written to the framework's remote file space as `vmd.current`, which
is the same channel the BEM package manager uses and the only one that crosses
between the two processes. Before the native library loads, the game process
copies it to `betterendfield/camera/current.vmd` under its own files directory
(skipped when a file of the stamped length is already there). `vmd_camera_file`
therefore carries `%files%/betterendfield/camera/current.vmd`, and
`RuntimeBootstrap` expands `%files%` to the game's files directory on the way into
`BETTER_ENDFIELD_CAMERA_CONFIG`: the settings app cannot write that path out, as
it neither knows which user or cloned profile the game runs under nor can write
into another UID's data directory. Clearing the import writes the key empty again,
and an empty value makes the native side take its existing "no VMD camera file is
configured" branch instead of reusing a path left over from a desktop
configuration. The panel's "VMD camera play/stop" button appears only once an
import exists, for the same reason the zoom keys are press-and-hold: a control
whose only outcome is a complaint is worse than one that is not there.

**Deferred playback.** The three keys that start a shot - the motion preset, the
keyframe replay and the VMD replay - do not fire on the tap. Tapping one of them
collapses the panel, and the key goes out a second later. The panel covers about a
third of the screen and takes roughly 100 ms to fade, and the tap lands while the
finger is still on the glass, so a key sent immediately starts the shot with the
controls in frame. Every other control still fires under the finger, which is where
an adjustment belongs.

The wait is posted on the foreground controller's own handler, never on a Compose
scope inside the panel: collapsing the panel is exactly the moment when a
composable's lifetime stops being something to rely on, and a scope cancelled with
the panel would swallow the key without leaving a line in the log. Keeping the wait
in the controller also provides the cancellation semantics - a second tap inside
the window replaces the pending key rather than queueing another, and re-expanding
the panel, pressing a volume key, backgrounding the game or tearing the surface down
all cancel it. The delay itself is a timing behaviour and has no product-level
evidence behind it; what was checked locally is that the release build and its
entry-point gate still pass.

**Standing down, and the handle coming back.** The handle goes with the panel for
the same reason the panel does: it is a 50 dp box drawn over the game, so a take
started while it is still on screen records it. With both gone there is no touch
target left, so the handle has to find its own way back, and it does - from either
of two directions. The runtime reporting that the take ended (a stop line, which
covers finishing, being switched away from, being stopped by its own hotkey and
leaving the free camera) fades it back in where it was left; so does the runtime
staying silent for `TAKEOFF_GRACE_MS` after the key went out. The panel stays
collapsed either way - it is one tap on the handle away, and expanding it is the
user's decision, not a side effect of a take ending.

That second path is not a nicety. A preset asked for while the free camera is
off, a keyframe list holding fewer than two entries and a VMD that failed to load
all return on the native side without logging either a start or a stop, so with no
grace those are stand-downs that never end - and the user has no reason to suspect
that a hidden overlay is still listening to the volume keys. Three seconds is
twelve times the latency of the 250 ms journal poll, which is the only thing
between a native line and this decision.

A take that runs forever never reports an end and so never brings the handle back:
the motion preset runs for as long as its configured duration (zero means forever),
and the keyframe and VMD loops never end at all. A volume key is the interrupt
there - it drops a key that has not fired yet, or stops the playback the runtime
reports as running and brings the handle back. Backgrounding the game and
returning restores it too, so no state here is a dead end.

Whether a take is running is taken from the runtime's own journal rather than
assumed from the key that was sent. The native module logs
`Free camera motion started`, `Free camera keyframe playback started` or
`VMD camera playback started` when a take begins, and
`Free camera playback stopped: <reason>` when it ends - for every reason, including
being switched and being stopped by its own hotkey - plus `Free camera disabled` on
the way out of the free camera. A stop line is also what ends a stand-down, and it
is honoured only for a take the overlay was told had started, so a stop belonging
to something else cannot bring the handle back early. A take that fails to start
logs no started line, so
the volume key cannot try to stop a playback that never began, and a take that has
already ended cannot be restarted by the same press. That last property is why the
start line is required instead of trusting the fired key: it makes the failure mode
"the volume key only restores the handle" instead of "the volume key re-runs a shot
the user thought was over".

Stopping re-sends the key that started the take, because the three playback hotkeys
are toggles in the native module (`StopPlayback("hotkey")` when the same kind is
already playing). It cannot be sent immediately: the virtual-key latch holds a pulse
for 180 ms and the input thread arms a hotkey on a rising edge only, so a second
press inside that window reads as one long press and toggles nothing. The controller
therefore releases the whole latch first and sends the pulse 120 ms later.

The volume keys are relayed by hooking `Activity.dispatchKeyEvent`, plus the host
activity's own override wherever that override is declared - Unity's base activity
is the kind of class that overrides it without calling super, so hooking only the
framework method would miss it, while asking only the leaf class would report "no
override" for an activity that merely extends one. The event is never consumed: the
phone's volume still changes, the relay stays purely additive, and a host that both
overrides the method and calls super simply delivers one press twice - the first call
consumes the state and the second finds nothing to do. Repeats from a held volume key
are ignored, so one press is one interruption.

**Touch steering.** The desktop free camera is aimed with the mouse: a low-level
hook accumulates `g_mouse_dx/dy`, and `StepFreeCamera` turns those into yaw and
pitch. A device has no cursor to hook, so the panel's look pad is the mouse
instead, and the whole of the rest of that path is shared desktop code.

The deltas travel as a new relay line, `m <dx> <dy>`, in the coordinates the hook
itself produces - screen pixels, x to the right and y downwards - because that is
what makes the shared mouse term the correct consumer: dragging right turns right
and dragging up looks up (the first is the desktop convention, the second the
touch one), and `mouse_invert_y` means the same thing on both platforms. Deltas
are *summed*, not queued, in two atomics in the compat layer; the camera module
folds them into `g_mouse_dx/g_mouse_dy` on every main-thread tick, under
`#if !defined(_WIN32)`. Nothing accumulates across the moment the camera comes up,
both because the fold runs whether or not the camera is armed and because
`EnterFreeCamera` already clears the mouse input.

The panel does not write a line per touch event. A drag reports at display rate
and the native side reads the relay every 10 ms, so the controller sums the deltas
and writes at most one line per 16 ms; the running total is identical, and the
relay sees tens of writes a second instead of hundreds. A drag in flight is
dropped rather than sent when the panel collapses, the game goes to the background
or the surface is torn down - a delta arriving after the control it came from is
gone would turn the camera with nothing on screen to explain it.

Sensitivity and inversion are the desktop settings, `mouse_sensitivity` (degrees
of turn per pixel dragged) and `mouse_invert_y`, now with a slider and a switch on
the Motion & Lens page. They had been written at their defaults since the Android
port began, with no UI, because there was no steering input for them to affect.
The slider's range is 0.02-0.5, narrower than the native clamp of 0.01-2.0: at the
default 0.1 a swipe across the pad turns the camera roughly a quarter turn, and at
0.5 more than a full circle.

Look input is ignored while a playback runs, which is not a new rule - the shared
`StepFreeCamera` clears the mouse input and returns while a preset, keyframe or
VMD take is in flight. That is what keeps a scripted shot reproducible no matter
what the thumb does.

The panel's zoom buttons are press-and-hold rather than a 180 ms tap, matching
what the desktop keys mean - a tap only steps the lens by about 3.6 degrees.
