package com.retrohub.tv.ui.components

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.retrohub.tv.i18n.tr
import com.retrohub.tv.i18n.trCount
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.retrohub.tv.R
import com.retrohub.tv.data.GameTile
import com.retrohub.tv.data.ProfileEntity
import com.retrohub.tv.ui.theme.LocalLite
import com.retrohub.tv.ui.theme.RH
import java.io.File

// ------------------------------------------------------------------ brand

/** White controller + "RetroHub TV" wordmark. Always left to right: it's a name, even in Arabic. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, height: Dp = 22.dp) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_controller),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(width = height * 1.5f, height = height),
            )
            Spacer(Modifier.width(12.dp))
            Text("RetroHub", fontSize = (height.value * 0.95f).sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            Text(" TV", fontSize = (height.value * 0.95f).sp, fontWeight = FontWeight.Light)
        }
    }
}

// ------------------------------------------------------------------ focusable building blocks

/** Card-like focusable: white focus ring, gentle zoom (none in Lite mode). */
@Composable
fun RhSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    container: Color = RH.Surface,
    focusedContainer: Color = RH.SurfaceHi,
    focusedScale: Float = 1.06f,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val lite = LocalLite.current
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = RH.Text,
            focusedContainerColor = focusedContainer,
            focusedContentColor = RH.Text,
            pressedContainerColor = focusedContainer,
            pressedContentColor = RH.Text,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else focusedScale),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
        ),
        content = content,
    )
}

/** Pill button: dark when idle, white with black text when focused. */
@Composable
fun RhButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    /** Translucent "glass" fill, for buttons that sit on top of artwork. */
    glass: Boolean = false,
) {
    val lite = LocalLite.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                glass -> Color.White.copy(alpha = if (primary) 0.24f else 0.14f)
                primary -> Color.White.copy(alpha = 0.16f)
                else -> Color.White.copy(alpha = 0.10f)
            },
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
            pressedContainerColor = Color(0xFFDDDDDD),
            pressedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else 1.05f),
    ) {
        Row(
            Modifier.padding(horizontal = if (icon != null) 18.dp else 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                if (text.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (text.isNotEmpty()) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Round icon-only button (top bar). */
@Composable
fun RhIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lite = LocalLite.current
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.85f),
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else 1.08f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
        }
    }
}

// ------------------------------------------------------------------ profiles

@Composable
fun Avatar(profile: ProfileEntity?, size: Dp, modifier: Modifier = Modifier) {
    val color = RH.profileColor(profile?.colorIndex ?: 0)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.65f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            profile?.name?.trim()?.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ------------------------------------------------------------------ artwork

/** Horizontal gradient from the reading-start edge: left in English, right in Arabic. */
@Composable
fun startScrim(vararg stops: Pair<Float, Color>): Brush {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return Brush.horizontalGradient(
        *stops,
        startX = if (rtl) Float.POSITIVE_INFINITY else 0f,
        endX = if (rtl) 0f else Float.POSITIVE_INFINITY,
    )
}

fun gamesLabel(count: Int): String = trCount(count, "{0} game", "{0} games")

/** Converts a stored image location (file path or content:// URI) into a Coil model. */
fun imageModel(location: String?): Any? = when {
    location.isNullOrBlank() -> null
    location.startsWith("content://") -> Uri.parse(location)
    else -> File(location)
}

/** Bounded thumbnail: requested at its on-screen pixel size, so bitmaps never exceed it. */
@Composable
fun Thumbnail(location: String?, title: String, width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val model = imageModel(location)
    if (model == null) {
        Monogram(title, modifier.size(width, height))
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val lite = LocalLite.current
    val request = remember(location, width, height, lite) {
        val f = if (lite) 0.75f else 1f
        val w = with(density) { (width.toPx() * f).toInt() }.coerceAtLeast(1)
        val h = with(density) { (height.toPx() * f).toInt() }.coerceAtLeast(1)
        ImageRequest.Builder(context).data(model).size(w, h).precision(Precision.INEXACT).build()
    }
    AsyncImage(
        model = request,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(width, height).background(RH.SurfaceHi),
    )
}

/** Fallback cover: dark gradient with the title, so a library without artwork still looks designed. */
@Composable
fun Monogram(title: String, modifier: Modifier = Modifier) {
    val hue = RH.ProfileColors[(title.hashCode() and 0x7fffffff) % RH.ProfileColors.size]
    Box(
        modifier.background(Brush.verticalGradient(listOf(hue.copy(alpha = 0.35f), Color(0xFF0D0D0D)))),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp),
        )
    }
}

/** Full-bleed background image faded into black (skipped in Lite mode). */
@Composable
fun Backdrop(location: String?, modifier: Modifier = Modifier, alpha: Float = 0.55f) {
    if (LocalLite.current) return
    val model = imageModel(location) ?: return
    val context = LocalContext.current
    val request = remember(location) {
        ImageRequest.Builder(context).data(model).size(960, 540).precision(Precision.INEXACT).build()
    }
    Box(modifier) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = alpha,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(startScrim(0f to Color.Black, 0.55f to Color.Black.copy(alpha = 0.55f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black)))
    }
}

val CardWidth = 128.dp
val CardHeight = 176.dp

/** Cover card with its title underneath; the title brightens while the card has focus. */
@Composable
fun GameCard(
    tile: GameTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(CardWidth)) {
        RhSurface(
            onClick = onClick,
            modifier = modifier
                .size(CardWidth, CardHeight)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            container = RH.SurfaceHi,
        ) {
            Thumbnail(tile.coverPath, tile.title, CardWidth, CardHeight)
            if (tile.favorite) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) { Text("★", color = RH.Gold, fontSize = 13.sp) }
            }
        }
        Text(
            tile.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (focused) RH.Text else RH.TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
    }
}

val WideWidth = 236.dp
val WideHeight = 133.dp

/**
 * Top-shelf tile: 16:9 screenshot (falls back to the cover, then a title card). On focus it
 * lifts — zoom plus a soft drop shadow — and its title brightens. Lite mode keeps only the ring.
 */
@Composable
fun WideGameTile(
    tile: GameTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val lite = LocalLite.current
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.width(WideWidth)) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .size(WideWidth, WideHeight)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = RH.SurfaceHi,
                focusedContainerColor = RH.SurfaceHi,
                contentColor = Color.White,
                focusedContentColor = Color.White,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = if (lite) 1f else 1.1f),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.5.dp, Color.White), shape = shape)),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = if (lite) Glow.None else Glow(elevationColor = Color.Black, elevation = 18.dp),
            ),
        ) {
            Thumbnail(tile.screenshotPath ?: tile.coverPath, tile.title, WideWidth, WideHeight)
            if (tile.favorite) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) { Text("★", color = RH.Gold, fontSize = 13.sp) }
            }
        }
        Text(
            tile.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) RH.Text else Color.Transparent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp, start = 2.dp, end = 2.dp),
        )
    }
}

// ------------------------------------------------------------------ text & rows

/** Translucent panel fills: near-black on the black screens, tinted glass on the Settings gradient. */
val PanelFill = Color.White.copy(alpha = 0.065f)
val PanelFocused = Color.White.copy(alpha = 0.14f)
val PanelLine = Color.White.copy(alpha = 0.10f)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(bottom = 12.dp))
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = RH.TextDim,
        modifier = modifier
            .border(1.dp, RH.Line, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/** A label / value row that opens an editor when clicked. */
@Composable
fun FieldRow(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    RhSurface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), container = PanelFill, focusedContainer = PanelFocused, focusedScale = 1.01f) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = RH.TextFaint)
            Spacer(Modifier.height(2.dp))
            Text(
                value.ifBlank { tr("Not set") },
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                color = if (value.isBlank()) RH.TextFaint else RH.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = RH.TextFaint)
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    RhSurface(onClick = onToggle, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), container = PanelFill, focusedContainer = PanelFocused, focusedScale = 1.01f) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = RH.TextFaint)
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier
                    .size(width = 46.dp, height = 26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (checked) Color.White else RH.TrackOff),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (checked) Color.Black else Color(0xFF8A8A8A)),
                )
            }
        }
    }
}

@Composable
fun InfoLine(label: String, value: String, valueColor: Color = RH.Text, mono: Boolean = false) {
    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RH.TextFaint, modifier = Modifier.width(140.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default, textDirection = TextDirection.Content),
            color = valueColor,
        )
    }
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier, color: Color = RH.TextDim) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelFill)
            .border(1.dp, PanelLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
