package com.retrohub.tv.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Icon
import com.retrohub.tv.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.BuildConfig
import com.retrohub.tv.data.AppSettings
import com.retrohub.tv.data.LiteMode
import com.retrohub.tv.perf.MemoryProfile
import com.retrohub.tv.saves.ProfileConfig
import com.retrohub.tv.saves.ProfileStorage
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.ChoiceDialog
import com.retrohub.tv.ui.components.ConfirmDialog
import com.retrohub.tv.ui.components.EditTextDialog
import com.retrohub.tv.ui.components.FieldRow
import com.retrohub.tv.ui.components.InfoLine
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.components.SectionTitle
import com.retrohub.tv.ui.components.ToggleRow
import com.retrohub.tv.ui.theme.RH
import java.util.Locale

private enum class SettingField(val label: String, val hint: String) {
    PACKAGE("RetroArch package", "Detected automatically: com.retroarch.aarch64, com.retroarch or com.retroarch.ra32."),
    ACTIVITY("RetroArch activity", "Default ${AppSettings.DEFAULT_ACTIVITY}"),
    CORE_DIR("Core directory", "Folder with *_libretro_android.so files. Detected automatically from the installed RetroArch."),
    CONFIG("Your RetroArch config (retroarch.cfg)", "Detected automatically (RetroArch's default location). Profiles include it, so your RetroArch settings still apply."),
    EXTERNAL("External directory (EXTERNAL)", "Optional. Leave empty unless your RetroArch setup needs it."),
    PROFILE_ROOT("Profile folder", "Where each profile's saves and states are kept. Default: RetroArch's own media folder, which every RetroArch build can write to."),
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.currentProfile.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SettingField?>(null) }
    var confirmClearArt by remember { mutableStateOf(false) }
    var chooseRetroArch by remember { mutableStateOf<List<String>?>(null) }
    var chooseExit by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }

    val ra = remember(s.retroArchPackage, tick) { vm.retroArchInfo() }
    val hasAccess = remember(tick) { StorageAccess.hasBroadFileAccess(context) }
    val safCount = remember(tick) { StorageAccess.persistedTreeCount(context) }
    val artBytes = remember(tick) { vm.artworkBytes() }
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    // Focus starts on the About card at the very top, so the page opens scrolled to the top.
    val focus = rememberFocusRestore(vm, "settings", "about", setOf("about", "f:switch") + SettingField.entries.map { "f:${it.name}" })

    Page(vm, title = "Settings", subtitle = "RetroHub TV ${BuildConfig.VERSION_NAME}", scroll = true, background = AuroraBackground) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutCard(focus.modifier("about"))
            Spacer(Modifier.height(10.dp))

            // ---------------- profiles ----------------
            SectionTitle("Profiles")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RhButton("Switch profile (${profile?.name ?: "…"})", { vm.navigate(Screen.Profiles()) }, focus.modifier("f:switch"))
                RhButton("Manage profiles", { vm.navigate(Screen.Profiles(manage = true)) })
            }
            ToggleRow("Ask who's playing at start", s.askProfileOnStart, { vm.updateSettings { it.copy(askProfileOnStart = !it.askProfileOnStart) } }, hint = "Shown when there is more than one profile")
            ToggleRow(
                "Separate saves per profile + Resume",
                s.profileSaves,
                { vm.updateSettings { it.copy(profileSaves = !it.profileSaves) } },
                hint = "Each profile gets its own RetroArch save and state folders. Games auto-save when you leave with Close Content and resume next time.",
            )
            if (s.profileSaves) {
                FieldRow(SettingField.PROFILE_ROOT.label, ProfileStorage.root(s).absolutePath, { editing = SettingField.PROFILE_ROOT }, focus.modifier("f:PROFILE_ROOT"), hint = SettingField.PROFILE_ROOT.hint)
                if (s.configFile.isBlank()) {
                    Notice(
                        "Set \"Your RetroArch config\" below. Without it, games started from a profile use RetroArch's default settings instead of yours.",
                        color = RH.Gold,
                    )
                }
            }

            // ---------------- RetroArch ----------------
            SectionTitle("RetroArch", Modifier.padding(top = 16.dp))
            if (ra == null) {
                Notice("RetroArch (${s.retroArchPackage}) is not installed or not visible. Install RetroArch 32-bit, or fix the package name.", color = RH.Danger)
            } else {
                InfoLine("Installed", "${ra.packageName} ${ra.versionName ?: ""}", RH.Ok)
                ra.dataDir?.let { InfoLine("Data dir", it, mono = true) }
                val grants = remember(ra.packageName, tick) { vm.retroArchFolderInfo() }
                if (grants != null) {
                    InfoLine("Game access", if (grants.isEmpty()) "No folders yet" else grants.joinToString(", "), if (grants.isEmpty()) RH.Gold else RH.Ok)
                    Text(
                        "This RetroArch (Play Store edition) can only open games in folders it has been given. " +
                            "RetroHub learns them from RetroArch's history: in RetroArch, Load Content → open your games folder " +
                            "and start any game once.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RH.TextFaint,
                    )
                }
            }
            FieldRow(SettingField.PACKAGE.label, s.retroArchPackage, { editing = SettingField.PACKAGE }, focus.modifier("f:PACKAGE"))
            FieldRow(SettingField.ACTIVITY.label, s.retroArchActivity, { editing = SettingField.ACTIVITY }, focus.modifier("f:ACTIVITY"))
            FieldRow(SettingField.CORE_DIR.label, s.coreDirectory, { editing = SettingField.CORE_DIR }, focus.modifier("f:CORE_DIR"), hint = SettingField.CORE_DIR.hint)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RhButton("Detect RetroArch", {
                    val found = vm.installedRetroArch()
                    if (found.size > 1) chooseRetroArch = found.map { it.packageName } else vm.detectRetroArch()
                }, primary = true)
                RhButton("Open RetroArch", { vm.openRetroArch() })
            }
            Text(
                "Install cores inside RetroArch (Main Menu → Load Core → Download a Core). RetroHub cannot list RetroArch's private core folder, " +
                    "so type core names exactly as installed. Open RetroArch once and allow its storage permission.",
                style = MaterialTheme.typography.bodySmall,
                color = RH.TextFaint,
            )
            FieldRow(SettingField.CONFIG.label, s.configFile, { editing = SettingField.CONFIG }, focus.modifier("f:CONFIG"), hint = SettingField.CONFIG.hint)
            FieldRow(SettingField.EXTERNAL.label, s.externalDirectory, { editing = SettingField.EXTERNAL }, focus.modifier("f:EXTERNAL"), hint = SettingField.EXTERNAL.hint)
            FieldRow(
                "Return to RetroHub from a game",
                ProfileConfig.exitLabel(s.exitCombo),
                { chooseExit = true },
                hint = "Controller shortcut that closes the game (progress auto-saves) and comes back here. " +
                    "Close Content in RetroArch's menu also returns here.",
            )
            ToggleRow("Send IME extra", s.sendImeExtra, { vm.updateSettings { it.copy(sendImeExtra = !it.sendImeExtra) } }, hint = "Passes the current keyboard so RetroArch's on-screen keyboard works")
            ToggleRow("Start RetroArch fresh for each game", s.clearTaskOnLaunch, { vm.updateSettings { it.copy(clearTaskOnLaunch = !it.clearTaskOnLaunch) } }, hint = "Closes a RetroArch left running in the background before each game. Turning this off can cause black screens.")

            // ---------------- metadata ----------------
            SectionTitle("Game info & artwork", Modifier.padding(top = 16.dp))
            Text(
                "Covers, screenshots and details (genre, developer, publisher, year, players) come from libretro-database and " +
                    "libretro-thumbnails. Games are matched by checksum, then by name. Needs an internet connection.",
                style = MaterialTheme.typography.bodySmall,
                color = RH.TextFaint,
            )
            ToggleRow("Download after every scan", s.metadataAuto, { vm.updateSettings { it.copy(metadataAuto = !it.metadataAuto) } })
            ToggleRow("Include screenshots", s.metadataSnaps, { vm.updateSettings { it.copy(metadataSnaps = !it.metadataSnaps) } }, hint = "Used as backgrounds on the dashboard and game pages")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RhButton("Download game info now", { vm.fetchMetadata(retryNotFound = false) }, icon = Icons.Filled.Refresh, primary = true)
                RhButton("Retry unmatched games", { vm.fetchMetadata(retryNotFound = true) })
                RhButton("Remove downloaded artwork", { confirmClearArt = true })
            }
            InfoLine("Artwork stored", String.format(Locale.US, "%.1f MB", artBytes / (1024.0 * 1024)))

            // ---------------- storage ----------------
            SectionTitle("Storage", Modifier.padding(top = 16.dp))
            Notice(
                when {
                    hasAccess && StorageAccess.needsAllFilesAccess() -> "All-files access granted: path folders can be scanned and profile saves written."
                    hasAccess -> "Storage permission granted: path folders can be scanned."
                    StorageAccess.needsAllFilesAccess() ->
                        "Android ${Build.VERSION.RELEASE}: ROM files are not media, so scanning by path and writing profile saves needs \"All files access\". " +
                            "Without it, use the system folder picker for each ROM folder (works only if your TV has one)."
                    else -> "Storage permission not granted yet."
                },
                color = if (hasAccess) RH.Ok else RH.Gold,
            )
            InfoLine("Picked SAF folders", safCount.toString())
            if (!hasAccess) {
                RhButton("Grant storage access", {
                    if (StorageAccess.needsAllFilesAccess()) {
                        if (!StorageAccess.openAllFilesAccessSettings(context)) {
                            vm.toast("This TV has no All-files access screen. Use the adb command shown below.")
                        }
                    } else {
                        legacyPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    }
                }, primary = true)
                Text("If the TV hides the permission screen, run from a computer:", style = MaterialTheme.typography.bodySmall, color = RH.TextFaint)
                Text(StorageAccess.adbGrantCommand(context), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            }

            // ---------------- performance ----------------
            SectionTitle("Performance", Modifier.padding(top = 16.dp))
            val mem = vm.memory
            val wouldBeLite = MemoryProfile.isLite(s.liteMode, mem)
            ToggleRow(
                "Lite mode: ${s.liteMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                wouldBeLite,
                {
                    vm.updateSettings {
                        it.copy(liteMode = when (it.liteMode) { LiteMode.AUTO -> LiteMode.ON; LiteMode.ON -> LiteMode.OFF; LiteMode.OFF -> LiteMode.AUTO })
                    }
                },
                hint = "Auto / On / Off. Lite: smaller image cache and thumbnails, no zoom, no backgrounds. " +
                    if (wouldBeLite != vm.isLite) "Restart RetroHub to apply." else "Active now: ${if (vm.isLite) "Lite" else "Standard"}.",
            )
            InfoLine("Device RAM", "${mem.totalMb} MB" + if (mem.lowRamDevice) " (low-RAM device)" else "")
            InfoLine("Image cache", "${MemoryProfile.imageMemoryCacheBytes(vm.isLite, mem) / (1024 * 1024)} MB memory, ${MemoryProfile.imageDiskCacheBytes(vm.isLite) / (1024 * 1024)} MB disk")

            // ---------------- library ----------------
            SectionTitle("Library", Modifier.padding(top = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RhButton("Find my games", { vm.navigate(Screen.Discover) }, primary = true)
                RhButton("Manage systems", { vm.navigate(Screen.Systems) })
                RhButton("Clear recently played", { vm.clearRecent() })
            }
        }
    }

    if (chooseExit) {
        ChoiceDialog("Return to RetroHub", ProfileConfig.EXIT_CHOICES.map { it.second }, onDismiss = { chooseExit = false }) { i ->
            chooseExit = false
            vm.updateSettings { it.copy(exitCombo = ProfileConfig.EXIT_CHOICES[i].first) }
        }
    }

    chooseRetroArch?.let { packages ->
        ChoiceDialog("Which RetroArch?", packages, onDismiss = { chooseRetroArch = null }) { i ->
            chooseRetroArch = null
            vm.detectRetroArch(packages[i])
        }
    }

    if (confirmClearArt) {
        ConfirmDialog(
            "Remove downloaded artwork?",
            "Deletes covers and screenshots RetroHub downloaded (artwork from your own artwork folders is kept). Game info can be downloaded again.",
            "Remove",
            onDismiss = { confirmClearArt = false },
            onConfirm = { confirmClearArt = false; vm.clearArtwork(); tick++ },
        )
    }

    editing?.let { f ->
        EditTextDialog(
            title = f.label,
            hint = f.hint,
            initial = when (f) {
                SettingField.PACKAGE -> s.retroArchPackage
                SettingField.ACTIVITY -> s.retroArchActivity
                SettingField.CORE_DIR -> s.coreDirectory
                SettingField.CONFIG -> s.configFile
                SettingField.EXTERNAL -> s.externalDirectory
                SettingField.PROFILE_ROOT -> ProfileStorage.root(s).absolutePath
            },
            onDismiss = { editing = null },
            onConfirm = { v ->
                val value = v.trim()
                vm.updateSettings {
                    when (f) {
                        SettingField.PACKAGE -> it.copy(retroArchPackage = value.ifEmpty { AppSettings.DEFAULT_PACKAGE })
                        SettingField.ACTIVITY -> it.copy(retroArchActivity = value.ifEmpty { AppSettings.DEFAULT_ACTIVITY })
                        SettingField.CORE_DIR -> it.copy(coreDirectory = value.trimEnd('/'))
                        SettingField.CONFIG -> it.copy(configFile = value)
                        SettingField.EXTERNAL -> it.copy(externalDirectory = value.trimEnd('/'))
                        SettingField.PROFILE_ROOT -> it.copy(profileRoot = value.trimEnd('/'))
                    }
                }
                editing = null
            },
        )
    }
}

/**
 * Settings background: deep violet → navy → teal, with a soft magenta glow in the top-right
 * corner. Static gradients are drawn once per frame without allocations, so this is cheap even
 * in Lite mode.
 */
private val AuroraBackground: Modifier = Modifier
    .background(
        Brush.linearGradient(
            0f to Color(0xFF2A0F5C),
            0.45f to Color(0xFF111B4E),
            1f to Color(0xFF04232E),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    )
    .background(
        Brush.radialGradient(
            listOf(Color(0xFFE0457B).copy(alpha = 0.32f), Color.Transparent),
            center = Offset(2200f, -150f),
            radius = 1500f,
        )
    )
    .background(
        Brush.radialGradient(
            listOf(Color(0xFF22D3EE).copy(alpha = 0.16f), Color.Transparent),
            center = Offset(-200f, 1300f),
            radius = 1300f,
        )
    )

@Composable
private fun AboutCard(modifier: Modifier) {
    // Focusable (it does nothing when pressed) so the D-pad can reach the top of the page.
    RhSurface(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        container = Color.White.copy(alpha = 0.08f),
        focusedContainer = Color.White.copy(alpha = 0.14f),
        focusedScale = 1.01f,
    ) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_controller),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(width = 66.dp, height = 44.dp),
        )
        Spacer(Modifier.width(26.dp))
        Column(Modifier.weight(1f)) {
            Text("ABOUT", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("RetroHub", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Text(" TV", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light))
                Spacer(Modifier.width(12.dp))
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Co-developed with Claude", style = MaterialTheme.typography.titleMedium)
            Text("Hassan Khallaf", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.75f))
        }
    }
}
}
