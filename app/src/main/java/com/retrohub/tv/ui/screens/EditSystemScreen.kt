package com.retrohub.tv.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.data.CoreCatalog
import com.retrohub.tv.data.SystemEntity
import com.retrohub.tv.data.SystemPresets
import com.retrohub.tv.launch.LaunchPlanner
import com.retrohub.tv.storage.FolderKind
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.storage.StoragePaths
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.ChoiceDialog
import com.retrohub.tv.ui.components.ConfirmDialog
import com.retrohub.tv.ui.components.EditTextDialog
import com.retrohub.tv.ui.components.FieldRow
import com.retrohub.tv.ui.components.InfoLine
import com.retrohub.tv.ui.components.Notice
import com.retrohub.tv.ui.components.RhButton
import com.retrohub.tv.ui.components.SectionTitle
import com.retrohub.tv.ui.components.ToggleRow
import com.retrohub.tv.ui.theme.RH
import java.io.File

private enum class Field(val label: String, val hint: String) {
    NAME("Name", "Shown on the dashboard."),
    FOLDER("ROM folder (path)", "Absolute path in internal storage, e.g. the folder holding this system's games."),
    EXT("File extensions", "Comma separated, without dots."),
    CORE_NAME("Core", "Chosen automatically. Pick another compatible core from the list if you prefer."),
    CORE_PATH("Core path (optional)", "Full path to a core .so. Overrides the core name."),
    BIOS_DIR("BIOS folder", "Must match RetroArch → Settings → Directory → System/BIOS."),
    BIOS_FILES("Required BIOS files", "Comma separated file names to verify, e.g. as listed in the core's documentation."),
    ART("Artwork folder (optional)", "Images named like the ROM: <rom name>.png / .jpg / .webp"),
    DB("Metadata database", "Exact libretro-database name, e.g. \"Nintendo - Game Boy\" (see github.com/libretro/libretro-database/tree/master/rdb)."),
}

@Composable
fun EditSystemScreen(vm: MainViewModel, screen: Screen.EditSystem) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf<SystemEntity?>(null) }
    var editing by remember { mutableStateOf<Field?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(Field.FOLDER) }
    var resumeTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(screen) {
        draft = if (screen.systemId != null) {
            vm.loadSystem(screen.systemId)
        } else {
            val preset = SystemPresets.all.getOrElse(screen.presetIndex) { SystemPresets.all.first() }
            SystemEntity(
                name = if (screen.presetIndex == 0) "" else preset.name,
                extensions = preset.extensions,
                metadataDb = preset.metadataDb,
            )
        }
    }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { StorageAccess.persistTree(context, uri) }
        val path = StorageAccess.treeToPath(uri)
        // Prefer the real path when RetroHub can read it: faster scans, and RetroArch needs paths anyway.
        val value = if (path != null && StorageAccess.hasBroadFileAccess(context)) path else uri.toString()
        draft = draft?.let { if (pickTarget == Field.ART) it.copy(artworkFolder = value) else it.copy(romFolder = value) }
        if (path == null) {
            vm.toast("This folder has no filesystem path. RetroHub can list it, but RetroArch cannot open its games.")
        }
    }

    val d = draft ?: return
    val focus = rememberFocusRestore(vm, screen.key, "f:NAME", Field.entries.mapTo(HashSet()) { "f:${it.name}" })

    fun launchPicker(target: Field) {
        pickTarget = target
        try {
            picker.launch(null)
        } catch (_: ActivityNotFoundException) {
            vm.toast("This TV has no system folder picker. Type the path instead and grant storage access in Settings.")
        }
    }

    var chooseDb by remember { mutableStateOf(false) }
    var chooseCore by remember { mutableStateOf(false) }
    run {
        Page(
            vm,
            title = if (d.id == 0L) "New system" else "Edit ${d.name}",
            subtitle = "Nothing is guessed: set the folder, file types and the core installed in your RetroArch.",
            scroll = true,
            actions = {
                RhButton("Save & scan", { vm.saveSystem(d, scanAfter = true) }, primary = true)
                RhButton("Save", { vm.saveSystem(d, scanAfter = false) })
                // Delete lives up here, away from the save buttons, so a stray D-pad press can't reach it.
                if (d.id != 0L) RhButton("Delete", { confirmDelete = true })
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldRow(Field.NAME.label, d.name, { editing = Field.NAME }, focus.modifier("f:NAME"))

                SectionTitle("ROMs", Modifier.padding8())
                FieldRow(Field.FOLDER.label, d.romFolder, { editing = Field.FOLDER }, focus.modifier("f:FOLDER"), hint = "Select to type a path")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RhButton("Pick folder (system picker)", { launchPicker(Field.FOLDER) })
                }
                val info = remember(d.romFolder, resumeTick) { StorageAccess.describeFolder(context, d.romFolder) }
                Notice(
                    info.description,
                    color = when (info.kind) {
                        FolderKind.SAF_UNMAPPED -> RH.Danger
                        FolderKind.UNSET -> RH.TextDim
                        else -> if (info.accessible) RH.Ok else RH.Gold
                    },
                )
                FieldRow(Field.EXT.label, d.extensions, { editing = Field.EXT }, focus.modifier("f:EXT"))
                ToggleRow("Scan sub-folders", d.recursive, { draft = d.copy(recursive = !d.recursive) }, hint = "Up to 6 levels deep")

                SectionTitle("RetroArch core", Modifier.padding8())
                FieldRow(
                    Field.CORE_NAME.label,
                    d.coreName.takeIf { it.isNotBlank() }?.let { id -> CoreCatalog.displayName(id)?.let { "$it  ·  $id" } ?: id }.orEmpty(),
                    { chooseCore = true },
                    focus.modifier("f:CORE_NAME"),
                    hint = Field.CORE_NAME.hint,
                )
                FieldRow(Field.CORE_PATH.label, d.corePath, { editing = Field.CORE_PATH }, focus.modifier("f:CORE_PATH"))
                val core = d.corePath.trim().ifEmpty { null } ?: LaunchPlanner.coreFile(d.coreName, settings.coreDirectory)
                InfoLine(
                    "Resolved core",
                    core ?: if (d.coreName.isBlank()) "Set a core name or path" else "Set the core directory in Settings",
                    if (core == null) RH.Danger else RH.Text,
                    mono = core != null,
                )

                SectionTitle("BIOS", Modifier.padding8())
                FieldRow(Field.BIOS_DIR.label, d.biosFolder, { editing = Field.BIOS_DIR }, focus.modifier("f:BIOS_DIR"), hint = Field.BIOS_DIR.hint)
                FieldRow(Field.BIOS_FILES.label, d.biosFiles, { editing = Field.BIOS_FILES }, focus.modifier("f:BIOS_FILES"))
                BiosStatus(d, resumeTick)

                SectionTitle("Game info & artwork", Modifier.padding8())
                FieldRow(
                    "Metadata database (libretro)",
                    d.metadataDb,
                    { chooseDb = true },
                    focus.modifier("f:DB"),
                    hint = "Covers, screenshots and game details are downloaded from this libretro database.",
                )
                FieldRow(Field.ART.label, d.artworkFolder, { editing = Field.ART }, focus.modifier("f:ART"), hint = Field.ART.hint + " Your own artwork always wins.")
                RhButton("Pick artwork folder", { launchPicker(Field.ART) })

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RhButton("Save & scan", { vm.saveSystem(d, scanAfter = true) }, primary = true)
                    RhButton("Save", { vm.saveSystem(d, scanAfter = false) })
                    RhButton("Cancel", { vm.back() })
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (chooseCore) {
        val cores = CoreCatalog.optionsFor(d.metadataDb)
        val options = cores.mapIndexed { i, c -> c.name + if (i == 0) "  (recommended)" else "" } + "Other… (type a core name)"
        ChoiceDialog("Core for ${d.name.ifBlank { "this system" }}", options, onDismiss = { chooseCore = false }) { i ->
            chooseCore = false
            if (i < cores.size) draft = d.copy(coreName = cores[i].id) else editing = Field.CORE_NAME
        }
    }

    if (chooseDb) {
        val options = listOf("None (no metadata)") + SystemPresets.metadataDatabases + "Other… (type the name)"
        ChoiceDialog("Metadata database", options, onDismiss = { chooseDb = false }) { i ->
            chooseDb = false
            when (i) {
                0 -> draft = d.copy(metadataDb = "")
                options.lastIndex -> editing = Field.DB
                else -> draft = d.copy(metadataDb = options[i])
            }
        }
    }

    editing?.let { field ->
        EditTextDialog(
            title = field.label,
            hint = field.hint,
            initial = when (field) {
                Field.NAME -> d.name
                Field.FOLDER -> d.romFolder
                Field.EXT -> d.extensions
                Field.CORE_NAME -> d.coreName
                Field.CORE_PATH -> d.corePath
                Field.BIOS_DIR -> d.biosFolder
                Field.BIOS_FILES -> d.biosFiles
                Field.ART -> d.artworkFolder
                Field.DB -> d.metadataDb
            },
            onDismiss = { editing = null },
            onConfirm = { raw ->
                val v = raw.trim()
                draft = when (field) {
                    Field.NAME -> d.copy(name = v)
                    Field.FOLDER -> d.copy(romFolder = v.trimEnd('/'))
                    Field.EXT -> d.copy(extensions = StoragePaths.parseExtensions(v).joinToString(","))
                    Field.CORE_NAME -> d.copy(coreName = v)
                    Field.CORE_PATH -> d.copy(corePath = v)
                    Field.BIOS_DIR -> d.copy(biosFolder = v.trimEnd('/'))
                    Field.BIOS_FILES -> d.copy(biosFiles = v)
                    Field.ART -> d.copy(artworkFolder = v.trimEnd('/'))
                    Field.DB -> d.copy(metadataDb = v)
                }
                editing = null
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${d.name}?",
            text = "Removes the system and its game list from RetroHub (favourites and play history for it too). ROM files are not touched.",
            confirm = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; vm.deleteSystem(d) },
        )
    }
}

@Composable
private fun BiosStatus(d: SystemEntity, tick: Int) {
    val context = LocalContext.current
    val files = remember(d.biosFiles) { d.biosFiles.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() } }
    if (files.isEmpty()) {
        Text(
            "RetroArch loads BIOS files from its own System/BIOS directory; RetroHub cannot pass a BIOS path at launch. " +
                "List required files here to check they are present.",
            style = MaterialTheme.typography.bodySmall,
            color = RH.TextFaint,
        )
        return
    }
    if (d.biosFolder.isBlank()) {
        Text("Set the BIOS folder to verify these files.", style = MaterialTheme.typography.bodySmall.copy(color = RH.Gold))
        return
    }
    val canCheck = remember(tick) { StorageAccess.hasBroadFileAccess(context) } && !StoragePaths.isContentUri(d.biosFolder)
    val status = remember(d.biosFolder, files, tick, canCheck) {
        if (!canCheck) emptyMap() else files.associateWith { File(d.biosFolder, it).isFile }
    }
    Column {
        if (!canCheck) {
            Text("Grant storage access in Settings to verify BIOS files.", style = MaterialTheme.typography.bodySmall.copy(color = RH.Gold))
        }
        files.forEach { name ->
            val ok = status[name]
            InfoLine(
                name,
                when (ok) { true -> "✓ found"; false -> "✗ missing"; null -> "not checked" },
                when (ok) { true -> RH.Ok; false -> RH.Danger; null -> RH.TextDim },
            )
        }
    }
}

private fun Modifier.padding8(): Modifier = this.padding(top = 12.dp)
