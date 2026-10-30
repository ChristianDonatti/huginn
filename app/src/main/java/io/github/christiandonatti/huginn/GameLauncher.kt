package io.github.christiandonatti.huginn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.ClipData
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.StrictMode
import androidx.core.content.FileProvider
import io.github.christiandonatti.huginn.shared.ExtraType
import io.github.christiandonatti.huginn.shared.Systems
import io.github.christiandonatti.huginn.shared.expandValue
import io.github.christiandonatti.huginn.shared.safDocumentUri
import io.github.christiandonatti.huginn.shared.splitArrayValue
import io.github.christiandonatti.huginn.shared.splitComponent
import io.github.christiandonatti.huginn.shared.stripExtension
import java.io.File

/** The emulators a game can use, which one is used, and whether it was picked by hand. */
class EmulatorChoice(
    /** Commands of the system that can run this game, installed or not. */
    val all: List<CommandDef>,
    /** The ones whose emulator is installed. */
    val installed: List<CommandDef>,
    /** What Huginn picks when the user did not choose. */
    val automatic: CommandDef?,
    /** The user's choice for this game or system, if it is still installed. */
    val chosen: CommandDef?,
    val chosenForGame: Boolean,
) {
    val current: CommandDef? get() = chosen ?: automatic
}

/** Starts games: picks the emulator and turns ES-DE's launch command into an Intent. */
object GameLauncher {
    fun choice(context: Context, game: Game): EmulatorChoice {
        val system = Rules.system(context, game.system)
        // "Native port" commands start an Android app named in a .app file; only those files can use them.
        val all = system?.commands.orEmpty().filter { it.spec.androidApp == null || game.extension == "app" }
        val installed = all.filter { Rules.canRun(context, it) }
        val auto = Systems.defaultCommand(game.system, all.map { it.label }, all.map { it in installed })?.let { all[it] }
        val forGame = Prefs.gameEmulator[game.id]?.let { label -> installed.firstOrNull { it.label == label } }
        val forSystem = Prefs.systemEmulator[game.system]?.let { label -> installed.firstOrNull { it.label == label } }
        return EmulatorChoice(all, installed, auto, forGame ?: forSystem, forGame != null)
    }

    /** Emulator choice for a whole system (Settings tab), ignoring per-game choices. */
    fun systemChoice(context: Context, system: String): EmulatorChoice {
        val all = Rules.system(context, system)?.commands.orEmpty().filter { it.spec.androidApp == null }
        val installed = all.filter { Rules.canRun(context, it) }
        val auto = Systems.defaultCommand(system, all.map { it.label }, all.map { it in installed })?.let { all[it] }
        val chosen = Prefs.systemEmulator[system]?.let { label -> installed.firstOrNull { it.label == label } }
        return EmulatorChoice(all, installed, auto, chosen, false)
    }

    sealed interface Result {
        data object Started : Result
        data class NoEmulator(val system: String) : Result
        /** The emulator gets the game through the folder picker and [folder] was never released to Huginn. */
        data class NeedsFolder(val folder: String) : Result
        data class Failed(val reason: String) : Result
    }

    /** What the last attempt to start a game sent to Android, for the report in the Screens tab. */
    var lastLaunch by mutableStateOf<String?>(null)
        private set

    /** Game being played, to add up play time when Huginn comes back on that screen. */
    private var session: Triple<String, Long, Int>? = null

    private class Built(
        val intent: Intent,
        /** Document URIs inside a released folder, handed over with read/write permission. */
        val grants: List<Uri> = emptyList(),
        /** The released folder, granted as a prefix so .cue/.bin and .m3u games can open their other files. */
        val tree: Uri? = null,
        /** The command uses the folder picker URI but no released folder holds the game. */
        val needsFolder: Boolean = false,
    )

    /**
     * Starts [game] on the given screen. With [askFolder], a game whose emulator needs the
     * folder picker URI first asks to release its folder (once per folder).
     */
    fun play(context: Context, game: Game, displayId: Int, askFolder: Boolean = true): Result {
        var command: CommandDef? = null
        var sent: Intent? = null
        val result: Result = run {
            val built = try {
                if (game.packageName != null) {
                    Built(context.packageManager.getLaunchIntentForPackage(game.packageName) ?: return@run Result.Failed("no launch activity"))
                } else {
                    command = choice(context, game).current ?: return@run Result.NoEmulator(game.system)
                    buildIntent(context, game, command!!)
                }
            } catch (e: Exception) {
                return@run Result.Failed("${e.javaClass.simpleName}: ${e.message}")
            }
            if (built.needsFolder && askFolder) {
                val folder = folderToRelease(game)
                if (folder != null && folder !in Prefs.askedFolders) return@run Result.NeedsFolder(folder)
            }
            if (command?.spec?.emulator == "RETROARCH") {
                // Like Cocoon: a RetroArch left in the background would show its menu instead of the new game.
                built.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                built.intent.component?.packageName?.let { pkg ->
                    runCatching { context.getSystemService(ActivityManager::class.java).killBackgroundProcesses(pkg) }
                }
            }
            var error: Exception? = null
            for (attempt in attempts(built)) {
                sent = attempt
                error = DualScreen.startOrError(context, attempt, displayId) ?: break
            }
            if (error != null) return@run Result.Failed("${error.javaClass.simpleName}: ${error.message}")
            Prefs.played(game.id)
            session = Triple(game.id, System.currentTimeMillis(), displayId)
            Result.Started
        }
        lastLaunch = describe(context, game, command, sent, displayId, null) + "\nresult: " + result
        Log.i("Huginn", lastLaunch ?: "")
        return result
    }

    /** The ROM folder (as added in Settings) that holds a game, to release it in the picker. */
    private fun folderToRelease(game: Game): String? {
        val path = game.path ?: return null
        return Prefs.romRoots.filter { path.startsWith("$it/") }.minByOrNull { it.length } ?: game.romPath
    }

    /**
     * The intent with permissions to the game and its folder; if Android refuses to pass the folder
     * permission, the same intent with the game only; then without permissions (the emulator's own access).
     */
    private fun attempts(built: Built): List<Intent> {
        if (built.grants.isEmpty()) return listOf(built.intent)
        val rw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        fun withClip(uris: List<Uri>, flags: Int) = Intent(built.intent).apply {
            val clip = ClipData.newRawUri("rom", uris.first())
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            clipData = clip
            addFlags(flags)
        }
        val list = ArrayList<Intent>()
        if (built.tree != null) list += withClip(built.grants + built.tree, rw or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        list += withClip(built.grants, rw)
        list += withClip(built.grants, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        list += Intent(built.intent)
        return list
    }

    /** Huginn is back on [displayId]: the game started there is over, add its play time. */
    fun endSession(displayId: Int) {
        val (id, start, display) = session ?: return
        if (display != displayId) return
        session = null
        val minutes = ((System.currentTimeMillis() - start) / 60_000L).toInt()
        if (minutes in 1..720) Prefs.addPlayTime(id, minutes)
    }

    private fun describe(context: Context, game: Game, command: CommandDef?, intent: Intent?, displayId: Int, note: String?): String {
        val lines = ArrayList<String>()
        lines += "game: ${game.title} [${game.system}]"
        game.path?.let { lines += "file: $it" }
        command?.let { lines += "command: ${it.label}" }
        if (intent != null) {
            lines += "component: ${intent.component?.flattenToShortString()}"
            intent.action?.let { lines += "action: $it" }
            intent.categories?.takeIf { it.isNotEmpty() }?.let { lines += "categories: ${it.joinToString()}" }
            intent.data?.let { lines += "data: $it" }
            intent.type?.let { lines += "type: $it" }
            intent.extras?.let { b ->
                for (key in b.keySet()) {
                    @Suppress("DEPRECATION")
                    val v = b.get(key)
                    lines += "extra $key = " + if (v is Array<*>) v.joinToString(",", "[", "]") else v.toString()
                }
            }
            lines += "flags: 0x" + Integer.toHexString(intent.flags)
            intent.clipData?.let { clip -> lines += "granted: " + (0 until clip.itemCount).joinToString { clip.getItemAt(it).uri.toString() } }
            intent.component?.packageName?.let { pkg ->
                lines += "emulator all files access: " + allFilesAccess(context, pkg)
                runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()?.let { lines += "emulator version: $it" }
            }
        }
        lines += "display: $displayId"
        note?.let { lines += it }
        return lines.joinToString("\n")
    }

    /** Whether an emulator has "All files access" (yes / no / ? when Android does not say). */
    private fun allFilesAccess(context: Context, pkg: String): String = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runCatching "n/a"
        val uid = context.packageManager.getApplicationInfo(pkg, 0).uid
        val ops = context.getSystemService(AppOpsManager::class.java)
        when (ops.unsafeCheckOpNoThrow("android:manage_external_storage", uid, pkg)) {
            AppOpsManager.MODE_ALLOWED -> "yes"
            AppOpsManager.MODE_DEFAULT ->
                if (context.packageManager.checkPermission(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE, pkg) == PackageManager.PERMISSION_GRANTED) "yes" else "no"
            else -> "no"
        }
    }.getOrDefault("?")

    private fun buildIntent(context: Context, game: Game, command: CommandDef): Built {
        val spec = command.spec
        val path = game.path ?: error("game without a file")
        val file = File(path)
        val userId = Process.myUid() / 100_000
        val component: ComponentName? = spec.emulator?.let { Rules.installed(context, it) }
        var grantRead = false
        val tree = FolderAccess.treeFor(context, path)?.second
        val grants = ArrayList<Uri>()
        var needsFolder = false

        fun readSmall(f: File): String? =
            if (f.isFile && f.length() <= 4096) runCatching { f.readText().trim() }.getOrNull() else null

        val resolve: (String) -> String? = { name ->
            when (name) {
                "ROM", "ROMRAW" -> path
                "ROMSAF" -> {
                    val inTree = tree?.let { FolderAccess.documentUri(it, path) }
                    if (inTree != null) grants += inTree else needsFolder = true
                    inTree?.toString() ?: safDocumentUri(path)
                }
                "ROMPROVIDER" -> {
                    grantRead = true
                    FileProvider.getUriForFile(context, "${context.packageName}.files", file).toString()
                }
                "BASENAME" -> stripExtension(file.name)
                "GAMEDIR", "GAMEDIRRAW" -> file.parent
                "ROMPATH", "ROMPATHRAW" -> game.romPath ?: file.parentFile?.parent
                "EXTERNALDATA" -> "/storage/emulated/$userId"
                "INTERNALDATA" -> "/data/user/$userId"
                "ANDROIDPACKAGE" -> component?.packageName
                "FILEINJECT" -> readSmall(file)
                else -> null
            }
        }
        val inject: (String) -> String? = { name ->
            val f = if (name.startsWith("/")) File(name) else File(file.parentFile, name)
            readSmall(f)
        }
        fun expand(value: String) = expandValue(value, resolve, inject)

        val intent = Intent()
        if (spec.androidApp != null) {
            val (pkg, cls) = splitComponent(expand(spec.androidApp!!))
            val launch = if (cls != null) Intent(Intent.ACTION_MAIN).setClassName(pkg, cls)
            else context.packageManager.getLaunchIntentForPackage(pkg) ?: error("$pkg is not installed")
            return Built(launch)
        }
        intent.component = component ?: error("emulator not installed")
        spec.action?.let { intent.action = it }
        spec.categories.forEach { intent.addCategory(it) }
        spec.data?.let { raw ->
            val value = expand(raw)
            val uri = when {
                value.contains("://") -> Uri.parse(value)
                value.startsWith("/") -> {
                    // A few old emulators still want file:// paths; Android blocks those by default.
                    StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
                    Uri.fromFile(File(value))
                }
                else -> Uri.parse(value)
            }
            if (spec.mimeType != null) intent.setDataAndType(uri, spec.mimeType) else intent.data = uri
        }
        for (extra in spec.extras) {
            val value = expand(extra.value)
            when (extra.type) {
                ExtraType.STRING -> intent.putExtra(extra.key, value)
                ExtraType.INT -> value.trim().toIntOrNull()?.let { intent.putExtra(extra.key, it) }
                ExtraType.BOOL -> intent.putExtra(extra.key, value.trim().lowercase() in setOf("true", "1"))
                ExtraType.STRING_ARRAY -> intent.putExtra(extra.key, splitArrayValue(value).toTypedArray())
            }
        }
        if (spec.clearTask) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (spec.clearTop) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (spec.noHistory) intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        if (grantRead) intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Built(intent, grants.distinct(), if (grants.isNotEmpty()) tree else null, needsFolder)
    }
}
