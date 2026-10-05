package com.gabriel.mylibrary.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.data.covers.CoverStore
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.model.CoverPreset
import com.gabriel.mylibrary.model.CoverType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private val coverDecodeDispatcher = Dispatchers.IO.limitedParallelism(2)

@Composable
fun ItemCover(item: LibraryItem, modifier: Modifier = Modifier, contentDescription: String? = null) {
    ItemCover(item.coverType, item.coverPresetId, item.coverFileName, modifier, contentDescription)
}

@Composable
fun ItemCover(
    coverType: CoverType,
    presetId: String?,
    fileName: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val store = remember(context) { CoverStore(context) }
    val image by produceState<ImageBitmap?>(null, coverType, fileName) {
        value = null
        if (coverType == CoverType.CUSTOM_FILE) {
            value = withContext(coverDecodeDispatcher) {
                try {
                    val file = store.resolveFile(fileName) ?: return@withContext null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                        bounds.outWidth > 40_000 || bounds.outHeight > 40_000) return@withContext null
                    var sample = 1
                    while ((max(bounds.outWidth, bounds.outHeight) + sample - 1) / sample > 720) sample *= 2
                    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    val preset = if (coverType == CoverType.BUILT_IN) CoverPreset.fromId(presetId) else null
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        when {
            preset != null -> BuiltInCover(preset, Modifier.fillMaxSize())
            image != null -> Image(image!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.fillMaxSize(0.46f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun BuiltInCover(preset: CoverPreset, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).semantics {
        if (contentDescription != null) this.contentDescription = contentDescription
    }) {
        val w = size.width
        val h = size.height
        fun point(x: Float, y: Float) = Offset(w * x, h * y)
        fun polygon(vararg points: Pair<Float, Float>): Path = Path().apply {
            points.forEachIndexed { index, (x, y) -> if (index == 0) moveTo(w * x, h * y) else lineTo(w * x, h * y) }
            close()
        }
        when (preset) {
            CoverPreset.MIDNIGHT_ORBIT -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFF091A32), Color(0xFF264564))))
                for (ring in 1..4) drawCircle(Color(0xFF95C1D8).copy(alpha = 0.4f), w * (0.2f + ring * 0.17f), point(0.53f, 0.52f), style = Stroke(w * 0.009f))
                drawCircle(Color(0xFFEBB16F), w * 0.20f, point(0.53f, 0.52f))
                drawCircle(Color(0xFFF4D5AA), w * 0.052f, point(0.86f, 0.27f))
                for (i in 0..14) drawCircle(Color(0xFFCEE4EE), w * (if (i % 3 == 0) 0.009f else 0.005f), point(((i * 37 + 13) % 100) / 100f, ((i * 53 + 9) % 100) / 100f))
            }
            CoverPreset.TERRACOTTA_SUN -> {
                drawRect(Color(0xFFF0DBB8))
                drawCircle(Color(0xFFC86643), w * 0.30f, point(0.64f, 0.32f))
                drawPath(Path().apply { moveTo(0f, h * 0.56f); cubicTo(w * 0.35f, h * 0.30f, w * 0.60f, h * 0.90f, w, h * 0.53f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFFD69669))
                drawPath(Path().apply { moveTo(0f, h * 0.82f); cubicTo(w * 0.20f, h * 0.57f, w * 0.65f, h * 0.60f, w, h * 0.86f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF743E36))
                drawLine(Color(0xFF743E36), point(0.12f, 0.14f), point(0.36f, 0.14f), w * 0.014f)
            }
            CoverPreset.FOREST_PATH -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFBDCCA9), Color(0xFF253F34))))
                drawCircle(Color(0xFFE5D8A7), w * 0.15f, point(0.72f, 0.25f))
                for (i in 0..6) {
                    val x = i * 0.19f - 0.06f
                    val peak = 0.26f + (i % 3) * 0.10f
                    drawPath(polygon(x to peak, (x - 0.19f) to 0.90f, (x + 0.19f) to 0.90f), if (i % 2 == 0) Color(0xFF284B3B) else Color(0xFF477052))
                }
                drawPath(Path().apply { moveTo(w * 0.46f, h * 0.55f); cubicTo(w * 0.77f, h * 0.76f, w * 0.10f, h * 0.83f, w * 0.61f, h * 1.08f) }, Color(0xFFD9C7A0), style = Stroke(w * 0.085f, cap = StrokeCap.Round))
            }
            CoverPreset.INDIGO_WAVES -> {
                drawRect(Color(0xFF17244D))
                val colors = listOf(0xFF334F88, 0xFF567AA4, 0xFF8EADBD, 0xFFCCD7D7)
                colors.forEachIndexed { index, color ->
                    val y = h * (0.25f + index * 0.19f)
                    drawPath(Path().apply { moveTo(-w * 0.2f, y); cubicTo(w * 0.18f, y - h * 0.3f, w * 0.45f, y + h * 0.3f, w * 1.2f, y - h * 0.08f); lineTo(w * 1.2f, h * 1.2f); lineTo(-w * 0.2f, h * 1.2f); close() }, Color(color))
                }
                drawCircle(Color(0xFFE9D49A), w * 0.09f, point(0.77f, 0.16f))
            }
            CoverPreset.ROSE_GEOMETRY -> {
                drawRect(Color(0xFFF0CFCD))
                drawPath(polygon(0f to 0.08f, 1f to 0.45f, 0f to 0.74f), Color(0xFF9D4F68))
                drawPath(polygon(1f to 0.12f, 0.2f to 0.60f, 1f to 0.98f), Color(0xFFCF8992))
                drawPath(polygon(0.25f to 0.20f, 0.90f to 0.66f, 0.16f to 0.95f), Color(0xFF653C59).copy(alpha = 0.9f))
                drawCircle(Color(0xFFF8E8CF), w * 0.21f, point(0.48f, 0.43f), style = Stroke(w * 0.018f))
            }
            CoverPreset.GOLDEN_ARCH -> {
                drawRect(Color(0xFF302D38))
                for (i in 0..6) {
                    val left = w * (0.08f + i * 0.057f)
                    val right = w - left
                    val top = h * (0.15f + i * 0.038f)
                    drawPath(Path().apply { moveTo(left, h * 0.94f); lineTo(left, top + w * 0.42f); cubicTo(left, top - h * 0.08f, right, top - h * 0.08f, right, top + w * 0.42f); lineTo(right, h * 0.94f) }, Color(0xFFE7B95F), style = Stroke(w * 0.015f, cap = StrokeCap.Round))
                }
                drawCircle(Color(0xFFF3D691), w * 0.06f, point(0.5f, 0.61f))
            }
            CoverPreset.TEAL_MOSAIC -> {
                drawRect(Color(0xFF163F4B))
                val colors = listOf(Color(0xFF438A8F), Color(0xFFB2D2C8), Color(0xFFE3BA74), Color(0xFF245D6A))
                for (row in 0..5) for (column in 0..3) {
                    val x = column * w / 4
                    val y = row * h / 6
                    val color = colors[(row * 3 + column) % colors.size]
                    if ((row + column) % 3 == 0) drawArc(color, 0f, 270f, true, Offset(x + w * 0.015f, y + h * 0.01f), Size(w * 0.22f, h * 0.146f))
                    else drawRoundRect(color, Offset(x + w * 0.015f, y + h * 0.01f), Size(w * 0.22f, h * 0.146f), CornerRadius(w * 0.024f))
                }
            }
            CoverPreset.LAVENDER_MOON -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF71678F), Color(0xFFB9B0CA))))
                drawCircle(Color(0xFFF3EBD8), w * 0.31f, point(0.53f, 0.36f))
                drawCircle(Color(0xFF867B9E), w * 0.28f, point(0.66f, 0.29f))
                drawPath(polygon(0f to 0.77f, 0.26f to 0.59f, 0.66f to 0.84f, 1f to 0.66f, 1f to 1f, 0f to 1f), Color(0xFF615D7B))
                drawPath(polygon(0f to 0.95f, 0.39f to 0.75f, 0.82f to 0.93f, 1f to 0.83f, 1f to 1f, 0f to 1f), Color(0xFF3E3E5A))
            }
            CoverPreset.CORAL_LINES -> {
                drawRect(Color(0xFFF2D7BF))
                rotate(-24f) {
                    for (i in -4..12) drawLine(if (i % 3 == 0) Color(0xFFA4433E) else Color(0xFFDD7C61), Offset(w * i * 0.13f, -h), Offset(w * i * 0.13f, h * 2f), w * 0.056f)
                }
                drawRoundRect(Color(0xFFF8EBDD), point(0.25f, 0.31f), Size(w * 0.50f, h * 0.38f), CornerRadius(w * 0.24f))
                drawCircle(Color(0xFFAB5047), w * 0.09f, point(0.5f, 0.5f))
            }
            CoverPreset.SAGE_LEAVES -> {
                drawRect(Color(0xFFE4E3CF))
                drawPath(Path().apply { moveTo(w * 0.30f, h * 1.05f); cubicTo(w * 0.75f, h * 0.6f, w * 0.35f, h * 0.5f, w * 0.62f, h * 0.08f) }, Color(0xFF496854), style = Stroke(w * 0.023f, cap = StrokeCap.Round))
                for (i in 0..5) {
                    val y = 0.20f + i * 0.12f
                    val x = 0.55f - (i % 2) * 0.06f
                    val side = if (i % 2 == 0) 1 else -1
                    drawPath(Path().apply { moveTo(w * x, h * y); quadraticTo(w * (x + side * 0.4f), h * (y - 0.12f), w * (x + side * 0.29f), h * (y + 0.10f)); quadraticTo(w * (x + side * 0.08f), h * (y + 0.12f), w * x, h * y); close() }, if (i % 2 == 0) Color(0xFF6E8867) else Color(0xFF9CAF88))
                }
            }
            CoverPreset.BLUE_HORIZON -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFC6DBE2), Color(0xFF6FA1B6))))
                drawCircle(Color(0xFFF4E5BE), w * 0.19f, point(0.39f, 0.34f))
                drawPath(polygon(0f to 0.59f, 0.33f to 0.38f, 0.63f to 0.65f, 0.87f to 0.46f, 1f to 0.57f, 1f to 1f, 0f to 1f), Color(0xFF4D7D95))
                drawRect(Color(0xFF285971), point(0f, 0.70f), Size(w, h * 0.3f))
                for (i in 0..5) drawLine(Color(0xFF87B3C1).copy(alpha = 0.7f), point(0.08f + i * 0.04f, 0.75f + i * 0.037f), point(0.90f - i * 0.05f, 0.75f + i * 0.037f), w * 0.008f)
            }
            CoverPreset.PLUM_STARS -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFF321F45), Color(0xFF69395F))))
                for (i in 0..11) {
                    val x = ((i * 37 + 19) % 100) / 100f
                    val y = ((i * 29 + 12) % 100) / 100f
                    val r = if (i % 3 == 0) 0.085f else 0.038f
                    drawPath(polygon(x to (y - r * 0.66f), (x + r * 0.3f) to (y - r * 0.2f), (x + r) to y, (x + r * 0.3f) to (y + r * 0.2f), x to (y + r * 0.66f), (x - r * 0.3f) to (y + r * 0.2f), (x - r) to y, (x - r * 0.3f) to (y - r * 0.2f)), if (i % 2 == 0) Color(0xFFE4BA90) else Color(0xFFBC8FAF))
                }
            }
        }
        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.14f), Color.Transparent)), size = Size(w * 0.055f, h))
    }
}

@Composable
fun CoverGalleryDialog(selectedPresetId: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxHeight = with(LocalDensity.current) { windowHeight.toDp() } * 0.82f
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight)) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.cover_gallery_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.cover_gallery_description), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(88.dp), modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(CoverPreset.entries, key = { it.id }) { preset ->
                        val selected = preset.id == selectedPresetId
                        val label = stringResource(preset.labelResource)
                        Column(Modifier.selectable(selected, role = Role.RadioButton, onClick = { onSelect(preset.id) }),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                                Box(Modifier.padding(3.dp).fillMaxWidth().aspectRatio(2f / 3f)) {
                                    BuiltInCover(preset, Modifier.fillMaxSize())
                                    if (selected) Surface(Modifier.align(Alignment.TopEnd).padding(4.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.padding(2.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cover_gallery_close)) }
                }
            }
        }
    }
}
