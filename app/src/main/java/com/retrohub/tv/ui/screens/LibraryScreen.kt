package com.retrohub.tv.ui.screens

import com.retrohub.tv.i18n.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.retrohub.tv.ui.components.ArtColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.retrohub.tv.data.GameTile
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.CardWidth
import com.retrohub.tv.ui.components.GameCard
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.gamesLabel
import com.retrohub.tv.ui.theme.LocalLite
import com.retrohub.tv.ui.theme.RH

@Composable
fun LibraryScreen(vm: MainViewModel, screen: Screen.Library) {
    val flow = remember(screen) { vm.games(screen.systemId, screen.favoritesOnly) }
    val games: List<GameTile>? by flow.collectAsState(initial = null)
    val systems by vm.systems.collectAsStateWithLifecycle()
    val list = games.orEmpty()

    // Background takes the colour of the focused game's artwork and fades into black.
    val context = LocalContext.current
    val lite = LocalLite.current
    val scope = rememberCoroutineScope()
    var tint by remember { mutableStateOf(Color.Black) }
    var tintJob by remember { mutableStateOf<Job?>(null) }
    val animatedTint by animateColorAsState(tint, animationSpec = tween(if (lite) 0 else 550), label = "libraryTint")
    fun tintFor(tile: GameTile) {
        tintJob?.cancel()
        tintJob = scope.launch { tint = ArtColors.of(context, tile) }
    }

    val filterKey = when {
        screen.favoritesOnly -> "f:fav"
        screen.systemId != null -> "f:${screen.systemId}"
        else -> "f:all"
    }
    val keys = remember(list, systems) {
        list.mapTo(HashSet()) { "g:${it.id}" } + systems.orEmpty().map { "f:${it.system.id}" } + setOf("f:all", "f:fav", "a:scan", "a:edit")
    }
    val focus = rememberFocusRestore(
        vm, screen.key, list.firstOrNull()?.let { "g:${it.id}" } ?: filterKey, keys, ready = games != null && systems != null,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to animatedTint, 0.7f to Color.Black)),
    ) {
        TopBar(vm, Tab.GAMES)
        Row(
            Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("Games"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Text(if (games == null) "" else gamesLabel(list.size), style = MaterialTheme.typography.bodyLarge, color = RH.TextDim)
        }
        // Filters and system actions share one row, so RIGHT from the last filter reaches the actions.
        Row(Modifier.fillMaxWidth().padding(end = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            item("all") {
                FilterPill(tr("All"), screen.systemId == null && !screen.favoritesOnly, focus.modifier("f:all")) {
                    vm.replaceTop(Screen.Library(null))
                }
            }
            item("fav") {
                FilterPill(tr("★ Favourites"), screen.favoritesOnly, focus.modifier("f:fav")) {
                    vm.replaceTop(Screen.Library(null, favoritesOnly = true))
                }
            }
            items(systems.orEmpty(), key = { it.system.id }) { s ->
                FilterPill(s.system.name, screen.systemId == s.system.id && !screen.favoritesOnly, focus.modifier("f:${s.system.id}")) {
                    vm.replaceTop(Screen.Library(s.system.id))
                }
            }
        }
            if (screen.systemId != null) {
                RhButton(tr("Scan"), { vm.scan(screen.systemId) }, focus.modifier("a:scan"))
                Spacer(Modifier.width(10.dp))
                RhButton(tr("Edit system"), { vm.navigate(Screen.EditSystem(screen.systemId)) }, focus.modifier("a:edit"))
            }
        }
        if (games != null && list.isEmpty()) {
            Notice(
                if (screen.favoritesOnly) tr("No favourites yet. Open a game and press ☆ to add it.")
                else tr("No games here yet. Check the system's ROM folder and extensions, then scan."),
                modifier = Modifier.padding(horizontal = 48.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(CardWidth + 22.dp),
                contentPadding = PaddingValues(start = 40.dp, end = 40.dp, top = 6.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.id }) { tile ->
                    // Grid cells are wider than the card; centre it so the card keeps its cover width.
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        GameCard(
                            tile = tile,
                            onClick = { vm.navigate(Screen.Game(tile.id)) },
                            onFocused = { tintFor(tile) },
                            modifier = focus.modifier("g:${tile.id}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val lite = LocalLite.current
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color(0xFF2A2A2A) else Color.Transparent,
            contentColor = if (selected) Color.White else RH.TextDim,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Color(0xFF555555) else RH.Line), shape = shape),
            focusedBorder = Border(BorderStroke(1.dp, Color.White), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else 1.05f),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
    }
}
