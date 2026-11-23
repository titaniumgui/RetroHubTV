package com.retrohub.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.data.SystemPresets
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.ChoiceDialog
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.components.gamesLabel
import com.retrohub.tv.ui.theme.RH
import java.text.DateFormat
import java.util.Date

@Composable
fun SystemsScreen(vm: MainViewModel) {
    val systemsOrNull by vm.systems.collectAsStateWithLifecycle()
    val systems = systemsOrNull.orEmpty()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var choosePreset by remember { mutableStateOf(false) }

    val keys = systems.mapTo(HashSet()) { "s:${it.system.id}" } + setOf("add", "scan", "find")
    val focus = rememberFocusRestore(
        vm, "systems", systems.firstOrNull()?.let { "s:${it.system.id}" } ?: "find", keys, ready = systemsOrNull != null,
    )

    Page(
        vm,
        title = "Systems",
        subtitle = "Each system is a ROM folder, its file types and the RetroArch core that runs them",
        actions = {
            RhButton("Find my games", { vm.navigate(Screen.Discover) }, focus.modifier("find"), icon = Icons.Filled.Search, primary = true)
            RhButton("Add system", { choosePreset = true }, focus.modifier("add"), icon = Icons.Filled.Add)
            if (busy == null) RhButton("Scan all", { vm.scan(null) }, focus.modifier("scan"), icon = Icons.Filled.Refresh)
            else RhButton("Stop", { vm.cancelWork() }, focus.modifier("scan"))
        },
    ) {
        if (systemsOrNull != null && systems.isEmpty()) {
            Notice("No systems yet. Press \"Add system\" and start from a preset (it fills in the name, file types and metadata database).")
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(systems, key = { it.system.id }) { item ->
                val s = item.system
                RhSurface(
                    onClick = { vm.navigate(Screen.EditSystem(s.id)) },
                    modifier = focus.modifier("s:${s.id}").fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    focusedScale = 1.01f,
                ) {
                    Row(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium)
                            Text(s.romFolder.ifBlank { "No ROM folder" }, style = MaterialTheme.typography.bodySmall, color = RH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Core: " + (s.corePath.ifBlank { s.coreName }.ifBlank { "not set" }) +
                                    "   ·   ." + s.extensions.replace(",", " .").ifBlank { "no extensions" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (s.coreName.isBlank() && s.corePath.isBlank()) RH.Danger else RH.TextFaint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(gamesLabel(item.gameCount), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (s.lastScan == 0L) "Never scanned"
                                else "Scanned " + DateFormat.getDateInstance(DateFormat.SHORT).format(Date(s.lastScan)),
                                style = MaterialTheme.typography.bodySmall,
                                color = RH.TextFaint,
                            )
                        }
                    }
                }
            }
        }
    }

    if (choosePreset) {
        ChoiceDialog(
            title = "Start from",
            options = SystemPresets.all.map { it.name },
            onDismiss = { choosePreset = false },
            onChoose = { index ->
                choosePreset = false
                vm.navigate(Screen.EditSystem(null, index))
            },
        )
    }
}
