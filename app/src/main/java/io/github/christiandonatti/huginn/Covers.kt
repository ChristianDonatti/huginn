package io.github.christiandonatti.huginn

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import io.github.christiandonatti.huginn.shared.cleanTitle
import io.github.christiandonatti.huginn.shared.regionOf
import io.github.christiandonatti.huginn.shared.stripExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Box art from the libretro thumbnails server (the covers RetroArch shows). For each system
 * Huginn downloads the list of covers once, matches the ROM names against it (No-Intro names
 * match exactly; other names by title and region) and keeps the images in its own storage.
 */
object Covers {
    private const val SERVER = "https://thumbnails.libretro.com"

    /** Goes up when new covers arrive, so the screens reload them. */
    var version by mutableStateOf(0)
        private set
    /** (done, total) while downloading. */
    var progress by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    /** Covers found in the last download. */
    var lastFound by mutableStateOf<Int?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val memory = LruCache<String, ImageBitmap>(90)
    private val missing = HashSet<String>()

    /** ES-DE system name to the libretro database name used by the thumbnails server. */
    private val libretro = mapOf(
        "nes" to "Nintendo - Nintendo Entertainment System", "famicom" to "Nintendo - Nintendo Entertainment System",
        "fds" to "Nintendo - Family Computer Disk System", "snes" to "Nintendo - Super Nintendo Entertainment System",
        "sfc" to "Nintendo - Super Nintendo Entertainment System", "n64" to "Nintendo - Nintendo 64",
        "gb" to "Nintendo - Game Boy", "gbc" to "Nintendo - Game Boy Color", "gba" to "Nintendo - Game Boy Advance",
        "nds" to "Nintendo - Nintendo DS", "n3ds" to "Nintendo - Nintendo 3DS", "gc" to "Nintendo - GameCube",
        "wii" to "Nintendo - Wii", "wiiu" to "Nintendo - Wii U", "virtualboy" to "Nintendo - Virtual Boy",
        "pokemini" to "Nintendo - Pokemon Mini", "psx" to "Sony - PlayStation", "ps2" to "Sony - PlayStation 2",
        "ps3" to "Sony - PlayStation 3", "psp" to "Sony - PlayStation Portable", "psvita" to "Sony - PlayStation Vita",
        "megadrive" to "Sega - Mega Drive - Genesis", "genesis" to "Sega - Mega Drive - Genesis",
        "mastersystem" to "Sega - Master System - Mark III", "gamegear" to "Sega - Game Gear",
        "segacd" to "Sega - Mega-CD - Sega CD", "megacd" to "Sega - Mega-CD - Sega CD", "sega32x" to "Sega - 32X",
        "saturn" to "Sega - Saturn", "dreamcast" to "Sega - Dreamcast", "sg-1000" to "Sega - SG-1000",
        "pcengine" to "NEC - PC Engine - TurboGrafx 16", "tg16" to "NEC - PC Engine - TurboGrafx 16",
        "pcenginecd" to "NEC - PC Engine CD - TurboGrafx-CD", "tg-cd" to "NEC - PC Engine CD - TurboGrafx-CD",
        "supergrafx" to "NEC - PC Engine SuperGrafx", "neogeocd" to "SNK - Neo Geo CD",
        "ngp" to "SNK - Neo Geo Pocket", "ngpc" to "SNK - Neo Geo Pocket Color",
        "atari2600" to "Atari - 2600", "atari5200" to "Atari - 5200", "atari7800" to "Atari - 7800",
        "atarilynx" to "Atari - Lynx", "atarijaguar" to "Atari - Jaguar", "wonderswan" to "Bandai - WonderSwan",
        "wonderswancolor" to "Bandai - WonderSwan Color", "colecovision" to "Coleco - ColecoVision",
        "intellivision" to "Mattel - Intellivision", "msx" to "Microsoft - MSX", "c64" to "Commodore - 64",
        "amiga" to "Commodore - Amiga", "3do" to "The 3DO Company - 3DO", "vectrex" to "GCE - Vectrex",
    )

    fun supports(system: String) = system in libretro

    fun file(context: Context, game: Game): File =
        File(context.filesDir, "covers/${game.system}/${sha1(game.id)}.png")

    /** Downloads the covers that are still missing (only one download runs at a time). */
    fun fetch(context: Context, games: List<Game>) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            val todo = games.filter { it.path != null && supports(it.system) && !file(app, it).exists() && it.id !in missing }
            if (todo.isEmpty()) { withContext(Dispatchers.Main) { lastFound = 0 }; return@launch }
            var done = 0
            var found = 0
            withContext(Dispatchers.Main) { progress = 0 to todo.size }
            for ((system, items) in todo.groupBy { it.system }) {
                val names = runCatching { listing(app, system) }.getOrNull()
                for (game in items) {
                    val name = names?.let { match(game, it) }
                    if (name != null && download(system, name, file(app, game))) {
                        found++
                        if (found % 6 == 0) withContext(Dispatchers.Main) { version++ }
                    } else {
                        missing += game.id
                    }
                    done++
                    withContext(Dispatchers.Main) { progress = done to todo.size }
                }
            }
            withContext(Dispatchers.Main) {
                progress = null
                lastFound = found
                version++
            }
        }
    }

    /** Names (without .png) of every box art the server has for a system, cached for two weeks. */
    private fun listing(context: Context, system: String): List<String> {
        val cache = File(context.filesDir, "covers/_index/$system.txt")
        if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < 14L * 24 * 3600 * 1000) {
            return cache.readLines().filter { it.isNotBlank() }
        }
        val html = get("$SERVER/${segment(libretro.getValue(system))}/Named_Boxarts/")?.toString(Charsets.UTF_8)
            ?: return emptyList()
        val names = Regex("""href="([^"?/]+\.png)"""").findAll(html)
            .map { URLDecoder.decode(it.groupValues[1].replace("+", "%2B"), "UTF-8").removeSuffix(".png") }
            .distinct().toList()
        if (names.isNotEmpty()) {
            cache.parentFile?.mkdirs()
            cache.writeText(names.joinToString("\n"))
        }
        return names
    }

    /** libretro writes these characters as "_" in file names. */
    private fun libretroSafe(name: String) = name.replace(Regex("""[&*/:`<>?\\|"]"""), "_")

    private fun key(name: String) = cleanTitle(name).lowercase().replace("&", "and").filter { it.isLetterOrDigit() }

    private val regionOrder = listOf("USA", "World", "Europe", "Brazil", "Japan")

    private fun match(game: Game, names: List<String>): String? {
        val base = libretroSafe(stripExtension(game.fileName ?: return null))
        if (base in names) return base
        val k = key(base)
        val same = names.filter { key(it) == k }
        if (same.isEmpty()) return null
        val wanted = listOfNotNull(regionOf(game.fileName ?: "")) + regionOrder
        return same.minWithOrNull(compareBy<String>({ n ->
            val r = regionOf("$n.png") ?: ""
            wanted.indexOfFirst { r.contains(it) }.let { if (it < 0) 99 else it }
        }, { it.length }))
    }

    private fun download(system: String, name: String, target: File): Boolean {
        val bytes = get("$SERVER/${segment(libretro.getValue(system))}/Named_Boxarts/${segment(name)}.png") ?: return false
        // PNG signature: anything else is an error page.
        if (bytes.size < 8 || bytes[0] != 0x89.toByte() || bytes[1] != 'P'.code.toByte()) return false
        target.parentFile?.mkdirs()
        val tmp = File(target.path + ".tmp")
        tmp.writeBytes(bytes)
        return tmp.renameTo(target)
    }

    private fun segment(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun get(url: String): ByteArray? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "Huginn/${BuildConfig.VERSION_NAME}")
        try {
            if (c.responseCode != 200) null else c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)

    /** The cover image, scaled down to about [maxSide] pixels; null when there is none. */
    fun load(context: Context, game: Game, maxSide: Int = 480): ImageBitmap? {
        val key = game.id + "@" + maxSide
        memory.get(key)?.let { return it }
        val f = file(context, game)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return bmp.asImageBitmap().also { memory.put(key, it) }
    }

    /** Removes downloaded covers, e.g. to download them again. */
    fun clear(context: Context) {
        File(context.filesDir, "covers").deleteRecursively()
        memory.evictAll()
        missing.clear()
        version++
    }
}

/** A game's cover, loaded in the background; null while loading or when there is none. */
@Composable
fun rememberCover(game: Game, maxSide: Int = 480): ImageBitmap? {
    val context = LocalContext.current
    if (game.path == null) return null
    val cover by produceState<ImageBitmap?>(null, game.id, Covers.version, maxSide) {
        value = withContext(Dispatchers.IO) { runCatching { Covers.load(context, game, maxSide) }.getOrNull() }
    }
    return cover
}
