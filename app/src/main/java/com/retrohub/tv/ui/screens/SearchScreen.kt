package com.retrohub.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.data.GameTile
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.CardWidth
import com.retrohub.tv.ui.components.GameCard
import com.retrohub.tv.ui.components.gamesLabel
import com.retrohub.tv.ui.theme.RH
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by remember { mutableStateOf(query) }
    LaunchedEffect(Unit) { snapshotFlow { query }.debounce(250).collect { debounced = it } }
    val resultsFlow = remember(debounced) { vm.search(debounced) }
    val results: List<GameTile>? by resultsFlow.collectAsState(initial = null)
    val list = results.orEmpty()

    val keys = remember(list) { list.mapTo(HashSet()) { "g:${it.id}" } + "field" }
    val focus = rememberFocusRestore(vm, "search", "field", keys)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var fieldFocused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(vm, Tab.SEARCH)
        Box(Modifier.padding(start = 48.dp, end = 48.dp, top = 22.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(RH.Surface, RoundedCornerShape(50))
                    .border(2.dp, if (fieldFocused) Color.White else RH.Line, RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = RH.TextDim, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search your games", fontSize = 22.sp, color = RH.TextFaint)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            focusManager.moveFocus(FocusDirection.Down)
                        }),
                        modifier = focus.modifier("field")
                            .fillMaxWidth()
                            .onFocusChanged { fieldFocused = it.isFocused }
                            // A single-line field swallows D-pad up/down; hand them back to focus search
                            // so the tabs above and the results below stay reachable.
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (e.key) {
                                    Key.DirectionUp -> { keyboard?.hide(); focusManager.moveFocus(FocusDirection.Up) }
                                    Key.DirectionDown -> { keyboard?.hide(); focusManager.moveFocus(FocusDirection.Down) }
                                    else -> false
                                }
                            },
                    )
                }
            }
        }
        Text(
            when {
                debounced.isBlank() -> "Type a title. Matches file names and database names."
                results == null -> "Searching…"
                list.isEmpty() -> "No games match \"$debounced\""
                else -> gamesLabel(list.size)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = RH.TextDim,
            modifier = Modifier.padding(start = 52.dp, top = 14.dp, bottom = 6.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(CardWidth + 22.dp),
            contentPadding = PaddingValues(start = 40.dp, end = 40.dp, top = 6.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(list, key = { it.id }) { tile ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GameCard(tile = tile, onClick = { vm.navigate(Screen.Game(tile.id)) }, modifier = focus.modifier("g:${tile.id}"))
                }
            }
        }
    }
}
