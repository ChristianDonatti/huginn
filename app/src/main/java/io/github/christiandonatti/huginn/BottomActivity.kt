package io.github.christiandonatti.huginn

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.christiandonatti.huginn.ui.BottomScreen
import io.github.christiandonatti.huginn.ui.theme.HuginnTheme

/** The Huginn panel on the Thor's bottom screen. */
class BottomActivity : HuginnActivity() {
    companion object {
        /** Display where a bottom panel currently exists, so the top screen does not open a second one. */
        var livesOn: Int? = null
            private set

        /** Whether the panel is on screen (not covered by another app). */
        var visible = false
            private set

        private var current: java.lang.ref.WeakReference<BottomActivity>? = null

        /** Closes the panel (when Huginn itself is closed or left while not the default launcher). */
        fun close() {
            current?.get()?.let { if (!it.isFinishing) it.finishAndRemoveTask() }
            current = null
        }
    }

    private var myDisplay: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Prefs.init(this)
        onBackPressedDispatcher.addCallback(this) { }
        myDisplay = DualScreen.currentDisplayId(this)
        livesOn = myDisplay
        current = java.lang.ref.WeakReference(this)
        AppRepository.refresh(this)
        setContent {
            HuginnTheme { BottomScreen(this) }
        }
    }

    override fun onStart() {
        super.onStart()
        visible = true
    }

    override fun onStop() {
        visible = false
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        GameLauncher.endSession(myDisplay ?: -1)
        AppRepository.refresh(this)
    }

    override fun onDestroy() {
        if (livesOn == myDisplay) livesOn = null
        super.onDestroy()
    }

}
