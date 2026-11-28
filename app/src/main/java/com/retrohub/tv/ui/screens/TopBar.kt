package com.retrohub.tv.ui.screens

import com.retrohub.tv.i18n.tr
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.Avatar
import com.retrohub.tv.ui.components.BrandMark
import com.retrohub.tv.ui.components.RhIconButton
import com.retrohub.tv.ui.theme.LocalLite
import com.retrohub.tv.ui.theme.RH
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

enum class Tab { HOME, GAMES, SEARCH, NONE }

/**
 * Top-shelf top bar: brand on the left, a floating glass pill of tabs in the centre, clock,
 * settings and the current profile on the right. The glass is a translucent fill (no real-time
 * blur), so it costs nothing on 1 GB devices.
 */
@Composable
fun TopBar(vm: MainViewModel, selected: Tab, focus: FocusRestore? = null) {
    val profile by vm.currentProfile.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 40.dp, top = 20.dp, bottom = 6.dp),
    ) {
        BrandMark(Modifier.align(Alignment.CenterStart))
        Row(
            Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(50))
                .background(GlassFill)
                .border(1.dp, GlassLine, RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TabPill(tr("Home"), selected == Tab.HOME, focus?.modifier("tab:home") ?: Modifier) { vm.goHome() }
            TabPill(tr("Games"), selected == Tab.GAMES, focus?.modifier("tab:games") ?: Modifier) { vm.goTab(Screen.Library(null)) }
            TabPill(tr("Search"), selected == Tab.SEARCH, focus?.modifier("tab:search") ?: Modifier) { vm.goTab(Screen.Search) }
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            Clock()
            Spacer(Modifier.width(14.dp))
            RhIconButton(Icons.Filled.Settings, tr("Settings"), { vm.navigate(Screen.Settings) }, focus?.modifier("top:settings") ?: Modifier)
            Spacer(Modifier.width(8.dp))
            val lite = LocalLite.current
            Surface(
                onClick = { vm.navigate(Screen.Profiles()) },
                modifier = (focus?.modifier("top:profile") ?: Modifier).size(44.dp),
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White),
                scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else 1.1f),
            ) {
                Box(Modifier.padding(3.dp)) { Avatar(profile, 38.dp) }
            }
        }
    }
}

/** Translucent "glass" used for the tab pill and secondary buttons over artwork. */
val GlassFill = Color.Black.copy(alpha = 0.38f)
val GlassLine = Color.White.copy(alpha = 0.22f)

@Composable
private fun TabPill(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val lite = LocalLite.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else 1.04f),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

/** Minute-resolution clock: one recomposition per minute. */
@Composable
private fun Clock() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            now = System.currentTimeMillis()
        }
    }
    Text(
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
        style = MaterialTheme.typography.titleSmall,
        color = Color.White.copy(alpha = 0.85f),
    )
}

/** Standard page: top bar, title row with actions, then content (optionally scrolling). */
@Composable
fun Page(
    vm: MainViewModel,
    title: String,
    subtitle: String? = null,
    scroll: Boolean = false,
    /** Optional full-screen background painted behind the page (e.g. the Settings gradient). */
    background: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().then(background)) {
        TopBar(vm, Tab.NONE)
        Column(
            Modifier
                .fillMaxSize()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = 48.dp, end = 48.dp, top = 18.dp, bottom = 40.dp),
        ) {
            // Title and actions share a row; the subtitle gets its own full-width line underneath,
            // so long subtitles never run into the buttons.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { actions() }
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = RH.TextDim)
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}
