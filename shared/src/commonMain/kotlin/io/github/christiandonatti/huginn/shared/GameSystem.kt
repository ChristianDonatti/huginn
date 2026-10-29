package io.github.christiandonatti.huginn.shared

/**
 * Consoles Huginn knows about. [folder] uses the same names as ES-DE, so a ROM folder already
 * organised for ES-DE, RetroBat or NeoStation is recognised as it is.
 */
enum class GameSystem(
    val folder: String,
    val shortName: String,
    val fullName: String,
    val extensions: List<String>,
) {
    NES("nes", "NES", "Nintendo Entertainment System", listOf("nes", "zip", "7z")),
    SNES("snes", "SNES", "Super Nintendo", listOf("sfc", "smc", "zip", "7z")),
    N64("n64", "N64", "Nintendo 64", listOf("n64", "z64", "v64", "zip", "7z")),
    GB("gb", "GB", "Game Boy", listOf("gb", "zip", "7z")),
    GBC("gbc", "GBC", "Game Boy Color", listOf("gbc", "zip", "7z")),
    GBA("gba", "GBA", "Game Boy Advance", listOf("gba", "zip", "7z")),
    NDS("nds", "DS", "Nintendo DS", listOf("nds", "zip", "7z")),
    N3DS("n3ds", "3DS", "Nintendo 3DS", listOf("3ds", "cci", "cia", "cxi", "3dsx")),
    GC("gc", "GameCube", "Nintendo GameCube", listOf("iso", "rvz", "gcz", "ciso")),
    WII("wii", "Wii", "Nintendo Wii", listOf("iso", "rvz", "wbfs", "gcz")),
    SWITCH("switch", "Switch", "Nintendo Switch", listOf("nsp", "xci")),
    PSX("psx", "PS1", "PlayStation", listOf("chd", "cue", "pbp", "m3u", "iso")),
    PS2("ps2", "PS2", "PlayStation 2", listOf("chd", "iso", "cso")),
    PSP("psp", "PSP", "PlayStation Portable", listOf("iso", "cso", "pbp", "chd")),
    MEGADRIVE("megadrive", "Mega Drive", "Sega Mega Drive", listOf("md", "bin", "gen", "smd", "zip", "7z")),
    DREAMCAST("dreamcast", "Dreamcast", "Sega Dreamcast", listOf("chd", "gdi", "cdi"));

    companion object {
        private val aliases = mapOf(
            "genesis" to MEGADRIVE,
            "md" to MEGADRIVE,
            "3ds" to N3DS,
            "ds" to NDS,
            "gamecube" to GC,
            "ngc" to GC,
            "ps1" to PSX,
            "psone" to PSX,
            "playstation" to PSX,
        )

        /** The system for a ROM folder name, ignoring case; null when the folder is not a console. */
        fun fromFolder(name: String): GameSystem? {
            val key = name.trim().lowercase()
            return entries.firstOrNull { it.folder == key } ?: aliases[key]
        }
    }
}
