package io.github.christiandonatti.huginn

import android.content.Intent
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
        Prefs.init(this)
        // A home screen has nowhere to go back to (the game page handles B itself).
        onBackPressedDispatcher.addCallback(this) { }
        AppRepository.refresh(this)
        Library.refresh(this)
        setContent {
            HuginnTheme {
                TopScreen(onOpenBottom = { DualScreen.showBottom(this, force = true) })
            }
        }
        if (savedInstanceState == null) window.decorView.post { ensureBottom(intent) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        window.decorView.post { ensureBottom(intent) }
    }

    /**
     * Puts the panel on the bottom screen when it is missing. Opened from the app list, the panel
     * also comes back when it exists but another app covers it; pressing Home leaves an app that
     * is open on the bottom screen alone.
     */
    private fun ensureBottom(intent: Intent?) {
        val fromAppList = intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true
        DualScreen.showBottom(this, force = fromAppList && !BottomActivity.visible)
    }

    override fun onResume() {
        super.onResume()
        AppRepository.refresh(this)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = super.dispatchKeyEvent(Gamepad.translate(event))
}
