package com.retrohub.tv.ui

sealed interface Screen {
    val key: String

    data object Home : Screen { override val key = "home" }
    data class Library(val systemId: Long?, val favoritesOnly: Boolean = false) : Screen {
        override val key = "library"
    }
    data class Game(val gameId: Long) : Screen { override val key = "game-$gameId" }
    data object Search : Screen { override val key = "search" }
    data object Systems : Screen { override val key = "systems" }
    /** [systemId] null = new system pre-filled from preset [presetIndex]. */
    data class EditSystem(val systemId: Long?, val presetIndex: Int = 0) : Screen {
        override val key = "edit-$systemId-$presetIndex"
    }
    data object Settings : Screen { override val key = "settings" }
    /** Automatic discovery: RetroArch playlists + storage scan, then review and add. */
    data object Discover : Screen { override val key = "discover" }
    /** "Who's playing?" picker; [manage] = rename / recolour / delete mode. */
    data class Profiles(val manage: Boolean = false) : Screen { override val key = "profiles-$manage" }
}
