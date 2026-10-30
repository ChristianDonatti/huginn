package io.github.christiandonatti.huginn

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** What both screens share: the folder picker with a callback, and the controller's A button. */
abstract class HuginnActivity : ComponentActivity() {
    private var pendingPick: ((String?) -> Unit)? = null

    private val treePicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = uri?.let { FolderAccess.onPicked(this, it) }
        pendingPick?.invoke(path)
        pendingPick = null
    }

    /** Opens Android's folder picker (at [initialPath] when given); [onResult] gets the path or null. */
    fun pickFolder(initialPath: String?, onResult: (String?) -> Unit) {
        pendingPick = onResult
        DualScreen.markStart()
        runCatching { treePicker.launch(initialPath?.let { FolderAccess.initialUri(it) }) }
            .onFailure { pendingPick = null; onResult(null) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = super.dispatchKeyEvent(Gamepad.translate(event))
}
