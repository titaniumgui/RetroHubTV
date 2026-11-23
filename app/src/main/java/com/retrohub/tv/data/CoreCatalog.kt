package com.retrohub.tv.data

/** A RetroArch core: file id (before _libretro_android.so) and its name in RetroArch's Core Downloader. */
data class CoreOption(val id: String, val name: String)

/**
 * Compatible cores per system (keyed by libretro-database name), recommended one first.
 * Picks favour cores that are accurate yet light enough for 1–2 GB Android TV boxes.
 *
 * RetroArch keeps installed cores in its private folder, which other apps can't list. So the
 * recommendation is used automatically, unless RetroArch's own playlists show which core it
 * already uses for a system (that wins). Every system can switch to another core from a list.
 */
object CoreCatalog {
    private val NES = listOf(
        CoreOption("fceumm", "Nintendo - NES / Famicom (FCEUmm)"),
        CoreOption("nestopia", "Nintendo - NES / Famicom (Nestopia)"),
        CoreOption("mesen", "Nintendo - NES / Famicom (Mesen)"),
        CoreOption("quicknes", "Nintendo - NES / Famicom (QuickNES)"),
    )
    private val SNES = listOf(
        CoreOption("snes9x", "Nintendo - SNES / SFC (Snes9x - Current)"),
        CoreOption("snes9x2010", "Nintendo - SNES / SFC (Snes9x 2010)"),
        CoreOption("bsnes", "Nintendo - SNES / SFC (bsnes)"),
    )
    private val N64 = listOf(
        CoreOption("mupen64plus_next_gles3", "Nintendo - Nintendo 64 (Mupen64Plus-Next)"),
        CoreOption("parallel_n64", "Nintendo - Nintendo 64 (ParaLLEl N64)"),
    )
    private val GB = listOf(
        CoreOption("gambatte", "Nintendo - Game Boy / Color (Gambatte)"),
        CoreOption("sameboy", "Nintendo - Game Boy / Color (SameBoy)"),
        CoreOption("mgba", "Nintendo - Game Boy Advance (mGBA)"),
    )
    private val GBA = listOf(
        CoreOption("mgba", "Nintendo - Game Boy Advance (mGBA)"),
        CoreOption("gpsp", "Nintendo - Game Boy Advance (gpSP)"),
        CoreOption("vba_next", "Nintendo - Game Boy Advance (VBA Next)"),
    )
    private val NDS = listOf(
        CoreOption("melonds", "Nintendo - DS (melonDS)"),
        CoreOption("desmume", "Nintendo - DS (DeSmuME)"),
    )
    private val SEGA_8_16 = listOf(
        CoreOption("genesis_plus_gx", "Sega - MS/GG/MD/CD (Genesis Plus GX)"),
        CoreOption("picodrive", "Sega - MS/GG/MD/CD/32X (PicoDrive)"),
    )
    private val SATURN = listOf(
        CoreOption("yabasanshiro", "Sega - Saturn (YabaSanshiro)"),
        CoreOption("yabause", "Sega - Saturn (Yabause)"),
    )
    private val DREAMCAST = listOf(CoreOption("flycast", "Sega - Dreamcast/NAOMI (Flycast)"))
    private val PS1 = listOf(
        CoreOption("pcsx_rearmed", "Sony - PlayStation (PCSX ReARMed)"),
        CoreOption("swanstation", "Sony - PlayStation (SwanStation)"),
        CoreOption("mednafen_psx", "Sony - PlayStation (Beetle PSX)"),
    )
    private val PSP = listOf(CoreOption("ppsspp", "Sony - PlayStation Portable (PPSSPP)"))
    private val PCE = listOf(
        CoreOption("mednafen_pce_fast", "NEC - PC Engine / CD (Beetle PCE FAST)"),
        CoreOption("mednafen_pce", "NEC - PC Engine / SuperGrafx / CD (Beetle PCE)"),
    )
    private val A2600 = listOf(
        CoreOption("stella", "Atari - 2600 (Stella)"),
        CoreOption("stella2014", "Atari - 2600 (Stella 2014)"),
    )
    private val NGP = listOf(
        CoreOption("mednafen_ngp", "SNK - Neo Geo Pocket / Color (Beetle NeoPop)"),
        CoreOption("race", "SNK - Neo Geo Pocket / Color (RACE)"),
    )
    private val WS = listOf(CoreOption("mednafen_wswan", "Bandai - WonderSwan/Color (Beetle Cygne)"))
    private val FBNEO = listOf(CoreOption("fbneo", "Arcade (FinalBurn Neo)"))
    private val MAME2003P = listOf(CoreOption("mame2003_plus", "Arcade (MAME 2003-Plus)"))

    private val byDb: Map<String, List<CoreOption>> = mapOf(
        "Nintendo - Nintendo Entertainment System" to NES,
        "Nintendo - Super Nintendo Entertainment System" to SNES,
        "Nintendo - Nintendo 64" to N64,
        "Nintendo - Game Boy" to GB,
        "Nintendo - Game Boy Color" to GB,
        "Nintendo - Game Boy Advance" to GBA,
        "Nintendo - Nintendo DS" to NDS,
        "Sega - Master System - Mark III" to SEGA_8_16,
        "Sega - Mega Drive - Genesis" to SEGA_8_16,
        "Sega - Mega-CD - Sega CD" to SEGA_8_16,
        "Sega - Game Gear" to SEGA_8_16,
        "Sega - Saturn" to SATURN,
        "Sega - Dreamcast" to DREAMCAST,
        "Sony - PlayStation" to PS1,
        "Sony - PlayStation Portable" to PSP,
        "NEC - PC Engine - TurboGrafx 16" to PCE,
        "Atari - 2600" to A2600,
        "SNK - Neo Geo Pocket" to NGP,
        "SNK - Neo Geo Pocket Color" to NGP,
        "Bandai - WonderSwan" to WS,
        "Bandai - WonderSwan Color" to WS,
        "FBNeo - Arcade Games" to FBNEO,
        "MAME 2003-Plus" to MAME2003P,
    )

    fun optionsFor(metadataDb: String): List<CoreOption> = byDb[metadataDb].orEmpty()

    fun recommended(metadataDb: String): CoreOption? = optionsFor(metadataDb).firstOrNull()

    /** Display name for a core id, e.g. "genesis_plus_gx" -> "Sega - MS/GG/MD/CD (Genesis Plus GX)". */
    fun displayName(id: String): String? = byDb.values.asSequence().flatten().firstOrNull { it.id == id }?.name

    /**
     * Core to use for a system: the one RetroArch already uses for it (from its playlists) if known,
     * otherwise the recommendation. Returns null for systems RetroHub has no catalog entry for.
     */
    fun choose(metadataDb: String, retroArchCore: String?): String? =
        retroArchCore?.takeIf { it.isNotBlank() } ?: recommended(metadataDb)?.id
}
