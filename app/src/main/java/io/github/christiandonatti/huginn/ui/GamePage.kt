package io.github.christiandonatti.huginn.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.christiandonatti.huginn.AppRepository
import io.github.christiandonatti.huginn.CommandDef
import io.github.christiandonatti.huginn.Game
import io.github.christiandonatti.huginn.GameLauncher
import io.github.christiandonatti.huginn.Prefs
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.shared.titleTags
import io.github.christiandonatti.huginn.ui.theme.HColors

/**
 * The game page: big cover, what the file is, which emulator opens it (switchable for this
 * game only), favourite, and play on either screen. B goes back to the carousel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamePage(
    game: Game,
    icon: ImageBitmap?,
    hasBottom: Boolean,
    onPlay: () -> Unit,
    onPlayOther: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val choice = remember(game.id, AppRepository.version, Prefs.systemEmulator, Prefs.gameEmulator) {
        if (game.packageName != null) null else runCatching { GameLauncher.choice(context, game) }.getOrNull()
    }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(game.id) { runCatching { playFocus.requestFocus() } }
    val favorite = game.id in Prefs.favorites
    val system = systemFullName(context, game.system)

    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.ButtonY) { Prefs.toggleFavorite(game.id); true }
                else if (e.type == KeyEventType.KeyDown && e.key == Key.ButtonX && hasBottom) { onPlayOther(); true }
                else false
            },
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        GameCover(
            game = game,
            systemLabel = systemShortName(context, game.system),
            titleSize = 22.sp,
            icon = icon,
            corner = 24,
            modifier = Modifier.fillMaxHeight(0.92f).width(230.dp),
        )
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(system.uppercase(), style = MaterialTheme.typography.labelSmall, color = HColors.TextMuted)
            Text(game.title, style = MaterialTheme.typography.displaySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val tags = game.fileName?.let { titleTags(it) }.orEmpty()
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { SmallChip(it) }
                }
            }
            val facts = listOfNotNull(
                game.fileName,
                formatSize(context, game.size),
                Prefs.lastPlayed[game.id]?.let { stringResource(R.string.last_played, formatWhen(it)) },
            )
            Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = HColors.TextSoft)
            game.path?.substringBeforeLast('/')?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(2.dp))
            if (choice != null) EmulatorPicker(game, choice)
            else if (game.packageName != null) Text(stringResource(R.string.android_game), style = MaterialTheme.typography.bodyMedium, color = HColors.TextSoft)

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    stringResource(R.string.play),
                    primary = true,
                    icon = Icons.Filled.PlayArrow,
                    onClick = onPlay,
                    modifier = Modifier.focusRequester(playFocus),
                )
                if (hasBottom) PillButton(stringResource(R.string.play_other_screen), primary = false, onClick = onPlayOther)
                PillButton(
                    stringResource(if (favorite) R.string.favorite_on else R.string.favorite_off),
                    primary = false,
                    icon = Icons.Filled.Star,
                    onClick = { Prefs.toggleFavorite(game.id) },
                )
                PillButton(stringResource(R.string.back), primary = false, icon = Icons.Filled.ArrowBack, onClick = onClose)
            }
            Text(stringResource(R.string.hint_page), style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted)
        }
    }
}

/**
 * "Emulator: < RetroArch · mGBA >". Left and right (or a tap) switch between automatic and
 * every installed emulator; the choice applies to this game only.
 */
@Composable
private fun EmulatorPicker(game: Game, choice: io.github.christiandonatti.huginn.EmulatorChoice) {
    val context = LocalContext.current
    if (choice.installed.isEmpty()) {
        val names = choice.all.map { if (it.spec.emulator == "RETROARCH") "RetroArch" else it.displayName }.distinct().take(5)
        Text(
            stringResource(R.string.no_emulator_long, systemShortName(context, game.system), names.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium,
            color = HColors.Accent,
        )
        return
    }
    // Option 0 is "automatic" (the system choice or Huginn's default).
    val options: List<CommandDef?> = listOf(null) + choice.installed
    val current = if (choice.chosenForGame) choice.chosen else null
    val index = options.indexOf(current).coerceAtLeast(0)
    fun select(i: Int) {
        val next = options[i.mod(options.size)]
        Prefs.setGameEmulator(game.id, next?.label)
    }
    val automaticName = (Prefs.systemEmulator[game.system]?.let { l -> choice.installed.firstOrNull { it.label == l } } ?: choice.automatic)?.displayName ?: "?"
    val label = current?.displayName ?: stringResource(R.string.emulator_auto, automaticName)

    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.emulator_label).uppercase(), style = MaterialTheme.typography.labelSmall, color = HColors.TextMuted)
        Row(
            Modifier
                .clip(shape)
                .background(HColors.Surface)
                .focusRing(focused, shape)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionLeft -> { select(index - 1); true }
                        Key.DirectionRight -> { select(index + 1); true }
                        else -> false
                    }
                }
                .clickable(interactionSource = source, indication = LocalIndication.current) { select(index + 1) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = HColors.TextMuted)
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${index + 1}/${options.size}", style = MaterialTheme.typography.labelMedium, color = HColors.TextMuted)
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = HColors.TextMuted)
        }
    }
}
