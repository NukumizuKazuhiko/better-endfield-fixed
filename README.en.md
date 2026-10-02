# Better Endfield

[English](README.en.md) · [简体中文](README.md)

Better Endfield is a modular extension project for *Arknights: Endfield*, with a Windows controller and an Android/LSPosed module. Features are implemented by separate modules, and the Android build reuses the relevant native module sources. Check the [changelog](CHANGELOG.md) and each release note for platform support and validation status.

> This is an independently maintained derivative of [Dr-hydra/Better-Endfield](https://github.com/Dr-hydra/Better-Endfield). It is not an official release of the upstream project or the game publisher.

## Platforms and features

| Platform | Main capabilities | Entry point |
| --- | --- | --- |
| Windows 10/11 x64 | Models and BEM appearances, character voice, OmniMix music, combat statistics, display enhancements, free camera and touch UI | WinUI controller; built-in injector by default |
| Android ARM64 | Models and BEM appearances, character voice, free camera, first person, camera motion, gyroscope and in-game control panel | LSPosed module and companion settings app |

Both platforms use the same standard `.bem` package format. See the [BEM creator guide](docs/BEM_CREATOR_GUIDE.md) for format and conversion limits. Validation on one platform does not establish validation on the other.

## Get started

### Windows

1. Use Windows 10/11 x64 and a compatible game client. Build from source with the commands below, or follow the instructions for a published package.
2. Select the game path and features in the Better Endfield controller. The default injector does not place loader files in the game directory.
3. Start the game from the controller. The optional XInput autostart mode is installed and removed from the controller settings.

### Android

1. Download the APK from the [Android 3.4.1 release](https://github.com/NukumizuKazuhiko/better-endfield-fixed/releases/tag/v3.4.1), and check its compatibility notes and checksum.
2. Install it, enable the module in LSPosed, and select the Endfield package you play.
3. Configure features in the companion app, fully stop the game, then start it again.

See the [Android guide](android/README.md) for setup, compatibility and troubleshooting. Bundled headwear assets target the native Android game at versionCode 50; other client versions need separate verification.

## Build from source

The Windows build requires Visual Studio 2022 with the C++ workload, .NET 9 SDK and CMake. Packaging an installer also requires Inno Setup 6. From the repository root:

```powershell
pwsh -File .\scripts\BuildBetterEndfield.ps1
pwsh -File .\scripts\BuildInstaller.ps1
```

The Android build requires JDK 21, Android SDK 37, NDK 27.2.12479018, CMake 3.22.1, Dobby v1.0.5 and the complete headwear catalog. Follow the [Android build guide](android/README.md#从源码构建); the build fails when that catalog is missing.

## Documentation

| Document | Contents |
| --- | --- |
| [Android guide](android/README.md) | Installation, LSPosed scope, builds and troubleshooting |
| [Technical reference](docs/TECHNICAL_DETAILS.md) | Module boundaries, loaders, configuration, logs and build details (Chinese) |
| [Documentation index](docs/README.md) | Current topic guides and historical records (Chinese) |
| [Changelog](CHANGELOG.md) | Releases and validation boundaries |
| [BEM creator guide](docs/BEM_CREATOR_GUIDE.md) | Package authoring and validation |

For bug reports, use [Issues](https://github.com/NukumizuKazuhiko/better-endfield-fixed/issues) and include the platform, game and module versions, and reproduction steps. The Android app can export its runtime log; review it for personal information before sharing.

## Origin and license

The project is licensed under [AGPL-3.0-only](LICENSE). See [third-party notices](THIRD_PARTY_NOTICES.md) for upstream and dependency attribution. The project is not affiliated with the game publisher. Back up your configuration before use, and check compatibility after game updates.
