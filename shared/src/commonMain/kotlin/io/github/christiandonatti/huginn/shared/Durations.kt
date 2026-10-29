package io.github.christiandonatti.huginn.shared

import kotlin.math.roundToInt

/** "40 min", "1 h 35", "2 h". Used for play time, battery estimates and the "how much time" picker. */
fun formatMinutes(totalMinutes: Int): String {
    val m = totalMinutes.coerceAtLeast(0)
    if (m < 60) return "$m min"
    val hours = m / 60
    val rest = m % 60
    return if (rest == 0) "$hours h" else "$hours h ${rest.toString().padStart(2, '0')}"
}

/**
 * How long the battery lasts in a game, in minutes, rounded to 5. [drainPerHour] is how many
 * percent the game uses per hour (measured during play; estimated per system at first).
 */
fun batteryMinutes(levelPercent: Int, drainPerHour: Double): Int {
    if (drainPerHour <= 0.0) return 0
    val minutes = levelPercent.coerceIn(0, 100) / drainPerHour * 60.0
    return ((minutes / 5.0).roundToInt() * 5)
}
