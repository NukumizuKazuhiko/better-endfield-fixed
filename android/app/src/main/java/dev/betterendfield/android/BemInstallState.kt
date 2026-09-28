package dev.betterendfield.android

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** One finite option group inside a BEM 1.1 package. */
class BemOptionGroup(
    val id: String,
    val name: String,
    val choices: List<Choice>,
) {
    class Choice(val id: String, val name: String)
}

/**
 * One installed package, as the management screen edits it.
 *
 * The selection is kept in the same encoded `group:choice&group:choice` form the
 * installer and the native reader use, so validating a change is the same call
 * the save path makes rather than a second, parallel implementation of the
 * reachability rules.
 */
class BemPackage(
    val generation: String,
    val name: String,
    val textureMode: String,
    val minorVersion: Int,
    val appearances: List<String>,
    val optionGroups: List<BemOptionGroup>,
    val raw: JSONObject,
) {
    var enabled by mutableStateOf(true)
    var appearanceIndex by mutableStateOf(0)
    var options by mutableStateOf("")
    var problem by mutableStateOf<String?>(null)
    val visibleGroups = mutableStateMapOf<String, Boolean>()

    val convertedTextures: Boolean get() = textureMode != "original"

    /** What this package currently claims it should be saved as. */
    fun draft(): JSONObject = JSONObject().apply {
        put("generation", generation)
        put("enabled", enabled)
        if (minorVersion >= 1) put("options", options) else put("appearance", appearances[appearanceIndex])
    }

    fun selections(): LinkedHashMap<String, String> = LinkedHashMap(BemOptions.parse(raw, options))

    /**
     * Applies a choice, refusing combinations the package cannot reach. A
     * refusal reverts the picker and explains itself instead of leaving a
     * selection the native reader would reinterpret.
     */
    fun chooseOption(groupId: String, choiceId: String) {
        val candidate = selections().also { it[groupId] = choiceId }
        try {
            if (!BemOptions.valid(raw, candidate)) {
                problem = "这个选项组合在包内不可达，请选择其他组合。"
                return
            }
            options = BemOptions.encode(candidate)
            problem = null
            refreshVisibility(candidate)
        } catch (error: Exception) {
            problem = "选项错误：" + error.message
        }
    }

    fun refreshVisibility(active: Map<String, String> = selections()) {
        val effective = try {
            BemOptions.effective(raw, active)
        } catch (error: Exception) {
            problem = "选项错误：" + error.message
            return
        }
        optionGroups.forEach { group ->
            visibleGroups[group.id] = effective.containsKey(group.id)
        }
    }

    fun selectedOption(groupId: String): String = selections()[groupId] ?: ""

    fun selectedAppearance(): String = appearances.getOrElse(appearanceIndex) { "" }
}

/**
 * The BEM page's state.
 *
 * The installer reports progress through static fields because it runs on its own
 * worker and can outlive this screen; polling them on the main thread is what
 * keeps a long texture conversion observable while the user navigates away and
 * back. The index is only re-parsed when its serialized form changes, so a live
 * option picker is never rebuilt underneath the finger that is using it.
 */
class BemInstallState(private val context: Context) {

    var status by mutableStateOf(BemInstaller.status)
        private set
    var busy by mutableStateOf(false)
        private set
    var removing by mutableStateOf(false)
        private set
    var percent by mutableStateOf(-1)
        private set
    var elapsedSeconds by mutableStateOf(0L)
        private set
    var packages by mutableStateOf<List<BemPackage>>(emptyList())
        private set
    var indexBroken by mutableStateOf<String?>(null)
        private set

    private var displayedIndex = ""

    /** Drafts survive a rebuild so a refresh cannot discard an uncommitted edit. */
    private val drafts = HashMap<String, Triple<Boolean, Int, String>>()

    fun refresh() {
        status = BemInstaller.status
        busy = BemInstaller.busy
        removing = BemInstaller.removing
        percent = BemInstaller.progressPercent
        elapsedSeconds = if (BemInstaller.busy) {
            (SystemClock.elapsedRealtime() - BemInstaller.startedAt) / 1000L
        } else {
            0L
        }
        val index = FrameworkSettings.open(context).getString(BemInstaller.INDEX, "[]") ?: "[]"
        if (index != displayedIndex) {
            displayedIndex = index
            rebuild(index)
        }
    }

    private fun rebuild(index: String) {
        drafts.clear()
        packages.forEach { pkg ->
            drafts[pkg.generation] = Triple(pkg.enabled, pkg.appearanceIndex, pkg.options)
        }
        val list = try {
            val parsed = JSONArray(index)
            (0 until parsed.length()).map { position -> parse(parsed.getJSONObject(position)) }
        } catch (error: Exception) {
            indexBroken = "安装列表读取失败：" + error.message
            emptyList()
        }
        if (list.isNotEmpty()) indexBroken = null
        packages = list
    }

    private fun parse(entry: JSONObject): BemPackage {
        val generation = entry.getString("generation")
        val minor = entry.optInt("bem_minor", 0)
        val appearances = buildList {
            val array = entry.optJSONArray("appearances") ?: JSONArray()
            for (index in 0 until array.length()) add(array.getString(index))
        }
        val groups = buildList {
            if (minor < 1) return@buildList
            val array = entry.getJSONArray("option_groups")
            for (index in 0 until array.length()) {
                val group = array.getJSONObject(index)
                val choices = group.getJSONArray("choices")
                add(
                    BemOptionGroup(
                        id = group.getString("id"),
                        name = group.getString("name"),
                        choices = (0 until choices.length()).map { choiceIndex ->
                            val choice = choices.getJSONObject(choiceIndex)
                            BemOptionGroup.Choice(choice.getString("id"), choice.getString("name"))
                        },
                    ),
                )
            }
        }

        val active = if (minor >= 1) {
            entry.optString("selected_options", entry.getString("default_options"))
        } else {
            entry.optString("selected_appearance", entry.optString("default_appearance"))
        }

        val pkg = BemPackage(
            generation = generation,
            name = entry.getString("name"),
            textureMode = entry.optString("texture_mode", "converted"),
            minorVersion = minor,
            appearances = appearances,
            optionGroups = groups,
            raw = entry,
        )

        if (minor >= 1) {
            // A stored selection can go stale when the package is re-imported with
            // a group removed; falling back to the package default keeps the screen
            // usable instead of throwing on open.
            pkg.options = try {
                BemOptions.encode(BemOptions.parse(entry, active))
            } catch (stale: Exception) {
                entry.getString("default_options")
            }
            pkg.refreshVisibility()
        } else {
            val stored = entry.optString("selected_appearance", entry.optString("default_appearance"))
            pkg.appearanceIndex = appearances.indexOfFirst { it == stored }.takeIf { it >= 0 } ?: 0
        }

        drafts[generation]?.let { (enabled, appearance, options) ->
            pkg.enabled = enabled
            if (minor >= 1) {
                pkg.options = options
                pkg.refreshVisibility()
            } else {
                pkg.appearanceIndex = appearance.coerceIn(0, (appearances.size - 1).coerceAtLeast(0))
            }
        } ?: run {
            pkg.enabled = entry.optBoolean("enabled", true)
        }
        return pkg
    }

    fun saveAll(): Boolean = try {
        val changes = JSONArray()
        packages.forEach { changes.put(it.draft()) }
        BemInstaller.saveAll(context, changes)
        BemInstaller.status = "全部设置已保存，重启游戏后生效。"
        status = BemInstaller.status
        true
    } catch (error: Exception) {
        BemInstaller.status = "保存失败：" + error.message
        status = BemInstaller.status
        false
    }

    fun convert(generation: String) {
        BemInstaller.convert(context, generation)
    }

    fun remove(generation: String) {
        try {
            BemInstaller.remove(context, generation)
        } catch (error: Exception) {
            BemInstaller.status = error.message ?: "移除失败"
            status = BemInstaller.status
        }
    }

    fun cancel() {
        BemInstaller.cancel()
    }

    fun import(uri: android.net.Uri) {
        BemInstaller.start(context, uri)
    }

    fun startFromViewIntent(uri: android.net.Uri) {
        BemInstaller.start(context, uri)
    }
}
