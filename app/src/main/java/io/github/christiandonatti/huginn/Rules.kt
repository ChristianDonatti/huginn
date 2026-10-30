package io.github.christiandonatti.huginn

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import io.github.christiandonatti.huginn.shared.LaunchSpec
import io.github.christiandonatti.huginn.shared.Systems
import io.github.christiandonatti.huginn.shared.parseCommand
import io.github.christiandonatti.huginn.shared.splitComponent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject

/** One way to start a system's games, e.g. "mGBA" through RetroArch or "My Boy! (Standalone)". */
class CommandDef(val label: String, val spec: LaunchSpec) {
    val displayName: String get() = Systems.emulatorName(label, spec.emulator)
}

class SystemDef(
    val name: String,
    val fullName: String,
    val extensions: Set<String>,
    val commands: List<CommandDef>,
) {
    val shortName: String get() = Systems.shortName(name, fullName)
}

/**
 * Systems and emulators from ES-DE's Android rules (assets/esde/rules.json, MIT licence), and
 * which of those emulators are installed.
 */
object Rules {
    @Volatile private var loaded: Pair<Map<String, SystemDef>, Map<String, List<String>>>? = null
    private val installedCache = HashMap<String, ComponentName?>()

    val androidSystem = SystemDef(Systems.ANDROID, "Android", emptySet(), emptyList())

    private fun load(context: Context): Pair<Map<String, SystemDef>, Map<String, List<String>>> {
        loaded?.let { return it }
        synchronized(this) {
            loaded?.let { return it }
            val text = context.assets.open("esde/rules.json").bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val emus = HashMap<String, List<String>>()
            val e = json.getJSONObject("emulators")
            for (key in e.keys()) {
                val arr = e.getJSONArray(key)
                emus[key] = List(arr.length()) { arr.getString(it) }
            }
            val systems = LinkedHashMap<String, SystemDef>()
            val s = json.getJSONArray("systems")
            for (i in 0 until s.length()) {
                val o = s.getJSONObject(i)
                val ext = o.getJSONArray("e")
                val cmds = o.getJSONArray("c")
                val commands = (0 until cmds.length()).mapNotNull { j ->
                    val pair = cmds.getJSONArray(j)
                    parseCommand(pair.getString(1))?.let { CommandDef(pair.getString(0), it) }
                }
                systems[o.getString("n")] = SystemDef(
                    name = o.getString("n"),
                    fullName = o.getString("f"),
                    extensions = (0 until ext.length()).map { ext.getString(it) }.toSet(),
                    commands = commands,
                )
            }
            return (systems to emus).also { loaded = it }
        }
    }

    fun systems(context: Context): Map<String, SystemDef> = load(context).first

    fun system(context: Context, name: String): SystemDef? =
        if (name == Systems.ANDROID) androidSystem else systems(context)[name]

    /** Every package name that belongs to an emulator, so emulators are not suggested as games. */
    fun emulatorPackages(context: Context): Set<String> =
        load(context).second.values.flatten().map { splitComponent(it).first }.toSet()

    /** Forget which emulators are installed (after apps were installed or removed). */
    fun invalidate() = synchronized(installedCache) { installedCache.clear() }

    /** The installed activity for an emulator name of the find rules, or null if none is installed. */
    fun installed(context: Context, emulator: String): ComponentName? {
        synchronized(installedCache) { if (installedCache.containsKey(emulator)) return installedCache[emulator] }
        val pm = context.packageManager
        val found = load(context).second[emulator].orEmpty().firstNotNullOfOrNull { entry ->
            val (pkg, cls) = splitComponent(entry)
            if (!isInstalled(pm, pkg)) return@firstNotNullOfOrNull null
            if (cls == null) {
                pm.getLaunchIntentForPackage(pkg)?.component
            } else {
                val component = ComponentName(pkg, cls)
                if (runCatching { pm.getActivityInfo(component, 0) }.isSuccess) component else null
            }
        }
        synchronized(installedCache) { installedCache[emulator] = found }
        return found
    }

    /** Whether a command can run: its emulator is installed (native Android games always can). */
    fun canRun(context: Context, command: CommandDef): Boolean =
        command.spec.androidApp != null || command.spec.emulator?.let { installed(context, it) } != null

    /** An emulator app found on the device. */
    class InstalledEmulator(val packageName: String, val label: String, val icon: ImageBitmap?, val systems: List<String>)

    /**
     * Every emulator from the rules that is installed, with the systems it can run, the way Cocoon
     * lists the players it found. Slow-ish (a package lookup per emulator): call off the main thread.
     */
    fun installedEmulators(context: Context): List<InstalledEmulator> {
        val pm = context.packageManager
        val systemsByEmu = HashMap<String, MutableSet<String>>()
        for (system in systems(context).values) {
            for (c in system.commands) c.spec.emulator?.let { systemsByEmu.getOrPut(it) { LinkedHashSet() } += system.name }
        }
        val byPackage = LinkedHashMap<String, MutableSet<String>>()
        for (name in load(context).second.keys) {
            val component = installed(context, name) ?: continue
            byPackage.getOrPut(component.packageName) { LinkedHashSet() } += systemsByEmu[name].orEmpty()
        }
        return byPackage.map { (pkg, systems) ->
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
            InstalledEmulator(
                packageName = pkg,
                label = info?.loadLabel(pm)?.toString() ?: pkg,
                icon = info?.let { runCatching { it.loadIcon(pm).toBitmap(72, 72).asImageBitmap() }.getOrNull() },
                systems = systems.toList(),
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean =
        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
}
