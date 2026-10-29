package io.github.christiandonatti.huginn

import android.view.KeyEvent

/**
 * Makes the Thor's A button act like "select" everywhere, so every button and card in Compose
 * reacts to it the same way it reacts to Enter or the centre of a D-pad.
 */
object Gamepad {
    fun translate(event: KeyEvent): KeyEvent =
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            KeyEvent(
                event.downTime, event.eventTime, event.action, KeyEvent.KEYCODE_DPAD_CENTER,
                event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source,
            )
        } else event
}
