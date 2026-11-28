package com.retrohub.tv.ui.screens

import com.retrohub.tv.i18n.tr
import com.retrohub.tv.i18n.trCount
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.detect.Proposal
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.PanelFill
import com.retrohub.tv.ui.components.PanelFocused
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.components.gamesLabel
import com.retrohub.tv.ui.theme.RH

@Composable
fun DiscoverScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val state by vm.discovery.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hasAccess = remember { StorageAccess.hasBroadFileAccess(context) }
    // Selection per proposal key; new systems start selected, already-added ones don't.
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val proposals = state?.proposals.orEmpty()
    LaunchedEffect(proposals) { proposals.forEach { if (it.key !in selected) selected[it.key] = !it.alreadyAdded } }

    // Start straight away the first time the screen opens.
    LaunchedEffect(Unit) { if (state == null && hasAccess) vm.startDiscovery() }

    val chosen = proposals.filter { selected[it.key] == true }
    val keys = proposals.mapTo(HashSet()) { "p:${it.key}" } + setOf("add", "again")
    val focus = rememberFocusRestore(
        vm, "discover", if (proposals.isNotEmpty()) "add" else "again", keys, ready = state?.done == true,
    )

    Page(
        vm,
        title = tr("Find my games"),
        subtitle = tr("RetroArch {0} · uses RetroArch's playlists, then searches your storage", settings.retroArchPackage),
        scroll = true,
        actions = {
            if (state?.done == true && proposals.isNotEmpty()) {
                RhButton(
                    trCount(chosen.size, "Add {0} system", "Add {0} systems"),
                    { vm.addProposals(chosen) },
                    focus.modifier("add"),
                    icon = Icons.Filled.Check,
                    primary = true,
                )
            }
            if (state?.running != true) {
                RhButton(if (state == null) tr("Search now") else tr("Search again"), { vm.startDiscovery() }, focus.modifier("again"), icon = Icons.Filled.Search)
            } else {
                RhButton(tr("Stop"), { vm.cancelDiscovery() })
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!hasAccess) {
                Notice(
                    tr("RetroHub needs storage access to look for games. Grant it in Settings → Storage, then come back."),
                    color = RH.Gold,
                )
                RhButton(tr("Open Settings"), { vm.navigate(Screen.Settings) })
            }
            val st = state
            when {
                st == null -> Unit
                st.running -> Notice(st.progress ?: tr("Searching…"))
                proposals.isEmpty() -> Notice(
                    tr("No games found. Put ROMs in a folder on internal storage (for example ROMs/snes) or scan them in RetroArch " +
                        "(Import Content), then search again. You can also add a system by hand under Settings → Manage systems."),
                )
                else -> {
                    Text(
                        trCount(proposals.size, "Found {0} system", "Found {0} systems") +
                            (if (st.playlistsFound > 0) " · " + trCount(st.playlistsFound, "{0} RetroArch playlist read", "{0} RetroArch playlists read") else "") +
                            ". " + tr("Choose what to add, then press Add."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RH.TextDim,
                    )
                    proposals.forEach { p ->
                        ProposalRow(p, selected[p.key] == true, focus.modifier("p:${p.key}")) {
                            selected[p.key] = !(selected[p.key] ?: false)
                        }
                    }
                    Text(
                        tr("Cores are picked for you. If RetroArch says a core is missing, install it once from RetroArch → " +
                            "Online Updater → Core Downloader. You can switch cores per system at any time."),
                        style = MaterialTheme.typography.bodySmall,
                        color = RH.TextFaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProposalRow(p: Proposal, checked: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    RhSurface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        container = PanelFill,
        focusedContainer = PanelFocused,
        focusedScale = 1.01f,
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color.White else Color.Transparent)
                    .border(2.dp, if (checked) Color.White else RH.TextFaint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    if (p.fromRetroArch) Tag(tr("From RetroArch"))
                    if (p.alreadyAdded) { Spacer(Modifier.width(6.dp)); Tag(tr("Already added")) }
                }
                Spacer(Modifier.height(2.dp))
                Text(p.romFolder, style = MaterialTheme.typography.bodySmall, color = RH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        p.coreName.isBlank() -> tr("Core: no recommendation for this system — choose one later")
                        p.coreFromRetroArch -> tr("Core: {0} · the one RetroArch already uses", p.coreName)
                        else -> tr("Core: {0} · chosen automatically", p.coreName)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.coreName.isNotBlank()) RH.Ok else RH.Gold,
                )
            }
            Text(gamesLabel(p.gameCount), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = RH.TextDim,
        modifier = Modifier
            .border(1.dp, PanelFill.copy(alpha = 0.4f), RoundedCornerShape(50))
            .background(PanelFill, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
