package com.retrohub.tv.perf

import android.app.ActivityManager
import android.content.Context
import com.retrohub.tv.data.LiteMode

data class DeviceMemory(val totalMb: Long, val memoryClassMb: Int, val lowRamDevice: Boolean)

/**
 * Memory-aware behaviour. Numbers are design targets, not measurements: tune them on real
 * 1–2 GB hardware (adb shell dumpsys meminfo com.retrohub.tv).
 */
object MemoryProfile {
    fun read(context: Context): DeviceMemory {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return DeviceMemory(
            totalMb = info.totalMem / (1024 * 1024),
            memoryClassMb = am.memoryClass,
            lowRamDevice = am.isLowRamDevice,
        )
    }

    /** Auto: Lite on Android Go / low-RAM flagged devices and anything under ~1.5 GB usable RAM. */
    fun isLite(mode: LiteMode, mem: DeviceMemory): Boolean = when (mode) {
        LiteMode.ON -> true
        LiteMode.OFF -> false
        LiteMode.AUTO -> mem.lowRamDevice || mem.totalMb < 1600 || mem.memoryClassMb <= 96
    }

    /** Decoded-bitmap memory cache budget. */
    fun imageMemoryCacheBytes(lite: Boolean, mem: DeviceMemory): Int {
        val cap = if (lite) 6 else 16
        // Never more than 1/8 of the per-app heap.
        val byHeap = mem.memoryClassMb / 8
        return (minOf(cap, byHeap).coerceAtLeast(2)) * 1024 * 1024
    }

    fun imageDiskCacheBytes(lite: Boolean): Long = if (lite) 24L shl 20 else 64L shl 20
}
