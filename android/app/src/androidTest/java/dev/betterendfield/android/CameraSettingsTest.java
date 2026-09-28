package dev.betterendfield.android;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

/** Configuration tests called by the registered BemInstallerTest instrumentation. */
final class CameraSettingsTest {
    static void run(Instrumentation instrumentation) {
        Context isolated = new ContextWrapper(instrumentation.getTargetContext()) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("camera-settings-test-" + name, mode);
            }
        };
        SharedPreferences preferences = FrameworkSettings.open(isolated);
        try {
            preferences.edit().clear().commit();
            ModuleSettings.republishConfigurations(isolated);
            check("0.03".equals(ModuleSettings.getFirstPersonEyeForward(isolated)), "forward default");
            check("0.05".equals(ModuleSettings.getFirstPersonEyeHeight(isolated)), "height default");
            check("0.03".equals(ModuleSettings.getFirstPersonNearClip(isolated)), "clip default");
            check(!ModuleSettings.isFirstPersonExtendLookRange(isolated), "look range default");
            ModuleSettings.setCameraSettings(isolated, false, false, false, true, true, true,
                    5, 60, 75, 0.1234, -0.25, 0.001, true, ModuleSettings.getFirstPersonAdvanced(isolated));
            ModuleSettings.republishConfigurations(isolated);
            String ini = ModuleConfigurations.read(preferences).camera();
            check(ini.contains("first_person_eye_forward=0.1234\n"), "forward round trip");
            check(ini.contains("first_person_eye_height=-0.25\n"), "height round trip");
            check(ini.contains("first_person_near_clip=0.001\n"), "clip round trip");
            check(ini.contains("first_person_extend_look_range=true\n"), "look range round trip");
            // A save of an unrelated camera option and disabling the feature must
            // retain the first-person preferences for the next game launch.
            saveStored(isolated, false);
            check(ModuleConfigurations.read(preferences).camera().isEmpty(), "disabled module");
            ModuleSettings.republishConfigurations(isolated);
            saveStored(isolated, true);
            check(ModuleConfigurations.read(preferences).camera().contains(
                    "first_person_eye_forward=0.1234\n"), "disabled/re-enabled retains forward");
            check(ModuleConfigurations.read(preferences).camera().contains(
                    "first_person_extend_look_range=true\n"), "disabled/re-enabled retains switch");
            ModuleSettings.setCameraSettings(isolated, false, false, false, true, true, true,
                    5, 60, 75, -1, 2, 0, false, ModuleSettings.getFirstPersonAdvanced(isolated));
            ini = ModuleConfigurations.read(preferences).camera();
            check(ini.contains("first_person_eye_forward=0\n"), "forward lower bound");
            check(ini.contains("first_person_eye_height=0.5\n"), "height upper bound");
            check(ini.contains("first_person_near_clip=0.001\n"), "clip lower bound");
            ModuleSettings.setCameraSettings(isolated, false, false, false, true, true, true,
                    5, 60, 75, 2, -2, 2, false, ModuleSettings.getFirstPersonAdvanced(isolated));
            ini = ModuleConfigurations.read(preferences).camera();
            check(ini.contains("first_person_eye_forward=0.5\n"), "forward upper bound");
            check(ini.contains("first_person_eye_height=-0.5\n"), "height lower bound");
            check(ini.contains("first_person_near_clip=1\n"), "clip upper bound");
            ModuleSettings.setCameraSettings(isolated, false, false, false, true, true, true,
                    5, 60, 75, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
                    ModuleSettings.getFirstPersonAdvanced(isolated));
            ini = ModuleConfigurations.read(preferences).camera();
            check(ini.contains("first_person_eye_forward=0.03\n"), "nonfinite forward fallback");
            check(ini.contains("first_person_eye_height=0.05\n"), "nonfinite height fallback");
            check(ini.contains("first_person_near_clip=0.03\n"), "nonfinite clip fallback");
            boolean[] sliderChecks = new boolean[3];
            instrumentation.runOnMainSync(() -> {
                ValueSlider slider = new ValueSlider(isolated, "clip", "", 0.001f, 1f, 999, 4);
                slider.setValue(0.03f);
                sliderChecks[0] = slider.getValue() == 0.03f;
                slider.setValue(0.1234f);
                sliderChecks[1] = slider.getValue() == 0.1234f;
                slider.setAvailable(false);
                slider.setAvailable(true);
                sliderChecks[2] = slider.getValue() == 0.1234f;
            });
            check(sliderChecks[0], "slider retains exact default");
            check(sliderChecks[1], "slider retains unedited stored precision");
            check(sliderChecks[2], "availability preserves value");
            testAdvanced(isolated);
        } finally {
            preferences.edit().clear().commit();
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static void saveStored(Context context, boolean enabled) {
        ModuleSettings.setCameraSettings(context, false, false, false, enabled, true, true,
                7, 60, 75,
                ModuleSettings.parse(ModuleSettings.getFirstPersonEyeForward(context), 0.03),
                ModuleSettings.parse(ModuleSettings.getFirstPersonEyeHeight(context), 0.05),
                ModuleSettings.parse(ModuleSettings.getFirstPersonNearClip(context), 0.03),
                ModuleSettings.isFirstPersonExtendLookRange(context), ModuleSettings.getFirstPersonAdvanced(context));
    }

    private static void testAdvanced(Context context) {
        ModuleSettings.FirstPersonAdvanced defaults = ModuleSettings.getFirstPersonAdvanced(context);
        check(!defaults.movement() && defaults.animationMode() == 0 && !defaults.yieldDialogue()
                && !defaults.thirdPersonInCombat()
                && defaults.transitionSeconds() == 0 && !defaults.externalHeadScale(), "advanced defaults off");
        check(defaults.sideLookLimit() == 60 && defaults.animationStrength() == 0.35, "advanced tuning defaults");
        ModuleSettings.setCameraSettings(context, false, false, false, true, true, true,
                5, 60, 75, 0.03, 0.05, 0.03, false, defaults);
        check(ModuleConfigurations.read(FrameworkSettings.open(context)).camera().contains(
                "first_person_third_person_in_combat=false\n"), "combat INI default off");
        ModuleSettings.FirstPersonAdvanced configured = new ModuleSettings.FirstPersonAdvanced(
                true, 42.5, 3, 0.1234, true, true, 0.2468, true);
        ModuleSettings.setCameraSettings(context, false, false, false, true, true, true,
                5, 60, 75, 0.03, 0.05, 0.03, false, configured);
        ModuleSettings.republishConfigurations(context);
        check(configured.equals(ModuleSettings.getFirstPersonAdvanced(context)), "advanced round trip");
        String ini = ModuleConfigurations.read(FrameworkSettings.open(context)).camera();
        for (String line : new String[]{"first_person_movement=true", "first_person_side_look_limit=42.5",
                "first_person_animation_mode=3", "first_person_animation_strength=0.1234",
                "first_person_yield_dialogue=true", "first_person_third_person_in_combat=true",
                "first_person_transition_seconds=0.2468",
                "first_person_external_head_scale=true"}) {
            check(ini.contains(line + "\n"), line);
        }
        ModuleSettings.setCameraSettings(context, false, false, false, false, true, true,
                5, 60, 75, 0.03, 0.05, 0.03, false, configured);
        ModuleSettings.republishConfigurations(context);
        check(configured.equals(ModuleSettings.getFirstPersonAdvanced(context)), "disabled advanced retained");
        saveStored(context, true);
        check(configured.equals(ModuleSettings.getFirstPersonAdvanced(context)), "re-enabled advanced retained");
        check(ModuleConfigurations.read(FrameworkSettings.open(context)).camera().contains(
                "first_person_third_person_in_combat=true\n"), "re-enabled combat INI retained");
        ModuleSettings.FirstPersonAdvanced bounded = new ModuleSettings.FirstPersonAdvanced(
                true, 100, 99, -1, true, true, 2, true);
        check(bounded.sideLookLimit() == 90 && bounded.animationMode() == 0
                && bounded.animationStrength() == 0 && bounded.transitionSeconds() == 1, "advanced bounds");
        bounded = new ModuleSettings.FirstPersonAdvanced(false, -1, 2, 2, false, false, -1, false);
        check(bounded.sideLookLimit() == 0 && bounded.animationMode() == 2
                && bounded.animationStrength() == 1 && bounded.transitionSeconds() == 0, "opposite advanced bounds");
        bounded = new ModuleSettings.FirstPersonAdvanced(false, Double.NaN, -1,
                Double.POSITIVE_INFINITY, false, false, Double.NaN, false);
        check(bounded.sideLookLimit() == 60 && bounded.animationMode() == 0
                && bounded.animationStrength() == 0.35 && bounded.transitionSeconds() == 0, "advanced finite fallback");
    }
}
