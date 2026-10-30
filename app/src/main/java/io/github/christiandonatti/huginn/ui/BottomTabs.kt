package io.github.christiandonatti.huginn.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.christiandonatti.huginn.AppRepository
import io.github.christiandonatti.huginn.BuildConfig
import io.github.christiandonatti.huginn.CommandDef
import io.github.christiandonatti.huginn.DualScreen
import io.github.christiandonatti.huginn.Covers
import io.github.christiandonatti.huginn.FolderAccess
import io.github.christiandonatti.huginn.GameLauncher
import io.github.christiandonatti.huginn.HuginnActivity
import io.github.christiandonatti.huginn.Rules
import io.github.christiandonatti.huginn.Library
import io.github.christiandonatti.huginn.Prefs
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.shared.Systems
import io.github.christiandonatti.huginn.ui.theme.HColors

/** Plural string for the current locale (pluralStringResource without the opt-in). */
@Composable
private fun plural(id: Int, count: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, count, *args)

private const val REPO_URL = "https://github.com/ChristianDonatti/huginn"

/** Consoles tab: one tile per filter; tapping one switches the carousel on the top screen. */
@Composable
fun ConsolesTab() {
    val context = LocalContext.current
    val games = rememberGames()
    val filters = remember(games, Prefs.lastPlayed, Prefs.favorites, AppRepository.apps) {
        filtersFor(context, games).filter { it.key != Home.APPS }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (games.isEmpty() || Library.scanning) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (games.isEmpty()) LibrarySetup(Modifier.fillMaxWidth())
                else Text(
                    stringResource(R.string.scanning_line, Library.scanningWhat?.substringAfterLast('/') ?: "…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = HColors.TextMuted,
                )
            }
        }
        if (games.isNotEmpty()) {
            items(filters, key = { it.key }) { f ->
                ConsoleTile(f, selected = Home.filter == f.key) { Home.filter = f.key }
            }
        }
    }
}

@Composable
private fun ConsoleTile(filter: Filter, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val color = when (filter.key) {
        Home.ALL -> HColors.Glow
        Home.RECENT -> HColors.TextSoft
        Home.FAVORITES -> HColors.Accent
        else -> Color(Systems.color(filter.key))
    }
    val subtitle = when (filter.key) {
        Home.ALL, Home.RECENT, Home.FAVORITES -> plural(R.plurals.game_count, filter.count, filter.count)
        else -> systemFullName(context, filter.key) + " · " + filter.count
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(if (selected) HColors.SurfaceHigh else HColors.Surface)
            .border(if (focused || selected) 2.dp else 1.dp, if (focused) HColors.Accent else if (selected) HColors.Text else HColors.Stroke, shape)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(8.dp).fillMaxHeight().background(color))
        Column(Modifier.padding(horizontal = 12.dp)) {
            Text(filter.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Settings tab: file access, ROM folders, emulator per console, and launcher shortcuts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(activity: Activity) {
    val context = LocalContext.current
    val here = DualScreen.currentDisplayId(activity)
    val access = rememberStorageAccess()
    val pick = rememberFolderPicker()
    val systems = remember(Library.roms) { Library.roms.map { it.system }.distinct().sortedBy { systemShortName(context, it).lowercase() } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle(stringResource(R.string.section_folders)) }
        item {
            if (access) {
                Text(stringResource(R.string.access_ok), style = MaterialTheme.typography.bodySmall, color = HColors.Ok)
            } else {
                SettingRow(Icons.Filled.Settings, stringResource(R.string.allow_access), stringResource(R.string.access_missing)) {
                    requestStorageAccess(activity)
                }
            }
        }
        items(Prefs.romRoots, key = { it }) { root -> RootRow(root, activity) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(R.string.add_folder), primary = false, icon = Icons.Filled.Add, onClick = pick)
                PillButton(stringResource(R.string.find_folders), primary = false, icon = Icons.Filled.Search, onClick = { autoDetectFolders(context) })
                PillButton(stringResource(R.string.rescan), primary = false, icon = Icons.Filled.Refresh, onClick = { Library.refresh(context) })
            }
        }
        item {
            val summary = when {
                Library.scanning -> stringResource(R.string.scanning_line, Library.scanningWhat?.substringAfterLast('/') ?: "…")
                Library.lastScan > 0 -> stringResource(R.string.library_summary, Library.roms.size, systems.size, formatWhen(Library.lastScan))
                else -> null
            }
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
        }

        if (systems.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_emulators)) }
            item { Text(stringResource(R.string.emulators_hint), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted) }
            items(systems, key = { "emu-$it" }) { system -> EmulatorRow(system) }
        }

        item { SectionTitle(stringResource(R.string.section_found)) }
        item { FoundEmulators() }

        item { SectionTitle(stringResource(R.string.section_covers)) }
        item {
            val p = Covers.progress
            val text = when {
                p != null -> stringResource(R.string.covers_progress, p.first, p.second)
                Covers.lastFound != null -> stringResource(R.string.covers_done, Covers.lastFound ?: 0)
                else -> stringResource(R.string.covers_hint)
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(R.string.covers_now), primary = false, icon = Icons.Filled.Refresh, onClick = { Covers.fetch(context, Library.roms) })
                PillButton(
                    stringResource(if (Prefs.downloadCovers) R.string.covers_auto_on else R.string.covers_auto_off),
                    primary = false,
                    onClick = { Prefs.setDownloadCovers(!Prefs.downloadCovers) },
                )
            }
        }

        item { SectionTitle(stringResource(R.string.section_launcher)) }
        item {
            SettingRow(Icons.Filled.Settings, stringResource(R.string.sc_android_settings), stringResource(R.string.sc_android_settings_desc)) {
                DualScreen.open(activity, Intent(Settings.ACTION_SETTINGS), here)
            }
        }
        item {
            SettingRow(Icons.Filled.Home, stringResource(R.string.sc_default_launcher), stringResource(R.string.sc_default_launcher_desc)) {
                DualScreen.open(activity, Intent(Settings.ACTION_HOME_SETTINGS), here)
            }
        }
        item {
            SettingRow(Icons.Filled.KeyboardArrowUp, stringResource(R.string.sc_top), stringResource(R.string.sc_top_desc)) {
                DualScreen.showTop(activity)
            }
        }
        item {
            SettingRow(Icons.Filled.Refresh, stringResource(R.string.sc_reload), stringResource(R.string.sc_reload_desc)) {
                AppRepository.refresh(activity, force = true)
            }
        }
        item {
            SettingRow(Icons.Filled.Info, stringResource(R.string.sc_repo), stringResource(R.string.sc_repo_desc)) {
                DualScreen.open(activity, Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)), here)
            }
        }
        item {
            Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.version_line, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodySmall,
                    color = HColors.TextMuted,
                )
                Text(stringResource(R.string.credits_esde), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
            }
        }
    }
}

/** One ROM folder: games found, whether it is released to the emulators, and remove. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RootRow(root: String, activity: Activity) {
    val context = LocalContext.current
    val released = remember(root, FolderAccess.version) { runCatching { FolderAccess.isReleased(context, root) }.getOrDefault(false) }
    val count = Library.perRoot[root]
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(HColors.Surface).border(1.dp, HColors.Stroke, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(root, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val status = stringResource(if (released) R.string.folder_released else R.string.folder_not_released)
        Text(
            listOfNotNull(count?.let { plural(R.plurals.game_count, it, it) }, status).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = if (released) HColors.Ok else HColors.Accent,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!released) {
                PillButton(stringResource(R.string.release_folder), primary = true, onClick = {
                    (activity as? HuginnActivity)?.pickFolder(root) { Prefs.markFolderAsked(root) }
                })
            }
            PillButton(stringResource(R.string.remove), primary = false, icon = Icons.Filled.Delete, onClick = {
                Prefs.removeRoot(root)
                Library.refresh(context)
            })
        }
    }
}

/** The emulator apps Huginn found, with their icons and the consoles they run. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoundEmulators() {
    val context = LocalContext.current
    val found by produceState<List<Rules.InstalledEmulator>?>(null, AppRepository.version) {
        value = withContext(Dispatchers.Default) { runCatching { Rules.installedEmulators(context) }.getOrDefault(emptyList()) }
    }
    val list = found
    when {
        list == null -> Text(stringResource(R.string.found_loading), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
        list.isEmpty() -> Text(stringResource(R.string.found_none), style = MaterialTheme.typography.bodySmall, color = HColors.Accent)
        else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { emu ->
                val shape = RoundedCornerShape(50)
                Row(
                    Modifier.clip(shape).background(HColors.Surface).border(1.dp, HColors.Stroke, shape).padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    emu.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(emu.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(
                            emu.systems.take(4).joinToString(", ") { systemShortName(context, it) } + if (emu.systems.size > 4) "…" else "",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                            color = HColors.TextMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = HColors.TextMuted,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

/** A full-width row for the settings list: icon, title, one line of explanation. */
@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, titleColor: Color = HColors.Text, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HColors.Surface)
            .focusRing(focused, shape)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = HColors.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Emulator for one console; a tap moves to the next installed one, then back to automatic. */
@Composable
private fun EmulatorRow(system: String) {
    val context = LocalContext.current
    val choice = remember(system, AppRepository.version, Prefs.systemEmulator) {
        runCatching { GameLauncher.systemChoice(context, system) }.getOrNull()
    } ?: return
    val name = systemShortName(context, system)
    if (choice.installed.isEmpty()) {
        SettingRow(Icons.Filled.Info, name, stringResource(R.string.emulator_none_installed), titleColor = HColors.Accent) { }
        return
    }
    val options: List<CommandDef?> = listOf(null) + choice.installed
    val index = options.indexOf(choice.chosen).coerceAtLeast(0)
    val current = choice.chosen?.displayName ?: stringResource(R.string.emulator_auto, choice.automatic?.displayName ?: "?")
    SettingRow(
        Icons.Filled.Settings,
        "$name · $current",
        plural(R.plurals.emulators_installed, choice.installed.size, choice.installed.size) + " · " + stringResource(R.string.tap_to_change),
    ) {
        Prefs.setSystemEmulator(system, options[(index + 1) % options.size]?.label)
    }
}
