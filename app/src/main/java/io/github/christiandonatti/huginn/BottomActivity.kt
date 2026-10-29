package io.github.christiandonatti.huginn

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.christiandonatti.huginn.ui.BottomScreen
import io.github.christiandonatti.huginn.ui.theme.HuginnTheme

/** The Huginn panel on the Thor's bottom screen. */
class BottomActivity : ComponentActivity() {
    companion object {
        /** Display where a bottom panel currently exists, so the top screen does not open a second one. */
        var livesOn: Int? = null
            private set
    }

    private var myDisplay: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { }
        myDisplay = DualScreen.currentDisplayId(this)
        livesOn = myDisplay
        AppRepository.refresh(this)
        setContent {
            HuginnTheme { BottomScreen(this) }
        }
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refresh(this)
    }

    override fun onDestroy() {
        if (livesOn == myDisplay) livesOn = null
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = super.dispatchKeyEvent(Gamepad.translate(event))
}
