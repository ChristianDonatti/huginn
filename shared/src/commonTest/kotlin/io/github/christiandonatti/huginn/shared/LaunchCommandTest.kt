package io.github.christiandonatti.huginn.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LaunchCommandTest {
    @Test
    fun retroArchCore() {
        val spec = parseCommand(
            "%EMULATOR_RETROARCH% %EXTRA_CONFIGFILE%=%EXTERNALDATA%/Android/data/%ANDROIDPACKAGE%/files/retroarch.cfg " +
                "%EXTRA_LIBRETRO%=%INTERNALDATA%/%ANDROIDPACKAGE%/cores/mgba_libretro_android.so %EXTRA_ROM%=%ROM%",
        )!!
        assertEquals("RETROARCH", spec.emulator)
        assertEquals(listOf("CONFIGFILE", "LIBRETRO", "ROM"), spec.extras.map { it.key })
        val vars = mapOf(
            "EXTERNALDATA" to "/storage/emulated/0",
            "INTERNALDATA" to "/data/user/0",
            "ANDROIDPACKAGE" to "com.retroarch.aarch64",
            "ROM" to "/storage/emulated/0/ROMs/gba/Game.gba",
        )
        assertEquals(
            "/data/user/0/com.retroarch.aarch64/cores/mgba_libretro_android.so",
            expandValue(spec.extras[1].value, vars::get),
        )
        assertEquals("/storage/emulated/0/ROMs/gba/Game.gba", expandValue(spec.extras[2].value, vars::get))
    }

    @Test
    fun standaloneWithFlagsCategoryAndBool() {
        val dolphin = parseCommand(
            "%EMULATOR_DOLPHIN% %ACTION%=android.intent.action.MAIN %CATEGORY%=android.intent.category.LEANBACK_LAUNCHER %EXTRA_AutoStartFile%=%ROMSAF%",
        )!!
        assertEquals("android.intent.action.MAIN", dolphin.action)
        assertEquals(listOf("android.intent.category.LEANBACK_LAUNCHER"), dolphin.categories)

        val duck = parseCommand(
            "%EMULATOR_DUCKSTATION% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %EXTRABOOL_resumeState%=false %EXTRA_bootPath%=%ROMSAF%",
        )!!
        assertTrue(duck.clearTask && duck.clearTop)
        assertEquals(Extra("resumeState", "false", ExtraType.BOOL), duck.extras[0])

        val azahar = parseCommand("%EMULATOR_AZAHAR% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %DATA%=%ROMSAF%")!!
        assertEquals("%ROMSAF%", azahar.data)
    }

    @Test
    fun emulatorNamesWithDashesAndDotsInExtras() {
        val spec = parseCommand("%EMULATOR_MAME4DROID-CURRENT% %EXTRA_com.epsxe.ePSXe.isoName%=%ROM%")!!
        assertEquals("MAME4DROID-CURRENT", spec.emulator)
        assertEquals("com.epsxe.ePSXe.isoName", spec.extras.single().key)
    }

    @Test
    fun quotedValuesStayTogether() {
        val spec = parseCommand(
            "%EMULATOR_MAME4DROID-CURRENT% %ACTION%=android.intent.action.VIEW %EXTRA_cli_params%=\"-rompath '%GAMEDIRRAW%' -flop1 '%ROMRAW%'\" %DATA%=adam",
        )!!
        assertEquals("-rompath '%GAMEDIRRAW%' -flop1 '%ROMRAW%'", spec.extras.single().value)
        assertEquals("adam", spec.data)
    }

    @Test
    fun injectReadsFiles() {
        val spec = parseCommand(
            "%EMULATOR_GAMENATIVE% %ACTION%=app.gamenative.LAUNCH_GAME %EXTRA_game_source%=STEAM %EXTRAINTEGER_app_id%=%INJECT%=%ROM%",
        )!!
        val appId = spec.extras.first { it.type == ExtraType.INT }
        val value = expandValue(appId.value, { if (it == "ROM") "/x/Hades.steam" else null }) { path ->
            if (path == "/x/Hades.steam") "1145360\n" else null
        }
        assertEquals("1145360", value)

        val vita = parseCommand("%EMULATOR_VITA3K% %EXTRAARRAY_AppStartParameters%=-r,%INJECT%=%BASENAME%.psvita")!!
        val array = vita.extras.single()
        assertEquals(ExtraType.STRING_ARRAY, array.type)
        val expanded = expandValue(array.value, { if (it == "BASENAME") "Persona 4" else null }) { "PCSE00120" }
        assertEquals(listOf("-r", "PCSE00120"), splitArrayValue(expanded))
    }

    @Test
    fun androidGamesAndUnknownCommands() {
        assertEquals("%FILEINJECT%", parseCommand("%ANDROIDAPP%=%FILEINJECT%")!!.androidApp)
        assertNull(parseCommand("PLACEHOLDER %ROM%"))
        assertEquals(listOf("p,two", "x"), splitArrayValue("p\\,two,x"))
        assertEquals("me.magnum.melonds" to "me.magnum.melonds.ui.emulator.EmulatorActivity",
            splitComponent("me.magnum.melonds/.ui.emulator.EmulatorActivity"))
        assertEquals("app.gamenative" to null, splitComponent("app.gamenative"))
    }
}
