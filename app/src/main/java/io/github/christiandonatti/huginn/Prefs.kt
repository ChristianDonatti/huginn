package io.github.christiandonatti.huginn

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Everything the user chose, kept in SharedPreferences and mirrored in Compose state so both
 * screens update together: ROM folders, emulator choices, favourites, what counts as a game.
 */
object Prefs {
    private lateinit var sp: SharedPreferences

    /** Folders that hold system folders (or are one), in the order they were added. */
    var romRoots by mutableStateOf<List<String>>(emptyList())
        private set

    /** Emulator (command label) chosen per system; missing means automatic. */
    var systemEmulator by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** Emulator chosen for a single game, over the system choice. */
    var gameEmulator by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var lastPlayed by mutableStateOf<Map<String, Long>>(emptyMap())
        private set

    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Apps the user marked as games, and games Android suggested that the user unmarked. */
    var markedGames by mutableStateOf<Set<String>>(emptySet())
        private set
    var unmarkedGames by mutableStateOf<Set<String>>(emptySet())
        private set

    /** ROM folders Huginn already asked to release in the folder picker (asked once each). */
    var askedFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Minutes played per game. */
    var playMinutes by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** Download box art from the libretro thumbnails server after each scan. */
    var downloadCovers by mutableStateOf(true)
        private set

    /** True once Huginn looked for ROM folders by itself, so it does not do it on every start. */
    var autoDetected by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.applicationContext.getSharedPreferences("huginn", Context.MODE_PRIVATE)
        romRoots = sp.getString("romRoots", "").orEmpty().lines().filter { it.isNotBlank() }
        systemEmulator = readMap("systemEmulator") { it as String }
        gameEmulator = readMap("gameEmulator") { it as String }
        lastPlayed = readMap("lastPlayed") { (it as Number).toLong() }
        favorites = sp.getStringSet("favorites", emptySet()).orEmpty().toSet()
        markedGames = sp.getStringSet("markedGames", emptySet()).orEmpty().toSet()
        unmarkedGames = sp.getStringSet("unmarkedGames", emptySet()).orEmpty().toSet()
        autoDetected = sp.getBoolean("autoDetected", false)
        askedFolders = sp.getStringSet("askedFolders", emptySet()).orEmpty().toSet()
        playMinutes = readMap("playMinutes") { (it as Number).toInt() }
        downloadCovers = sp.getBoolean("downloadCovers", true)
    }

    private fun <T> readMap(key: String, convert: (Any) -> T): Map<String, T> {
        val json = runCatching { JSONObject(sp.getString(key, "{}") ?: "{}") }.getOrNull() ?: return emptyMap()
        return json.keys().asSequence().associateWith { convert(json.get(it)) }
    }

    private fun writeMap(key: String, map: Map<String, Any>) {
        sp.edit().putString(key, JSONObject(map).toString()).apply()
    }

    fun addRoot(path: String): Boolean {
        if (path in romRoots) return false
        romRoots = romRoots + path
        sp.edit().putString("romRoots", romRoots.joinToString("\n")).apply()
        return true
    }

    fun removeRoot(path: String) {
        romRoots = romRoots - path
        sp.edit().putString("romRoots", romRoots.joinToString("\n")).apply()
    }

    fun markFolderAsked(folder: String) {
        askedFolders = askedFolders + folder
        sp.edit().putStringSet("askedFolders", askedFolders).apply()
    }

    fun addPlayTime(gameId: String, minutes: Int) {
        playMinutes = playMinutes + (gameId to (playMinutes[gameId] ?: 0) + minutes)
        writeMap("playMinutes", playMinutes)
    }

    fun setDownloadCovers(on: Boolean) {
        downloadCovers = on
        sp.edit().putBoolean("downloadCovers", on).apply()
    }

    fun markAutoDetected() {
        autoDetected = true
        sp.edit().putBoolean("autoDetected", true).apply()
    }

    fun setSystemEmulator(system: String, label: String?) {
        systemEmulator = if (label == null) systemEmulator - system else systemEmulator + (system to label)
        writeMap("systemEmulator", systemEmulator)
    }

    fun setGameEmulator(gameId: String, label: String?) {
        gameEmulator = if (label == null) gameEmulator - gameId else gameEmulator + (gameId to label)
        writeMap("gameEmulator", gameEmulator)
    }

    fun played(gameId: String) {
        lastPlayed = lastPlayed + (gameId to System.currentTimeMillis())
        writeMap("lastPlayed", lastPlayed)
    }

    fun toggleFavorite(gameId: String) {
        favorites = if (gameId in favorites) favorites - gameId else favorites + gameId
        sp.edit().putStringSet("favorites", favorites).apply()
    }

    /** Marks or unmarks an app as a game, remembering the choice over Android's suggestion. */
    fun setAppIsGame(packageName: String, isGame: Boolean) {
        markedGames = if (isGame) markedGames + packageName else markedGames - packageName
        unmarkedGames = if (isGame) unmarkedGames - packageName else unmarkedGames + packageName
        sp.edit().putStringSet("markedGames", markedGames).putStringSet("unmarkedGames", unmarkedGames).apply()
    }
}
