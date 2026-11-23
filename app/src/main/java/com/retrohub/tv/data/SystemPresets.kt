package com.retrohub.tv.data

/**
 * Starting points for a new system: display name, common file extensions and the
 * libretro-database name used for metadata (each verified to exist in libretro-database/rdb).
 * Core, ROM folder and BIOS stay blank on purpose: RetroArch has several cores per platform and
 * every install differs, so the user chooses them.
 */
data class SystemPreset(
    val name: String,
    val extensions: String,
    val metadataDb: String = "",
    /** Folder names that identify this system during automatic discovery (lower case, no spaces). */
    val folderHints: List<String> = emptyList(),
)

object SystemPresets {
    val all = listOf(
        SystemPreset("Custom system", ""),
        SystemPreset("Nintendo Entertainment System", "nes,fds,unf,unif,zip,7z", "Nintendo - Nintendo Entertainment System", listOf("nes", "famicom", "fc", "entertainmentsystem")),
        SystemPreset("Super Nintendo", "sfc,smc,fig,swc,bs,zip,7z", "Nintendo - Super Nintendo Entertainment System", listOf("snes", "sfc", "superfamicom", "supernintendo", "superentertainmentsystem")),
        SystemPreset("Nintendo 64", "n64,v64,z64,zip,7z", "Nintendo - Nintendo 64", listOf("n64", "nintendo64")),
        SystemPreset("Game Boy", "gb,zip,7z", "Nintendo - Game Boy", listOf("gb", "gameboy")),
        SystemPreset("Game Boy Color", "gbc,zip,7z", "Nintendo - Game Boy Color", listOf("gbc", "gameboycolor")),
        SystemPreset("Game Boy Advance", "gba,zip,7z", "Nintendo - Game Boy Advance", listOf("gba", "gameboyadvance")),
        SystemPreset("Nintendo DS", "nds,zip,7z", "Nintendo - Nintendo DS", listOf("nds", "ds", "nintendods")),
        SystemPreset("Sega Master System", "sms,zip,7z", "Sega - Master System - Mark III", listOf("sms", "mastersystem")),
        SystemPreset("Sega Genesis / Mega Drive", "md,gen,smd,bin,zip,7z", "Sega - Mega Drive - Genesis", listOf("md", "genesis", "megadrive", "gen")),
        SystemPreset("Sega CD", "cue,bin,chd,m3u,iso", "Sega - Mega-CD - Sega CD", listOf("segacd", "megacd", "scd")),
        SystemPreset("Sega Saturn", "cue,bin,chd,m3u,iso", "Sega - Saturn", listOf("saturn", "ss")),
        SystemPreset("Sega Dreamcast", "gdi,cdi,chd,m3u", "Sega - Dreamcast", listOf("dreamcast", "dc")),
        SystemPreset("Game Gear", "gg,zip,7z", "Sega - Game Gear", listOf("gg", "gamegear")),
        SystemPreset("PlayStation", "cue,bin,chd,pbp,m3u,iso,img,ccd", "Sony - PlayStation", listOf("psx", "ps1", "playstation", "playstation1", "psone", "ps")),
        SystemPreset("PlayStation Portable", "iso,cso,pbp", "Sony - PlayStation Portable", listOf("psp")),
        SystemPreset("PC Engine / TurboGrafx-16", "pce,cue,bin,chd,zip,7z", "NEC - PC Engine - TurboGrafx 16", listOf("pce", "pcengine", "tg16", "turbografx", "turbografx16")),
        SystemPreset("Atari 2600", "a26,bin,zip,7z", "Atari - 2600", listOf("atari2600", "a2600", "2600", "atari", "vcs")),
        SystemPreset("Neo Geo Pocket", "ngp,zip,7z", "SNK - Neo Geo Pocket", listOf("ngp", "neogeopocket")),
        SystemPreset("Neo Geo Pocket Color", "ngc,zip,7z", "SNK - Neo Geo Pocket Color", listOf("ngpc", "ngc", "neogeopocketcolor")),
        SystemPreset("WonderSwan", "ws,zip,7z", "Bandai - WonderSwan", listOf("ws", "wonderswan")),
        SystemPreset("WonderSwan Color", "wsc,zip,7z", "Bandai - WonderSwan Color", listOf("wsc", "wonderswancolor")),
        SystemPreset("Arcade (FBNeo)", "zip,7z", "FBNeo - Arcade Games", listOf("fbneo", "fba", "arcade")),
        SystemPreset("Arcade (MAME 2003-Plus)", "zip,7z", "MAME 2003-Plus", listOf("mame", "mame2003", "mame2003plus")),
    )

    /** Database names offered in the system editor. */
    val metadataDatabases: List<String> = all.map { it.metadataDb }.filter { it.isNotEmpty() }.distinct()
}
