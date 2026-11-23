package com.retrohub.tv.storage

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Storage model used by RetroHub TV:
 *
 * 1. Path folders (recommended): ROMs live in shared internal storage, e.g.
 *    /storage/emulated/0/ROMs/snes. Listing non-media files by path needs
 *      - Android 5–9:  READ_EXTERNAL_STORAGE
 *      - Android 10:   READ_EXTERNAL_STORAGE + requestLegacyExternalStorage (set in the manifest)
 *      - Android 11+:  "All files access" (MANAGE_EXTERNAL_STORAGE). Many TVs hide this settings
 *                      page; it can be granted over adb (see [adbGrantCommand]).
 *
 * 2. SAF folders: picked with ACTION_OPEN_DOCUMENT_TREE, persisted with
 *    takePersistableUriPermission and listed through DocumentsContract (no extra permission).
 *    RetroArch still needs a real path, so these only launch when the provider's document
 *    ID maps to one (see [StoragePaths]). Some TVs ship without a folder picker at all.
 *
 * RetroArch itself (com.retroarch.ra32 targets API 28 with requestLegacyExternalStorage) reads
 * shared storage by path once *its own* storage permission is granted, so open RetroArch once
 * and accept its storage prompt.
 */
object StorageAccess {

    fun primaryRoot(): String = Environment.getExternalStorageDirectory().absolutePath

    fun hasBroadFileAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun needsAllFilesAccess(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Opens the All-files access page. Returns false when the TV has no such settings screen. */
    fun openAllFilesAccessSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val candidates = listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }

    fun adbGrantCommand(context: Context): String =
        if (needsAllFilesAccess()) {
            "adb shell appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow"
        } else {
            "adb shell pm grant ${context.packageName} android.permission.READ_EXTERNAL_STORAGE"
        }

    fun persistedTreeCount(context: Context): Int =
        context.contentResolver.persistedUriPermissions.count { it.isReadPermission }

    fun persistTree(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun hasTreePermission(context: Context, uri: String): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri.toString() == uri && it.isReadPermission }

    /** Maps a picked tree URI to a path, or null when RetroArch could not open it. */
    fun treeToPath(treeUri: Uri): String? = runCatching {
        StoragePaths.documentIdToPath(treeUri.authority, DocumentsContract.getTreeDocumentId(treeUri), primaryRoot())
    }.getOrNull()

    fun describeFolder(context: Context, folder: String): FolderInfo {
        val value = folder.trim()
        if (value.isEmpty()) return FolderInfo(FolderKind.UNSET, null, "No ROM folder set.")
        if (StoragePaths.isContentUri(value)) {
            val uri = Uri.parse(value)
            val granted = hasTreePermission(context, value)
            val path = treeToPath(uri)
            val text = when {
                !granted -> "SAF folder, but the permission is gone. Pick the folder again."
                path != null -> "SAF folder → $path. RetroArch will be given this path."
                else -> "SAF folder from ${uri.authority}. It has no filesystem path, so RetroArch " +
                    "cannot open these games (it does not accept content:// URIs). Move the ROMs to " +
                    "internal storage and pick them from the \"Internal storage\" root, or type the path."
            }
            return FolderInfo(if (path != null) FolderKind.SAF_MAPPED else FolderKind.SAF_UNMAPPED, path, text, granted)
        }
        val access = hasBroadFileAccess(context)
        val text = if (access) {
            "Path folder. RetroArch receives the file paths directly."
        } else {
            "Path folder, but RetroHub cannot list it yet: grant storage access in Settings " +
                "(or pick the folder with the system folder picker)."
        }
        return FolderInfo(FolderKind.PATH, value, text, access)
    }
}

enum class FolderKind { UNSET, PATH, SAF_MAPPED, SAF_UNMAPPED }

data class FolderInfo(
    val kind: FolderKind,
    val path: String?,
    val description: String,
    val accessible: Boolean = false,
)
