package io.github.christiandonatti.huginn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** An installed app that can be opened from the launcher. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    /** Main colour of the icon; it tints the card and the glow behind the carousel. */
    val color: Color,
)

/**
 * The apps installed on the device, shared by the top and bottom screens. Reloads only when the
 * set of apps changed, and keeps icons and colours of apps it already knows.
 */
object AppRepository {
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val cache = HashMap<String, AppEntry>()
    private var signature = ""

    fun refresh(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        scope.launch { load(app, force) }
    }

    private suspend fun load(context: Context, force: Boolean) = mutex.withLock {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val found = pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
        val newSignature = found.joinToString("|") { it.activityInfo.packageName + "/" + it.activityInfo.name }
        if (!force && newSignature == signature && apps.isNotEmpty()) return@withLock
        if (force) cache.clear()

        val list = found.mapNotNull { info ->
            val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            val key = component.flattenToString()
            cache[key] ?: runCatching { build(pm, info, component) }.getOrNull()?.also { cache[key] = it }
        }.sortedBy { it.label.lowercase() }

        withContext(Dispatchers.Main) {
            apps = list
            signature = newSignature
            loading = false
        }
    }

    private fun build(pm: PackageManager, info: ResolveInfo, component: ComponentName): AppEntry {
        val label = info.loadLabel(pm).toString()
        val bitmap = info.loadIcon(pm).toBitmap(144, 144)
        val palette = Palette.from(bitmap).generate()
        val rgb = (palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch)?.rgb
        return AppEntry(
            label = label,
            packageName = component.packageName,
            component = component,
            icon = bitmap.asImageBitmap(),
            color = if (rgb != null) Color(rgb) else Color(0xFF2A7FB0),
        )
    }
}
