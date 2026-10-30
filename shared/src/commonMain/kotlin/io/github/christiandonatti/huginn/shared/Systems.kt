package io.github.christiandonatti.huginn.shared

/**
 * What Huginn adds on top of ES-DE's system list: other common names for ROM folders, short
 * names for the interface, a colour for generated covers, and which systems run better in a
 * standalone emulator than in RetroArch.
 */
object Systems {
    /** Huginn's own system for Android games (apps marked as games). */
    const val ANDROID = "android"

    private val aliases = mapOf(
        "3ds" to "n3ds", "nintendo3ds" to "n3ds",
        "ds" to "nds", "nintendods" to "nds",
        "gamecube" to "gc", "ngc" to "gc",
        "ps1" to "psx", "psone" to "psx", "playstation" to "psx", "playstation1" to "psx",
        "playstation2" to "ps2", "playstation3" to "ps3", "vita" to "psvita",
        "supernintendo" to "snes", "nintendo64" to "n64",
        "gameboy" to "gb", "gameboycolor" to "gbc", "gameboyadvance" to "gba",
        "sms" to "mastersystem", "gg" to "gamegear", "md" to "megadrive",
        "pce" to "pcengine", "tg16" to "tg16", "dc" to "dreamcast",
        "ns" to "switch", "nswitch" to "switch", "nintendoswitch" to "switch",
    )

    /** The ES-DE system name for a folder name, or the lower-case name itself. */
    fun canonical(folder: String): String {
        val key = folder.trim().lowercase().replace(" ", "").replace("-", "").replace("_", "")
        return aliases[key] ?: folder.trim().lowercase()
    }

    private val shortNames = mapOf(
        "nes" to "NES", "famicom" to "Famicom", "fds" to "Disk System", "snes" to "SNES", "sfc" to "Super Famicom",
        "n64" to "N64", "gb" to "Game Boy", "gbc" to "GB Color", "gba" to "GBA", "nds" to "DS", "n3ds" to "3DS",
        "gc" to "GameCube", "wii" to "Wii", "wiiu" to "Wii U", "switch" to "Switch", "virtualboy" to "Virtual Boy",
        "psx" to "PS1", "ps2" to "PS2", "ps3" to "PS3", "psp" to "PSP", "psvita" to "Vita",
        "megadrive" to "Mega Drive", "genesis" to "Genesis", "mastersystem" to "Master System", "gamegear" to "Game Gear",
        "segacd" to "Sega CD", "megacd" to "Mega CD", "sega32x" to "32X", "saturn" to "Saturn", "dreamcast" to "Dreamcast",
        "arcade" to "Arcade", "mame" to "MAME", "fbneo" to "FBNeo", "neogeo" to "Neo Geo", "neogeocd" to "Neo Geo CD",
        "ngp" to "NGP", "ngpc" to "NGP Color", "pcengine" to "PC Engine", "tg16" to "TurboGrafx", "atari2600" to "Atari 2600",
        "atari7800" to "Atari 7800", "atarilynx" to "Lynx", "wonderswan" to "WonderSwan", "wonderswancolor" to "WS Color",
        "dos" to "DOS", "scummvm" to "ScummVM", "ports" to "Ports", "steam" to "Steam", "windows" to "Windows",
        "androidgames" to "Android", "androidapps" to "Apps", ANDROID to "Android", "pico8" to "PICO-8",
    )

    fun shortName(system: String, fullName: String): String = shortNames[system] ?: fullName

    private val colors = mapOf(
        "nes" to 0xFFB3242BL, "famicom" to 0xFFB3242BL, "fds" to 0xFFC8a23aL, "snes" to 0xFF5B4BA0L, "sfc" to 0xFFB0393BL,
        "n64" to 0xFF2E7D4FL, "gb" to 0xFF7E8C3AL, "gbc" to 0xFF7A3FA0L, "gba" to 0xFF4B3FA8L, "nds" to 0xFF5E6E82L,
        "n3ds" to 0xFFC0392BL, "gc" to 0xFF5B3FA0L, "wii" to 0xFF2A9BC9L, "wiiu" to 0xFF1E88A8L, "switch" to 0xFFE4002BL,
        "psx" to 0xFF6F7884L, "ps2" to 0xFF1F3A8AL, "ps3" to 0xFF2B3440L, "psp" to 0xFF34495EL, "psvita" to 0xFF1F4FA8L,
        "megadrive" to 0xFF2C2C54L, "genesis" to 0xFF2C2C54L, "mastersystem" to 0xFFB22222L, "gamegear" to 0xFF3A3A5CL,
        "segacd" to 0xFF2E4A7AL, "sega32x" to 0xFF8E2C2CL, "saturn" to 0xFF4A5A73L, "dreamcast" to 0xFFE0662BL,
        "arcade" to 0xFFC9971CL, "mame" to 0xFFC9971CL, "fbneo" to 0xFFC9971CL, "neogeo" to 0xFFB8860BL,
        "pcengine" to 0xFFE07B39L, "tg16" to 0xFFE07B39L, "steam" to 0xFF1B2838L, "windows" to 0xFF0067B8L,
        "androidgames" to 0xFF2FA866L, ANDROID to 0xFF2FA866L, "ports" to 0xFF8A5A2BL, "dos" to 0xFF3F5F5FL,
    )

    private val fallback = listOf(0xFF2A7FB0L, 0xFF16314CL, 0xFF8A5A2BL, 0xFF5B4BA0L, 0xFF2E7D4FL, 0xFFB3242BL)

    /** ARGB colour for covers and tiles of a system. */
    fun color(system: String): Long =
        colors[system] ?: fallback[(system.hashCode() and Int.MAX_VALUE) % fallback.size]

    /**
     * Which command to use by default, among the installed ones: a standalone emulator the
     * person installed first (in ES-DE's order), and RetroArch only when no standalone one is
     * installed for that system. Someone who installed melonDS or My Boy! wants to use it; a
     * RetroArch core may not even be downloaded.
     */
    fun defaultCommand(system: String, labels: List<String>, installed: List<Boolean>): Int? {
        val candidates = labels.indices.filter { installed.getOrElse(it) { false } }
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { labels[it].contains("(Standalone)") } ?: candidates.first()
    }

    /** "RetroArch · mGBA" for RetroArch cores, "melonDS" for "melonDS (Standalone)". */
    fun emulatorName(label: String, emulator: String?): String =
        if (emulator == "RETROARCH") "RetroArch · $label" else label.replace(" (Standalone)", "")
}
