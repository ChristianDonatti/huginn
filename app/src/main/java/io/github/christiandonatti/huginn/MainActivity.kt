package io.github.christiandonatti.huginn

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.christiandonatti.huginn.ui.TopScreen
import io.github.christiandonatti.huginn.ui.theme.HuginnTheme

/** The home screen on the Thor's top screen. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A home screen has nowhere to go back to.
        onBackPressedDispatcher.addCallback(this) { }
        AppRepository.refresh(this)
        setContent {
            HuginnTheme {
                TopScreen(onOpenBottom = { DualScreen.showBottom(this, force = true) })
            }
        }
        // First start (after boot or after choosing Huginn): make sure the bottom panel is there.
        if (savedInstanceState == null) {
            window.decorView.post { DualScreen.showBottom(this, force = false) }
        }
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refresh(this)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = super.dispatchKeyEvent(Gamepad.translate(event))
}
