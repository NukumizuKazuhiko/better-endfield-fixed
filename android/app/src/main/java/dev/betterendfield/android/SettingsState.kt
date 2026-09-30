package dev.betterendfield.android

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The native loader compares a 30-byte VMD header; the import check mirrors it. */
private const val VMD_HEADER_BYTES = 30
private const val VMD_TIME_PATTERN = "yyyy-MM-dd HH:mm"

/**
 * Page indices for the settings tree.
 *
 * Four tabs, and five sub-pages that are reached from a card inside a tab. Both
 * kinds are addressed by the same integer because the shell switches on one
 * value; [parentOf] is what keeps the tab strip highlighted while a sub-page is
 * open, so "which tab am I in" stays answerable on every screen.
 *
 * The sub-page ids start well above the tab ids so [isSubPage] is a range check
 * rather than a second table to keep in sync.
 */
object SettingsPage {
    /* --- tabs, in the order they are shown --- */

    const val HOME = 0
    const val EXPERIENCE = 1
    const val CHARACTERS = 2
    const val TOOLS = 3

    val TAB_ORDER = intArrayOf(HOME, EXPERIENCE, CHARACTERS, TOOLS)

    val TAB_LABELS = intArrayOf(
        R.string.page_home,
        R.string.page_experience,
        R.string.page_characters,
        R.string.page_tools,
    )

    /* --- sub-pages, reached from a card --- */

    const val FIRST_PERSON = 10
    const val APPEARANCE = 11
    const val LOG = 12
    const val ABOUT = 13
    const val CAMERA_MOTION = 14

    fun isSubPage(page: Int): Boolean = page >= FIRST_PERSON

    /** The tab a page belongs to, so a sub-page keeps its parent highlighted. */
    fun parentOf(page: Int): Int = when (page) {
        FIRST_PERSON, CAMERA_MOTION -> EXPERIENCE
        APPEARANCE -> CHARACTERS
        LOG, ABOUT -> TOOLS
        else -> page
    }

    fun titleOf(page: Int): Int = when (page) {
        FIRST_PERSON -> R.string.page_first_person
        CAMERA_MOTION -> R.string.page_camera_motion
        APPEARANCE -> R.string.page_appearance
        LOG -> R.string.page_log
        ABOUT -> R.string.page_about
        else -> TAB_LABELS[parentOf(page)]
    }
}

/** The per-character language choices, in the order the desktop parser expects. */
val LANGUAGE_VALUES = arrayOf("FollowGlobal", "Chinese", "English", "Japanese", "Korean")
val LANGUAGE_LABELS = listOf("跟随游戏", "中文", "English", "日本語", "한국어")

/**
 * Every setting the companion app can change, held as Compose state.
 *
 * One holder rather than a view model per page: the pages cross-reference each
 * other (the diagnostics page reports what the enhancement page selected, and
 * the enhancement page's switches gate each other), and `ModuleSettings` is
 * itself a single flat preference store. Keeping the reads and writes in one
 * place is what makes "what will the game load" answerable in one pass.
 */
class SettingsState(private val context: Context) {

    var page by mutableStateOf(SettingsPage.HOME)
        private set

    var status by mutableStateOf(context.getString(R.string.current_setting_ready))
        private set

    /**
     * How many settings have been written since this screen was opened.
     *
     * Every write lands in the framework snapshot the game process reads on its
     * next start, so "changes are pending" is knowable here; which ones the
     * *running* process has already picked up is not, and the home page says so
     * in exactly those terms instead of guessing at a restart.
     */
    var pendingChanges by mutableStateOf(0)
        private set

    val restartPending: Boolean get() = pendingChanges > 0

    private fun markPending() {
        pendingChanges++
    }

    fun openPage(index: Int) {
        page = index
        // Both the tools tab and the log page report the snapshot the game will
        // load next, which any other page can have changed since it was built.
        if (index == SettingsPage.TOOLS || index == SettingsPage.LOG) refreshDiagnostics()
        if (index == SettingsPage.HOME) refreshHome()
    }

    /** Returns from a sub-page to the tab that owns it. */
    fun back() {
        if (SettingsPage.isSubPage(page)) openPage(SettingsPage.parentOf(page))
    }

    fun restorePage(index: Int) {
        page = index
    }

    /* ----------------------------------------------------------------- 开屏模型 */

    private var modelIndex: ModelPresetIndex? = null
    private var modelLoadError: String? = null

    var modelTableStatus by mutableStateOf("")
        private set
    var modelSelectionStatus by mutableStateOf("")
        private set

    /**
     * The preset table itself stays private; the screen only ever needs the two
     * display strings, and `ModulePresetIndex.Character` is deliberately not part
     * of this class's surface.
     */
    private var characters: List<ModelPresetIndex.Character> = emptyList()
    private var actions: List<ModelPresetIndex.Action> = emptyList()

    var characterNames by mutableStateOf<List<String>>(emptyList())
        private set
    var actionIds by mutableStateOf<List<String>>(emptyList())
        private set
    var characterIndex by mutableStateOf(0)
        private set
    var actionIndex by mutableStateOf(0)
        private set

    var modelEnabled by mutableStateOf(false)
        private set
    var modelFinalLoop by mutableStateOf(true)
        private set
    var modelForceLoop by mutableStateOf(false)
        private set
    var modelCrossfade by mutableStateOf(false)
        private set
    var logoEnabled by mutableStateOf(false)
        private set

    var modelScale by mutableStateOf("1.0")
        private set
    var modelLoopStart by mutableStateOf("0.968")
        private set
    var modelLoopEnd by mutableStateOf("2.3760002")
        private set
    var modelCrossfadeDuration by mutableStateOf("0.20")
        private set
    var logoColor by mutableStateOf("#FFC928")
        private set

    /** The colour the wheel shows; kept separate so a half-typed hex does not reset it. */
    var logoWheelArgb by mutableStateOf(0x00FFC928)
        private set

    /** False when the desktop preset table is missing, which disables the page. */
    var modelAvailable by mutableStateOf(true)
        private set

    private fun loadModelPresets() {
        modelIndex = try {
            ModelPresetIndex.load(context)
        } catch (error: Exception) {
            modelLoadError = error.message
            null
        }
        val index = modelIndex
        if (index == null) {
            modelAvailable = false
            modelTableStatus = context.getString(R.string.model_table_failed, modelLoadError ?: "")
            return
        }
        characters = index.characters()
        characterNames = characters.map { it.displayName() }
        modelEnabled = ModuleSettings.isModelEnabled(context)
        modelFinalLoop = ModuleSettings.isModelFinalLoop(context)
        modelForceLoop = ModuleSettings.isModelForceLoop(context)
        modelCrossfade = ModuleSettings.isModelCrossfade(context)
        if (modelCrossfade) modelFinalLoop = true
        logoEnabled = ModuleSettings.isLogoEnabled(context)
        modelScale = ModuleSettings.getModelScale(context)
        modelLoopStart = ModuleSettings.getModelLoopStart(context)
        modelLoopEnd = ModuleSettings.getModelLoopEnd(context)
        modelCrossfadeDuration = ModuleSettings.getModelCrossfadeDuration(context)
        logoColor = ModuleSettings.getLogoColor(context)
        logoWheelArgb = logoColor.removePrefix("#").trim().toIntOrNull(16) ?: 0x00FFC928
        characterIndex = findCharacterPosition(ModuleSettings.getModelCharacter(context))
        refreshActionOptions(ModuleSettings.getModelAction(context))
        updateModelSelectionStatus()
        modelTableStatus = context.getString(
            R.string.model_table_ready,
            characters.size,
            index.actionCount(),
        )
    }

    private fun findCharacterPosition(id: String): Int =
        characters.indexOfFirst { it.id().equals(id, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?: characters.indexOfFirst { "chr_0013_aglina".equals(it.id(), ignoreCase = true) }
                .takeIf { it >= 0 }
            ?: 0

    /** Reloads the action list for the selected character, keeping [preferred] if it exists. */
    private fun refreshActionOptions(preferred: String) {
        val character = characters.getOrNull(characterIndex) ?: return
        actions = character.actions()
        actionIds = actions.map { it.id() }
        val desired = preferred.ifEmpty { character.defaultActionId() }
        actionIndex = actions.indexOfFirst { it.id().equals(desired, ignoreCase = true) }
            .takeIf { it >= 0 } ?: 0
        updateModelSelectionStatus()
    }

    private fun updateModelSelectionStatus() {
        val character = characters.getOrNull(characterIndex)
        val action = actions.getOrNull(actionIndex)
        if (character != null && action != null) {
            modelSelectionStatus = context.getString(
                R.string.model_selection_ready,
                character.id(),
                action.id(),
            )
        }
    }

    fun selectCharacter(index: Int) {
        if (index == characterIndex) return
        characterIndex = index
        refreshActionOptions("")
        saveModelSettings()
    }

    fun selectAction(index: Int) {
        if (index == actionIndex) return
        actionIndex = index
        saveModelSettings()
    }

    fun updateModelEnabled(value: Boolean) {
        modelEnabled = value
        saveModelSettings()
    }

    fun updateModelFinalLoop(value: Boolean) {
        modelFinalLoop = value
        // Crossfading has nothing to blend into without a loop, so turning the
        // loop off has to turn the blend off with it rather than leave a switch
        // that silently does nothing.
        if (!value) modelCrossfade = false
        saveModelSettings()
    }

    fun updateModelForceLoop(value: Boolean) {
        modelForceLoop = value
        saveModelSettings()
    }

    fun updateModelCrossfade(value: Boolean) {
        modelCrossfade = value
        if (value) modelFinalLoop = true
        saveModelSettings()
    }

    fun updateLogoEnabled(value: Boolean) {
        logoEnabled = value
        saveModelSettings()
    }

    fun updateModelScale(value: String) {
        modelScale = value
    }

    fun updateModelLoopStart(value: String) {
        modelLoopStart = value
    }

    fun updateModelLoopEnd(value: String) {
        modelLoopEnd = value
    }

    fun updateModelCrossfadeDuration(value: String) {
        modelCrossfadeDuration = value
    }

    fun updateLogoColor(value: String) {
        logoColor = value
        logoColor.removePrefix("#").trim().toIntOrNull(16)?.let { logoWheelArgb = it and 0xFFFFFF }
    }

    /** Called by the wheel while dragging and once more when the finger lifts. */
    fun onWheelColorChanged(argb: Int, committed: Boolean) {
        val masked = argb and 0xFFFFFF
        logoWheelArgb = masked
        logoColor = String.format(Locale.ROOT, "#%06X", masked)
        if (committed) saveModelSettings()
    }

    fun choosePaletteColor(hex: String) {
        logoColor = hex
        hex.removePrefix("#").toIntOrNull(16)?.let { logoWheelArgb = it and 0xFFFFFF }
        saveModelSettings()
    }

    fun saveModelSettings() {
        if (modelIndex == null) return
        val character = characters.getOrNull(characterIndex) ?: return
        val action = actions.getOrNull(actionIndex) ?: return

        val scale = modelScale.trim().toDoubleOrNull()
        if (scale == null || !scale.isFinite() || scale < 0.05 || scale > 20.0) {
            modelSelectionStatus = context.getString(
                R.string.model_settings_invalid,
                "模型缩放应为 0.05–20",
            )
            return
        }

        val loopStart = modelLoopStart.trim().toDoubleOrNull()
        val loopEnd = modelLoopEnd.trim().toDoubleOrNull()
        val crossfadeDuration = modelCrossfadeDuration.trim().toDoubleOrNull()
        if (loopStart == null || loopEnd == null || crossfadeDuration == null ||
            !loopStart.isFinite() || loopStart < 0.0 || loopStart > 30.0 ||
            !loopEnd.isFinite() || loopEnd < 0.05 || loopEnd > 60.0 ||
            loopEnd < loopStart + 0.05 ||
            !crossfadeDuration.isFinite() || crossfadeDuration < 0.01 || crossfadeDuration > 10.0 ||
            crossfadeDuration > (loopEnd - loopStart) * 0.5
        ) {
            modelSelectionStatus = context.getString(
                R.string.model_settings_invalid,
                "循环区间或混合时长无效",
            )
            return
        }

        val color = logoColor.trim().uppercase(Locale.ROOT)
        if (!color.matches(HEX_COLOR)) {
            modelSelectionStatus = context.getString(
                R.string.model_settings_invalid,
                "主题色应为 #RRGGBB",
            )
            return
        }
        logoColor = color
        color.removePrefix("#").toIntOrNull(16)?.let { logoWheelArgb = it }

        val configuration = if (modelEnabled || logoEnabled) {
            buildModelConfiguration(character, action, scale, color, loopStart, loopEnd, crossfadeDuration)
        } else {
            ""
        }
        ModuleSettings.setModelSettings(
            context,
            modelEnabled,
            character.id(),
            action.id(),
            modelFinalLoop,
            modelForceLoop,
            modelCrossfade,
            number(loopStart, 8),
            number(loopEnd, 8),
            number(crossfadeDuration, 8),
            number(scale, 8),
            logoEnabled,
            color,
            configuration,
        )
        modelScale = number(scale, 8)
        modelLoopStart = number(loopStart, 8)
        modelLoopEnd = number(loopEnd, 8)
        modelCrossfadeDuration = number(crossfadeDuration, 8)
        updateModelSelectionStatus()
        markPending()
        status = context.getString(R.string.model_restart_required)
    }

    /**
     * The configuration string the native model module parses. Field order and
     * spelling are the desktop contract; changing either silently stops the game
     * from finding a key, so this mirrors the original line for line.
     */
    private fun buildModelConfiguration(
        character: ModelPresetIndex.Character,
        action: ModelPresetIndex.Action,
        scale: Double,
        color: String,
        loopStart: Double,
        loopEnd: Double,
        crossfadeDuration: Double,
    ): String = buildString {
        appendLine("schema_version=5")
        appendLine("enabled=true")
        appendLine("model_replacement_enabled=$modelEnabled")
        appendLine("logo_theme_enabled=$logoEnabled")
        appendLine("logo_theme_color=$color")
        appendLine("diagnostics=true")
        appendLine("character=${character.id()}")
        appendLine("final_action=${action.id()}")
        appendLine("model_path=${character.modelPath()}")
        appendLine("model_path_hash=${character.modelPathHash()}")
        appendLine("model_bundle_hash=${character.modelBundleHash()}")
        appendAsset("sit_loop", character.sitLoop())
        appendAsset("sit_special", character.sitSpecial())
        appendAsset("sit_to_walk", character.sitToWalk())
        appendLine("final_path=${action.path()}")
        appendLine("final_path_hash=${action.pathHash()}")
        appendLine("final_label=${action.id()}")
        appendLine("final_native_loop=${action.nativeLoop()}")
        appendLine("start_yaw=-120")
        appendLine("turn_duration=3.0333335")
        appendLine("scale=${number(scale, 8)}")
        appendLine("forward_lean_sample=1")
        appendLine("sit_loop_speed=1")
        appendLine("sit_special_speed=1")
        appendLine("sit_to_walk_speed=1")
        appendLine("final_speed=1")
        appendLine("final_loop=$modelFinalLoop")
        appendLine("force_loop=$modelForceLoop")
        appendLine("use_crossfade=$modelCrossfade")
        appendLine("loop_start=${number(loopStart, 8)}")
        appendLine("loop_end=${number(loopEnd, 8)}")
        appendLine("crossfade_duration=${number(crossfadeDuration, 8)}")
    }

    private fun StringBuilder.appendAsset(prefix: String, asset: ModelPresetIndex.Asset) {
        appendLine("${prefix}_path=${asset.path()}")
        appendLine("${prefix}_path_hash=${asset.pathHash()}")
        appendLine("${prefix}_label=${asset.label()}")
    }

    /* ----------------------------------------------------------------- 角色配音 */

    var voiceTableStatus by mutableStateOf("")
        private set
    var voiceReady by mutableStateOf(false)
        private set

    /** Character ids in catalog order; the serialized rule string follows it. */
    var voiceCharacterIds by mutableStateOf<List<String>>(emptyList())
        private set
    var voiceCharacterNames by mutableStateOf<List<String>>(emptyList())
        private set

    private val voiceSelection = mutableStateMapOf<String, Int>()
    private var lastSavedRules = ""

    fun prepareVoice(intent: Intent) {
        if (BuildConfig.DEBUG && intent.hasExtra("voice_rules")) {
            ModuleSettings.setVoiceRules(context, intent.getStringExtra("voice_rules"))
        }
        val index = try {
            VoiceCatalogIndex.load(context)
        } catch (error: Exception) {
            voiceTableStatus = context.getString(R.string.voice_table_failed, error.message)
            status = context.getString(R.string.voice_table_unavailable)
            return
        }
        lastSavedRules = ModuleSettings.getVoiceRules(context)
        val configured = parseRules(lastSavedRules)
        val choices = index.characters()
        choices.forEach { choice ->
            voiceSelection[choice.characterId()] = languagePosition(
                configured[choice.characterId()] ?: "FollowGlobal",
            )
        }
        voiceCharacterIds = choices.map { it.characterId() }
        voiceCharacterNames = choices.map { it.displayName() }
        voiceReady = true
        voiceTableStatus = context.getString(
            R.string.voice_table_ready,
            choices.size - 1,
            index.catalogCount(),
        )
    }

    fun voiceLanguage(characterId: String): Int = voiceSelection[characterId] ?: 0

    fun updateVoiceLanguage(characterId: String, position: Int) {
        voiceSelection[characterId] = position
        saveVoiceRules()
    }

    private fun saveVoiceRules() {
        if (voiceSelection.isEmpty()) return
        val serialized = voiceCharacterIds.mapNotNull { id ->
            val position = voiceSelection[id] ?: 0
            if (position <= 0 || position >= LANGUAGE_VALUES.size) {
                null
            } else {
                "$id:${LANGUAGE_VALUES[position]}"
            }
        }.joinToString(";")
        if (serialized == lastSavedRules) return
        ModuleSettings.setVoiceRules(context, serialized)
        lastSavedRules = serialized
        markPending()
        status = context.getString(R.string.restart_required)
    }

    private fun parseRules(value: String): Map<String, String> {
        val rules = LinkedHashMap<String, String>()
        if (value.isEmpty()) return rules
        for (item in value.split(";")) {
            val separator = item.indexOf(':')
            if (separator > 0 && separator + 1 < item.length) {
                rules[item.substring(0, separator)] = item.substring(separator + 1)
            }
        }
        return rules
    }

    private fun languagePosition(value: String): Int =
        LANGUAGE_VALUES.indexOfFirst { it.equals(value, ignoreCase = true) }.takeIf { it >= 0 } ?: 0

    /* ----------------------------------------------------------------- 画面增强 */

    // betterendfield.ui
    var hideUid by mutableStateOf(false)
        private set
    var hideHud by mutableStateOf(false)
        private set

    // betterendfield.camera
    var disableDither by mutableStateOf(false)
        private set
    var freeCamera by mutableStateOf(false)
        private set
    var worldPause by mutableStateOf(false)
        private set
    var firstPerson by mutableStateOf(false)
        private set
    var firstPersonHideHead by mutableStateOf(true)
        private set
    var firstPersonFillNeck by mutableStateOf(true)
        private set
    var cameraSpeed by mutableStateOf(5.0f)
        private set
    var cameraFov by mutableStateOf(60.0f)
        private set
    var firstPersonFov by mutableStateOf(75.0f)
        private set
    var firstPersonEyeForward by mutableStateOf(0.03f)
        private set
    var firstPersonEyeHeight by mutableStateOf(0.05f)
        private set
    var firstPersonNearClip by mutableStateOf(0.03f)
        private set
    var firstPersonExtendLookRange by mutableStateOf(false)
        private set

    // The advanced first-person block, stored as one record by ModuleSettings.
    var firstPersonMovement by mutableStateOf(false)
        private set
    var firstPersonSideLookLimit by mutableStateOf(60f)
        private set
    var firstPersonAnimationMode by mutableStateOf(0)
        private set
    var firstPersonAnimationStrength by mutableStateOf(0.35f)
        private set
    var firstPersonYieldDialogue by mutableStateOf(false)
        private set
    var firstPersonThirdPersonInCombat by mutableStateOf(false)
        private set
    var firstPersonTransitionSeconds by mutableStateOf(0f)
        private set
    var firstPersonExternalHeadScale by mutableStateOf(false)
        private set

    /*
     * The free camera's motion/keyframe/VMD block, stored as one record by
     * ModuleSettings. mouseInvertY and mouseSensitivity travel with it because
     * the configuration has always carried them, and since the panel grew a look
     * pad they are what steers it: they are the desktop mouse settings, applied
     * to the drag deltas the pad sends.
     */
    var mouseInvertY by mutableStateOf(false)
        private set
    var mouseSensitivity by mutableStateOf(0.1f)
        private set
    var motionPreset by mutableStateOf(0)
        private set
    var motionSpeed by mutableStateOf(1f)
        private set
    var orbitSpeed by mutableStateOf(20f)
        private set
    var motionDuration by mutableStateOf(0f)
        private set
    var motionTargetHeight by mutableStateOf(1.2f)
        private set
    var keyframeSegmentSeconds by mutableStateOf(3f)
        private set
    var keyframeLoop by mutableStateOf(false)
        private set
    var vmdScale by mutableStateOf(0.07f)
        private set
    var vmdFovBias by mutableStateOf(5f)
        private set
    var vmdLoop by mutableStateOf(false)
        private set

    /*
     * The imported VMD slot. The payload itself never lives here - it goes into
     * the framework's remote file space, which is the only place the game process
     * can read it from. These fields are what the page shows about it.
     */
    var vmdImported by mutableStateOf(false)
        private set
    var vmdName by mutableStateOf("")
        private set
    var vmdBytes by mutableStateOf(0L)
        private set
    var vmdImportedAt by mutableStateOf("")
        private set
    var vmdImportStatus by mutableStateOf("")
        private set
    var vmdImporting by mutableStateOf(false)
        private set

    // betterendfield.actions
    var sustainedDash by mutableStateOf(false)
        private set
    var liinoCleanDash by mutableStateOf(false)
        private set
    var dashAglina by mutableStateOf(true)
        private set
    var dashLiino by mutableStateOf(true)
        private set

    // The in-game panel.
    var overlayEnabled by mutableStateOf(false)
        private set

    private fun loadEnhancement() {
        hideUid = ModuleSettings.isHideUidEnabled(context)
        hideHud = ModuleSettings.isHideHudEnabled(context)
        disableDither = ModuleSettings.isDisableDitherEnabled(context)
        freeCamera = ModuleSettings.isFreeCameraEnabled(context)
        worldPause = ModuleSettings.isWorldPauseEnabled(context)
        firstPerson = ModuleSettings.isFirstPersonEnabled(context)
        firstPersonHideHead = ModuleSettings.isFirstPersonHideHead(context)
        firstPersonFillNeck = ModuleSettings.isFirstPersonFillNeck(context)
        cameraSpeed = ModuleSettings.parse(ModuleSettings.getCameraSpeed(context), 5.0).toFloat()
        cameraFov = ModuleSettings.parse(ModuleSettings.getCameraFieldOfView(context), 60.0).toFloat()
        firstPersonFov = ModuleSettings.parse(ModuleSettings.getFirstPersonFieldOfView(context), 75.0).toFloat()
        firstPersonEyeForward = ModuleSettings.parse(ModuleSettings.getFirstPersonEyeForward(context), 0.03).toFloat()
        firstPersonEyeHeight = ModuleSettings.parse(ModuleSettings.getFirstPersonEyeHeight(context), 0.05).toFloat()
        firstPersonNearClip = ModuleSettings.parse(ModuleSettings.getFirstPersonNearClip(context), 0.03).toFloat()
        firstPersonExtendLookRange = ModuleSettings.isFirstPersonExtendLookRange(context)
        val advanced = ModuleSettings.getFirstPersonAdvanced(context)
        firstPersonMovement = advanced.movement()
        firstPersonSideLookLimit = advanced.sideLookLimit().toFloat()
        firstPersonAnimationMode = advanced.animationMode()
        firstPersonAnimationStrength = advanced.animationStrength().toFloat()
        firstPersonYieldDialogue = advanced.yieldDialogue()
        firstPersonThirdPersonInCombat = advanced.thirdPersonInCombat()
        firstPersonTransitionSeconds = advanced.transitionSeconds().toFloat()
        firstPersonExternalHeadScale = advanced.externalHeadScale()
        val motion = ModuleSettings.getCameraMotion(context)
        mouseInvertY = motion.invertY()
        mouseSensitivity = motion.sensitivity().toFloat()
        motionPreset = ModuleSettings.MOTION_PRESETS
            .indexOfFirst { name -> name.equals(motion.preset(), ignoreCase = true) }
            .coerceAtLeast(0)
        motionSpeed = motion.speed().toFloat()
        orbitSpeed = motion.orbitSpeed().toFloat()
        motionDuration = motion.duration().toFloat()
        motionTargetHeight = motion.targetHeight().toFloat()
        keyframeSegmentSeconds = motion.segmentSeconds().toFloat()
        keyframeLoop = motion.keyframeLoop()
        vmdScale = motion.vmdScale().toFloat()
        vmdFovBias = motion.vmdFovBias().toFloat()
        vmdLoop = motion.vmdLoop()
        vmdImported = ModuleSettings.isVmdImported(context)
        vmdName = ModuleSettings.getVmdName(context)
        vmdBytes = ModuleSettings.getVmdBytes(context)
        vmdImportedAt = formatVmdTime(ModuleSettings.getVmdTime(context))
        sustainedDash = ModuleSettings.isSustainedDashEnabled(context)
        liinoCleanDash = ModuleSettings.isLiinoCleanDashEnabled(context)
        dashAglina = ModuleSettings.isDashCharacterEnabled(context, "aglina")
        dashLiino = ModuleSettings.isDashCharacterEnabled(context, "liino")
        overlayEnabled = ModuleSettings.isOverlayEnabled(context)
    }

    /** Re-reads the panel switch, which the settings Activity can leave and re-enter. */
    fun refreshOverlayFromStore() {
        overlayEnabled = ModuleSettings.isOverlayEnabled(context)
    }

    fun updateHideUid(value: Boolean) {
        hideUid = value
        saveInterfaceSettings()
    }

    fun updateHideHud(value: Boolean) {
        hideHud = value
        saveInterfaceSettings()
    }

    fun updateDisableDither(value: Boolean) {
        disableDither = value
        saveCameraSettings()
    }

    fun updateFreeCamera(value: Boolean) {
        freeCamera = value
        saveCameraSettings()
    }

    fun updateWorldPause(value: Boolean) {
        worldPause = value
        saveCameraSettings()
    }

    fun updateFirstPerson(value: Boolean) {
        firstPerson = value
        saveCameraSettings()
    }

    fun updateFirstPersonHideHead(value: Boolean) {
        firstPersonHideHead = value
        saveCameraSettings()
    }

    fun updateFirstPersonFillNeck(value: Boolean) {
        firstPersonFillNeck = value
        saveCameraSettings()
    }

    fun updateCameraSpeed(value: Float) {
        cameraSpeed = value
        saveCameraSettings()
    }

    fun updateCameraFov(value: Float) {
        cameraFov = value
        saveCameraSettings()
    }

    fun updateFirstPersonFov(value: Float) {
        firstPersonFov = value
        saveCameraSettings()
    }

    fun updateFirstPersonEyeForward(value: Float) {
        firstPersonEyeForward = value
        saveCameraSettings()
    }

    fun updateFirstPersonEyeHeight(value: Float) {
        firstPersonEyeHeight = value
        saveCameraSettings()
    }

    fun updateFirstPersonNearClip(value: Float) {
        firstPersonNearClip = value
        saveCameraSettings()
    }

    fun updateFirstPersonExtendLookRange(value: Boolean) {
        firstPersonExtendLookRange = value
        saveCameraSettings()
    }

    fun updateFirstPersonMovement(value: Boolean) {
        firstPersonMovement = value
        saveCameraSettings()
    }

    fun updateFirstPersonSideLookLimit(value: Float) {
        firstPersonSideLookLimit = value
        saveCameraSettings()
    }

    fun updateFirstPersonAnimationMode(value: Int) {
        firstPersonAnimationMode = value
        saveCameraSettings()
    }

    fun updateFirstPersonAnimationStrength(value: Float) {
        firstPersonAnimationStrength = value
        saveCameraSettings()
    }

    fun updateFirstPersonYieldDialogue(value: Boolean) {
        firstPersonYieldDialogue = value
        saveCameraSettings()
    }

    fun updateFirstPersonThirdPersonInCombat(value: Boolean) {
        firstPersonThirdPersonInCombat = value
        saveCameraSettings()
    }

    fun updateFirstPersonTransitionSeconds(value: Float) {
        firstPersonTransitionSeconds = value
        saveCameraSettings()
    }

    fun updateFirstPersonExternalHeadScale(value: Boolean) {
        firstPersonExternalHeadScale = value
        saveCameraSettings()
    }

    /** A look-pad drag turns the camera up instead of down when this is on. */
    fun updateMouseInvertY(value: Boolean) {
        mouseInvertY = value
        saveCameraSettings()
    }

    /**
     * Degrees of turn per screen pixel dragged, which is the desktop mouse
     * setting applied to the pad. The slider's range is narrower than the native
     * clamp (0.01-2.0): at 0.5 one swipe across the pad turns the camera more
     * than a full circle, and below 0.02 the pad would read as broken rather
     * than slow.
     */
    fun updateMouseSensitivity(value: Float) {
        mouseSensitivity = value
        saveCameraSettings()
    }

    fun updateMotionPreset(index: Int) {
        motionPreset = index
        saveCameraSettings()
    }

    fun updateMotionSpeed(value: Float) {
        motionSpeed = value
        saveCameraSettings()
    }

    fun updateOrbitSpeed(value: Float) {
        orbitSpeed = value
        saveCameraSettings()
    }

    fun updateMotionDuration(value: Float) {
        motionDuration = value
        saveCameraSettings()
    }

    fun updateMotionTargetHeight(value: Float) {
        motionTargetHeight = value
        saveCameraSettings()
    }

    fun updateKeyframeSegmentSeconds(value: Float) {
        keyframeSegmentSeconds = value
        saveCameraSettings()
    }

    fun updateKeyframeLoop(value: Boolean) {
        keyframeLoop = value
        saveCameraSettings()
    }

    fun updateVmdScale(value: Float) {
        vmdScale = value
        saveCameraSettings()
    }

    fun updateVmdFovBias(value: Float) {
        vmdFovBias = value
        saveCameraSettings()
    }

    fun updateVmdLoop(value: Boolean) {
        vmdLoop = value
        saveCameraSettings()
    }

    /**
     * Imports a .vmd into the slot the game process reads.
     *
     * The file is validated here, before anything is published, so a wrong file is
     * refused with a reason instead of reaching the game and coming back as an
     * opaque native log line. Both rules are the native loader's own: its 64 MiB
     * ceiling and the two header generations [ModuleSettings.isVmdMotion] accepts.
     *
     * The payload travels through the framework's remote file space because that
     * is the only channel between this process and the game's - different UIDs,
     * so neither can read the other's data directory - and the native side takes a
     * path, which is why the game process copies it once on startup.
     */
    fun importVmd(uri: android.net.Uri) {
        if (vmdImporting) return
        vmdImporting = true
        vmdImportStatus = context.getString(R.string.motion_vmd_import_working)
        val name = vmdDisplayName(uri)
        Thread({
            val outcome = runCatching {
                val temporary = File(context.cacheDir, "vmd-import.tmp")
                try {
                    var bytes = 0L
                    var header = ByteArray(0)
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException(context.getString(R.string.motion_vmd_unreadable))
                    stream.use { input ->
                        FileOutputStream(temporary, false).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                if (header.isEmpty()) {
                                    header = buffer.copyOfRange(0, minOf(read, VMD_HEADER_BYTES))
                                }
                                bytes += read
                                if (bytes > ModuleSettings.vmdMaximumBytes()) {
                                    throw IllegalStateException(
                                        context.getString(R.string.motion_vmd_too_large),
                                    )
                                }
                                output.write(buffer, 0, read)
                            }
                            output.fd.sync()
                        }
                    }
                    if (!ModuleSettings.isVmdMotion(header)) {
                        throw IllegalStateException(context.getString(R.string.motion_vmd_not_a_motion))
                    }
                    if (!FrameworkSettings.publishVmd(temporary)) {
                        throw IllegalStateException(
                            context.getString(R.string.motion_vmd_publish_failed),
                        )
                    }
                    bytes
                } finally {
                    temporary.delete()
                }
            }
            Handler(Looper.getMainLooper()).post {
                vmdImporting = false
                outcome.fold(
                    onSuccess = { bytes ->
                        ModuleSettings.setVmdImport(context, name, bytes, System.currentTimeMillis())
                        vmdImported = true
                        vmdName = name
                        vmdBytes = bytes
                        vmdImportedAt = formatVmdTime(System.currentTimeMillis())
                        vmdImportStatus = context.getString(
                            R.string.motion_vmd_imported, name, formatVmdBytes(bytes),
                        )
                        // Until an import existed the configuration wrote the slot
                        // empty, so it has to be rewritten for the path to travel.
                        saveCameraSettings()
                    },
                    onFailure = { error ->
                        vmdImportStatus = context.getString(
                            R.string.motion_vmd_import_failed,
                            error.message ?: error.toString(),
                        )
                    },
                )
            }
        }, "BetterEndfield-VmdImport").start()
    }

    /**
     * Drops the slot. The configuration stops naming the file even when the
     * framework refuses to delete it: an unreferenced file is inert, while a
     * dangling reference would keep the module reading a motion the user removed.
     */
    fun clearVmd() {
        val dropped = FrameworkSettings.removeVmd()
        ModuleSettings.clearVmdImport(context)
        vmdImported = false
        vmdName = ""
        vmdBytes = 0L
        vmdImportedAt = ""
        vmdImportStatus = context.getString(
            if (dropped) R.string.motion_vmd_cleared else R.string.motion_vmd_cleared_unlinked,
        )
        saveCameraSettings()
    }

    private fun vmdDisplayName(uri: android.net.Uri): String {
        try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0)
                }
        } catch (ignored: RuntimeException) {
            // Fall through to the last path segment below.
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
            ?: context.getString(R.string.motion_vmd_unnamed)
    }

    private fun formatVmdTime(millis: Long): String =
        if (millis <= 0L) "" else SimpleDateFormat(VMD_TIME_PATTERN, Locale.ROOT).format(Date(millis))

    private fun formatVmdBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0)
        bytes >= 1024L -> String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0)
        else -> "$bytes 字节"
    }

    fun updateSustainedDash(value: Boolean) {
        sustainedDash = value
        saveDashSettings()
    }

    fun updateLiinoCleanDash(value: Boolean) {
        liinoCleanDash = value
        saveDashSettings()
    }

    fun updateDashAglina(value: Boolean) {
        dashAglina = value
        saveDashSettings()
    }

    fun updateDashLiino(value: Boolean) {
        dashLiino = value
        saveDashSettings()
    }

    /**
     * The panel switch lives on its own page rather than going through
     * [afterEnhancementChange]: the overlay is not a module, so it neither
     * restarts the game nor changes what loads.
     */
    fun updateOverlayEnabled(value: Boolean) {
        overlayEnabled = value
        ModuleSettings.setOverlayEnabled(context, value)
        status = context.getString(
            if (value) R.string.overlay_turned_on else R.string.overlay_turned_off,
        )
    }

    private fun saveInterfaceSettings() {
        ModuleSettings.setInterfaceSettings(context, hideUid, hideHud)
        afterEnhancementChange()
    }

    private fun saveCameraSettings() {
        ModuleSettings.setCameraSettings(
            context,
            disableDither,
            freeCamera,
            worldPause,
            firstPerson,
            firstPersonHideHead,
            firstPersonFillNeck,
            cameraSpeed.toDouble(),
            cameraFov.toDouble(),
            firstPersonFov.toDouble(),
            firstPersonEyeForward.toDouble(),
            firstPersonEyeHeight.toDouble(),
            firstPersonNearClip.toDouble(),
            firstPersonExtendLookRange,
            ModuleSettings.FirstPersonAdvanced(
                firstPersonMovement,
                firstPersonSideLookLimit.toDouble(),
                firstPersonAnimationMode,
                firstPersonAnimationStrength.toDouble(),
                firstPersonYieldDialogue,
                firstPersonThirdPersonInCombat,
                firstPersonTransitionSeconds.toDouble(),
                firstPersonExternalHeadScale,
            ),
            ModuleSettings.CameraMotion(
                mouseInvertY,
                mouseSensitivity.toDouble(),
                // The preset travels as its desktop name; the record maps
                // anything unknown back to orbit, so the index only has to stay
                // inside the array the picker was built from.
                ModuleSettings.MOTION_PRESETS[
                    motionPreset.coerceIn(0, ModuleSettings.MOTION_PRESETS.size - 1),
                ],
                motionSpeed.toDouble(),
                orbitSpeed.toDouble(),
                motionDuration.toDouble(),
                motionTargetHeight.toDouble(),
                keyframeSegmentSeconds.toDouble(),
                keyframeLoop,
                vmdImported,
                vmdScale.toDouble(),
                vmdFovBias.toDouble(),
                vmdLoop,
            ),
        )
        afterEnhancementChange()
    }

    private fun saveDashSettings() {
        ModuleSettings.setSustainedDashSettings(
            context,
            sustainedDash,
            liinoCleanDash,
            dashAglina,
            dashLiino,
        )
        afterEnhancementChange()
    }

    private fun afterEnhancementChange() {
        refreshDiagnostics()
        markPending()
        status = context.getString(R.string.enhancement_restart_required)
    }

    /*
     * The dependent options. World pause is a free-camera sub-mode, the first-person
     * mesh options only mean anything in first person, and the clean-exhaust option
     * belongs to Liino; a switch that cannot do anything reads as broken, so each of
     * these is dimmed rather than hidden.
     */
    val worldPauseAvailable get() = freeCamera
    val cameraSpeedAvailable get() = freeCamera
    val cameraFovAvailable get() = freeCamera
    val hideHeadAvailable get() = firstPerson
    val fillNeckAvailable get() = firstPerson && firstPersonHideHead
    val firstPersonFovAvailable get() = firstPerson
    val firstPersonEyeForwardAvailable get() = firstPerson
    val firstPersonEyeHeightAvailable get() = firstPerson
    val firstPersonNearClipAvailable get() = firstPerson
    val firstPersonExtendLookRangeAvailable get() = firstPerson
    val firstPersonMovementAvailable get() = firstPerson
    val firstPersonSideLookLimitAvailable get() = firstPerson && firstPersonMovement
    val firstPersonAnimationModeAvailable get() = firstPerson
    val firstPersonAnimationStrengthAvailable get() = firstPerson && firstPersonAnimationMode != 0
    val firstPersonYieldDialogueAvailable get() = firstPerson
    val firstPersonThirdPersonInCombatAvailable get() = firstPerson
    val firstPersonTransitionSecondsAvailable get() = firstPerson
    val firstPersonExternalHeadScaleAvailable get() = firstPerson && firstPersonHideHead
    /*
     * The whole motion block hangs off the free camera. The desktop module only
     * polls the preset, keyframe and VMD hotkeys while the free camera is armed,
     * so a row that could be edited with the camera off would never be read -
     * the same reason "camera speed" is dimmed whenever the camera is off.
     */
    val cameraMotionAvailable get() = freeCamera

    /**
     * What the import row says about the slot right now. The page needs the name
     * and the size visible before the game is started: the file is copied into
     * the game process only on the next launch, so this line is the only evidence
     * the user gets that the right .vmd went in.
     */
    val vmdSummary get() = if (!vmdImported) {
        context.getString(R.string.motion_vmd_not_imported)
    } else {
        context.getString(
            R.string.motion_vmd_slot_summary, vmdName, formatVmdBytes(vmdBytes), vmdImportedAt,
        )
    }
    val dashCharactersAvailable get() = sustainedDash
    val liinoCleanDashAvailable get() = sustainedDash && dashLiino

    /* ----------------------------------------------------------------------- 诊断 */

    var diagnosticsOverlay by mutableStateOf("")
        private set
    var diagnosticsModules by mutableStateOf("")
        private set
    var diagnosticsRuntimeLog by mutableStateOf("")
        private set

    fun refreshDiagnostics() {
        diagnosticsOverlay = context.getString(
            R.string.diagnostics_overlay,
            state(overlayEnabled),
            "无需系统悬浮权限",
        )
        diagnosticsModules = context.getString(R.string.diagnostics_modules, loadedModules())
        val runtimeLog = FrameworkSettings.readRemoteLog().trim()
        diagnosticsRuntimeLog = if (runtimeLog.isEmpty()) {
            context.getString(R.string.diagnostics_runtime_empty)
        } else {
            context.getString(R.string.diagnostics_runtime) + "\n\n" + runtimeLog
        }
    }

    /**
     * The module ids the game process will actually start, read back from the same
     * configuration strings it reads. An empty configuration is how a module is kept
     * out of the process, so "absent from this list" is the real state rather than a
     * guess.
     */
    fun loadedModuleIds(): List<String> = buildList {
        if (ModuleSettings.getVoiceRules(context).isNotEmpty()) add("voice.character")
        if (modelEnabled || logoEnabled) add("model")
        if (hideUid || hideHud) add("ui")
        if (disableDither || freeCamera || firstPerson) add("camera")
        if (sustainedDash) add("actions")
    }

    private fun loadedModules(): String {
        val loaded = loadedModuleIds()
        return if (loaded.isEmpty()) context.getString(R.string.state_none) else loaded.joinToString(" · ")
    }

    private fun state(on: Boolean): String =
        context.getString(if (on) R.string.state_on else R.string.state_off)

    /* -------------------------------------------------------------------- 卡片状态 */

    val interfaceCardStatus: String
        get() = moduleStatus("betterendfield.ui", hideUid || hideHud)

    val cameraCardStatus: String
        get() = moduleStatus("betterendfield.camera", disableDither || freeCamera || firstPerson)

    val dashCardStatus: String
        get() = context.getString(R.string.dash_pose_note, "pose_*.bin") + "\n" +
            moduleStatus("betterendfield.actions", sustainedDash && (dashAglina || dashLiino))

    /**
     * Says whether the module will be loaded at all, which is the one thing this
     * screen can state for certain. Whether each Hook resolved is only knowable
     * inside the game, and is reported there.
     */
    private fun moduleStatus(moduleId: String, loaded: Boolean): String =
        "$moduleId  ·  " + context.getString(
            if (loaded) R.string.module_will_load else R.string.module_will_not_load,
        )

    /* ----------------------------------------------------------------------- 首页 */

    /**
     * The installed packages, read through the same state object the management
     * screen uses. Reading the index a second time here would be a second parser
     * to keep in step with [BemInstallState], and the two would eventually
     * disagree about what "enabled" means.
     */
    private val bem = BemInstallState(context)

    val appearancePackages: List<BemPackage> get() = bem.packages

    fun refreshHome() {
        bem.refresh()
    }

    /** What the model module will put on the login screen, or that it is off. */
    val loginDisplaySummary: String
        get() {
            val parts = buildList {
                if (modelEnabled) {
                    add(characterNames.getOrNull(characterIndex) ?: context.getString(R.string.state_none))
                    actionIds.getOrNull(actionIndex)?.let(::add)
                }
                if (logoEnabled) add("Logo $logoColor")
            }
            return if (parts.isEmpty()) {
                context.getString(R.string.home_login_display_off)
            } else {
                parts.joinToString(" · ")
            }
        }

    /**
     * Which package is currently in charge, which is the only thing the stored
     * index can answer. Whether the *running* game has loaded it is a different
     * question and is answered by the journal, not here.
     */
    val appearanceSummary: String
        get() {
            val packages = bem.packages
            if (packages.isEmpty()) return context.getString(R.string.home_appearance_none)
            val active = packages.firstOrNull { it.enabled }
                ?: return context.getString(R.string.home_appearance_idle, packages.size)
            val detail = runCatching {
                if (active.minorVersion >= 1) {
                    val count = active.selections().size
                    if (count == 0) "" else " · $count 项选项"
                } else {
                    val appearance = active.selectedAppearance()
                    if (appearance.isEmpty()) "" else " · $appearance"
                }
            }.getOrDefault("")
            return active.name + detail
        }

    /* -------------------------------------------------------------------- 运行日志 */

    var journalText by mutableStateOf("")
        private set

    fun refreshJournal() {
        journalText = FrameworkSettings.readRemoteLog().trim()
    }

    /**
     * The export body. The remote snapshot is everything this process can see:
     * the game's own ring and the native journal are assembled inside the game
     * and published as one string, so exporting is that string plus the header
     * that makes it identifiable once it leaves the device.
     */
    fun journalBody(): String = buildString {
        append("Better-Endfield journal\n")
        append("build ").append(BuildConfig.VERSION_NAME)
            .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
        append("exported at ").append(stamp()).append("\n\n")
        append(journalText.ifBlank { context.getString(R.string.log_empty) }).append('\n')
    }

    fun journalFileName(): String = "betterendfield-log-${stamp()}.txt"

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())

    /*
     * This block must stay at the *end* of the class body, after every
     * `by mutableStateOf` property it writes.
     *
     * Kotlin runs property initialisers and init blocks in source order, and a
     * delegated property's backing field is only created where it is declared.
     * A loader placed above the properties it fills writes through a null
     * delegate and takes the whole Activity down before the first frame --
     * exactly the launch crash 3.3.22 shipped with (loadEnhancement() writing
     * hideUid: MutableState.setValue on a null object reference).
     * loadModelPresets() happens to touch only earlier declarations, which is
     * why the failure only showed up in the enhancement block.
     */
    init {
        ModuleSettings.republishConfigurations(context)
        loadModelPresets()
        loadEnhancement()
    }

    private companion object {
        val HEX_COLOR = Regex("#[0-9A-F]{6}")
    }
}

/** The stored form of a number: fixed decimals, no trailing zeros. */
fun number(value: Double, decimals: Int): String {
    val text = String.format(Locale.ROOT, "%.${decimals}f", value)
    val trimmed = text.trimEnd('0').trimEnd('.')
    return trimmed.ifEmpty { text }
}
