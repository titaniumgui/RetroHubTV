package com.retrohub.tv

import com.retrohub.tv.storage.StoragePaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoragePathsTest {
    private val primary = "/storage/emulated/0"

    @Test fun primaryVolumeMapsToPath() {
        assertEquals(
            "/storage/emulated/0/ROMs/snes",
            StoragePaths.documentIdToPath(StoragePaths.EXTERNAL_STORAGE_AUTHORITY, "primary:ROMs/snes", primary),
        )
    }

    @Test fun primaryRootMapsToRoot() {
        assertEquals(primary, StoragePaths.documentIdToPath(StoragePaths.EXTERNAL_STORAGE_AUTHORITY, "primary:", primary))
    }

    @Test fun removableVolumeMapsToStorageId() {
        assertEquals(
            "/storage/1A2B-3C4D/games/a.zip",
            StoragePaths.documentIdToPath(StoragePaths.EXTERNAL_STORAGE_AUTHORITY, "1A2B-3C4D:games/a.zip", primary),
        )
    }

    @Test fun downloadsRawIdMapsToPath() {
        assertEquals(
            "/storage/emulated/0/Download/x.zip",
            StoragePaths.documentIdToPath(StoragePaths.DOWNLOADS_AUTHORITY, "raw:/storage/emulated/0/Download/x.zip", primary),
        )
    }

    @Test fun otherProvidersHaveNoPath() {
        assertNull(StoragePaths.documentIdToPath("com.google.android.apps.docs.storage", "primary:x", primary))
        assertNull(StoragePaths.documentIdToPath(StoragePaths.DOWNLOADS_AUTHORITY, "msf:42", primary))
    }

    @Test fun extensionsAreNormalised() {
        assertEquals(setOf("sfc", "smc", "zip"), StoragePaths.parseExtensions(" .SFC, smc;zip ,"))
    }

    @Test fun titlesAreCleaned() {
        assertEquals("Super Mario World (USA)", StoragePaths.titleFromFileName("Super_Mario_World (USA).sfc"))
        assertEquals("sfc", StoragePaths.extensionOf("a.b.SFC"))
        assertEquals("", StoragePaths.extensionOf(".hidden"))
    }
}
