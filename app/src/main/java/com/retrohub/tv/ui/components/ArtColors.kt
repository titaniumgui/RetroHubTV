package com.retrohub.tv.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.retrohub.tv.data.GameTile
import com.retrohub.tv.ui.theme.RH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Picks a background colour from a game's artwork for the Games tab.
 * Decodes the cover at 24×24 (about 2 KB), takes the most vivid hue, then tones it down so
 * white text stays readable. Results are cached per image, so revisiting a game is free.
 */
object ArtColors {
    private val cache = LruCache<String, Int>(128)

    suspend fun of(context: Context, tile: GameTile): Color {
        val location = tile.coverPath ?: tile.screenshotPath ?: return fallback(tile.title)
        cache.get(location)?.let { return Color(it) }
        val model = imageModel(location) ?: return fallback(tile.title)
        val bitmap = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(model)
                .size(24, 24)
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
        }
        val argb = bitmap?.let { tone(dominantVividColor(it)) } ?: return fallback(tile.title)
        cache.put(location, argb)
        return Color(argb)
    }

    /** Games without artwork get a stable colour from their title, like their title card. */
    fun fallback(title: String): Color {
        val base = RH.ProfileColors[(title.hashCode() and 0x7fffffff) % RH.ProfileColors.size]
        return Color(tone(android.graphics.Color.argb(255, (base.red * 255).toInt(), (base.green * 255).toInt(), (base.blue * 255).toInt())))
    }

    /** Hue histogram weighted by saturation × brightness; greys and near-blacks are ignored. */
    private fun dominantVividColor(bmp: Bitmap): Int {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h).also { bmp.getPixels(it, 0, w, 0, 0, w, h) }
        val buckets = DoubleArray(18)
        val sumHue = DoubleArray(18)
        val sumSat = DoubleArray(18)
        val hsv = FloatArray(3)
        var avgR = 0L; var avgG = 0L; var avgB = 0L
        for (p in pixels) {
            avgR += (p shr 16) and 0xFF; avgG += (p shr 8) and 0xFF; avgB += p and 0xFF
            android.graphics.Color.colorToHSV(p, hsv)
            if (hsv[1] < 0.22f || hsv[2] < 0.18f) continue
            val weight = (hsv[1] * hsv[2]).toDouble()
            val b = ((hsv[0] / 360f) * buckets.size).toInt().coerceIn(0, buckets.size - 1)
            buckets[b] += weight
            sumHue[b] += hsv[0] * weight
            sumSat[b] += hsv[1] * weight
        }
        val best = buckets.indices.maxByOrNull { buckets[it] } ?: 0
        if (buckets[best] <= 0.0) {
            val n = pixels.size.coerceAtLeast(1)
            return android.graphics.Color.rgb((avgR / n).toInt(), (avgG / n).toInt(), (avgB / n).toInt())
        }
        val hue = (sumHue[best] / buckets[best]).toFloat()
        val sat = (sumSat[best] / buckets[best]).toFloat()
        return android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.6f))
    }

    /** Deep, rich version of the colour: readable under white text, never neon. */
    private fun tone(argb: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] = (hsv[1] * 1.1f).coerceIn(0.25f, 0.85f)
        hsv[2] = 0.46f
        return android.graphics.Color.HSVToColor(hsv)
    }
}
