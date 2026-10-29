package io.github.christiandonatti.huginn

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Display
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/** What the Screens tab shows about each display. */
data class ScreenInfo(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val dpi: Int,
    val hz: Int,
    val isMain: Boolean,
    val isPrivate: Boolean,
    val isPresentation: Boolean,
    val isOn: Boolean,
)

/**
 * The two screens of the Thor. On the Thor the top screen is the default display (id 0) and the
 * bottom one is a second built-in display; on a phone there is only one.
 */
object DualScreen {
    /** When this process started, for the "open for" line in the Screens tab. */
    val startedAt: Long = SystemClock.elapsedRealtime()

    /** Last thing that went wrong when opening something on a screen, shown in the Screens tab. */
    var lastError by mutableStateOf<String?>(null)
        private set

    private fun manager(context: Context) =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    fun displays(context: Context): List<Display> = manager(context).displays.toList()

    /** The second built-in screen, ignoring private displays (screen recorders and the like). */
    fun bottomDisplay(context: Context): Display? = displays(context).firstOrNull {
        it.displayId != Display.DEFAULT_DISPLAY && (it.flags and Display.FLAG_PRIVATE) == 0
    }

    fun currentDisplayId(activity: Activity): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display?.displayId ?: Display.DEFAULT_DISPLAY
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.displayId
        }

    /** The screen that is not the one this activity is on (or the same one on a single screen). */
    fun otherDisplayId(activity: Activity): Int {
        val here = currentDisplayId(activity)
        return if (here == Display.DEFAULT_DISPLAY) bottomDisplay(activity)?.displayId ?: here
        else Display.DEFAULT_DISPLAY
    }

    private fun startOn(context: Context, intent: Intent, displayId: Int): Boolean = try {
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options.toBundle())
        true
    } catch (e: Exception) {
        lastError = "${e.javaClass.simpleName}: ${e.message}"
        false
    }

    /**
     * Puts the Huginn panel on the bottom screen. Without [force] it does nothing when the panel
     * already exists there, so an app the user opened on the bottom screen is not covered.
     */
    fun showBottom(activity: Activity, force: Boolean) {
        val bottom = bottomDisplay(activity)
        if (bottom == null) {
            if (force) startOn(activity, Intent(activity, BottomActivity::class.java), currentDisplayId(activity))
            return
        }
        if (!force && BottomActivity.livesOn == bottom.displayId) return
        startOn(activity, Intent(activity, BottomActivity::class.java), bottom.displayId)
    }

    /** Brings the Huginn home back on the top screen. */
    fun showTop(activity: Activity) {
        startOn(activity, Intent(activity, MainActivity::class.java), Display.DEFAULT_DISPLAY)
    }

    /** Opens an app on the given screen; false (and [lastError]) if Android refused. */
    fun launch(context: Context, app: AppEntry, displayId: Int): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return startOn(context, intent, displayId)
    }

    /** Opens a system screen (settings, browser) on the given screen. */
    fun open(context: Context, intent: Intent, displayId: Int): Boolean = startOn(context, intent, displayId)

    fun describe(context: Context): List<ScreenInfo> = displays(context).map { d ->
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        d.getRealMetrics(metrics)
        ScreenInfo(
            id = d.displayId,
            name = d.name ?: "",
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            dpi = metrics.densityDpi,
            hz = d.refreshRate.roundToInt(),
            isMain = d.displayId == Display.DEFAULT_DISPLAY,
            isPrivate = (d.flags and Display.FLAG_PRIVATE) != 0,
            isPresentation = (d.flags and Display.FLAG_PRESENTATION) != 0,
            isOn = d.state == Display.STATE_ON,
        )
    }
}
