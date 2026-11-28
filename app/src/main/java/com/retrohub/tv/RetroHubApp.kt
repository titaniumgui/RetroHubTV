package com.retrohub.tv

import com.retrohub.tv.i18n.AppLanguage
import com.retrohub.tv.i18n.I18n
import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.retrohub.tv.data.AppDatabase
import com.retrohub.tv.data.SettingsStore
import com.retrohub.tv.launch.RetroArchLauncher
import com.retrohub.tv.metadata.MetadataService
import com.retrohub.tv.detect.GameDiscovery
import com.retrohub.tv.detect.RetroArchDetector
import com.retrohub.tv.detect.RetroArchStorage
import com.retrohub.tv.perf.DeviceMemory
import com.retrohub.tv.perf.MemoryProfile
import com.retrohub.tv.scan.RomScanner

/** Manual DI container: no reflection-based frameworks, nothing started in the background. */
class RetroHubApp : Application(), ImageLoaderFactory {

    val database by lazy { AppDatabase.create(this) }
    val settingsStore by lazy { SettingsStore(this) }
    val scanner by lazy { RomScanner(this, database) }
    val launcher by lazy { RetroArchLauncher(this) }
    val metadata by lazy { MetadataService(this, database) }
    val retroArchDetector by lazy { RetroArchDetector(this, launcher) }
    val retroArchStorage by lazy { RetroArchStorage(this) }
    val discovery by lazy { GameDiscovery(retroArchStorage) }
    val memory: DeviceMemory by lazy { MemoryProfile.read(this) }

    /** Fixed for the process lifetime (the image cache is sized from it); changes apply on restart. */
    val isLite: Boolean by lazy { MemoryProfile.isLite(settingsStore.current.liteMode, memory) }

    private var imageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        AppLanguage.fromCode(settingsStore.current.language)?.let(I18n::apply)
    }

    override fun newImageLoader(): ImageLoader {
        val lite = isLite
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(MemoryProfile.imageMemoryCacheBytes(lite, memory))
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork"))
                    .maxSizeBytes(MemoryProfile.imageDiskCacheBytes(lite))
                    .build()
            }
            .allowRgb565(true) // half the memory of ARGB_8888 for opaque cover art
            .crossfade(!lite)
            .respectCacheHeaders(false)
            .build()
            .also { imageLoader = it }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // UI hidden usually means RetroArch is now in front: hand every decoded bitmap back to it.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            imageLoader?.memoryCache?.clear()
        }
    }
}
