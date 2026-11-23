package com.retrohub.tv.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.data.ProfileEntity
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.Avatar
import com.retrohub.tv.ui.components.BrandMark
import com.retrohub.tv.ui.components.ChoiceDialog
import com.retrohub.tv.ui.components.ConfirmDialog
import com.retrohub.tv.ui.components.EditTextDialog
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.theme.RH

@Composable
fun ProfilesScreen(vm: MainViewModel, screen: Screen.Profiles) {
    val profilesOrNull by vm.profiles.collectAsStateWithLifecycle()
    val profiles = profilesOrNull.orEmpty()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<ProfileEntity?>(null) }
    var renaming by remember { mutableStateOf<ProfileEntity?>(null) }
    var deleting by remember { mutableStateOf<ProfileEntity?>(null) }

    val keys = profiles.mapTo(HashSet()) { "p:${it.id}" } + setOf("add", "manage")
    val focus = rememberFocusRestore(vm, screen.key, "p:${settings.currentProfileId}", keys, ready = profilesOrNull != null)

    Column(Modifier.fillMaxSize().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark()
        Spacer(Modifier.height(40.dp))
        Text(if (screen.manage) "Manage profiles" else "Who's playing?", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(10.dp))
        Text(
            if (screen.manage) "Select a profile to rename, recolour or delete it."
            else "Each profile keeps its own favourites, history and save games.",
            style = MaterialTheme.typography.bodyLarge,
            color = RH.TextDim,
        )
        Spacer(Modifier.height(30.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
        ) {
            items(profiles, key = { it.id }) { p ->
                ProfileTile(
                    name = p.name,
                    modifier = focus.modifier("p:${p.id}"),
                    current = p.id == settings.currentProfileId,
                    avatar = { Avatar(p, 128.dp) },
                    badge = screen.manage,
                ) {
                    if (screen.manage) actionsFor = p else vm.selectProfile(p.id)
                }
            }
            item("add") {
                ProfileTile("Add profile", focus.modifier("add"), current = false, avatar = {
                    Box(Modifier.size(128.dp).border(2.dp, RH.Line, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = RH.TextDim, modifier = Modifier.size(44.dp))
                    }
                }) { adding = true }
            }
        }
        Spacer(Modifier.height(24.dp))
        RhButton(
            if (screen.manage) "Done" else "Manage profiles",
            { vm.replaceTop(Screen.Profiles(manage = !screen.manage)) },
            focus.modifier("manage"),
            icon = if (screen.manage) null else Icons.Filled.Edit,
        )
    }

    if (adding) {
        EditTextDialog("New profile", "", "Name shown on the dashboard.", onDismiss = { adding = false }) {
            vm.addProfile(it); adding = false
        }
    }
    actionsFor?.let { p ->
        ChoiceDialog(p.name, listOf("Rename", "Change colour", "Delete"), onDismiss = { actionsFor = null }) { i ->
            actionsFor = null
            when (i) {
                0 -> renaming = p
                1 -> vm.recolorProfile(p)
                2 -> deleting = p
            }
        }
    }
    renaming?.let { p ->
        EditTextDialog("Rename profile", p.name, null, onDismiss = { renaming = null }) { vm.renameProfile(p, it); renaming = null }
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "Delete ${p.name}?",
            text = "Removes this profile's favourites and play history. Its save files stay on storage in the RetroHub profiles folder.",
            confirm = "Delete",
            onDismiss = { deleting = null },
            onConfirm = { deleting = null; vm.deleteProfile(p) },
        )
    }
}

@Composable
private fun ProfileTile(
    name: String,
    modifier: Modifier,
    current: Boolean,
    avatar: @Composable () -> Unit,
    badge: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
        RhSurface(
            onClick = onClick,
            modifier = modifier.size(140.dp).onFocusChanged { focused = it.isFocused },
            shape = CircleShape,
            container = Color.Transparent,
            focusedContainer = Color.Transparent,
            focusedScale = 1.1f,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                avatar()
                if (badge) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(8.dp).size(30.dp)
                            .border(2.dp, Color.Black, CircleShape),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.padding(5.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = if (focused || current) RH.Text else RH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (current) Text("Current", style = MaterialTheme.typography.labelSmall, color = RH.TextFaint)
    }
}
