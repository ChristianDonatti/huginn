package io.github.christiandonatti.huginn.shared

/**
 * Launch commands in the format ES-DE uses on Android, for example
 * `%EMULATOR_MELONDS% %ACTION%=me.magnum.melonds.LAUNCH_ROM %EXTRA_uri%=%ROMSAF%`.
 *
 * Each command describes an Android Intent: which emulator (looked up in the find rules), the
 * action, data, extras and flags. Values keep their variables (%ROM%, %ROMSAF%...) until the
 * game is known; [expandValue] fills them in.
 */
enum class ExtraType { STRING, INT, BOOL, STRING_ARRAY }

data class Extra(val key: String, val value: String, val type: ExtraType)

data class LaunchSpec(
    /** Name in the emulator find rules, such as RETROARCH or DOLPHIN. */
    val emulator: String? = null,
    /** For native Android games: a package (optionally "package/activity"), often %FILEINJECT%. */
    val androidApp: String? = null,
    val action: String? = null,
    val categories: List<String> = emptyList(),
    val mimeType: String? = null,
    val data: String? = null,
    val extras: List<Extra> = emptyList(),
    val clearTask: Boolean = false,
    val clearTop: Boolean = false,
    val noHistory: Boolean = false,
)

/** Splits on spaces, keeping "quoted parts" together (the quotes themselves are dropped). */
fun tokenizeCommand(command: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var any = false
    for (ch in command) {
        when {
            ch == '"' -> { quoted = !quoted; any = true }
            ch.isWhitespace() && !quoted -> {
                if (any) tokens += current.toString()
                current.clear()
                any = false
            }
            else -> { current.append(ch); any = true }
        }
    }
    if (any) tokens += current.toString()
    return tokens
}

/** Reads one launch command; null when it launches nothing Huginn knows how to start. */
fun parseCommand(command: String): LaunchSpec? {
    var spec = LaunchSpec()
    for (token in tokenizeCommand(command)) {
        when {
            token == "%ACTIVITY_CLEAR_TASK%" -> spec = spec.copy(clearTask = true)
            token == "%ACTIVITY_CLEAR_TOP%" -> spec = spec.copy(clearTop = true)
            token == "%ACTIVITY_NO_HISTORY%" -> spec = spec.copy(noHistory = true)
            token.startsWith("%EMULATOR_") && token.endsWith("%") && !token.contains("%=") ->
                spec = spec.copy(emulator = token.removePrefix("%EMULATOR_").removeSuffix("%"))
            else -> {
                val split = token.indexOf("%=")
                if (split < 0) continue
                val key = token.substring(0, split + 1)
                val value = token.substring(split + 2)
                spec = when {
                    key == "%ACTION%" -> spec.copy(action = value)
                    key == "%CATEGORY%" -> spec.copy(categories = spec.categories + value)
                    key == "%MIMETYPE%" -> spec.copy(mimeType = value)
                    key == "%DATA%" -> spec.copy(data = value)
                    key == "%ANDROIDAPP%" -> spec.copy(androidApp = value)
                    key.startsWith("%EXTRA_") -> spec.withExtra(key, "%EXTRA_", value, ExtraType.STRING)
                    key.startsWith("%EXTRAINTEGER_") -> spec.withExtra(key, "%EXTRAINTEGER_", value, ExtraType.INT)
                    key.startsWith("%EXTRABOOL_") -> spec.withExtra(key, "%EXTRABOOL_", value, ExtraType.BOOL)
                    key.startsWith("%EXTRAARRAY_") -> spec.withExtra(key, "%EXTRAARRAY_", value, ExtraType.STRING_ARRAY)
                    else -> spec
                }
            }
        }
    }
    return if (spec.emulator == null && spec.androidApp == null) null else spec
}

private fun LaunchSpec.withExtra(key: String, prefix: String, value: String, type: ExtraType) =
    copy(extras = extras + Extra(key.removePrefix(prefix).removeSuffix("%"), value, type))

private val variable = Regex("%([A-Z][A-Z0-9_]*)%")

/**
 * Fills in the variables of a value. [resolve] gives the value of a variable such as ROM or
 * ROMSAF (null leaves it as it is). `%INJECT%=file` is then replaced by the contents of that
 * file, read by [inject]; the file name runs to the end of the value or the next comma.
 */
fun expandValue(value: String, resolve: (String) -> String?, inject: (String) -> String? = { null }): String {
    var out = variable.replace(value) { m ->
        if (m.groupValues[1] == "INJECT") m.value else resolve(m.groupValues[1]) ?: m.value
    }
    while (true) {
        val at = out.indexOf("%INJECT%=")
        if (at < 0) break
        val start = at + "%INJECT%=".length
        val end = out.indexOf(',', start).let { if (it < 0) out.length else it }
        val contents = inject(out.substring(start, end))?.trim().orEmpty()
        out = out.substring(0, at) + contents + out.substring(end)
    }
    return out
}

/** The entries of an %EXTRAARRAY_ value: comma separated, with "\," for a literal comma. */
fun splitArrayValue(value: String): List<String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < value.length) {
        val ch = value[i]
        if (ch == '\\' && i + 1 < value.length && value[i + 1] == ',') {
            current.append(','); i += 2; continue
        }
        if (ch == ',') { parts += current.toString(); current.clear() } else current.append(ch)
        i++
    }
    parts += current.toString()
    return parts
}

/** A find-rules entry "package/activity" (activity may start with "."), as package and class. */
fun splitComponent(entry: String): Pair<String, String?> {
    val slash = entry.indexOf('/')
    if (slash < 0) return entry.trim() to null
    val pkg = entry.substring(0, slash).trim()
    val cls = entry.substring(slash + 1).trim()
    return pkg to (if (cls.startsWith(".")) pkg + cls else cls)
}
