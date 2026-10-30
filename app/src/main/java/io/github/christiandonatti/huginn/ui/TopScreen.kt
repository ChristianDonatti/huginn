package io.github.christiandonatti.huginn.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.christiandonatti.huginn.AppEntry
import io.github.christiandonatti.huginn.AppRepository
import io.github.christiandonatti.huginn.DualScreen
import io.github.christiandonatti.huginn.Game
import io.github.christiandonatti.huginn.GameLauncher
import io.github.christiandonatti.huginn.Library
import io.github.christiandonatti.huginn.Prefs
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.rememberCover
import io.github.christiandonatti.huginn.ui.theme.HColors
import io.github.christiandonatti.huginn.ui.theme.Unbounded

/**
 * The home on the top screen: a row of filters (all, recent, favourites, each console, apps)
 * over a Fwip-style carousel, with the glow taking the colour of what is selected. Y opens the
 * game page; L1 and R1 switch filters.
 */
@Composable
fun TopScreen(onOpenBottom: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val games = rememberGames()
    val apps = AppRepository.apps
    val filters = remember(games, apps, Prefs.lastPlayed, Prefs.favorites) { filtersFor(context, games) }
    val filter = Home.filter.takeIf { key -> filters.any { it.key == key } } ?: Home.ALL
    val shown = remember(filter, games, Prefs.lastPlayed, Prefs.favorites) { gamesFor(filter, games) }
    val selectedIds = remember { mutableStateMapOf<String, String>() }
    var page by remember { mutableStateOf<Game?>(null) }
    val hasBottom = remember { DualScreen.bottomDisplay(context) != null }

    val gameIndex = shown.indexOfFirst { it.id == selectedIds[filter] }.coerceAtLeast(0)
    val currentGame = if (filter == Home.APPS) null else shown.getOrNull(gameIndex)
    val appIndex = apps.indexOfFirst { it.component.flattenToString() == selectedIds[Home.APPS] }.coerceAtLeast(0)
    val currentApp = if (filter == Home.APPS) apps.getOrNull(appIndex) else null
    val appIcons = remember(apps) { apps.associateBy { it.packageName } }

    val glowTarget = when {
        currentApp != null -> currentApp.color
        currentGame != null -> appIcons[currentGame.packageName]?.color ?: Color(io.github.christiandonatti.huginn.shared.Systems.color(currentGame.system))
        else -> HColors.Glow
    }
    val glow by animateColorAsState(glowTarget, tween(650), label = "glow")

    fun here() = DualScreen.currentDisplayId(activity)
    fun other() = DualScreen.otherDisplayId(activity)
    fun openApp(app: AppEntry, otherScreen: Boolean) {
        if (!DualScreen.launch(context, app, if (otherScreen) other() else here())) {
            Toast.makeText(context, context.getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        }
    }
    fun cycleFilter(step: Int) {
        val i = filters.indexOfFirst { it.key == filter }
        Home.filter = filters[(i + step).mod(filters.size)].key
    }

    BackHandler(enabled = page != null) { page = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(HColors.Ink)
            .drawBehind { glowLayer(glow) }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || page != null) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.ButtonL1 -> { cycleFilter(-1); true }
                    Key.ButtonR1 -> { cycleFilter(1); true }
                    Key.ButtonY -> { currentGame?.let { page = it }; currentGame != null }
                    Key.ButtonX -> when {
                        currentApp != null -> { openApp(currentApp, true); true }
                        currentGame != null -> { playGame(context, currentGame, other()); true }
                        else -> false
                    }
                    else -> false
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            TopHeader(onOpenBottom)
            Spacer(Modifier.height(6.dp))
            val open = page
            if (open != null) {
                GamePage(
                    game = open,
                    icon = appIcons[open.packageName]?.icon,
                    hasBottom = hasBottom,
                    onPlay = { playGame(context, open, here()) },
                    onPlayOther = { playGame(context, open, other()) },
                    onClose = { page = null },
                    modifier = Modifier.weight(1f),
                )
            } else {
                FilterRow(filters, filter, onSelect = { Home.filter = it })
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        filter == Home.APPS && currentApp != null -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            AppInfo(currentApp, appIndex, apps.size, hasBottom, { openApp(currentApp, false) }, { openApp(currentApp, true) }, Modifier.weight(0.38f))
                            Spacer(Modifier.width(16.dp))
                            AppCarousel(apps, appIndex, { selectedIds[Home.APPS] = apps[it].component.flattenToString() }, { openApp(it, false) }, Modifier.weight(0.62f))
                        }
                        filter == Home.APPS -> Text(
                            stringResource(if (AppRepository.loading) R.string.loading_apps else R.string.no_apps),
                            color = HColors.TextMuted,
                        )
                        currentGame != null -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            GameInfo(
                                game = currentGame,
                                index = gameIndex,
                                total = shown.size,
                                onPlay = { playGame(context, currentGame, here()) },
                                onDetails = { page = currentGame },
                                modifier = Modifier.weight(0.38f),
                            )
                            Spacer(Modifier.width(16.dp))
                            GameCarousel(
                                games = shown,
                                selected = gameIndex,
                                icons = appIcons,
                                onSelect = { selectedIds[filter] = shown[it].id },
                                onOpen = { playGame(context, it, here()) },
                                modifier = Modifier.weight(0.62f),
                            )
                        }
                        else -> LibrarySetup()
                    }
                }
            }
            StatusLine(hasBottom)
        }
    }
}

private fun DrawScope.glowLayer(glow: Color) {
    val r1 = size.maxDimension * 0.55f
    val c1 = Offset(size.width * 0.16f, size.height * 0.02f)
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent), center = c1, radius = r1), radius = r1, center = c1)
    val r2 = size.maxDimension * 0.38f
    val c2 = Offset(size.width * 0.96f, size.height * 1.05f)
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.28f), Color.Transparent), center = c2, radius = r2), radius = r2, center = c2)
}

@Composable
private fun TopHeader(onOpenBottom: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.huginn_mark), contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(6.dp))
        Text("huginn", fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = HColors.Text)
        Spacer(Modifier.width(12.dp))
        SmallChip(stringResource(R.string.phase_chip))
        Spacer(Modifier.weight(1f))
        Text(rememberClock(), style = MaterialTheme.typography.labelLarge, color = HColors.TextSoft)
        Spacer(Modifier.width(14.dp))
        PillButton(
            text = stringResource(R.string.panel_below),
            primary = false,
            icon = Icons.Filled.KeyboardArrowDown,
            onClick = onOpenBottom,
            focusable = false,
        )
    }
}

/** The status at the bottom: scanning progress, or a note when there is only one screen. */
@Composable
private fun StatusLine(hasBottom: Boolean) {
    val text = when {
        Library.scanning -> stringResource(R.string.scanning_line, Library.scanningWhat?.substringAfterLast('/') ?: "…")
        !hasBottom -> stringResource(R.string.single_screen)
        else -> null
    }
    if (text != null) Text(text, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted, maxLines = 1)
}

/** Filter chips; touch to pick, or L1/R1 with the controller (the chips do not take focus). */
@Composable
private fun FilterRow(filters: List<Filter>, selected: String, onSelect: (String) -> Unit) {
    val state = rememberLazyListState()
    val index = filters.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    LaunchedEffect(index, filters.size) { state.animateScrollToItem((index - 1).coerceAtLeast(0)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("L1", style = MaterialTheme.typography.labelSmall, color = HColors.TextMuted)
        Spacer(Modifier.width(8.dp))
        LazyRow(
            state = state,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(filters, key = { _, f -> f.key }) { _, f ->
                val on = f.key == selected
                val shape = RoundedCornerShape(50)
                Row(
                    Modifier
                        .clip(shape)
                        .background(if (on) HColors.Text else HColors.Surface)
                        .border(1.dp, if (on) HColors.Text else HColors.Stroke, shape)
                        .focusProperties { canFocus = false }
                        .clickable { onSelect(f.key) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(f.label, style = MaterialTheme.typography.labelLarge, color = if (on) HColors.Ink else HColors.TextSoft, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Text(f.count.toString(), style = MaterialTheme.typography.labelMedium, color = if (on) HColors.Ink.copy(alpha = 0.6f) else HColors.TextMuted)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("R1", style = MaterialTheme.typography.labelSmall, color = HColors.TextMuted)
    }
}

@Composable
private fun GameInfo(game: Game, index: Int, total: Int, onPlay: () -> Unit, onDetails: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val emulator = remember(game.id, AppRepository.version, Prefs.systemEmulator, Prefs.gameEmulator) {
        if (game.packageName != null) null else runCatching { GameLauncher.choice(context, game).current?.displayName }.getOrNull()
    }
    val meta = listOfNotNull(
        if (game.packageName != null) stringResource(R.string.android_game) else emulator ?: stringResource(R.string.no_emulator_short),
        game.region,
        formatSize(context, game.size),
    ).joinToString(" · ")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            (systemShortName(context, game.system) + " · " + stringResource(R.string.item_count, index + 1, total)).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = HColors.TextMuted,
        )
        Text(game.title, style = MaterialTheme.typography.displaySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(meta, style = MaterialTheme.typography.bodySmall, color = if (emulator == null && game.packageName == null) HColors.Accent else HColors.TextMuted, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(stringResource(R.string.play), primary = true, icon = Icons.Filled.PlayArrow, onClick = onPlay, focusable = false)
            PillButton(stringResource(R.string.details), primary = false, icon = Icons.Filled.Info, onClick = onDetails, focusable = false)
        }
        Text(stringResource(R.string.hint_games), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
    }
}

@Composable
private fun GameCarousel(
    games: List<Game>,
    selected: Int,
    icons: Map<String, AppEntry>,
    onSelect: (Int) -> Unit,
    onOpen: (Game) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val requesters = remember(games) { List(games.size) { FocusRequester() } }
    val labels = remember(games) { games.map { it.system }.distinct().associateWith { systemShortName(context, it) } }

    LaunchedEffect(games) {
        if (games.isEmpty()) return@LaunchedEffect
        val target = selected.coerceIn(0, games.lastIndex)
        state.scrollToItem(target)
        withFrameNanos { }
        runCatching { requesters[target].requestFocus() }
    }

    LazyRow(
        state = state,
        modifier = modifier.height(250.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        itemsIndexed(games, key = { _, g -> g.id }) { i, game ->
            val isSelected = i == selected
            val width by animateDpAsState(if (isSelected) 150.dp else 112.dp, tween(260), label = "w")
            val shape = RoundedCornerShape(20.dp)
            GameCover(
                game = game,
                systemLabel = labels[game.system] ?: game.system,
                titleSize = if (isSelected) 15.sp else 12.sp,
                icon = icons[game.packageName]?.icon,
                cover = rememberCover(game, 360),
                modifier = Modifier
                    .width(width)
                    .height(width * 4 / 3)
                    .focusRequester(requesters[i])
                    .onFocusChanged { if (it.isFocused) onSelect(i) }
                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) HColors.Text else HColors.Stroke, shape)
                    .clip(shape)
                    .clickable {
                        if (isSelected) onOpen(game) else {
                            onSelect(i)
                            runCatching { requesters[i].requestFocus() }
                        }
                    },
            )
        }
    }
}

@Composable
private fun AppInfo(
    app: AppEntry,
    index: Int,
    total: Int,
    hasBottom: Boolean,
    onOpen: () -> Unit,
    onOpenOther: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.app_count, index + 1, total).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = HColors.TextMuted,
        )
        Text(app.label, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(stringResource(R.string.open), primary = true, icon = Icons.Filled.PlayArrow, onClick = onOpen, focusable = false)
            if (hasBottom) {
                PillButton(stringResource(R.string.open_other_screen), primary = false, onClick = onOpenOther, focusable = false)
            }
        }
        Text(stringResource(R.string.hint_keys), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
    }
}

@Composable
private fun AppCarousel(
    apps: List<AppEntry>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpen: (AppEntry) -> Unit,
    modifier: Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val requesters = remember(apps) { List(apps.size) { FocusRequester() } }

    LaunchedEffect(apps) {
        if (apps.isEmpty()) return@LaunchedEffect
        val target = selected.coerceIn(0, apps.lastIndex)
        state.scrollToItem(target)
        withFrameNanos { }
        runCatching { requesters[target].requestFocus() }
    }

    LazyRow(
        state = state,
        modifier = modifier.height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.component.flattenToString() }) { i, app ->
            AppCard(
                app = app,
                selected = i == selected,
                requester = requesters[i],
                onFocused = { onSelect(i) },
                onClick = {
                    if (i == selected) onOpen(app)
                    else {
                        onSelect(i)
                        runCatching { requesters[i].requestFocus() }
                    }
                },
            )
        }
    }
}

@Composable
private fun AppCard(
    app: AppEntry,
    selected: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val size by animateDpAsState(if (selected) 132.dp else 104.dp, tween(260), label = "card")
    val shape = RoundedCornerShape(26.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
        Box(
            Modifier
                .size(size)
                .focusRequester(requester)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .clip(shape)
                .background(Brush.verticalGradient(listOf(app.color.copy(alpha = 0.92f), lerp(app.color, Color.Black, 0.6f))))
                .border(if (selected) 3.dp else 1.dp, if (selected) HColors.Text else HColors.Stroke, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(size * 0.56f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) HColors.Text else HColors.TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
