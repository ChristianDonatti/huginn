package io.github.christiandonatti.huginn

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.christiandonatti.huginn.shared.PRIMARY_STORAGE
import io.github.christiandonatti.huginn.shared.Systems
import io.github.christiandonatti.huginn.shared.cleanTitle
import io.github.christiandonatti.huginn.shared.listingExtensions
import io.github.christiandonatti.huginn.shared.referencedFiles
import io.github.christiandonatti.huginn.shared.regionOf
import io.github.christiandonatti.huginn.shared.sortKey as titleSortKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A game in the library: a ROM file (or folder) or an Android app marked as a game. */
data class Game(
    /** The file path for ROMs, "app:<package>" for Android games. */
    val id: String,
    val system: String,
    val title: String,
    val path: String? = null,
    /** The folder that holds the system folders, for ES-DE's %ROMPATH%. */
    val romPath: String? = null,
    val packageName: String? = null,
    val size: Long = 0,
    val region: String? = null,
) {
    val sortKey: String = titleSortKey(title)
    val fileName: String? get() = path?.substringAfterLast('/')
    val extension: String? get() = fileName?.substringAfterLast('.', "")?.lowercase()
}

/**
 * The game library: scans the ROM folders, keeps the result in a cache file so the home is
 * filled at once on the next start, and adds the Android apps that are games.
 */
object Library {
    var roms by mutableStateOf<List<Game>>(emptyList())
        private set
    var scanning by mutableStateOf(false)
        private set
    /** Folder being scanned right now, for the status line. */
    var scanningWhat by mutableStateOf<String?>(null)
        private set
    var lastScan by mutableStateOf(0L)
        private set
    /** Games found per ROM folder in the last scan. */
    var perRoot by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var cacheLoaded = false

    private val skipFolders = setOf(
        "media", "downloaded_media", "images", "videos", "manuals", "bios", "saves", "states",
        "screenshots", "covers", "boxart", "box art", "snap", "snaps", "wheel", "marquees", "titles",
        "fanart", "artwork", "music",
    )

    private fun cacheFile(context: Context) = File(context.filesDir, "library.json")

    /** Loads the cache (once) and scans the folders again in the background. */
    fun refresh(context: Context, rescan: Boolean = true) {
        val app = context.applicationContext
        scope.launch {
            mutex.withLock {
                if (!cacheLoaded) {
                    cacheLoaded = true
                    readCache(app)?.let { withContext(Dispatchers.Main) { roms = it.first; lastScan = it.second } }
                }
                if (rescan) scan(app)
            }
        }
    }

    /** Looks for ROM folders by name, adds the new ones and scans; [done] gets how many were added. */
    fun autoDetect(context: Context, done: (Int) -> Unit) {
        val app = context.applicationContext
        scope.launch {
            val added = runCatching { StorageAccess.findRomFolders(app) }.getOrDefault(emptyList()).count { Prefs.addRoot(it) }
            Prefs.markAutoDetected()
            withContext(Dispatchers.Main) { done(added) }
            mutex.withLock { scan(app) }
        }
    }

    private suspend fun scan(context: Context) {
        if (!StorageAccess.hasAccess(context)) return
        withContext(Dispatchers.Main) { scanning = true }
        try {
            if (Prefs.romRoots.isEmpty() && !Prefs.autoDetected) {
                StorageAccess.findRomFolders(context).forEach { Prefs.addRoot(it) }
                Prefs.markAutoDetected()
            }
            val systems = Rules.systems(context)
            val seen = HashSet<String>()
            val found = ArrayList<Game>()
            val counts = LinkedHashMap<String, Int>()
            for (root in Prefs.romRoots) {
                val before = found.size
                for (dir in systemFolders(File(root), systems.keys)) {
                    val system = systems[Systems.canonical(dir.name)] ?: continue
                    withContext(Dispatchers.Main) { scanningWhat = dir.path }
                    scanSystem(dir, system, seen, found)
                }
                counts[root] = found.size - before
            }
            val sorted = found.sortedWith(compareBy({ it.system }, { it.sortKey }))
            val now = System.currentTimeMillis()
            writeCache(context, sorted, now)
            withContext(Dispatchers.Main) {
                roms = sorted
                perRoot = counts
                lastScan = now
            }
            if (Prefs.downloadCovers) Covers.fetch(context, sorted)
        } finally {
            withContext(Dispatchers.Main) {
                scanning = false
                scanningWhat = null
            }
        }
    }

    /** The folder itself if it is a system folder, else its system subfolders, two levels deep. */
    private fun systemFolders(root: File, known: Set<String>): List<File> {
        fun isSystem(f: File) = Systems.canonical(f.name) in known
        if (!root.isDirectory) return emptyList()
        if (isSystem(root)) return listOf(root)
        val result = ArrayList<File>()
        for (child in root.listFiles().orEmpty().filter { it.isDirectory && !it.isHidden }.sortedBy { it.name.lowercase() }) {
            if (isSystem(child)) result += child
            else if (child.name.lowercase() !in skipFolders) {
                child.listFiles().orEmpty().filter { it.isDirectory && !it.isHidden && isSystem(it) }.sortedBy { it.name.lowercase() }.forEach { result += it }
            }
        }
        return result
    }

    private fun scanSystem(dir: File, system: SystemDef, seen: MutableSet<String>, out: MutableList<Game>) {
        val candidates = ArrayList<File>()
        fun walk(folder: File, depth: Int) {
            for (f in folder.listFiles().orEmpty()) {
                if (f.name.startsWith(".")) continue
                val ext = f.name.substringAfterLast('.', "").lowercase()
                if (f.isDirectory) {
                    // ES-DE treats a folder named like a game file ("Game.ps3") as the game.
                    if (ext.isNotEmpty() && ext in system.extensions) candidates += f
                    else if (depth < 3 && f.name.lowercase() !in skipFolders) walk(f, depth + 1)
                } else if (ext in system.extensions) {
                    candidates += f
                }
            }
        }
        walk(dir, 0)

        // Discs of a playlist and tracks of a cue sheet are part of another entry.
        val hidden = HashSet<String>()
        for (f in candidates) {
            val ext = f.name.substringAfterLast('.', "").lowercase()
            if (ext !in listingExtensions || f.isDirectory || f.length() > 256 * 1024) continue
            val text = runCatching { f.readText() }.getOrNull() ?: continue
            referencedFiles(f.name, text).forEach { hidden += File(f.parentFile, it).normalize().path }
        }

        val romPath = dir.parentFile?.path
        for (f in candidates) {
            val path = f.normalize().path
            if (path in hidden || !seen.add(path)) continue
            out += Game(
                id = path,
                system = system.name,
                title = cleanTitle(f.name),
                path = path,
                romPath = romPath,
                size = if (f.isDirectory) 0 else f.length(),
                region = regionOf(f.name),
            )
        }
    }

    private fun writeCache(context: Context, games: List<Game>, time: Long) {
        val arr = JSONArray()
        for (g in games) {
            arr.put(JSONObject().apply {
                put("s", g.system); put("t", g.title); put("p", g.path); put("r", g.romPath)
                put("z", g.size); g.region?.let { put("g", it) }
            })
        }
        val json = JSONObject().put("v", 1).put("time", time).put("games", arr)
        runCatching {
            val tmp = File(context.filesDir, "library.json.tmp")
            tmp.writeText(json.toString())
            tmp.renameTo(cacheFile(context))
        }
    }

    private fun readCache(context: Context): Pair<List<Game>, Long>? = runCatching {
        val json = JSONObject(cacheFile(context).readText())
        val arr = json.getJSONArray("games")
        val games = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val path = o.getString("p")
            Game(
                id = path, system = o.getString("s"), title = o.getString("t"), path = path,
                romPath = o.optString("r").ifEmpty { null }, size = o.optLong("z"),
                region = o.optString("g").ifEmpty { null },
            )
        }
        games to json.optLong("time")
    }.getOrNull()

    /** Installed apps that are games: marked by the user, or flagged as games by Android. */
    fun androidGames(context: Context, apps: List<AppEntry>): List<Game> {
        val emulators = runCatching { Rules.emulatorPackages(context) }.getOrDefault(emptySet())
        return apps.asSequence()
            .filter { app ->
                val pkg = app.packageName
                pkg in Prefs.markedGames || (app.flaggedAsGame && pkg !in Prefs.unmarkedGames && pkg !in emulators)
            }
            .distinctBy { it.packageName }
            .map { Game(id = "app:${it.packageName}", system = Systems.ANDROID, title = it.label, packageName = it.packageName) }
            .toList()
    }

    /** Whether an app counts as a game right now (for the badge in the Apps tab). */
    fun isGameApp(app: AppEntry, emulators: Set<String>): Boolean =
        app.packageName in Prefs.markedGames ||
            (app.flaggedAsGame && app.packageName !in Prefs.unmarkedGames && app.packageName !in emulators)
}

/** Whether Android lists an app as a game (Play Store category or the old isGame flag). */
fun ApplicationInfo.isGameByAndroid(): Boolean {
    @Suppress("DEPRECATION")
    val flag = (flags and ApplicationInfo.FLAG_IS_GAME) != 0
    return flag || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && category == ApplicationInfo.CATEGORY_GAME)
}

/** All files access (to read ROM folders like ES-DE does) and finding ROM folders by name. */
object StorageAccess {
    fun hasAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Root of every storage volume: internal storage and SD cards. */
    fun volumes(context: Context): List<File> {
        val result = LinkedHashSet<File>()
        result += File(PRIMARY_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sm = context.getSystemService(StorageManager::class.java)
            sm?.storageVolumes?.mapNotNullTo(result) { it.directory }
        }
        File("/storage").listFiles().orEmpty()
            .filter { it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead() }
            .forEach { result += it }
        return result.toList()
    }

    private val rootNames = setOf("roms", "rom", "games", "jogos", "emulation", "es-de", "retroarch")

    /**
     * Folders that look like a ROM collection: a "ROMs" (or similar) folder at the top of a
     * storage volume, or inside "Emulation", holding at least one folder named after a system.
     */
    fun findRomFolders(context: Context): List<String> {
        val known = Rules.systems(context).keys
        fun systemCount(dir: File) = dir.listFiles().orEmpty().count { it.isDirectory && Systems.canonical(it.name) in known }
        val found = ArrayList<String>()
        for (volume in volumes(context)) {
            val tops = volume.listFiles().orEmpty().filter { it.isDirectory && it.name.lowercase() in rootNames }
            for (top in tops) {
                val candidates = listOf(top) + top.listFiles().orEmpty().filter { it.isDirectory && it.name.lowercase() in rootNames }
                for (c in candidates) {
                    if (systemCount(c) > 0 && found.none { c.path.startsWith("$it/") || it == c.path }) found += c.path
                }
            }
        }
        return found
    }
}
