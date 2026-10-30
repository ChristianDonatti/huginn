package io.github.christiandonatti.huginn.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.christiandonatti.huginn.AppEntry
import io.github.christiandonatti.huginn.AppRepository
import io.github.christiandonatti.huginn.Library
import io.github.christiandonatti.huginn.Prefs
import io.github.christiandonatti.huginn.Rules
import io.github.christiandonatti.huginn.BuildConfig
import io.github.christiandonatti.huginn.DualScreen
import io.github.christiandonatti.huginn.GameLauncher
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.ScreenInfo
import io.github.christiandonatti.huginn.shared.formatMinutes
import io.github.christiandonatti.huginn.ui.theme.HColors
import kotlinx.coroutines.delay

/** The panel on the bottom screen: consoles, every app, settings, and what Huginn sees of the screens. */
@Composable
fun BottomScreen(activity: Activity) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(HColors.Ink)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusRow()
        Segmented(
            labels = listOf(
                stringResource(R.string.tab_consoles),
                stringResource(R.string.tab_apps),
                stringResource(R.string.tab_settings),
                stringResource(R.string.tab_screens),
            ),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> ConsolesTab()
                1 -> AppsTab(activity)
                2 -> SettingsTab(activity)
                else -> ScreensTab(activity)
            }
        }
    }
}

private fun readBattery(context: Context): Pair<Int, Boolean> {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) to bm.isCharging
}

private fun readWifi(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

@Composable
private fun StatusRow() {
    val context = LocalContext.current
    val clock = rememberClock()
    var battery by remember { mutableStateOf(readBattery(context)) }
    var wifi by remember { mutableStateOf(readWifi(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            battery = readBattery(context)
            wifi = readWifi(context)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(clock, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (wifi) HColors.Ok else HColors.TextMuted))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(if (wifi) R.string.wifi_on else R.string.wifi_off), style = MaterialTheme.typography.labelMedium, color = HColors.TextSoft)
        Spacer(Modifier.width(14.dp))
        val level = "${battery.first}%" + if (battery.second) " · " + stringResource(R.string.charging) else ""
        Text(level, style = MaterialTheme.typography.labelMedium, color = HColors.TextSoft)
    }
}

@Composable
private fun AppsTab(activity: Activity) {
    val context = LocalContext.current
    val apps = AppRepository.apps
    val emulators = remember(AppRepository.version) { runCatching { Rules.emulatorPackages(context) }.getOrDefault(emptySet()) }
    var otherScreen by rememberSaveable { mutableStateOf(false) }
    fun toggleGame(app: AppEntry) {
        val nowGame = !Library.isGameApp(app, emulators)
        Prefs.setAppIsGame(app.packageName, nowGame)
        Toast.makeText(
            context,
            context.getString(if (nowGame) R.string.marked_game else R.string.unmarked_game, app.label),
            Toast.LENGTH_SHORT,
        ).show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.open_in), style = MaterialTheme.typography.labelMedium, color = HColors.TextMuted)
            Spacer(Modifier.width(10.dp))
            Segmented(
                labels = listOf(stringResource(R.string.target_here), stringResource(R.string.target_other)),
                selected = if (otherScreen) 1 else 0,
                onSelect = { otherScreen = it == 1 },
                modifier = Modifier.weight(1f),
            )
        }
        Text(stringResource(R.string.hint_mark_game), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(apps, key = { it.component.flattenToString() }) { app ->
                val isGame = Library.isGameApp(app, emulators)
                AppGridItem(app, isGame, onLongClick = { toggleGame(app) }) {
                    val target = if (otherScreen) DualScreen.otherDisplayId(activity) else DualScreen.currentDisplayId(activity)
                    if (!DualScreen.launch(context, app, target)) {
                        Toast.makeText(context, context.getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(app: AppEntry, isGame: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(
        Modifier
            .clip(shape)
            .then(if (focused) Modifier.border(2.dp, HColors.Accent, shape) else Modifier)
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.ButtonY) { onLongClick(); true } else false
            }
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(52.dp))
            if (isGame) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(HColors.Accent)
                        .border(2.dp, HColors.Ink, CircleShape),
                )
            }
        }
        Text(
            app.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScreensTab(activity: Activity) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val here = DualScreen.currentDisplayId(activity)
    val screens = remember(version) { DualScreen.describe(context) }
    val minutesOpen = ((SystemClock.elapsedRealtime() - DualScreen.startedAt) / 60_000L).toInt()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(stringResource(R.string.screens_title), style = MaterialTheme.typography.titleMedium)
        }
        items(screens) { s -> ScreenCard(s, isHere = s.id == here) }
        item {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.device_line, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE),
                    style = MaterialTheme.typography.bodySmall,
                    color = HColors.TextSoft,
                )
                Text(stringResource(R.string.uptime, formatMinutes(minutesOpen)), style = MaterialTheme.typography.bodySmall, color = HColors.TextSoft)
                Text(
                    stringResource(R.string.version_line, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodySmall,
                    color = HColors.TextSoft,
                )
                DualScreen.lastError?.let {
                    Text(stringResource(R.string.last_error, it), style = MaterialTheme.typography.bodySmall, color = HColors.Accent)
                }
                GameLauncher.lastLaunch?.let { report ->
                    Text(stringResource(R.string.last_launch), style = MaterialTheme.typography.labelSmall, color = HColors.TextMuted, modifier = Modifier.padding(top = 8.dp))
                    Text(report, style = MaterialTheme.typography.bodySmall, color = HColors.TextSoft)
                    PillButton(stringResource(R.string.copy_report), primary = false, onClick = {
                        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Huginn", report))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
        item {
            PillButton(stringResource(R.string.refresh), primary = false, icon = Icons.Filled.Refresh, onClick = { version++ })
        }
    }
}

@Composable
private fun ScreenCard(s: ScreenInfo, isHere: Boolean) {
    val tThis = stringResource(R.string.screen_this)
    val tMain = stringResource(R.string.screen_main)
    val tOff = stringResource(R.string.screen_off)
    val tPrivate = stringResource(R.string.screen_private)
    val tPresentation = stringResource(R.string.screen_presentation)
    val tags = listOfNotNull(
        tThis.takeIf { isHere },
        tMain.takeIf { s.isMain },
        tOff.takeIf { !s.isOn },
        tPrivate.takeIf { s.isPrivate },
        tPresentation.takeIf { s.isPresentation },
    )
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HColors.Surface)
            .border(if (isHere) 2.dp else 1.dp, if (isHere) HColors.Accent else HColors.Stroke, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            "#${s.id} · ${s.name}" + if (tags.isNotEmpty()) " · " + tags.joinToString(", ") else "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.screen_line, s.width, s.height, s.dpi, s.hz),
            style = MaterialTheme.typography.bodySmall,
            color = HColors.TextMuted,
        )
    }
}
