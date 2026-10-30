package io.github.christiandonatti.huginn.ui

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.christiandonatti.huginn.AppRepository
import io.github.christiandonatti.huginn.Game
import io.github.christiandonatti.huginn.GameLauncher
import io.github.christiandonatti.huginn.HuginnActivity
import io.github.christiandonatti.huginn.Library
import io.github.christiandonatti.huginn.Prefs
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.Rules
import io.github.christiandonatti.huginn.shared.Systems
import io.github.christiandonatti.huginn.ui.theme.HColors
import io.github.christiandonatti.huginn.ui.theme.Unbounded

/** What the home shows; shared by both screens so the Consoles tab can switch the top screen. */
object Home {
    const val ALL = "all"
    const val RECENT = "recent"
    const val FAVORITES = "fav"
    const val APPS = "apps"

    var filter by mutableStateOf(ALL)
}

/** One chip of the filter row: everything, recents, favourites, a system, or the apps. */
data class Filter(val key: String, val label: String, val count: Int)

/** Every game: ROMs from the scan plus Android games. */
@Composable
fun rememberGames(): List<Game> {
    val context = LocalContext.current
    val roms = Library.roms
    val apps = AppRepository.apps
    val marked = Prefs.markedGames
    val unmarked = Prefs.unmarkedGames
    return remember(roms, apps, marked, unmarked) { roms + Library.androidGames(context, apps) }
}

fun systemShortName(context: Context, system: String): String =
    runCatching { Rules.system(context, system)?.shortName }.getOrNull() ?: system

fun systemFullName(context: Context, system: String): String =
    runCatching { Rules.system(context, system)?.fullName }.getOrNull() ?: system

fun filtersFor(context: Context, games: List<Game>): List<Filter> {
    val list = ArrayList<Filter>()
    list += Filter(Home.ALL, context.getString(R.string.filter_all), games.size)
    val recent = games.count { it.id in Prefs.lastPlayed }
    if (recent > 0) list += Filter(Home.RECENT, context.getString(R.string.filter_recent), recent)
    val favs = games.count { it.id in Prefs.favorites }
    if (favs > 0) list += Filter(Home.FAVORITES, context.getString(R.string.filter_favorites), favs)
    games.groupBy { it.system }
        .toList()
        .sortedBy { (system, _) -> if (system == Systems.ANDROID) "~" else systemShortName(context, system).lowercase() }
        .forEach { (system, items) -> list += Filter(system, systemShortName(context, system), items.size) }
    list += Filter(Home.APPS, context.getString(R.string.filter_apps), AppRepository.apps.size)
    return list
}

fun gamesFor(filter: String, games: List<Game>): List<Game> = when (filter) {
    Home.ALL -> games.sortedBy { it.sortKey }
    Home.RECENT -> games.filter { it.id in Prefs.lastPlayed }.sortedByDescending { Prefs.lastPlayed[it.id] }
    Home.FAVORITES -> games.filter { it.id in Prefs.favorites }.sortedBy { it.sortKey }
    else -> games.filter { it.system == filter }.sortedBy { it.sortKey }
}

fun formatSize(context: Context, bytes: Long): String? =
    if (bytes > 0) Formatter.formatShortFileSize(context, bytes) else null

fun formatWhen(time: Long): String =
    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

/**
 * Starts a game and explains in a toast when it could not. When the emulator needs the ROM
 * folder released to Huginn, opens the folder picker there first (once per folder), then plays.
 */
fun playGame(context: Context, game: Game, displayId: Int, askFolder: Boolean = true) {
    when (val r = GameLauncher.play(context, game, displayId, askFolder)) {
        GameLauncher.Result.Started -> Unit
        is GameLauncher.Result.NeedsFolder -> {
            val activity = runCatching { context.findActivity() }.getOrNull() as? HuginnActivity
            if (activity == null) {
                playGame(context, game, displayId, askFolder = false)
            } else {
                Toast.makeText(context, context.getString(R.string.release_folder_toast), Toast.LENGTH_LONG).show()
                activity.pickFolder(r.folder) {
                    Prefs.markFolderAsked(r.folder)
                    playGame(context, game, displayId, askFolder = false)
                }
            }
        }
        is GameLauncher.Result.NoEmulator -> Toast.makeText(
            context, context.getString(R.string.no_emulator_toast, systemShortName(context, r.system)), Toast.LENGTH_LONG,
        ).show()
        is GameLauncher.Result.Failed -> Toast.makeText(
            context, context.getString(R.string.play_failed, game.title, r.reason), Toast.LENGTH_LONG,
        ).show()
    }
}

/**
 * Generated cover for games without art (art comes in phase 2): the system's colour, its short
 * name, the title in Unbounded and a big faded initial, like Fwip's placeholder covers.
 */
@Composable
fun GameCover(
    game: Game,
    systemLabel: String,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 13.sp,
    icon: ImageBitmap? = null,
    corner: Int = 18,
    cover: ImageBitmap? = null,
) {
    val base = Color(Systems.color(game.system))
    Box(
        modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.linearGradient(
                    listOf(lerp(base, Color.White, 0.08f), lerp(base, HColors.Ink, 0.7f)),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
    ) {
        if (cover != null) {
            // Box art keeps its own shape (SNES boxes are wide, GBA almost square) over the system colour.
            Image(
                cover,
                contentDescription = game.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
            return@Box
        }
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(72.dp))
        } else {
            Text(
                game.title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "",
                fontFamily = Unbounded,
                fontWeight = FontWeight.Bold,
                fontSize = (titleSize.value * 7).sp,
                color = Color.White.copy(alpha = 0.09f),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-titleSize.value * 1.2f).dp),
                maxLines = 1,
            )
        }
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Text(systemLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f), maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (icon == null) {
                Text(
                    game.title,
                    fontFamily = Unbounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSize,
                    lineHeight = (titleSize.value * 1.18f).sp,
                    color = HColors.Text,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
