package io.github.christiandonatti.huginn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
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
    fun play(context: Context, game: Game, displayId: Int): Result {
        val intent = try {
            if (game.packageName != null) {
                context.packageManager.getLaunchIntentForPackage(game.packageName)
                    ?: return Result.Failed("no launch activity")
            } else {
                val command = choice(context, game).current ?: return Result.NoEmulator(game.system)
                buildIntent(context, game, command)
            }
        } catch (e: Exception) {
            return Result.Failed("${e.javaClass.simpleName}: ${e.message}")
        }
        if (!DualScreen.open(context, intent, displayId)) return Result.Failed(DualScreen.lastError ?: "")
        Prefs.played(game.id)
        return Result.Started
    }

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
