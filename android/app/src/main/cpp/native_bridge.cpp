#include "core/log.h"
#include "core/runtime.h"
#include "core/command_pump.h"
#include "modules/module.h"
#include "modules/character_voice/character_voice_module.h"
#include "modules/desktop/desktop_module.h"
#include "modules/login_model/login_model_module.h"
#include "modules/custom_model/resource_probe.h"
#include "modules/custom_model/custom_model_module.h"
#include "modules/camera/first_person_look_probe.h"
#include "modules/custom_model/android_mesh_builder.h"
#include "../../../../../native/shared/third_party_modules/third_party_host.h"

#include "android_virtual_keys.h"
#include "android_frame.h"
#include "android_pc_mouse.h"
#include "android_camera.h"
#include "core/jni_binding.h"
#include "core/local_music_android.h"
#include "core/runtime_status.h"

#include <jni.h>
#include <dlfcn.h>

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <thread>
#include <vector>
#include <string>

// Each desktop feature module keeps its own entry point; the Android CMake build
// renames the shared BetterEndfield_GetModuleApiV1 symbol per translation unit so
// all of them can live in this one shared library.
extern "C" const BE_ModuleApiV1* BetterEndfield_GetUiModuleApiV1();
extern "C" const BE_ModuleApiV1* BetterEndfield_GetCameraModuleApiV1();
extern "C" const BE_ModuleApiV1* BetterEndfield_GetActionsModuleApiV1();

namespace betterendfield {
// Defined in input_relay.cpp; serves the panel's key presses through a file.
void StartInputRelay();
}  // namespace betterendfield

// Served over JNI rather than the relay. Name-based lookup cannot reach the
// bridge class from the copy that lives in the game's namespace, so JNI_OnLoad
// binds these against the class resolved from the calling thread's context
// classloader -- see core/jni_binding.h.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_pcMouseCaptureRequested(JNIEnv*, jclass);
extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_pcMouseCaptured(JNIEnv*, jclass, jboolean);
extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_pcMouseMotion(JNIEnv*, jclass, jfloat, jfloat);
extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_frame(JNIEnv*, jclass);
extern "C" JNIEXPORT jstring JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_runtimeStatus(JNIEnv*, jclass);

namespace betterendfield {
namespace {

constexpr auto kPollInterval = std::chrono::milliseconds(100);
constexpr auto kInitialDelay = std::chrono::seconds(1);
constexpr int kMaximumAttempts = 1200;
std::atomic_bool g_runtime_started{false};
// The runtime status string the panel reads over JNI. Every writer lives in
// this file -- the module startup sequence below -- so a snapshot can never
// report a module as ready when the loop that starts it did not run.
RuntimeStatus g_runtime_status;
std::vector<std::unique_ptr<Module>> g_modules;
std::unique_ptr<Il2CppRuntime> g_il2cpp_runtime;
// The JNI frame entry reads this from the engine's render thread while the
// worker that owns it is still starting up, so publish it with a release store
// rather than letting the two threads race on the unique_ptr itself.
std::atomic<Il2CppRuntime*> g_il2cpp_published{nullptr};
BetterEndfield::ThirdParty::ThirdPartyHost g_third_party;
HookBroker g_third_party_hooks;
std::unique_ptr<DesktopModule> g_third_party_helper;

const char* Configured(const char* variable) {
    const char* value = std::getenv(variable);
    return value != nullptr && value[0] != '\0' ? value : nullptr;
}

void RunModules() {
    g_runtime_status.Set("runtime", "waiting_il2cpp");
    // libil2cpp.so is mapped before the IL2CPP domain is safe to enter. The
    // proven read-only POC used this guard; connecting immediately can call
    // il2cpp_thread_attach while domain initialization is still in progress.
    std::this_thread::sleep_for(kInitialDelay);
    LogInfo("runtime", "Android module runtime started");

    if (const char* index = Configured("BETTER_ENDFIELD_THIRD_PARTY_INDEX")) {
        std::string hook_error;
        const bool hook_ready = g_third_party_hooks.Initialize(hook_error);
        if (!hook_ready) LogError("third-party", hook_error.c_str());
        g_third_party.Start(index, "android-arm64",
            [](const auto& id, const auto& message) {
                LogInfo(id.c_str(), message.c_str());
            }, nullptr, hook_ready ? g_third_party_hooks.ChainApi() : nullptr);
    }

    g_il2cpp_runtime = std::make_unique<Il2CppRuntime>();
    Il2CppRuntime& runtime = *g_il2cpp_runtime;
    g_il2cpp_published.store(&runtime, std::memory_order_release);
    for (int attempt = 1; attempt <= kMaximumAttempts; ++attempt) {
        if (runtime.Connect()) {
            break;
        }
        if (attempt == kMaximumAttempts) {
            LogError("runtime", "timed out waiting for libil2cpp.so");
            g_runtime_status.Set("runtime", "failed_il2cpp_timeout");
            return;
        }
        std::this_thread::sleep_for(kPollInterval);
    }

    Il2CppThreadScope thread(runtime);
    if (!thread.attached()) {
        LogError("runtime", "failed to attach worker to the IL2CPP domain");
        g_runtime_status.Set("runtime", "failed_thread_attach");
        return;
    }

    if (Configured("BETTER_ENDFIELD_THIRD_PARTY_INDEX") != nullptr) {
        static const BE_ModuleApiV1 helper{
            {"third-party.runtime.helper", "Third-party optional helpers", "1", 1},
            [](const BE_HostApiV1*) -> BE_Result { return BE_Result_Ok; },
            [](const char*) -> BE_Result { return BE_Result_Ok; },
            []() {}};
        g_third_party_helper = std::make_unique<DesktopModule>(
            "third-party.runtime.helper", "BETTER_ENDFIELD_THIRD_PARTY_INDEX",
            []() -> const BE_ModuleApiV1* { return &helper; },
            "third-party runtime helper ready");
        if (g_third_party_helper->Start(runtime).active)
            g_third_party.SetRuntime(g_third_party_helper->OptionalHostApi());
    }

    const char* custom_probe = std::getenv("BETTER_ENDFIELD_CUSTOM_MODEL_PROBE");
    if (Configured("BETTER_ENDFIELD_CUSTOM_MODEL_CONFIG") != nullptr) {
        g_modules.emplace_back(std::make_unique<CustomModelModule>());
    }
    if (custom_probe != nullptr && std::string(custom_probe) == "1") {
        g_modules.emplace_back(std::make_unique<CustomModelResourceProbe>());
    }
    // Read-only metadata probe for the first-person look entry point. Gated the
    // same way as the resource probe: a debug system property, so a release
    // install never runs it.
    const char* look_probe = std::getenv("BETTER_ENDFIELD_FP_LOOK_PROBE");
    if (look_probe != nullptr && std::string(look_probe) == "1") {
        g_modules.emplace_back(std::make_unique<FirstPersonLookProbe>());
    }
    if (Configured("BETTER_ENDFIELD_VOICE_RULES") != nullptr) {
        g_modules.emplace_back(std::make_unique<CharacterVoiceModule>());
    }
    if (Configured("BETTER_ENDFIELD_MODEL_CONFIG") != nullptr) {
        g_modules.emplace_back(std::make_unique<LoginModelModule>());
    }
    // The three ported desktop modules. Their configurations are independent, so
    // a user who only wants one of them never has the others in the process.
    if (Configured("BETTER_ENDFIELD_UI_CONFIG") != nullptr) {
        g_modules.emplace_back(std::make_unique<DesktopModule>(
            "betterendfield.ui",
            "BETTER_ENDFIELD_UI_CONFIG",
            &BetterEndfield_GetUiModuleApiV1,
            "same-source desktop UI module active (hide UID/watermark, all-HUD toggle)"));
    }
    if (Configured("BETTER_ENDFIELD_CAMERA_CONFIG") != nullptr) {
        ConfigureAndroidMeshBuilder(runtime);
        ConfigureHeadwearCanary(runtime);
        g_modules.emplace_back(std::make_unique<DesktopModule>(
            "betterendfield.camera",
            "BETTER_ENDFIELD_CAMERA_CONFIG",
            &BetterEndfield_GetCameraModuleApiV1,
            "same-source desktop camera module active (free camera, world pause, "
            "first person, near-camera dither)"));
    }
    if (Configured("BETTER_ENDFIELD_ACTIONS_CONFIG") != nullptr) {
        g_modules.emplace_back(std::make_unique<DesktopModule>(
            "betterendfield.actions",
            "BETTER_ENDFIELD_ACTIONS_CONFIG",
            &BetterEndfield_GetActionsModuleApiV1,
            "same-source desktop sustained-dash module active"));
    }

    // Every module is about to be started. Recording an outcome per module is
    // what lets the panel tell a module that came up from one that never did.
    g_runtime_status.Set("runtime", "starting_modules");
    for (const auto& module : g_modules) {
        g_runtime_status.Set(module->Id(), "starting");
        const ModuleResult result = module->Start(runtime);
        g_runtime_status.Set(module->Id(), result.active ? "ready" : "failed");
        LogInfo(module->Id(), result.message.c_str());
    }
    // Name what ran. Without this a probe that was compiled in but never
    // registered looks exactly like one that ran and found nothing, and the
    // two need opposite fixes.
    std::string selected;
    for (const auto& module : g_modules) {
        selected += " ";
        selected += module->Id();
    }
    LogInfo("runtime", (std::string("modules started:") +
        (selected.empty() ? " (none)" : selected)).c_str());
    g_runtime_status.Set("runtime", "startup_complete");
}

bool AnyModuleRequested() {
    static constexpr const char* kVariables[]{
        "BETTER_ENDFIELD_VOICE_RULES",
        "BETTER_ENDFIELD_MODEL_CONFIG",
        "BETTER_ENDFIELD_UI_CONFIG",
        "BETTER_ENDFIELD_CAMERA_CONFIG",
        "BETTER_ENDFIELD_ACTIONS_CONFIG",
        "BETTER_ENDFIELD_CUSTOM_MODEL_CONFIG",
        "BETTER_ENDFIELD_THIRD_PARTY_INDEX",
    };
    for (const char* variable : kVariables) {
        if (Configured(variable) != nullptr) return true;
    }
    const char* custom_probe = std::getenv("BETTER_ENDFIELD_CUSTOM_MODEL_PROBE");
    if (custom_probe != nullptr && std::string(custom_probe) == "1") return true;
    // The look probe has to count as a request on its own: with no configuration
    // for any real module, JNI_OnLoad would otherwise skip the IL2CPP worker and
    // the probe would never get a runtime to enumerate against.
    const char* look_probe = std::getenv("BETTER_ENDFIELD_FP_LOOK_PROBE");
    return look_probe != nullptr && std::string(look_probe) == "1";
}

}  // namespace
}  // namespace betterendfield

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    // The Java side may load a second copy of this library under the module
    // classloader when JNI resolution against the game-classloader copy
    // fails. That copy only serves JNI symbols; the module runtime already
    // runs in the first copy, and a second one would double-install hooks.
    const char* guard = std::getenv("BETTER_ENDFIELD_RUNTIME_STARTED");
    if (guard != nullptr && std::string(guard) == "1") {
        betterendfield::LogInfo(
            "runtime", "secondary library copy; runtime already started elsewhere");
        return JNI_VERSION_1_6;
    }
    if (!betterendfield::AnyModuleRequested()) {
        betterendfield::LogInfo(
            "runtime", "no Android modules selected; IL2CPP worker not started");
        return JNI_VERSION_1_6;
    }
    if (!betterendfield::g_runtime_started.exchange(true, std::memory_order_acq_rel)) {
        // The static above is per library copy, and Android gives every
        // classloader its own copy. The environment variable is process-wide,
        // so setting it here is what actually stops a second copy from
        // starting a second runtime (and double-installing every hook).
        setenv("BETTER_ENDFIELD_RUNTIME_STARTED", "1", 1);
        // The panel-facing paths served over JNI, the way upstream serves them.
        // The bridge class is not visible from the namespace this library loads
        // into, so bind them explicitly against the class the calling thread's
        // *context* classloader resolves -- RuntimeBootstrap points that loader
        // at the module's own for this call.
        JNIEnv* environment = nullptr;
        if (vm == nullptr
                || vm->GetEnv(reinterpret_cast<void**>(&environment), JNI_VERSION_1_6) != JNI_OK
                || environment == nullptr) {
            betterendfield::LogError("runtime", "PC mouse/frame JNI natives not bound: no environment");
        } else {
            // The same class is needed again below: the MMD director's local
            // track is played by Java, so the native player has to resolve its
            // four audio statics on the one class it can reach.
            jclass bridge_class = nullptr;
            static const JNINativeMethod kPcMouseMethods[]{
                {"pcMouseCaptureRequested", "()Z", reinterpret_cast<void*>(
                    &Java_dev_betterendfield_android_NativeCommandBridge_pcMouseCaptureRequested)},
                {"pcMouseCaptured", "(Z)V", reinterpret_cast<void*>(
                    &Java_dev_betterendfield_android_NativeCommandBridge_pcMouseCaptured)},
                {"pcMouseMotion", "(FF)V", reinterpret_cast<void*>(
                    &Java_dev_betterendfield_android_NativeCommandBridge_pcMouseMotion)},
                {"frame", "()V", reinterpret_cast<void*>(
                    &Java_dev_betterendfield_android_NativeCommandBridge_frame)},
            };
            if (!betterendfield::BindContextLoaderNatives(environment,
                    "dev.betterendfield.android.NativeCommandBridge", kPcMouseMethods,
                    static_cast<jint>(sizeof(kPcMouseMethods) / sizeof(kPcMouseMethods[0])),
                    &bridge_class)) {
                // The rest of the panel channel is the file relay and keeps
                // working, so a refused binding costs the PC layout its
                // relative-mouse path instead of the whole runtime. Clear the
                // exception jni_binding.h leaves behind: nativeLoad would
                // otherwise report the module as unloaded.
                if (environment->ExceptionCheck()) environment->ExceptionClear();
                betterendfield::LogError("runtime",
                    "PC mouse/frame JNI natives not bound; PC layout keeps its touch path "
                    "and the frame clients stay idle");
            } else {
                betterendfield::LogInfo("runtime", "PC mouse and frame JNI natives bound");
            }
            // The status string is read by the same panel class, so it needs the
            // same context-classloader binding. Bound separately so that a
            // refused read costs the status line, not the mouse path.
            static const JNINativeMethod kRuntimeStatusMethods[]{
                {"runtimeStatus", "()Ljava/lang/String;", reinterpret_cast<void*>(
                    &Java_dev_betterendfield_android_NativeCommandBridge_runtimeStatus)},
            };
            if (!betterendfield::BindContextLoaderNatives(environment,
                    "dev.betterendfield.android.NativeCommandBridge", kRuntimeStatusMethods,
                    static_cast<jint>(sizeof(kRuntimeStatusMethods) / sizeof(kRuntimeStatusMethods[0])))) {
                if (environment->ExceptionCheck()) environment->ExceptionClear();
                betterendfield::LogError("runtime", "runtime status JNI native not bound");
            } else {
                betterendfield::LogInfo("runtime", "runtime status JNI native bound");
            }
            // The MMD director drives its local track through
            // BE_LocalMusicApiV1, and on Android that interface is four Java
            // statics on this bridge (see local_music_android.cpp, which
            // resolves them by name). A refusal costs the music track only.
            if (!bridge_class || !betterendfield::InitializeAndroidMusic(vm, environment, bridge_class)) {
                betterendfield::LogError("mmd.music", "Java media API unavailable; body and camera remain usable");
            } else {
                betterendfield::LogInfo("mmd.music", "Java media API bound");
            }
        }
        // The panel presses keys through a file relay (see input_relay.cpp):
        // JNI resolution is classloader-scoped and the panel's classes live in
        // the LSPosed module classloader, one classloader away from this copy.
        betterendfield::StartInputRelay();
        betterendfield::g_runtime_status.Set("runtime", "loaded");
        std::thread(betterendfield::RunModules).detach();
    }
    return JNI_VERSION_1_6;
}

// Diagnostics: reports whether libil2cpp.so is visible from the calling
// library copy's linker namespace (RTLD_NOLOAD — never loads a second copy).
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_probeIl2Cpp(JNIEnv*, jclass) {
    void* image = dlopen("libil2cpp.so", RTLD_NOLOAD | RTLD_NOW);
    return image != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_submit(
        JNIEnv* environment, jclass, jstring payload) {
    if (!environment || !payload) return JNI_FALSE;
    const char* text = environment->GetStringUTFChars(payload, nullptr);
    if (!text) return JNI_FALSE;
    const bool accepted = betterendfield::SubmitRuntimeCommand(text, std::strlen(text));
    environment->ReleaseStringUTFChars(payload, text);
    return accepted ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_status(
        JNIEnv* environment, jclass) {
    if (!environment) return nullptr;
    const std::string status = betterendfield::CopyRuntimeCommandStatus();
    return environment->NewStringUTF(status.c_str());
}

// The runtime status string: the module startup sequence recorded above, plus
// the camera module's live counters and the frame clients' own state. Upstream
// serves it over JNI, and the reader is the panel class -- which name-based
// lookup cannot reach from the namespace this library loads into, so it is
// bound the way the PC mouse calls are.
extern "C" JNIEXPORT jstring JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_runtimeStatus(
        JNIEnv* environment, jclass) {
    if (!environment) return nullptr;
    const std::string status = betterendfield::g_runtime_status.Copy() +
        betterendfield::AndroidCameraValuesStatus() +
        "camera.capabilities=" + std::to_string(betterendfield::AndroidCameraCapabilities()) + "\n" +
        "camera.active=" + std::to_string(betterendfield::AndroidCameraActive()) + "\n" +
        "ui.hud_hidden=" + (betterendfield::AndroidHudHidden() ? "1\n" : "0\n");
    return environment->NewStringUTF(status.c_str());
}

// The in-game panel's controls. This deliberately bypasses the runtime command
// pump: the pump is a single-slot, generation-checked queue drained on a Unity
// hook, which is right for configuration but would drop the release event of a
// press-and-hold control such as the free-camera movement pad. The latch is a
// plain atomic, so a press is visible to the desktop polling code immediately.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_key(
        JNIEnv*, jclass, jint virtual_key, jint action) {
    return betterendfield::SetVirtualKey(
        static_cast<int>(virtual_key),
        static_cast<betterendfield::VirtualKeyAction>(action)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_releaseKeys(JNIEnv*, jclass) {
    betterendfield::ReleaseAllVirtualKeys();
}
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_pcMouseCaptureRequested(JNIEnv*, jclass) {
    // Announced once. On a device with no mouse attached this is the only call
    // the bridge ever makes, so the line doubles as proof that the binding is
    // live and not merely present as an exported symbol.
    static std::atomic_bool announced{false};
    if (!announced.exchange(true)) {
        betterendfield::LogInfo("runtime", "PC mouse bridge: JNI call path live");
    }
    return betterendfield::AndroidPcMouseCaptureRequested() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_pcMouseCaptured(JNIEnv*, jclass, jboolean captured) {
    betterendfield::SetAndroidPcMouseCaptured(captured == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_pcMouseMotion(JNIEnv*, jclass, jfloat dx, jfloat dy) {
    betterendfield::AddAndroidPcMouseMotion(dx, dy);
}

// One frame as the engine hands it to the scriptable render pipeline. This is
// the only per-frame tick the native layer gets, and the one a frozen world
// cannot silence, so every frame client is driven from here: the PC layout's
// relative mouse reads its motion snapshot once per frame, and a snapshot that
// never advances hands the game the same zero for every later read.
extern "C" JNIEXPORT void JNICALL
Java_dev_betterendfield_android_NativeCommandBridge_frame(JNIEnv*, jclass) {
    betterendfield::Il2CppRuntime* runtime =
        betterendfield::g_il2cpp_published.load(std::memory_order_acquire);
    if (runtime != nullptr) {
        // The render thread outlives every frame and is not attached on every
        // player, so attach it once and leave it attached. Attaching and
        // detaching around each call would tear down a thread state the engine
        // also uses, and the frame clients enter the domain on this thread.
        static thread_local bool attached = false;
        if (!attached) {
            attached = runtime->AttachCurrentThread() != nullptr;
        }
        if (!attached) return;
    }
    betterendfield::DispatchAndroidFrame();
}
