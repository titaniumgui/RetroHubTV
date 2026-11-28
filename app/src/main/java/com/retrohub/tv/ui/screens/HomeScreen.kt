package com.retrohub.tv.ui.screens

import com.retrohub.tv.ui.components.startScrim
import com.retrohub.tv.i18n.tr
import com.retrohub.tv.i18n.relativeTime
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.retrohub.tv.data.GameTile
import com.retrohub.tv.data.SystemWithCount
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.components.SectionTitle
import com.retrohub.tv.ui.components.WideGameTile
import com.retrohub.tv.ui.components.gamesLabel
import com.retrohub.tv.ui.components.imageModel
import com.retrohub.tv.ui.theme.LocalLite
import com.retrohub.tv.ui.theme.RH
import kotlinx.coroutines.delay

/** Which shelf the hero game came from; drives the small label above the title. */
private data class HeroGame(val tile: GameTile, val label: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val recentOrNull by vm.recent.collectAsStateWithLifecycle()
    val favoritesOrNull by vm.favorites.collectAsStateWithLifecycle()
    val systemsOrNull by vm.systems.collectAsStateWithLifecycle()
    val profile by vm.currentProfile.collectAsStateWithLifecycle()
    val ready = recentOrNull != null && favoritesOrNull != null && systemsOrNull != null
    val recent = recentOrNull.orEmpty()
    val favorites = favoritesOrNull.orEmpty()
    val systems = systemsOrNull.orEmpty()
    val names by vm.systemNames.collectAsStateWithLifecycle()
    val total by vm.gameCount.collectAsStateWithLifecycle()

    // The hero shows the last focused game; before anything is focused, the most recent one.
    var focusedHero by remember { mutableStateOf<HeroGame?>(null) }
    val featured = recent.firstOrNull()?.let { HeroGame(it, tr("CONTINUE PLAYING")) }
        ?: favorites.firstOrNull()?.let { HeroGame(it, tr("FAVOURITE")) }
    val hero = focusedHero ?: featured

    val keys = buildSet {
        listOf("tab:home", "tab:games", "tab:search", "top:settings", "top:profile", "hero:play", "hero:info", "sys:add", "sys:all", "onboard:find").forEach(::add)
        recent.forEach { add("recent:${it.id}") }
        favorites.forEach { add("fav:${it.id}") }
        systems.forEach { add("sys:${it.system.id}") }
    }
    val default = recent.firstOrNull()?.let { "recent:${it.id}" }
        ?: favorites.firstOrNull()?.let { "fav:${it.id}" }
        ?: systems.firstOrNull()?.let { "sys:${it.system.id}" }
        ?: "onboard:find"
    val focus = rememberFocusRestore(vm, "home", default, keys, ready)

    // Console-style vertical scrolling: the focused shelf snaps under the hero with its title visible.
    val rowSpec = LocalBringIntoViewSpec.current
    val titleOffsetPx = with(LocalDensity.current) { ShelfTitleHeight.toPx() }
    val pinnedSpec = remember(titleOffsetPx) { PinToTopSpec(titleOffsetPx) }

    Box(Modifier.fillMaxSize()) {
        HeroArt(hero?.tile)
        Column(Modifier.fillMaxSize()) {
            TopBar(vm, Tab.HOME, focus)
            Hero(vm, hero, hero?.tile?.let { names[it.systemId] }, profile?.name, total, focus)
            if (ready) CompositionLocalProvider(LocalBringIntoViewSpec provides pinnedSpec) {
                LazyColumn(
                    // Room below the last shelf so it can also be pinned under the hero.
                    contentPadding = PaddingValues(bottom = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (systems.isEmpty()) item("onboarding") {
                        // Button first: the focused element is pinned to the top of the list.
                        Column(Modifier.padding(start = 56.dp, end = 56.dp, top = 8.dp, bottom = 18.dp)) {
                            RhButton(tr("Find my games automatically"), { vm.navigate(Screen.Discover) }, focus.modifier("onboard:find"), primary = true, glass = true)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                tr("RetroHub reads the games RetroArch already knows about, then searches your storage for ROMs. " +
                                    "You review the result before anything is added."),
                                style = MaterialTheme.typography.bodyLarge,
                                color = RH.TextDim,
                            )
                        }
                    }
                    if (recent.isNotEmpty()) item("recent") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                            Shelf(tr("Continue playing"), recent, "recent", focus, onFocused = { focusedHero = HeroGame(it, tr("CONTINUE PLAYING")) }) {
                                vm.navigate(Screen.Game(it.id))
                            }
                        }
                    }
                    if (favorites.isNotEmpty()) item("fav") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                            Shelf(tr("Favourites"), favorites, "fav", focus, onFocused = { focusedHero = HeroGame(it, tr("FAVOURITE")) }) {
                                vm.navigate(Screen.Game(it.id))
                            }
                        }
                    }
                    item("systems") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                            SystemShelf(vm, systems, focus)
                        }
                    }
                }
            }
        }
    }
}

/** Shelf title + top padding that stay visible above the focused tile. */
private val ShelfTitleHeight = 50.dp

@OptIn(ExperimentalFoundationApi::class)
private class PinToTopSpec(private val titlePx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = offset - titlePx
}

/**
 * Full-bleed artwork behind the top of the screen, faded to black at the bottom and on the left
 * (where the text sits). The image only changes after focus rests for a moment, so scrolling
 * quickly along a shelf doesn't decode every screenshot. Skipped entirely in Lite mode.
 */
@Composable
private fun HeroArt(tile: GameTile?) {
    if (LocalLite.current) return
    val target = tile?.screenshotPath ?: tile?.coverPath
    var shown by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (shown != null) delay(280)
        shown = target
    }
    val model = imageModel(shown) ?: return
    val context = LocalContext.current
    val request = remember(shown) {
        ImageRequest.Builder(context).data(model).size(960, 540).precision(Precision.INEXACT).build()
    }
    Box(Modifier.fillMaxWidth().height(430.dp)) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                startScrim(0f to Color.Black.copy(alpha = 0.92f), 0.45f to Color.Black.copy(alpha = 0.45f), 0.8f to Color.Transparent)
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.7f), 0.3f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black)
            )
        )
    }
}

@Composable
private fun Hero(
    vm: MainViewModel,
    hero: HeroGame?,
    systemName: String?,
    profileName: String?,
    total: Int,
    focus: FocusRestore,
) {
    Column(
        Modifier
            .fillMaxWidth(0.62f)
            .height(214.dp)
            .padding(start = 56.dp, top = 22.dp),
    ) {
        if (hero == null) {
            Text(tr("WELCOME"), style = MaterialTheme.typography.labelMedium, color = RH.TextDim)
            Spacer(Modifier.height(6.dp))
            Text(
                if (profileName != null) tr("Hello, {0}", profileName) else tr("Hello"),
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(tr("{0} in your library", gamesLabel(total)), style = MaterialTheme.typography.bodyLarge, color = RH.TextDim)
            return@Column
        }
        val t = hero.tile
        Text(hero.label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.height(6.dp))
        Text(t.title, style = MaterialTheme.typography.displayLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        val meta = listOfNotNull(
            systemName,
            t.genre,
            t.releaseYear?.toString(),
            t.lastPlayed?.let { tr("Played {0}", relativeTime(it)) },
        ).joinToString("  ·  ")
        Text(meta, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RhButton(tr("Play"), { vm.play(t.id) }, focus.modifier("hero:play"), icon = Icons.Filled.PlayArrow, primary = true, glass = true)
            RhButton(tr("Details"), { vm.navigate(Screen.Game(t.id)) }, focus.modifier("hero:info"), icon = Icons.Filled.Info, glass = true)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Shelf(
    title: String,
    games: List<GameTile>,
    keyPrefix: String,
    focus: FocusRestore,
    onFocused: (GameTile) -> Unit,
    onClick: (GameTile) -> Unit,
) {
    Column {
        SectionTitle(title, Modifier.padding(start = 56.dp))
        LazyRow(
            // Vertical room for the zoom and shadow of the focused tile.
            contentPadding = PaddingValues(start = 56.dp, end = 56.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.focusRestorer(),
        ) {
            items(games, key = { it.id }) { tile ->
                WideGameTile(
                    tile = tile,
                    onClick = { onClick(tile) },
                    onFocused = { onFocused(tile) },
                    modifier = focus.modifier("$keyPrefix:${tile.id}"),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SystemShelf(vm: MainViewModel, systems: List<SystemWithCount>, focus: FocusRestore) {
    Column {
        SectionTitle(tr("Systems"), Modifier.padding(start = 56.dp))
        LazyRow(
            contentPadding = PaddingValues(start = 56.dp, end = 56.dp, top = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.focusRestorer(),
        ) {
            items(systems, key = { it.system.id }) { s ->
                SystemTile(s.system.name, gamesLabel(s.gameCount), focus.modifier("sys:${s.system.id}")) {
                    vm.goTab(Screen.Library(s.system.id))
                }
            }
            if (systems.isNotEmpty()) item("all") {
                SystemTile(tr("All games"), tr("Every system"), focus.modifier("sys:all")) { vm.goTab(Screen.Library(null)) }
            }
            item("add") {
                SystemTile(tr("+ Add system"), tr("ROM folder · core · BIOS"), focus.modifier("sys:add")) { vm.navigate(Screen.Systems) }
            }
        }
    }
}

@Composable
private fun SystemTile(name: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    RhSurface(
        onClick = onClick,
        modifier = modifier.size(width = 236.dp, height = 96.dp),
        shape = RoundedCornerShape(14.dp),
        container = Color.White.copy(alpha = 0.08f),
        focusedContainer = Color.White.copy(alpha = 0.16f),
        focusedScale = 1.08f,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = RH.TextDim)
        }
    }
}
