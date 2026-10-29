package io.github.christiandonatti.huginn.ui

import android.widget.Toast
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
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
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.ui.theme.HColors
import io.github.christiandonatti.huginn.ui.theme.Unbounded

/**
 * Phase 0 home on the top screen: the installed apps in a Fwip-style carousel, with the glow
 * behind taking the colour of the selected icon. Games replace the apps in phase 1.
 */
@Composable
fun TopScreen(onOpenBottom: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val apps = AppRepository.apps
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val index = if (apps.isEmpty()) 0 else selected.coerceIn(0, apps.lastIndex)
    val current = apps.getOrNull(index)
    val glow by animateColorAsState(current?.color ?: HColors.Glow, tween(650), label = "glow")
    val hasBottom = remember { DualScreen.bottomDisplay(context) != null }

    fun open(app: AppEntry, otherScreen: Boolean) {
        val target = if (otherScreen) DualScreen.otherDisplayId(activity) else DualScreen.currentDisplayId(activity)
        if (!DualScreen.launch(context, app, target)) {
            Toast.makeText(context, context.getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(HColors.Ink)
            .drawBehind { glowLayer(glow) }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.ButtonX && current != null) {
                    open(current, otherScreen = true)
                    true
                } else false
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            TopHeader(onOpenBottom)
            Spacer(Modifier.height(8.dp))
            if (current == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (AppRepository.loading) R.string.loading_apps else R.string.no_apps),
                        color = HColors.TextMuted,
                    )
                }
            } else {
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppInfo(
                        app = current,
                        index = index,
                        total = apps.size,
                        hasBottom = hasBottom,
                        onOpen = { open(current, false) },
                        onOpenOther = { open(current, true) },
                        modifier = Modifier.weight(0.38f),
                    )
                    Spacer(Modifier.width(16.dp))
                    AppCarousel(
                        apps = apps,
                        selected = index,
                        onSelect = { selected = it },
                        onOpen = { open(it, false) },
                        modifier = Modifier.weight(0.62f),
                    )
                }
            }
            if (!hasBottom) {
                Text(stringResource(R.string.single_screen), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
            }
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

    // Put the controller on the selected card when the list (re)appears.
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
