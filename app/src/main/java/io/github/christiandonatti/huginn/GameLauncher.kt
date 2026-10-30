package io.github.christiandonatti.huginn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.AppOpsManager
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
        data class Failed(val reason: String) : Result
    }

    /** Starts [game] on the given screen. */
    /**
     * What the last attempt to start a game sent to Android, shown in the Screens tab with a copy
     * button so a failed launch can be reported exactly.
     */
    var lastLaunch by mutableStateOf<String?>(null)
        private set

    fun play(context: Context, game: Game, displayId: Int): Result {
        var command: CommandDef? = null
        val result = run {
            val intent = try {
                if (game.packageName != null) {
                    context.packageManager.getLaunchIntentForPackage(game.packageName)
                        ?: return@run Result.Failed("no launch activity")
                } else {
                    command = choice(context, game).current ?: return@run Result.NoEmulator(game.system)
                    buildIntent(context, game, command!!)
                }
            } catch (e: Exception) {
                return@run Result.Failed("${e.javaClass.simpleName}: ${e.message}")
            }.also { lastLaunch = describe(context, game, command, it, displayId, null) }
            if (!DualScreen.open(context, intent, displayId)) return@run Result.Failed(DualScreen.lastError ?: "")
            Prefs.played(game.id)
            Result.Started
        }
        if (result != Result.Started) {
            val head = lastLaunch?.takeIf { it.contains(game.title) } ?: describe(context, game, command, null, displayId, null)
            lastLaunch = head + "\nresult: " + result
        } else {
            lastLaunch = lastLaunch + "\nresult: started"
        }
        Log.i("Huginn", lastLaunch ?: "")
        return result
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

    private fun buildIntent(context: Context, game: Game, command: CommandDef): Intent {
        val spec = command.spec
        val path = game.path ?: error("game without a file")
        val file = File(path)
        val userId = Process.myUid() / 100_000
        val component: ComponentName? = spec.emulator?.let { Rules.installed(context, it) }
        var grantRead = false

        fun readSmall(f: File): String? =
            if (f.isFile && f.length() <= 4096) runCatching { f.readText().trim() }.getOrNull() else null

        val resolve: (String) -> String? = { name ->
            when (name) {
                "ROM", "ROMRAW" -> path
                "ROMSAF" -> safDocumentUri(path)
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
            return launch
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
        return intent
    }
}
