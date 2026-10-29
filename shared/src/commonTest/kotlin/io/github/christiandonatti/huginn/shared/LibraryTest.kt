package io.github.christiandonatti.huginn.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryTest {
    @Test
    fun titlesLoseTagsAndExtension() {
        assertEquals("Super Mario World", cleanTitle("Super Mario World (USA) [!].sfc"))
        assertEquals("The Legend of Zelda - A Link to the Past", cleanTitle("Legend of Zelda, The - A Link to the Past (USA).sfc"))
        assertEquals("Pokemon Emerald", cleanTitle("Pokemon_Emerald.gba"))
        assertEquals("Final Fantasy VII", cleanTitle("Final Fantasy VII (USA) (Disc 1).chd"))
        assertEquals("Persona 5", cleanTitle("Persona 5.ps3"))
        assertEquals("(Hack)", cleanTitle("(Hack).gba"))
        assertEquals("USA", regionOf("Game (USA).gba"))
        assertEquals("Europe, Brazil", regionOf("Game (Europe, Brazil) (En,Fr).md"))
        assertEquals("Japan", regionOf("Game (J) [!].nes"))
        assertNull(regionOf("Game (Rev 1).gba"))
        assertEquals("legend of zelda", sortKey("The Legend of Zelda"))
    }

    @Test
    fun storagePaths() {
        assertEquals("primary:ROMs/gba/Game.gba", safDocumentId("/storage/emulated/0/ROMs/gba/Game.gba"))
        assertEquals("primary:ROMs/gba", safDocumentId("/sdcard//ROMs/gba/"))
        assertEquals("1A2B-3C4D:ROMs/psx/FF7.m3u", safDocumentId("/storage/1A2B-3C4D/ROMs/psx/FF7.m3u"))
        assertNull(safDocumentId("/data/local/tmp/x"))
        assertEquals("/storage/emulated/0/ROMs", pathFromDocumentId("primary:ROMs"))
        assertEquals("/storage/1A2B-3C4D", pathFromDocumentId("1A2B-3C4D:"))
        assertEquals(
            "content://com.android.externalstorage.documents/document/primary%3AROMs%2Fnds%2FPok%C3%A9mon%20Black.nds",
            safDocumentUri("/storage/emulated/0/ROMs/nds/Pokémon Black.nds"),
        )
    }

    @Test
    fun multiFileGames() {
        assertEquals(listOf("FF7 (Disc 1).chd", "FF7 (Disc 2).chd"), referencedFiles("FF7.m3u", "#EXTM3U\nFF7 (Disc 1).chd\r\nFF7 (Disc 2).chd\n"))
        assertEquals(
            listOf("Game (Track 1).bin", "Game (Track 2).bin"),
            referencedFiles("Game.cue", "FILE \"Game (Track 1).bin\" BINARY\n  TRACK 01 MODE2/2352\nFILE \"Game (Track 2).bin\" BINARY\n"),
        )
        assertEquals(listOf("track01.bin", "track 02.raw"), referencedFiles("x.gdi", "2\n1 0 4 2352 track01.bin 0\n2 600 0 2352 \"track 02.raw\" 0\n"))
    }

    @Test
    fun systemsAndDefaults() {
        assertEquals("n3ds", Systems.canonical("3DS"))
        assertEquals("gc", Systems.canonical("GameCube"))
        assertEquals("gba", Systems.canonical("GBA"))
        assertEquals("psx", Systems.canonical("PS1"))
        val labels = listOf("melonDS DS", "melonDS (Standalone)", "DraStic (Standalone)")
        assertEquals(1, Systems.defaultCommand("nds", labels, listOf(true, true, true)))
        assertEquals(0, Systems.defaultCommand("nds", labels, listOf(true, false, false)))
        assertEquals(0, Systems.defaultCommand("gba", listOf("mGBA", "My Boy! (Standalone)"), listOf(true, true)))
        assertNull(Systems.defaultCommand("gba", listOf("mGBA"), listOf(false)))
        assertEquals("RetroArch · mGBA", Systems.emulatorName("mGBA", "RETROARCH"))
        assertEquals("melonDS", Systems.emulatorName("melonDS (Standalone)", "MELONDS"))
    }
}
