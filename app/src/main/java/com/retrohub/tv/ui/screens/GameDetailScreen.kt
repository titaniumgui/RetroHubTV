package com.retrohub.tv.ui.screens

import com.retrohub.tv.i18n.tr
import com.retrohub.tv.i18n.trCount
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.data.CoreCatalog
import com.retrohub.tv.data.GameEntity
import com.retrohub.tv.data.GameStatsEntity
import com.retrohub.tv.launch.LaunchPlanner
import com.retrohub.tv.saves.ProfileConfig
import com.retrohub.tv.saves.SaveStateInfo
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.Backdrop
import com.retrohub.tv.ui.components.Chip
import com.retrohub.tv.ui.components.ChoiceDialog
import com.retrohub.tv.ui.components.ConfirmDialog
import com.retrohub.tv.ui.components.EditTextDialog
import com.retrohub.tv.ui.components.InfoLine
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.components.SectionTitle
import com.retrohub.tv.ui.components.Thumbnail
import com.retrohub.tv.ui.theme.RH
import kotlinx.coroutines.flow.map
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameDetailScreen(vm: MainViewModel, gameId: Long) {
    // (game, loaded): lets us tell "still loading" from "no longer in the library".
    val gameFlow = remember(gameId) { vm.game(gameId).map { it to true } }
    val loaded by gameFlow.collectAsState(initial = null to false)
    val statsFlow = remember(gameId) { vm.stats(gameId) }
    val stats by statsFlow.collectAsState(initial = null)
    if (loaded.second && loaded.first == null) {
        MissingGame(vm)
        return
    }
    val g = loaded.first ?: return
    val systemFlow = remember(g.systemId) { vm.system(g.systemId) }
    val system by systemFlow.collectAsState(initial = null)
    val s = system ?: return
    val settings by vm.settings.collectAsStateWithLifecycle()
    val plan = remember(g, s, settings) { vm.planFor(g, s) }

    // Re-read save states whenever we come back from RetroArch.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    var states by remember { mutableStateOf<List<SaveStateInfo>>(emptyList()) }
    LaunchedEffect(g.fileName, settings.currentProfileId, settings.profileSaves, tick) { states = vm.saveStates(g) }
    val auto = states.firstOrNull { it.slot < 0 }

    var menu by remember { mutableStateOf(false) }
    var editCore by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var technical by remember { mutableStateOf(false) }

    val focus = rememberFocusRestore(vm, "game", "play", setOf("play", "fresh", "fav", "more"))
    val favorite = stats?.favorite == true

    Box(Modifier.fillMaxSize()) {
        Backdrop(g.screenshotPath ?: g.coverPath, Modifier.fillMaxSize(), alpha = 0.5f)
        Row(Modifier.fillMaxSize().padding(start = 56.dp, end = 48.dp, top = 48.dp)) {
            Thumbnail(g.coverPath, g.title, 236.dp, 324.dp, Modifier.clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(44.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(s.name.uppercase(), style = MaterialTheme.typography.labelMedium, color = RH.TextDim)
                Spacer(Modifier.height(6.dp))
                Text(g.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    g.releaseYear?.let { Chip(it.toString()) }
                    g.genre?.let { Chip(it) }
                    g.players?.let { Chip(trCount(it, "{0} player", "{0} players")) }
                    g.region?.let { Chip(it) }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // One button whose label changes, so focus survives when an auto-save appears.
                    RhButton(if (auto != null) tr("Resume") else tr("Play"), { vm.play(g.id) }, focus.modifier("play"), icon = Icons.Filled.PlayArrow, primary = true)
                    if (auto != null) {
                        RhButton(tr("Start fresh"), { vm.play(g.id, fresh = true) }, focus.modifier("fresh"), icon = Icons.Filled.Refresh)
                    }
                    RhButton(if (favorite) tr("Favourite") else tr("Add to favourites"), { vm.setFavorite(g.id, !favorite) }, focus.modifier("fav"), icon = if (favorite) Icons.Filled.Star else null)
                    RhButton("", { menu = true }, focus.modifier("more"), icon = Icons.Filled.MoreVert)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    tr("Back to RetroHub: {0}.", ProfileConfig.exitHint(settings.exitCombo)),
                    style = MaterialTheme.typography.bodySmall,
                    color = RH.TextDim,
                )
                Spacer(Modifier.height(16.dp))
                if (plan is LaunchPlanner.Result.Blocked) {
                    Notice(plan.reason, color = RH.Danger)
                    Spacer(Modifier.height(16.dp))
                }
                Details(g, stats)
                CoreLine(g.coreOverride.ifBlank { s.coreName })
                if (settings.profileSaves) {
                    Spacer(Modifier.height(22.dp))
                    SaveStatesSection(states)
                }
                if (technical) {
                    Spacer(Modifier.height(20.dp))
                    Technical(g, plan)
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }

    if (menu) {
        val options = buildList {
            add(tr("Core for this game") to { editCore = true })
            add(tr("Edit {0}", s.name) to { vm.navigate(Screen.EditSystem(s.id)) })
            if (states.isNotEmpty()) add(tr("Delete saved progress") to { confirmDelete = true })
            add((if (technical) tr("Hide technical details") else tr("Show technical details")) to { technical = !technical })
        }
        ChoiceDialog(tr("Options"), options.map { it.first }, onDismiss = { menu = false }) { i ->
            menu = false
            options[i].second()
        }
    }
    if (editCore) {
        EditTextDialog(
            title = tr("Core for this game"),
            initial = g.coreOverride,
            hint = tr("Core name (the part before _libretro_android.so) or a full .so path. Leave empty to use the system core") +
                (s.coreName.takeIf { it.isNotBlank() }?.let { " ($it)." } ?: "."),
            onDismiss = { editCore = false },
            onConfirm = { vm.setCoreOverride(g.id, it); editCore = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = tr("Delete saved progress?"),
            text = tr("Removes this profile's {0} save state(s) for {1}, including the auto-save. In-game battery saves are kept. This cannot be undone.", states.size, g.title),
            confirm = tr("Delete"),
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; vm.deleteStates(states) { tick++ } },
        )
    }
}

@Composable
private fun MissingGame(vm: MainViewModel) {
    val focus = rememberFocusRestore(vm, "missing", "back", setOf("back"))
    Column(Modifier.fillMaxSize().padding(56.dp)) {
        Text(tr("This game is no longer in your library"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(tr("It was removed by a scan or its system was deleted."), style = MaterialTheme.typography.bodyLarge, color = RH.TextDim)
        Spacer(Modifier.height(24.dp))
        RhButton(tr("Back"), { vm.back() }, focus.modifier("back"), primary = true)
    }
}

/** Which core runs this game, with where to get it if RetroArch reports it missing. */
@Composable
private fun CoreLine(coreId: String) {
    if (coreId.isBlank()) return
    val name = CoreCatalog.displayName(coreId)
    InfoLine(tr("Core"), name ?: coreId)
    if (name != null) {
        Text(
            tr("If RetroArch says the core is missing: RetroArch → Online Updater → Core Downloader → {0}", name),
            style = MaterialTheme.typography.bodySmall,
            color = RH.TextFaint,
            modifier = Modifier.padding(start = 156.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun Details(g: GameEntity, stats: GameStatsEntity?) {
    Column {
        g.developer?.let { InfoLine(tr("Developer"), it) }
        g.publisher?.let { InfoLine(tr("Publisher"), it) }
        InfoLine(tr("Played"), when (val n = stats?.playCount ?: 0) { 0 -> tr("Never"); 1 -> tr("Once"); else -> trCount(n, "{0} time", "{0} times") })
        stats?.lastPlayed?.let { InfoLine(tr("Last played"), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))) }
        InfoLine(tr("File"), g.fileName)
        InfoLine(tr("Size"), formatSize(g.sizeBytes))
        if (g.coreOverride.isNotBlank()) InfoLine(tr("Core override"), g.coreOverride)
    }
}

@Composable
private fun SaveStatesSection(states: List<SaveStateInfo>) {
    Column {
        SectionTitle(tr("Save states"))
        if (states.isEmpty()) {
            Text(
                tr("No saves yet for this profile. Progress is saved automatically whenever you return to RetroHub " +
                    "(controller shortcut or Close Content). To save mid-game: RetroArch's menu → Save States → Save State."),
                style = MaterialTheme.typography.bodyMedium,
                color = RH.TextDim,
            )
            return
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(states, key = { it.file.path }) { st ->
                RhSurface(onClick = {}, modifier = Modifier.width(176.dp), container = RH.Surface) {
                    Column {
                        Thumbnail(st.thumbnail?.absolutePath, st.label, 176.dp, 120.dp)
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(st.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(st.modified)),
                                style = MaterialTheme.typography.bodySmall,
                                color = RH.TextDim,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tr("Resume loads the auto save. Load other slots in RetroArch: menu → Save States → State Slot → Load State. " +
                "Leaving with the TV's Home button does not update the auto save."),
            style = MaterialTheme.typography.bodySmall,
            color = RH.TextFaint,
        )
    }
}

@Composable
private fun Technical(g: GameEntity, plan: LaunchPlanner.Result) {
    Column {
        SectionTitle(tr("Technical details"))
        InfoLine(tr("Path"), g.path ?: tr("No filesystem path (SAF only)"), if (g.path == null) RH.Danger else RH.Text)
        g.crc?.let { InfoLine("CRC32", it, mono = true) }
        g.dbName?.let { InfoLine(tr("Database name"), it) }
        if (plan is LaunchPlanner.Result.Ready) {
            InfoLine(tr("Core file"), plan.plan.corePath, mono = true)
            InfoLine(tr("Config"), plan.plan.extra("CONFIGFILE") ?: tr("RetroArch default"))
            Spacer(Modifier.height(10.dp))
            Text(tr("Equivalent adb command"), style = MaterialTheme.typography.labelMedium, color = RH.TextFaint)
            Text(LaunchPlanner.adbCommand(plan.plan), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = RH.TextDim)
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> tr("Unknown")
    bytes < 1024 * 1024 -> tr("{0} KB", String.format(Locale.US, "%.1f", bytes / 1024.0))
    bytes < 1024L * 1024 * 1024 -> tr("{0} MB", String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024)))
    else -> tr("{0} GB", String.format(Locale.US, "%.2f", bytes / (1024.0 * 1024 * 1024)))
}
