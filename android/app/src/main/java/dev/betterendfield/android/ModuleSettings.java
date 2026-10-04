package dev.betterendfield.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

final class ModuleSettings {
    private static final String VOICE_CATALOGS = "voice_catalogs";
    static final String VOICE_RULES = "voice_language_rules";
    private static final String MODEL_ENABLED = "model_replacement_enabled";
    private static final String MODEL_RUNTIME_ENABLED = "model_runtime_enabled";
    private static final String MODEL_CHARACTER = "model_character";
    private static final String MODEL_ACTION = "model_action";
    private static final String MODEL_FINAL_LOOP = "model_final_loop";
    private static final String MODEL_FORCE_LOOP = "model_force_loop";
    private static final String MODEL_CROSSFADE = "model_crossfade";
    private static final String MODEL_LOOP_START = "model_loop_start";
    private static final String MODEL_LOOP_END = "model_loop_end";
    private static final String MODEL_CROSSFADE_DURATION = "model_crossfade_duration";
    private static final String MODEL_SCALE = "model_scale";
    private static final String LOGO_ENABLED = "logo_theme_enabled";
    private static final String LOGO_COLOR = "logo_theme_color";
    static final String MODEL_CONFIGURATION = "model_configuration";

    // betterendfield.ui — the desktop interface module.
    private static final String UI_HIDE_UID = "ui_hide_uid";
    static final String UI_HIDE_HUD = "ui_hide_hud";
    static final String UI_CONFIGURATION = "ui_configuration";

    // betterendfield.camera — the desktop camera module.
    private static final String CAMERA_DITHER = "camera_disable_dither";
    static final String CAMERA_FREE = "camera_free_enabled";
    static final String CAMERA_PAUSE = "camera_pause_enabled";
    static final String CAMERA_FIRST_PERSON = "camera_first_person_enabled";
    private static final String CAMERA_FP_HIDE_HEAD = "camera_first_person_hide_head";
    private static final String CAMERA_FP_FILL_NECK = "camera_first_person_fill_neck";
    private static final String CAMERA_SPEED = "camera_movement_speed";
    private static final String CAMERA_FOV = "camera_field_of_view";
    private static final String CAMERA_FP_FOV = "camera_first_person_fov";
    private static final String CAMERA_FP_EYE_FORWARD = "camera_first_person_eye_forward";
    private static final String CAMERA_FP_EYE_HEIGHT = "camera_first_person_eye_height";
    private static final String CAMERA_FP_NEAR_CLIP = "camera_first_person_near_clip";
    private static final String CAMERA_FP_EXTEND_LOOK_RANGE = "camera_first_person_extend_look_range";
    // The gyroscope look source. These live alongside the first-person keys
    // because the sensor only ever feeds the look channel while the camera
    // module is in the process; they are not part of the native configuration
    // string's contract, only the settings screen's record of the user's choice.
    private static final String CAMERA_FP_GYRO_ENABLED = "camera_first_person_gyro_enabled";
    private static final String CAMERA_FP_GYRO_HORIZONTAL = "camera_first_person_gyro_horizontal";
    private static final String CAMERA_FP_GYRO_VERTICAL = "camera_first_person_gyro_vertical";
    private static final String CAMERA_FP_GYRO_INVERT_HORIZONTAL =
            "camera_first_person_gyro_invert_horizontal";
    private static final String CAMERA_FP_GYRO_INVERT_VERTICAL =
            "camera_first_person_gyro_invert_vertical";
    private static final String CAMERA_FP_GYRO_DEADZONE = "camera_first_person_gyro_deadzone";
    private static final String CAMERA_FP_GYRO_SMOOTHING = "camera_first_person_gyro_smoothing";
    static final float FP_EYE_FORWARD_MINIMUM = 0f, FP_EYE_FORWARD_MAXIMUM = 0.5f;
    static final float FP_EYE_HEIGHT_MINIMUM = -0.5f, FP_EYE_HEIGHT_MAXIMUM = 0.5f;
    static final float FP_NEAR_CLIP_MINIMUM = 0.001f, FP_NEAR_CLIP_MAXIMUM = 1f;
    static final String CAMERA_CONFIGURATION = "camera_configuration";

    /**
     * Turns on the read-only first-person look probe for the next game launch.
     *
     * This is a research switch, not a feature: it exists only so the developer
     * can ask the game what methods and fields its look path actually has. It is
     * deliberately a preference rather than a system property or an environment
     * variable, because the phone is not rooted and the settings app is the only
     * thing that can reach the game process without root. It is read once at
     * library load, so toggling it takes effect on the next launch, and it does
     * nothing at all unless it is on.
     */
    static final String DEBUG_FP_LOOK_PROBE = "debug_first_person_look_probe";

    /**
     * Where the imported VMD camera motion lives once the game process has
     * materialized it. The settings app cannot build this path: it does not know
     * which user or cloned profile the game runs under, and it cannot write into
     * another app's data directory at all. So the configuration carries the
     * placeholder and {@link RuntimeBootstrap} substitutes the game's own files
     * directory immediately before handing the string to the native loader.
     */
    static final String VMD_FILE_SLOT = "%files%/betterendfield/camera/current.vmd";
    /** The name the settings app publishes the imported .vmd under. */
    static final String VMD_REMOTE_NAME = "vmd.current";
    /** Whether a .vmd has been imported; both the panel and the config read it. */
    static final String CAMERA_VMD_IMPORTED = "camera_vmd_imported";
    /** The published payload's length, so the game side can skip an identical copy. */
    static final String VMD_BYTES = "camera_vmd_bytes";
    private static final String CAMERA_VMD_NAME = "camera_vmd_name";
    private static final String CAMERA_VMD_TIME = "camera_vmd_time";

    /** Mirrors the native loader's header contract in {@code LoadVmdCamera}. */
    private static final String VMD_HEADER_0002 = "Vocaloid Motion Data 0002";
    private static final String VMD_HEADER_LEGACY = "Vocaloid Motion Data file";
    private static final int VMD_HEADER_BYTES = 30;

    /**
     * The motion preset names {@code ParseMotionPreset} accepts, in the order the
     * settings picker shows them. Index 0 has to stay {@code orbit}: it is the
     * desktop default and the fallback for anything unparsable.
     */
    static final String[] MOTION_PRESETS = {"orbit", "dolly_zoom", "crane", "truck"};

    // betterendfield.actions — the desktop sustained special dash.
    private static final String DASH_ENABLED = "dash_enabled";
    private static final String DASH_LIINO_CLEAN = "dash_liino_clean";
    private static final String DASH_AGLINA = "dash_character_aglina";
    private static final String DASH_LIINO = "dash_character_liino";
    static final String ACTIONS_CONFIGURATION = "actions_configuration";

    // Superseded by UI_HIDE_UID and CAMERA_DITHER. Version 3.2.2 and earlier
    // implemented those two switches in a separate Android-only enhancement
    // module; they now run through the shared desktop sources. The old keys are
    // still read once so an upgrade keeps the user's choice.
    private static final String LEGACY_HIDE_UID = "enhancement_hide_uid";
    private static final String LEGACY_DISABLE_DITHER = "enhancement_disable_dither";

    static final String OVERLAY_ENABLED = "overlay_enabled";
    private static final String OVERLAY_TRANSPARENCY = "overlay_transparency";
    private static final String OVERLAY_AUTO_SNAP = "overlay_auto_snap";
    private static final String COMMAND_GENERATION = "command_generation";

    static final float OVERLAY_TRANSPARENCY_MAXIMUM = 80f;
    static final float SPEED_MINIMUM = 0.2f;
    static final float SPEED_MAXIMUM = 60.0f;
    static final float FOV_MINIMUM = 20.0f;
    static final float FOV_MAXIMUM = 120.0f;

    private ModuleSettings() {}

    static boolean isOverlayEnabled(Context context) {
        return preferences(context).getBoolean(OVERLAY_ENABLED, false);
    }

    static void setOverlayEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(OVERLAY_ENABLED, enabled).commit();
    }

    record OverlayAppearance(float transparency, boolean autoSnap) {
        OverlayAppearance {
            transparency = Float.isFinite(transparency)
                    ? Math.max(0f, Math.min(OVERLAY_TRANSPARENCY_MAXIMUM, transparency)) : 0f;
        }

        float alpha() { return 1f - transparency / 100f; }
    }

    static OverlayAppearance getOverlayAppearance(SharedPreferences settings) {
        return new OverlayAppearance(settings.getFloat(OVERLAY_TRANSPARENCY, 0f),
                settings.getBoolean(OVERLAY_AUTO_SNAP, false));
    }

    static float getOverlayTransparency(Context context) {
        return getOverlayAppearance(preferences(context)).transparency();
    }

    static boolean isOverlayAutoSnap(Context context) {
        return getOverlayAppearance(preferences(context)).autoSnap();
    }

    static void setOverlayTransparency(Context context, float transparency) {
        float bounded = new OverlayAppearance(transparency, false).transparency();
        preferences(context).edit().putFloat(OVERLAY_TRANSPARENCY, bounded).commit();
    }

    static void setOverlayAutoSnap(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(OVERLAY_AUTO_SNAP, enabled).commit();
    }

    static long nextCommandGeneration(Context context) {
        SharedPreferences settings = preferences(context);
        long next = settings.getLong(COMMAND_GENERATION, 0L) + 1L;
        settings.edit().putLong(COMMAND_GENERATION, next).commit();
        return next;
    }

    // ---------------------------------------------------------------- interface

    static boolean isHideUidEnabled(Context context) {
        SharedPreferences settings = preferences(context);
        return settings.contains(UI_HIDE_UID)
                ? settings.getBoolean(UI_HIDE_UID, false)
                : settings.getBoolean(LEGACY_HIDE_UID, false);
    }

    static boolean isHideHudEnabled(Context context) {
        return preferences(context).getBoolean(UI_HIDE_HUD, false);
    }

    static void setInterfaceSettings(Context context, boolean hideUid, boolean hideHud) {
        // An empty configuration is what keeps the module out of the game process,
        // so it has to be empty exactly when nothing is selected.
        String configuration = hideUid || hideHud
                ? "schema_version=1\n"
                        + "enabled=true\n"
                        + "diagnostics=true\n"
                        + "hide_uid_enabled=" + hideUid + "\n"
                        + "hide_hud_enabled=" + hideHud + "\n"
                        + "hide_hud_hotkey=" + Hotkeys.HIDE_HUD_NAME + "\n"
                // The touch layout and the Android/cloud platform claim only exist
                // to make a desktop client look like a phone. This is a phone.
                        + "mobile_ui_enabled=false\n"
                        + "platform_spoof_enabled=false\n"
                : "";
        preferences(context)
                .edit()
                .putBoolean(UI_HIDE_UID, hideUid)
                .putBoolean(UI_HIDE_HUD, hideHud)
                .putString(UI_CONFIGURATION, configuration)
                .commit();
    }

    // ------------------------------------------------------------------- camera

    static boolean isDisableDitherEnabled(Context context) {
        SharedPreferences settings = preferences(context);
        return settings.contains(CAMERA_DITHER)
                ? settings.getBoolean(CAMERA_DITHER, false)
                : settings.getBoolean(LEGACY_DISABLE_DITHER, false);
    }

    static boolean isFreeCameraEnabled(Context context) {
        return preferences(context).getBoolean(CAMERA_FREE, false);
    }

    static boolean isWorldPauseEnabled(Context context) {
        return preferences(context).getBoolean(CAMERA_PAUSE, false);
    }

    static boolean isFirstPersonEnabled(Context context) {
        return preferences(context).getBoolean(CAMERA_FIRST_PERSON, false);
    }

    static boolean isFirstPersonHideHead(Context context) {
        return preferences(context).getBoolean(CAMERA_FP_HIDE_HEAD, true);
    }

    static boolean isFirstPersonFillNeck(Context context) {
        return preferences(context).getBoolean(CAMERA_FP_FILL_NECK, true);
    }

    static String getCameraSpeed(Context context) {
        return preferences(context).getString(CAMERA_SPEED, "5");
    }

    static String getCameraFieldOfView(Context context) {
        return preferences(context).getString(CAMERA_FOV, "60");
    }

    static String getFirstPersonFieldOfView(Context context) {
        return preferences(context).getString(CAMERA_FP_FOV, "75");
    }

    static String getFirstPersonEyeForward(Context context) {
        return preferences(context).getString(CAMERA_FP_EYE_FORWARD, "0.03");
    }

    static String getFirstPersonEyeHeight(Context context) {
        return preferences(context).getString(CAMERA_FP_EYE_HEIGHT, "0.05");
    }

    static String getFirstPersonNearClip(Context context) {
        return preferences(context).getString(CAMERA_FP_NEAR_CLIP, "0.03");
    }

    static boolean isFirstPersonExtendLookRange(Context context) {
        return preferences(context).getBoolean(CAMERA_FP_EXTEND_LOOK_RANGE, false);
    }

    /** Advanced camera preferences share one normalized save/read contract. */
    record FirstPersonAdvanced(boolean movement, double sideLookLimit,
            double lookUpLimit, double lookDownLimit, int animationMode,
            double animationStrength, boolean yieldDialogue, boolean thirdPersonInCombat, double transitionSeconds,
            boolean externalHeadScale) {
        FirstPersonAdvanced {
            sideLookLimit = bounded(sideLookLimit, 60, 0, 90);
            lookUpLimit = bounded(lookUpLimit, 89, 0, 89);
            lookDownLimit = bounded(lookDownLimit, 89, 0, 89);
            animationMode = animationMode >= 0 && animationMode <= 3 ? animationMode : 0;
            animationStrength = bounded(animationStrength, 0.35, 0, 1);
            transitionSeconds = bounded(transitionSeconds, 0, 0, 1);
        }

        String toIniLines() {
            return "first_person_movement=" + movement + "\n"
                    + "first_person_side_look_limit=" + number(sideLookLimit) + "\n"
                    + "first_person_look_up_limit=" + number(lookUpLimit) + "\n"
                    + "first_person_look_down_limit=" + number(lookDownLimit) + "\n"
                    + "first_person_animation_mode=" + animationMode + "\n"
                    + "first_person_animation_strength=" + number(animationStrength) + "\n"
                    + "first_person_yield_dialogue=" + yieldDialogue + "\n"
                    + "first_person_third_person_in_combat=" + thirdPersonInCombat + "\n"
                    + "first_person_transition_seconds=" + number(transitionSeconds) + "\n"
                    + "first_person_external_head_scale=" + externalHeadScale + "\n";
        }

        void store(SharedPreferences.Editor edit) {
            edit.putBoolean("camera_first_person_movement", movement)
                    .putString("camera_first_person_side_look_limit", number(sideLookLimit))
                    .putString("camera_first_person_look_up_limit", number(lookUpLimit))
                    .putString("camera_first_person_look_down_limit", number(lookDownLimit))
                    .putInt("camera_first_person_animation_mode", animationMode)
                    .putString("camera_first_person_animation_strength", number(animationStrength))
                    .putBoolean("camera_first_person_yield_dialogue", yieldDialogue)
                    .putBoolean("camera_first_person_third_person_in_combat", thirdPersonInCombat)
                    .putString("camera_first_person_transition_seconds", number(transitionSeconds))
                    .putBoolean("camera_first_person_external_head_scale", externalHeadScale);
        }
    }

    static FirstPersonAdvanced getFirstPersonAdvanced(Context context) {
        SharedPreferences prefs = preferences(context);
        return new FirstPersonAdvanced(
                prefs.getBoolean("camera_first_person_movement", false),
                parse(prefs.getString("camera_first_person_side_look_limit", "60"), 60),
                parse(prefs.getString("camera_first_person_look_up_limit", "89"), 89),
                parse(prefs.getString("camera_first_person_look_down_limit", "89"), 89),
                prefs.getInt("camera_first_person_animation_mode", 0),
                parse(prefs.getString("camera_first_person_animation_strength", "0.35"), 0.35),
                prefs.getBoolean("camera_first_person_yield_dialogue", false),
                prefs.getBoolean("camera_first_person_third_person_in_combat", false),
                parse(prefs.getString("camera_first_person_transition_seconds", "0"), 0),
                prefs.getBoolean("camera_first_person_external_head_scale", false));
    }

    /**
     * The gyroscope look source.
     *
     * Every number is clamped here to the range {@link GyroscopeController}
     * accepts, so a value this screen allows can never be silently corrected on
     * its way to the sensor loop. The record is also the one place the settings
     * screen, the game-process bootstrap and the controller agree on defaults:
     * {@link #disabled()} is what a fresh install and every "not configured"
     * read produce, which is why the sensor is never armed by accident.
     *
     * <p>The two invert switches exist because the correct sign depends on how a
     * device is held and how its sensor axes are laid out; the controller picks
     * a sensible mapping from the screen rotation and these are the escape hatch
     * for a device that disagrees. They are not a substitute for getting the
     * default right.
     */
    record FirstPersonGyro(boolean enabled, double horizontalSensitivity,
            double verticalSensitivity, boolean invertHorizontal, boolean invertVertical,
            double deadzone, double smoothing) {
        /** Matches the controller's own clamps: 0.2–5.0× either axis. */
        static final double SENSITIVITY_MINIMUM = 0.2, SENSITIVITY_MAXIMUM = 5.0;
        /** Below the sensor's noise floor is meaningless; above 0.25 swallows real input. */
        static final double DEADZONE_MINIMUM = 0.0, DEADZONE_MAXIMUM = 0.25;
        /** 0 is no filtering at all; the controller rejects anything heavier than this. */
        static final double SMOOTHING_MINIMUM = 0.0, SMOOTHING_MAXIMUM = 0.9;

        FirstPersonGyro {
            horizontalSensitivity = bounded(horizontalSensitivity, 1.0,
                    SENSITIVITY_MINIMUM, SENSITIVITY_MAXIMUM);
            verticalSensitivity = bounded(verticalSensitivity, 1.0,
                    SENSITIVITY_MINIMUM, SENSITIVITY_MAXIMUM);
            deadzone = bounded(deadzone, GyroscopeController.defaultDeadzone(),
                    DEADZONE_MINIMUM, DEADZONE_MAXIMUM);
            smoothing = bounded(smoothing, GyroscopeController.defaultSmoothing(),
                    SMOOTHING_MINIMUM, SMOOTHING_MAXIMUM);
        }

        /** The state every read starts from, and what "off" means everywhere. */
        static FirstPersonGyro disabled() {
            // Inversion matches the read defaults: off and default are the same
            // pointing behaviour, so re-enabling never silently re-flips an axis.
            return new FirstPersonGyro(false, 1.0, 1.0, true, true,
                    GyroscopeController.defaultDeadzone(), GyroscopeController.defaultSmoothing());
        }

        /**
         * The keys the native camera module parses.
         *
         * The sensor itself is armed on this side and the deltas reach the game
         * through the input relay, so nothing here is what "turns the gyroscope
         * on". What the native side does need is where those deltas should go:
         * {@code first_person_gyro_look} is what selects the first-person
         * consumer over the free camera's, and the two scales are what the game's
         * RotateCamera* receive per relay pixel. Writing only the enabled flag
         * (as an earlier revision did) left the native side unable to tell a
         * gyroscope from a finger, so the switch turned on and nothing moved.
         *
         * <p>The scales are the gyroscope's own, not the free camera's
         * {@code mouse_sensitivity}: that value is tuned for a drag across a
         * screen, and it is also clobbered by the camera-motion screen.
         */
        String toIniLines() {
            return "first_person_gyro_enabled=" + enabled + "\n"
                    + "first_person_gyro_look=" + enabled + "\n"
                    + "first_person_gyro_horizontal=" + number(horizontalSensitivity) + "\n"
                    + "first_person_gyro_vertical=" + number(verticalSensitivity) + "\n"
                    + "first_person_gyro_invert_horizontal=" + invertHorizontal + "\n"
                    + "first_person_gyro_invert_vertical=" + invertVertical + "\n"
                    + "first_person_gyro_deadzone=" + number(deadzone) + "\n"
                    + "first_person_gyro_smoothing=" + number(smoothing) + "\n";
        }

        void store(SharedPreferences.Editor edit) {
            edit.putBoolean(CAMERA_FP_GYRO_ENABLED, enabled)
                    .putString(CAMERA_FP_GYRO_HORIZONTAL, number(horizontalSensitivity))
                    .putString(CAMERA_FP_GYRO_VERTICAL, number(verticalSensitivity))
                    .putBoolean(CAMERA_FP_GYRO_INVERT_HORIZONTAL, invertHorizontal)
                    .putBoolean(CAMERA_FP_GYRO_INVERT_VERTICAL, invertVertical)
                    .putString(CAMERA_FP_GYRO_DEADZONE, number(deadzone))
                    .putString(CAMERA_FP_GYRO_SMOOTHING, number(smoothing));
        }
    }

    static FirstPersonGyro getFirstPersonGyro(Context context) {
        return readFirstPersonGyro(preferences(context));
    }

    /** The settings-process read of the look probe switch. */
    static boolean readFirstPersonLookProbe(Context context) {
        return preferences(context).getBoolean(DEBUG_FP_LOOK_PROBE, false);
    }

    /**
     * Written from the settings process only. The value reaches the game through
     * the framework's remote preferences, which is what makes this switch usable
     * on a non-rooted device where neither `setprop` nor an exported environment
     * variable can reach the game's process.
     */
    static void writeFirstPersonLookProbe(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(DEBUG_FP_LOOK_PROBE, enabled).apply();
    }

    /**
     * The same read, from a preferences snapshot the caller already holds.
     *
     * The game process must use this overload: {@link #preferences(Context)}
     * opens the file that belongs to <em>the context's own package</em>, and the
     * context there is the game's Application, so the file it would open is the
     * game's own "module_settings" — which no settings screen ever writes. The
     * module's values reach the game through the framework's remote
     * preferences, and {@code getRemotePreferences("module_settings")} is what
     * carries them.
     */
    static FirstPersonGyro readFirstPersonGyro(SharedPreferences prefs) {
        return new FirstPersonGyro(
                prefs.getBoolean(CAMERA_FP_GYRO_ENABLED, false),
                parse(prefs.getString(CAMERA_FP_GYRO_HORIZONTAL, "1"), 1),
                parse(prefs.getString(CAMERA_FP_GYRO_VERTICAL, "1"), 1),
                // Both axes default to inverted. The raw sensor axes reach the
                // camera looking right and looking down in the opposite sense to
                // what the module expects, which the first device run confirmed
                // as "both axes are backwards". Keeping that correction in the
                // defaults rather than in the sensor mapping leaves the two
                // switches meaningful: they still mean "flip this axis", and a
                // user whose device differs can turn either one back off.
                prefs.getBoolean(CAMERA_FP_GYRO_INVERT_HORIZONTAL, true),
                prefs.getBoolean(CAMERA_FP_GYRO_INVERT_VERTICAL, true),
                parse(prefs.getString(CAMERA_FP_GYRO_DEADZONE,
                        String.valueOf(GyroscopeController.defaultDeadzone())),
                        GyroscopeController.defaultDeadzone()),
                parse(prefs.getString(CAMERA_FP_GYRO_SMOOTHING,
                        String.valueOf(GyroscopeController.defaultSmoothing())),
                        GyroscopeController.defaultSmoothing()));
    }

    /**
     * The free camera's motion, keyframe and VMD parameters.
     *
     * One normalized save/read contract, like {@link FirstPersonAdvanced}: every
     * number is clamped to the exact range the native module clamps it to, so a
     * value this screen accepts can never be silently changed again on the way
     * into the game. {@code mouseInvertY} and {@code sensitivity} are part of the
     * block because the configuration always carried them (they used to be the
     * literals {@code false} and {@code 0.1}); the on-screen look pad is what will
     * finally give them something to steer, at which point only the UI for them
     * is still missing.
     *
     * {@code vmdImported} decides whether {@code vmd_camera_file} carries the
     * {@link #VMD_FILE_SLOT} placeholder. It is a switch rather than a path
     * because the Android side has exactly one deliverable slot: the file the
     * settings app published and the game process materialized. The publication
     * metadata (name, size, time) is deliberately kept out of this record - none
     * of it reaches the native loader, it only describes the slot on screen.
     */
    record CameraMotion(boolean invertY, double sensitivity, String preset, double speed,
            double orbitSpeed, double duration, double targetHeight, double segmentSeconds,
            boolean keyframeLoop, boolean vmdImported, double vmdScale, double vmdFovBias,
            boolean vmdLoop) {
        CameraMotion {
            sensitivity = bounded(sensitivity, 0.1, 0.02, 0.5);
            speed = bounded(speed, 1.0, -20, 20);
            orbitSpeed = bounded(orbitSpeed, 20.0, -180, 180);
            duration = bounded(duration, 0, 0, 600);
            targetHeight = bounded(targetHeight, 1.2, -5, 5);
            segmentSeconds = bounded(segmentSeconds, 3.0, 0.2, 60);
            vmdScale = bounded(vmdScale, 0.07, 0.001, 10);
            vmdFovBias = bounded(vmdFovBias, 5.0, -60, 60);
            preset = presetName(preset);
        }

        String toIniLines() {
            return "mouse_invert_y=" + invertY + "\n"
                    + "mouse_sensitivity=" + number(sensitivity) + "\n"
                    + "motion_preset=" + preset + "\n"
                    + "motion_speed=" + number(speed) + "\n"
                    + "orbit_speed=" + number(orbitSpeed) + "\n"
                    + "motion_duration=" + number(duration) + "\n"
                    + "motion_target_height=" + number(targetHeight) + "\n"
                    + "keyframe_segment_seconds=" + number(segmentSeconds) + "\n"
                    + "keyframe_loop=" + keyframeLoop + "\n"
                    // The slot is a placeholder rather than a path because only
                    // the game process knows its own files directory. An empty
                    // value still travels as a key: the native reader then sees
                    // the same key set every launch and reports "no VMD camera
                    // file is configured" instead of having to guess.
                    + "vmd_camera_file=" + (vmdImported ? VMD_FILE_SLOT : "") + "\n"
                    + "vmd_camera_scale=" + number(vmdScale) + "\n"
                    + "vmd_camera_fov_bias=" + number(vmdFovBias) + "\n"
                    + "vmd_camera_loop=" + vmdLoop + "\n";
        }

        void store(SharedPreferences.Editor edit) {
            edit.putBoolean("camera_mouse_invert_y", invertY)
                    .putString("camera_mouse_sensitivity", number(sensitivity))
                    .putString("camera_motion_preset", preset)
                    .putString("camera_motion_speed", number(speed))
                    .putString("camera_orbit_speed", number(orbitSpeed))
                    .putString("camera_motion_duration", number(duration))
                    .putString("camera_motion_target_height", number(targetHeight))
                    .putString("camera_keyframe_segment_seconds", number(segmentSeconds))
                    .putBoolean("camera_keyframe_loop", keyframeLoop)
                    .putBoolean(CAMERA_VMD_IMPORTED, vmdImported)
                    .putString("camera_vmd_scale", number(vmdScale))
                    .putString("camera_vmd_fov_bias", number(vmdFovBias))
                    .putBoolean("camera_vmd_loop", vmdLoop);
        }
    }

    /** Maps a stored or user-typed preset onto the four names the parser knows. */
    static String presetName(String value) {
        if (value != null) {
            String text = value.trim().toLowerCase(Locale.ROOT);
            for (String preset : MOTION_PRESETS) {
                if (preset.equals(text)) return preset;
            }
            // ParseMotionPreset also accepts these two short forms. Recognising
            // them here keeps the round trip lossless: without it a preset that
            // reached the store from anywhere else would silently become orbit
            // the next time any row on this screen was touched.
            if ("dolly".equals(text)) return MOTION_PRESETS[1];
            if ("pan".equals(text)) return MOTION_PRESETS[3];
        }
        return MOTION_PRESETS[0];
    }

    static CameraMotion getCameraMotion(Context context) {
        SharedPreferences prefs = preferences(context);
        return new CameraMotion(
                prefs.getBoolean("camera_mouse_invert_y", false),
                parse(prefs.getString("camera_mouse_sensitivity", "0.1"), 0.1),
                prefs.getString("camera_motion_preset", MOTION_PRESETS[0]),
                parse(prefs.getString("camera_motion_speed", "1"), 1.0),
                parse(prefs.getString("camera_orbit_speed", "20"), 20.0),
                parse(prefs.getString("camera_motion_duration", "0"), 0),
                parse(prefs.getString("camera_motion_target_height", "1.2"), 1.2),
                parse(prefs.getString("camera_keyframe_segment_seconds", "3"), 3.0),
                prefs.getBoolean("camera_keyframe_loop", false),
                prefs.getBoolean(CAMERA_VMD_IMPORTED, false),
                parse(prefs.getString("camera_vmd_scale", "0.07"), 0.07),
                parse(prefs.getString("camera_vmd_fov_bias", "5"), 5.0),
                prefs.getBoolean("camera_vmd_loop", false));
    }

    // ------------------------------------------------------------- VMD slot

    /**
     * Records a just-published VMD import. The payload itself is not copied into
     * preferences; only what the pages and the game process need to reason about
     * it: that it exists, what it was called, how long it is and when it arrived.
     */
    static void setVmdImport(Context context, String name, long bytes, long timeMillis) {
        preferences(context)
                .edit()
                .putBoolean(CAMERA_VMD_IMPORTED, true)
                .putString(CAMERA_VMD_NAME, name == null ? "" : name)
                .putLong(VMD_BYTES, bytes)
                .putLong(CAMERA_VMD_TIME, timeMillis)
                .commit();
    }

    static void clearVmdImport(Context context) {
        preferences(context)
                .edit()
                .putBoolean(CAMERA_VMD_IMPORTED, false)
                .remove(CAMERA_VMD_NAME)
                .remove(VMD_BYTES)
                .remove(CAMERA_VMD_TIME)
                .commit();
    }

    static boolean isVmdImported(Context context) {
        return preferences(context).getBoolean(CAMERA_VMD_IMPORTED, false);
    }

    static String getVmdName(Context context) {
        return preferences(context).getString(CAMERA_VMD_NAME, "");
    }

    static long getVmdBytes(Context context) {
        return preferences(context).getLong(VMD_BYTES, 0L);
    }

    static long getVmdTime(Context context) {
        return preferences(context).getLong(CAMERA_VMD_TIME, 0L);
    }

    /**
     * Accepts a file whose first 30 bytes are a VMD header, mirroring
     * {@code LoadVmdCamera} exactly: the two generations it knows, nothing else.
     *
     * The check lives here rather than in the settings screen so the same rule
     * can be driven from a plain JVM test. Getting it wrong is invisible until
     * the game refuses the file, and a wrong file that is accepted here is
     * reported by the native loader as "the file is not a VMD motion" - which
     * tells the user nothing about which step rejected it.
     */
    static boolean isVmdMotion(byte[] header) {
        if (header == null || header.length < VMD_HEADER_BYTES) return false;
        String text = new String(header, 0, VMD_HEADER_BYTES, java.nio.charset.StandardCharsets.ISO_8859_1);
        return text.startsWith(VMD_HEADER_0002) || text.startsWith(VMD_HEADER_LEGACY);
    }

    /** The native loader's ceiling, so an import is refused here instead of there. */
    static long vmdMaximumBytes() {
        return 64L * 1024 * 1024;
    }

    /**
     * What one write of the camera configuration did, for the page's status line.
     *
     * {@code firstEnable} is the one change a reload cannot carry. Whether the
     * game process loads the camera module at all is decided by the
     * configuration that process started with, so switching the camera on when
     * the stored configuration was empty still needs a restart - and the page
     * has to say so instead of claiming the change is already live.
     */
    record CameraWrite(boolean changed, boolean firstEnable, boolean delivered) {}

    static CameraWrite setCameraSettings(
            Context context,
            boolean disableDither,
            boolean freeCamera,
            boolean worldPause,
            boolean firstPerson,
            boolean hideHead,
            boolean fillNeck,
            double movementSpeed,
            double fieldOfView,
            double firstPersonFov,
            double eyeForward,
            double eyeHeight,
            double nearClip,
            boolean extendLookRange,
            FirstPersonAdvanced advanced,
            CameraMotion motion) {
        return setCameraSettings(context, disableDither, freeCamera, worldPause, firstPerson,
                hideHead, fillNeck, movementSpeed, fieldOfView, firstPersonFov, eyeForward,
                eyeHeight, nearClip, extendLookRange, advanced, motion,
                getFirstPersonGyro(context));
    }

    /**
     * The camera configuration, with the gyroscope block passed explicitly.
     *
     * The gyroscope is a second entry point because it is edited by its own
     * screen: the first-person page writes it, and every other camera write has
     * to carry the stored value forward rather than reset it. Overloading keeps
     * the many existing callers from having to know the sensor exists.
     */
    static CameraWrite setCameraSettings(
            Context context,
            boolean disableDither,
            boolean freeCamera,
            boolean worldPause,
            boolean firstPerson,
            boolean hideHead,
            boolean fillNeck,
            double movementSpeed,
            double fieldOfView,
            double firstPersonFov,
            double eyeForward,
            double eyeHeight,
            double nearClip,
            boolean extendLookRange,
            FirstPersonAdvanced advanced,
            CameraMotion motion,
            FirstPersonGyro gyro) {
        eyeForward = bounded(eyeForward, 0.03, 0.0, 0.5);
        eyeHeight = bounded(eyeHeight, 0.05, -0.5, 0.5);
        nearClip = bounded(nearClip, 0.03, 0.001, 1.0);
        // The three scalars that go straight to the native parser's own clamps,
        // which are 0.5-100 (speed) and 20-120 (both FOVs). Without this the
        // text could carry a value the reader silently corrects - or "NaN",
        // which "%.4f" renders literally and the parser then hands to the float
        // that drives the camera position. The screen's speed slider starts at
        // 0.2, below the native floor, so that end of its travel has always
        // meant 0.5 in the game; this only makes the configuration say so.
        movementSpeed = bounded(movementSpeed, 5.0, 0.5, 100.0);
        fieldOfView = bounded(fieldOfView, 60.0, 20.0, 120.0);
        firstPersonFov = bounded(firstPersonFov, 75.0, 20.0, 120.0);
        // World pause is a free-camera sub-mode on desktop: its hotkey is only
        // read while the free camera is armed, so offering it alone would be a
        // switch that does nothing.
        boolean pause = worldPause && freeCamera;
        // The gyroscope does NOT force the free camera on.
        //
        // It used to: while the gyroscope's deltas had exactly one consumer -
        // StepFreeCamera, which reads them only with the free camera armed -
        // leaving the free camera off made the switch a lie, so turning the
        // gyroscope on turned on the camera it could reach. That is no longer the
        // arrangement. The native side now folds the same deltas into the game's
        // own first-person rotation, so the gyroscope has a consumer with the
        // free camera down (see ApplyFirstPersonLook in
        // native/modules/camera/module.cpp). Forcing the free camera on would now
        // be actively harmful: both consumers drain one accumulator, so the two
        // cameras would each steal half of the other's motion.
        //
        // The module still has to be running for any of this to exist, and the
        // first person is what the gyroscope is for, so it keeps the test.
        boolean any = disableDither || freeCamera || firstPerson;
        if (gyro.enabled()) {
            firstPerson = true;
            any = true;
        }
        String previous = preferences(context).getString(CAMERA_CONFIGURATION, "");
        if (previous == null) previous = "";
        String configuration = any
                ? "schema_version=3\n"
                        + "enabled=true\n"
                        + "diagnostics=true\n"
                        + "disable_dither_enabled=" + disableDither + "\n"
                        + "free_camera_enabled=" + freeCamera + "\n"
                        + "pause_enabled=" + pause + "\n"
                        + "first_person_camera_enabled=" + firstPerson + "\n"
                        + "first_person_hide_head=" + hideHead + "\n"
                        + "first_person_fill_neck_hole=" + fillNeck + "\n"
                        + "movement_speed=" + number(movementSpeed) + "\n"
                        + "field_of_view=" + number(fieldOfView) + "\n"
                        + "first_person_fov=" + number(firstPersonFov) + "\n"
                        + "first_person_eye_forward=" + number(eyeForward) + "\n"
                        + "first_person_eye_height=" + number(eyeHeight) + "\n"
                        + "first_person_near_clip=" + number(nearClip) + "\n"
                        + "first_person_extend_look_range=" + extendLookRange + "\n"
                        + advanced.toIniLines()
                        + gyro.toIniLines()
                        + "toggle_hotkey=" + Hotkeys.FREE_CAMERA_NAME + "\n"
                        + "pause_hotkey=" + Hotkeys.WORLD_PAUSE_NAME + "\n"
                        + "first_person_hotkey=" + Hotkeys.FIRST_PERSON_NAME + "\n"
                        // "false" here would mean "ignore the look channel", not
                        // "do not hook a cursor". The key gates two different
                        // things: on Windows it arms the low-level mouse hook,
                        // but StepFreeCamera also skips the panel's drag deltas
                        // when it is off, because both feed g_mouse_dx/dy. There
                        // is no cursor to hook on a phone, yet the on-screen look
                        // pad and the gyroscope still have to steer the camera,
                        // so the flag has to stay on and the pointer terms simply
                        // stay zero.
                        + "free_camera_mouse_look=true\n"
                        + "free_camera_smoothing=0.3\n"
                        + motion.toIniLines()
                        // The panel presses the exact codes the desktop module
                        // polls; pin them so a native default change cannot
                        // silently detach the on-screen buttons.
                        + "roll_left_hotkey=" + Hotkeys.ROLL_LEFT_NAME + "\n"
                        + "roll_right_hotkey=" + Hotkeys.ROLL_RIGHT_NAME + "\n"
                        + "fov_wide_hotkey=" + Hotkeys.FOV_WIDE_NAME + "\n"
                        + "fov_narrow_hotkey=" + Hotkeys.FOV_NARROW_NAME + "\n"
                        + "view_reset_hotkey=" + Hotkeys.VIEW_RESET_NAME + "\n"
                        + "motion_hotkey=" + Hotkeys.MOTION_NAME + "\n"
                        + "keyframe_add_hotkey=" + Hotkeys.KEYFRAME_ADD_NAME + "\n"
                        + "keyframe_play_hotkey=" + Hotkeys.KEYFRAME_PLAY_NAME + "\n"
                        + "keyframe_clear_hotkey=" + Hotkeys.KEYFRAME_CLEAR_NAME + "\n"
                        + "vmd_play_hotkey=" + Hotkeys.VMD_PLAY_NAME + "\n"
                : "";
        SharedPreferences.Editor edit = preferences(context)
                .edit()
                .putBoolean(CAMERA_DITHER, disableDither)
                .putBoolean(CAMERA_FREE, freeCamera)
                .putBoolean(CAMERA_PAUSE, pause)
                .putBoolean(CAMERA_FIRST_PERSON, firstPerson)
                .putBoolean(CAMERA_FP_HIDE_HEAD, hideHead)
                .putBoolean(CAMERA_FP_FILL_NECK, fillNeck)
                .putString(CAMERA_SPEED, number(movementSpeed))
                .putString(CAMERA_FOV, number(fieldOfView))
                .putString(CAMERA_FP_FOV, number(firstPersonFov))
                .putString(CAMERA_FP_EYE_FORWARD, number(eyeForward))
                .putString(CAMERA_FP_EYE_HEIGHT, number(eyeHeight))
                .putString(CAMERA_FP_NEAR_CLIP, number(nearClip))
                .putBoolean(CAMERA_FP_EXTEND_LOOK_RANGE, extendLookRange)
                .putString(CAMERA_CONFIGURATION, configuration);
        advanced.store(edit);
        motion.store(edit);
        gyro.store(edit);
        edit.commit();
        if (configuration.equals(previous)) return new CameraWrite(false, false, false);
        // The native module also reads this string once, from the environment
        // the game process sets before loading the library - which is why a
        // parameter change used to cost a restart. Handing the same string to
        // the runtime command pump is what makes the change live: the game
        // process relays it and the camera module replays it through that same
        // boot-time entry point. An empty configuration is delivered too; it is
        // a valid instruction ("turn the camera module's features off") and the
        // native reload carries the exits that assignment alone cannot express.
        boolean delivered = ModuleCommandRouter.issue(context, "camera_config", configuration);
        return new CameraWrite(true, !configuration.isEmpty() && previous.isEmpty(), delivered);
    }

    // ----------------------------------------------------------- sustained dash

    static boolean isSustainedDashEnabled(Context context) {
        return preferences(context).getBoolean(DASH_ENABLED, false);
    }

    static boolean isLiinoCleanDashEnabled(Context context) {
        return preferences(context).getBoolean(DASH_LIINO_CLEAN, false);
    }

    static boolean isDashCharacterEnabled(Context context, String codename) {
        return preferences(context).getBoolean(
                "liino".equals(codename) ? DASH_LIINO : DASH_AGLINA, true);
    }

    static void setSustainedDashSettings(
            Context context,
            boolean enabled,
            boolean liinoClean,
            boolean aglina,
            boolean liino) {
        StringBuilder characters = new StringBuilder();
        if (aglina) characters.append("aglina");
        if (liino) {
            if (characters.length() > 0) characters.append(',');
            characters.append("liino");
        }
        // With no character selected the desktop module would arm every one of
        // them, because an absent key means "all" for older configurations.
        boolean active = enabled && characters.length() > 0;
        String configuration = active
                ? "schema_version=3\n"
                        + "enabled=true\n"
                        + "diagnostics=true\n"
                // The bone-pose overlay is not optional on Android: the banks ship
                // in the APK and are materialized before the module starts, so the
                // looping segment always runs from them rather than the native-only hold.
                        + "external_loop=true\n"
                        + "liino_clean=" + liinoClean + "\n"
                        + "characters=" + characters + "\n"
                : "";
        preferences(context)
                .edit()
                .putBoolean(DASH_ENABLED, enabled)
                .putBoolean(DASH_LIINO_CLEAN, liinoClean)
                .putBoolean(DASH_AGLINA, aglina)
                .putBoolean(DASH_LIINO, liino)
                .putString(ACTIONS_CONFIGURATION, configuration)
                .commit();
    }

    /**
     * Rewrites every configuration string from the stored switches. Used once per
     * launch of the settings screen so an upgrade that introduced a new key, or a
     * carried-over pre-3.3 enhancement choice, reaches the game without the user
     * having to touch each page.
     */
    static void republishConfigurations(Context context) {
        setInterfaceSettings(context, isHideUidEnabled(context), isHideHudEnabled(context));
        setCameraSettings(
                context,
                isDisableDitherEnabled(context),
                isFreeCameraEnabled(context),
                isWorldPauseEnabled(context),
                isFirstPersonEnabled(context),
                isFirstPersonHideHead(context),
                isFirstPersonFillNeck(context),
                parse(getCameraSpeed(context), 5.0),
                parse(getCameraFieldOfView(context), 60.0),
                parse(getFirstPersonFieldOfView(context), 75.0),
                parse(getFirstPersonEyeForward(context), 0.03),
                parse(getFirstPersonEyeHeight(context), 0.05),
                parse(getFirstPersonNearClip(context), 0.03),
                isFirstPersonExtendLookRange(context), getFirstPersonAdvanced(context),
                getCameraMotion(context));
        setSustainedDashSettings(
                context,
                isSustainedDashEnabled(context),
                isLiinoCleanDashEnabled(context),
                isDashCharacterEnabled(context, "aglina"),
                isDashCharacterEnabled(context, "liino"));
    }

    static double parse(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException | NullPointerException invalid) {
            return fallback;
        }
    }

    static String number(double value) {
        String text = String.format(Locale.ROOT, "%.4f", value);
        return text.contains(".")
                ? text.replaceFirst("0+$", "").replaceFirst("\\.$", "") : text;
    }

    private static double bounded(double value, double fallback, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
    }

    // -------------------------------------------------------------------- voice

    static String getVoiceCatalogs(Context context) {
        return preferences(context).getString(VOICE_CATALOGS, "");
    }

    static void setVoiceCatalogs(Context context, String catalogs) {
        preferences(context)
                .edit()
                .putString(VOICE_CATALOGS, catalogs)
                .remove("voice_language")
                .commit();
    }

    static String getVoiceRules(Context context) {
        SharedPreferences preferences = preferences(context);
        String rules = preferences.getString(VOICE_RULES, "");
        if (rules != null && !rules.isEmpty()) {
            return rules;
        }
        String legacy = preferences.getString(VOICE_CATALOGS, "");
        if (legacy == null || legacy.isEmpty()) {
            return "";
        }
        StringBuilder migrated = new StringBuilder();
        for (String item : legacy.split(",")) {
            String speaker = switch (item) {
                case "aglina" -> "chr_0013_aglina";
                case "liino" -> "chr_0035_liino";
                default -> "";
            };
            if (!speaker.isEmpty()) {
                if (migrated.length() > 0) migrated.append(';');
                migrated.append(speaker).append(":Japanese");
            }
        }
        return migrated.toString();
    }

    static void setVoiceRules(Context context, String rules) {
        preferences(context)
                .edit()
                .putString(VOICE_RULES, rules == null ? "" : rules)
                .remove(VOICE_CATALOGS)
                .remove("voice_language")
                .commit();
    }

    // -------------------------------------------------------------- login model

    static void setModelEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(MODEL_ENABLED, enabled).commit();
    }

    static boolean isModelRuntimeEnabled(Context context) {
        SharedPreferences settings = preferences(context);
        return settings.contains(MODEL_RUNTIME_ENABLED)
                ? settings.getBoolean(MODEL_RUNTIME_ENABLED, false)
                : settings.getBoolean(MODEL_ENABLED, false);
    }

    static void setModelRuntimeEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(MODEL_RUNTIME_ENABLED, enabled).commit();
    }

    static boolean isModelEnabled(Context context) {
        return preferences(context).getBoolean(MODEL_ENABLED, false);
    }

    static String getModelCharacter(Context context) {
        return preferences(context).getString(MODEL_CHARACTER, "chr_0013_aglina");
    }

    static String getModelAction(Context context) {
        return preferences(context).getString(MODEL_ACTION, "");
    }

    static boolean isModelFinalLoop(Context context) {
        return preferences(context).getBoolean(MODEL_FINAL_LOOP, true);
    }

    static boolean isModelForceLoop(Context context) {
        return preferences(context).getBoolean(MODEL_FORCE_LOOP, false);
    }

    static boolean isModelCrossfade(Context context) {
        return preferences(context).getBoolean(MODEL_CROSSFADE, false);
    }

    static String getModelLoopStart(Context context) {
        return preferences(context).getString(MODEL_LOOP_START, "0.968");
    }

    static String getModelLoopEnd(Context context) {
        return preferences(context).getString(MODEL_LOOP_END, "2.3760002");
    }

    static String getModelCrossfadeDuration(Context context) {
        return preferences(context).getString(MODEL_CROSSFADE_DURATION, "0.20");
    }

    static String getModelScale(Context context) {
        return preferences(context).getString(MODEL_SCALE, "1.0");
    }

    static boolean isLogoEnabled(Context context) {
        return preferences(context).getBoolean(LOGO_ENABLED, false);
    }

    static String getLogoColor(Context context) {
        return preferences(context).getString(LOGO_COLOR, "#FFC928");
    }

    static void setModelSettings(
            Context context,
            boolean enabled,
            String character,
            String action,
            boolean finalLoop,
            boolean forceLoop,
            boolean crossfade,
            String loopStart,
            String loopEnd,
            String crossfadeDuration,
            String scale,
            boolean logoEnabled,
            String logoColor,
            String configuration) {
        preferences(context)
                .edit()
                .putBoolean(MODEL_ENABLED, enabled)
                .putBoolean(MODEL_RUNTIME_ENABLED, enabled)
                .putString(MODEL_CHARACTER, character)
                .putString(MODEL_ACTION, action)
                .putBoolean(MODEL_FINAL_LOOP, finalLoop)
                .putBoolean(MODEL_FORCE_LOOP, forceLoop)
                .putBoolean(MODEL_CROSSFADE, crossfade)
                .putString(MODEL_LOOP_START, loopStart)
                .putString(MODEL_LOOP_END, loopEnd)
                .putString(MODEL_CROSSFADE_DURATION, crossfadeDuration)
                .putString(MODEL_SCALE, scale)
                .putBoolean(LOGO_ENABLED, logoEnabled)
                .putString(LOGO_COLOR, logoColor)
                .putString(MODEL_CONFIGURATION, configuration)
                .commit();
    }

    private static SharedPreferences preferences(Context context) {
        return FrameworkSettings.open(context);
    }
}
