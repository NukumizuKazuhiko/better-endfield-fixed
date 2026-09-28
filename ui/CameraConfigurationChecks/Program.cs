using BetterEndfield.UI.Services;
using BetterEndfield.UI.Models;
using System.Text;

string directory = Path.Combine(args.Length > 0 ? args[0] : Path.GetTempPath(), "camera-config-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(directory);
string path = Path.Combine(directory, "BetterEndfield.ini");
try
{
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\nfirst_person_eye_forward=0.12\nfirst_person_eye_height=-0.15\nfirst_person_near_clip=0.008\nfirst_person_extend_look_range=true\n", Encoding.Unicode);
    await ConfigurationService.SaveCameraEnhancementConfigurationAsync(true, false, false, "9", "8", 5, 60, configurationPath: path);
    string actual = await File.ReadAllTextAsync(path);
    foreach (string expected in new[] { "first_person_eye_forward=0.12", "first_person_eye_height=-0.15", "first_person_near_clip=0.008", "first_person_extend_look_range=true" })
        if (!actual.Contains(expected)) throw new InvalidOperationException("Camera save lost " + expected);
    Console.WriteLine("PASS: saving other camera settings preserves four first-person parameters");
    var loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.12 || loaded.FirstPersonEyeHeight != -0.15 || loaded.FirstPersonNearClip != 0.008 || !loaded.FirstPersonExtendLookRange)
        throw new InvalidOperationException("First-person values changed on load");
    await File.WriteAllTextAsync(path, loaded.ToIni(), Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.12 || loaded.FirstPersonEyeHeight != -0.15 || loaded.FirstPersonNearClip != 0.008 || !loaded.FirstPersonExtendLookRange)
        throw new InvalidOperationException("Full configuration round-trip lost first-person values");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=3\nfirst_person_eye_forward=0.5\nfirst_person_eye_height=-0.5\nfirst_person_near_clip=0.001\nfirst_person_extend_look_range=true\n", Encoding.Unicode);
    await ConfigurationService.LoadModConfigurationAsync("", path);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.5 || loaded.FirstPersonEyeHeight != -0.5 || loaded.FirstPersonNearClip != 0.001 || !loaded.FirstPersonExtendLookRange)
        throw new InvalidOperationException("Schema migration lost first-person values");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.03 || loaded.FirstPersonEyeHeight != 0.05 || loaded.FirstPersonNearClip != 0.03 || loaded.FirstPersonExtendLookRange)
        throw new InvalidOperationException("Missing keys must use native defaults");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\nfirst_person_eye_forward=NaN\nfirst_person_eye_height=-4\nfirst_person_near_clip=Infinity\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.03 || loaded.FirstPersonEyeHeight != -0.5 || loaded.FirstPersonNearClip != 0.03)
        throw new InvalidOperationException("Invalid input must stay finite and within native ranges");
    await ConfigurationService.SaveCameraEnhancementConfigurationAsync(false, false, false, "9", "8", 5, 60,
        configurationPath: path, firstPersonEyeForward: 9, firstPersonEyeHeight: 9,
        firstPersonNearClip: -1, firstPersonExtendLookRange: true);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.5 || loaded.FirstPersonEyeHeight != 0.5 || loaded.FirstPersonNearClip != 0.001 || !loaded.FirstPersonExtendLookRange)
        throw new InvalidOperationException("Edited save must normalize and persist all four keys");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\nfirst_person_eye_forward=invalid\nfirst_person_eye_height=Infinity\nfirst_person_near_clip=12\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonEyeForward != 0.03 || loaded.FirstPersonEyeHeight != 0.05 || loaded.FirstPersonNearClip != 1)
        throw new InvalidOperationException("Malformed input and upper limit mismatch");
    Console.WriteLine("PASS: full round-trip, defaults, migration, boundary and invalid input");
    string[] advancedValues = ["first_person_movement=true", "first_person_side_look_limit=42",
        "first_person_animation_mode=3", "first_person_animation_strength=0.6",
        "first_person_yield_dialogue=true", "first_person_third_person_in_combat=true",
        "first_person_transition_seconds=0.4",
        "first_person_external_head_scale=true"];
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\n" + string.Join('\n', advancedValues) + "\n", Encoding.Unicode);
    await ConfigurationService.SaveCameraEnhancementConfigurationAsync(true, false, false, "9", "8", 5, 60, configurationPath: path);
    actual = await File.ReadAllTextAsync(path);
    foreach (string expected in advancedValues)
        if (!actual.Contains(expected)) throw new InvalidOperationException("Camera save lost advanced setting " + expected);
    Console.WriteLine("PASS: editing other camera settings preserves all eight advanced parameters");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\nfirst_person_animation_mode=9\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonExtras.AnimationMode != 0)
        throw new InvalidOperationException("Invalid animation mode must disable motion");
    foreach (string mode in new[] { "-1", "1.5", "NaN", "Infinity", "bad" })
    {
        await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\nfirst_person_animation_mode=" + mode + "\n", Encoding.Unicode);
        loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
        if (loaded.FirstPersonExtras.AnimationMode != 0) throw new InvalidOperationException("Unsafe animation mode accepted: " + mode);
    }
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\n" + string.Join('\n', advancedValues) + "\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    await File.WriteAllTextAsync(path, loaded.ToIni(), Encoding.Unicode);
    actual = await File.ReadAllTextAsync(path);
    foreach (string expected in advancedValues)
        if (!actual.Contains(expected)) throw new InvalidOperationException("Full save lost advanced setting " + expected);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    var advanced = loaded.FirstPersonExtras;
    if (!advanced.Movement || advanced.SideLookLimit != 42 || advanced.AnimationMode != 3 || advanced.AnimationStrength != 0.6 ||
        !advanced.YieldDialogue || !advanced.ThirdPersonInCombat || advanced.TransitionSeconds != 0.4 || !advanced.ExternalHeadScale)
        throw new InvalidOperationException("Advanced full round-trip mismatch");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=3\n" + string.Join('\n', advancedValues) + "\n", Encoding.Unicode);
    await ConfigurationService.LoadModConfigurationAsync("", path);
    actual = await File.ReadAllTextAsync(path);
    foreach (string expected in advancedValues)
        if (!actual.Contains(expected)) throw new InvalidOperationException("Migration lost advanced setting " + expected);
    await ConfigurationService.SaveCameraEnhancementConfigurationAsync(false, false, false, "9", "8", 5, 60,
        configurationPath: path, firstPersonExtras: new FirstPersonExtras
        {
            Movement = true, SideLookLimit = 25, AnimationMode = 2, AnimationStrength = 0.75,
            YieldDialogue = true, ThirdPersonInCombat = true, TransitionSeconds = 0.5, ExternalHeadScale = true
        });
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    advanced = loaded.FirstPersonExtras;
    if (loaded.FirstPersonCameraEnabled || !advanced.Movement || advanced.SideLookLimit != 25 || advanced.AnimationMode != 2 ||
        advanced.AnimationStrength != 0.75 || !advanced.YieldDialogue || !advanced.ThirdPersonInCombat || advanced.TransitionSeconds != 0.5 || !advanced.ExternalHeadScale)
        throw new InvalidOperationException("Explicit advanced edits or disabled-feature retention failed");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    advanced = loaded.FirstPersonExtras;
    if (advanced.Movement || advanced.SideLookLimit != 60 || advanced.AnimationMode != 0 || advanced.AnimationStrength != 0.35 ||
        advanced.YieldDialogue || advanced.ThirdPersonInCombat || advanced.TransitionSeconds != 0 || advanced.ExternalHeadScale)
        throw new InvalidOperationException("Advanced defaults mismatch");
    await ConfigurationService.SaveCameraEnhancementConfigurationAsync(false, false, false, "9", "8", 5, 60,
        configurationPath: path, firstPersonExtras: new FirstPersonExtras
        { SideLookLimit = 120, AnimationMode = 7, AnimationStrength = double.NaN, TransitionSeconds = -2 });
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    advanced = loaded.FirstPersonExtras;
    if (advanced.SideLookLimit != 90 || advanced.AnimationMode != 0 || advanced.AnimationStrength != 0.35 || advanced.TransitionSeconds != 0)
        throw new InvalidOperationException("Advanced normalization mismatch");
    await File.WriteAllTextAsync(path, "[betterendfield.camera]\nschema_version=4\nfirst_person_side_look_limit=-1\nfirst_person_animation_strength=2\nfirst_person_transition_seconds=9\n", Encoding.Unicode);
    loaded = await ConfigurationService.LoadModConfigurationAsync("", path);
    if (loaded.FirstPersonExtras.SideLookLimit != 0 || loaded.FirstPersonExtras.AnimationStrength != 1 || loaded.FirstPersonExtras.TransitionSeconds != 1)
        throw new InvalidOperationException("Advanced boundary clamp mismatch");
    Console.WriteLine("PASS: advanced full round-trip, explicit edit, migration, disabled retention, defaults and invalid input");
}
finally { Directory.Delete(directory, recursive: true); }
