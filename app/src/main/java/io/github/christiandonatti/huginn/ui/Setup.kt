package io.github.christiandonatti.huginn.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.christiandonatti.huginn.DualScreen
import io.github.christiandonatti.huginn.HuginnActivity
import io.github.christiandonatti.huginn.Library
import io.github.christiandonatti.huginn.Prefs
import io.github.christiandonatti.huginn.R
import io.github.christiandonatti.huginn.StorageAccess
import io.github.christiandonatti.huginn.shared.pathFromDocumentId
import io.github.christiandonatti.huginn.ui.theme.HColors

/** Whether Huginn may read the ROM folders, checked again whenever the screen comes back. */
@Composable
fun rememberStorageAccess(): Boolean {
    val context = LocalContext.current
    var access by remember { mutableStateOf(StorageAccess.hasAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = StorageAccess.hasAccess(context)
        if (now && !access) Library.refresh(context)
        access = now
    }
    return access
}

/** Opens Android's "All files access" page for Huginn (or the old permission prompt). */
fun requestStorageAccess(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val here = DualScreen.currentDisplayId(activity)
        val specific = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${activity.packageName}"))
        if (!DualScreen.open(activity, specific, here)) {
            DualScreen.open(activity, Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), here)
        }
    } else {
        activity.requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
    }
}

/**
 * Android's folder picker; the chosen folder becomes a ROM folder, is scanned, and stays
 * released to Huginn so emulators can be handed its games.
 */
@Composable
fun rememberFolderPicker(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            val activity = runCatching { context.findActivity() }.getOrNull() as? HuginnActivity
            activity?.pickFolder(null) { path ->
                if (path != null) {
                    Prefs.addRoot(path)
                    Prefs.markFolderAsked(path)
                    Library.refresh(context)
                }
            }
            Unit
        }
    }
}

fun autoDetectFolders(context: android.content.Context) {
    Library.autoDetect(context) { added ->
        val text = if (added > 0) context.resources.getQuantityString(R.plurals.folders_found, added, added)
        else context.getString(R.string.folders_none_found)
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}

/**
 * What to do when the library is empty: allow file access, choose the ROM folder, or rescan.
 * Shown in the middle of the top screen and at the top of the Consoles tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibrarySetup(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val access = rememberStorageAccess()
    val pick = rememberFolderPicker()
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .widthIn(max = 560.dp)
            .clip(shape)
            .background(HColors.Surface)
            .border(1.dp, HColors.Stroke, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val (title, text) = when {
            !access -> stringResource(R.string.setup_access_title) to stringResource(R.string.setup_access_text)
            Prefs.romRoots.isEmpty() -> stringResource(R.string.setup_folder_title) to stringResource(R.string.setup_folder_text)
            Library.scanning -> stringResource(R.string.setup_scanning_title) to (Library.scanningWhat ?: "")
            else -> stringResource(R.string.setup_empty_title) to
                stringResource(R.string.setup_empty_text, Prefs.romRoots.joinToString("\n"))
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.bodyMedium, color = HColors.TextSoft)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                !access -> PillButton(stringResource(R.string.allow_access), primary = true, onClick = { requestStorageAccess(activity) })
                Library.scanning -> Unit
                Prefs.romRoots.isEmpty() -> {
                    PillButton(stringResource(R.string.choose_folder), primary = true, icon = Icons.Filled.Add, onClick = pick)
                    PillButton(stringResource(R.string.find_folders), primary = false, icon = Icons.Filled.Search, onClick = { autoDetectFolders(context) })
                }
                else -> {
                    PillButton(stringResource(R.string.rescan), primary = true, icon = Icons.Filled.Refresh, onClick = { Library.refresh(context) })
                    PillButton(stringResource(R.string.add_folder), primary = false, icon = Icons.Filled.Add, onClick = pick)
                }
            }
        }
    }
}
