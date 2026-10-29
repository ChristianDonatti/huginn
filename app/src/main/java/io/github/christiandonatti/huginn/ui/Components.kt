package io.github.christiandonatti.huginn.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.christiandonatti.huginn.ui.theme.HColors
import kotlinx.coroutines.delay
import java.util.Date

fun Context.findActivity(): Activity {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    error("No activity")
}

/** Border that turns gold when the controller is on this element. */
fun Modifier.focusRing(focused: Boolean, shape: Shape, idle: androidx.compose.ui.graphics.Color = HColors.Stroke): Modifier =
    if (focused) this.border(2.dp, HColors.Accent, shape) else this.border(1.dp, idle, shape)

/** The current time in the device's 12 or 24 hour format, updated every few seconds. */
@Composable
fun rememberClock(): String {
    val context = LocalContext.current
    val format = remember { DateFormat.getTimeFormat(context) }
    var now by remember { mutableStateOf(format.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            now = format.format(Date())
            delay(10_000)
        }
    }
    return now
}

@Composable
fun PillButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusable: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        modifier
            .clip(shape)
            .background(if (primary) HColors.Text else HColors.Surface)
            .focusRing(focused, shape, if (primary) HColors.Text else HColors.Stroke)
            .then(if (focusable) Modifier else Modifier.focusProperties { canFocus = false })
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val ink = if (primary) HColors.Ink else HColors.Text
        if (icon != null) Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Text(text, color = ink, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun SmallChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(HColors.Surface)
            .border(1.dp, HColors.Stroke, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = HColors.TextSoft)
    }
}

/** Segmented control: a row of pills where one is selected. */
@Composable
fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val outer = RoundedCornerShape(50)
    Row(
        modifier
            .clip(outer)
            .background(HColors.Surface)
            .border(1.dp, HColors.Stroke, outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val source = remember { MutableInteractionSource() }
            val focused by source.collectIsFocusedAsState()
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (on) HColors.Text else androidx.compose.ui.graphics.Color.Transparent)
                    .then(if (focused) Modifier.border(2.dp, HColors.Accent, shape) else Modifier)
                    .clickable(interactionSource = source, indication = LocalIndication.current) { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) HColors.Ink else HColors.TextSoft, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Big touch target for the bottom screen: icon, title and one line of explanation. */
@Composable
fun ActionTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HColors.Surface)
            .focusRing(focused, shape)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = HColors.Accent, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HColors.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
