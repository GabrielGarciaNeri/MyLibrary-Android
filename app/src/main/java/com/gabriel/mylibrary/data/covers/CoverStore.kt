package com.gabriel.mylibrary.data.covers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class CoverStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "covers")

    fun resolveFile(filename: String?): File? {
        if (filename == null || !FILE_NAME.matches(filename)) return null
        val candidate = File(directory, filename)
        return runCatching { candidate.takeIf { it.isFile && it.canonicalFile.parentFile == directory.canonicalFile } }.getOrNull()
    }

    fun delete(filename: String) {
        runCatching { resolveFile(filename)?.delete() }
    }

    suspend fun importImage(uri: Uri): String {
        var destination: File? = null
        try {
            return withContext(Dispatchers.IO) {
                if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) throw IOException("Cover storage is unavailable")
                val token = UUID.randomUUID().toString()
                val source = File(directory, "import_$token.tmp")
                val encoded = File(directory, "encoded_$token.tmp")
                var decoded: Bitmap? = null
                try {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(source).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            var length = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read == -1) break
                                length += read
                                if (length > MAX_SOURCE_BYTES) throw IOException("The selected image is too large")
                                output.write(buffer, 0, read)
                            }
                            if (length == 0L) throw IOException("The selected image is empty")
                        }
                    } ?: throw IOException("The selected image cannot be opened")
                    currentCoroutineContext().ensureActive()
                    decoded = if (Build.VERSION.SDK_INT >= 28) decodeModern(source) else decodeLegacy(source)
                    val bitmap = decoded ?: throw IOException("The selected file is not a readable image")
                    currentCoroutineContext().ensureActive()
                    val flattened = createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    try {
                        Canvas(flattened).apply {
                            drawColor(Color.WHITE)
                            drawBitmap(bitmap, 0f, 0f, null)
                        }
                        FileOutputStream(encoded).use { output ->
                            if (!flattened.compress(Bitmap.CompressFormat.JPEG, 86, output)) {
                                throw IOException("The image could not be saved")
                            }
                            output.fd.sync()
                        }
                    } finally {
                        flattened.recycle()
                    }
                    currentCoroutineContext().ensureActive()
                    val filename = "cover_$token.jpg"
                    val target = File(directory, filename)
                    destination = target
                    if (!encoded.renameTo(target)) throw IOException("The cover could not be stored")
                    filename
                } finally {
                    decoded?.recycle()
                    source.delete()
                    encoded.delete()
                }
            }
        } catch (error: Throwable) {
            destination?.delete()
            throw error
        }
    }

    @androidx.annotation.RequiresApi(28)
    private fun decodeModern(file: File): Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        validateSize(info.size.width, info.size.height)
        val factor = minOf(1.0, MAX_EDGE.toDouble() / max(info.size.width, info.size.height))
        decoder.setTargetSize(max(1, (info.size.width * factor).roundToInt()), max(1, (info.size.height * factor).roundToInt()))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setOnPartialImageListener { false }
    }

    private fun decodeLegacy(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        validateSize(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while ((max(bounds.outWidth, bounds.outHeight) + sample - 1) / sample > MAX_EDGE) sample *= 2
        var result = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: throw IOException("The selected file is not a readable image")
        try {
            val orientation = runCatching {
                ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                }
            }
            if (!matrix.isIdentity) {
                val oriented = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
                if (oriented !== result) result.recycle()
                result = oriented
            }
            val factor = minOf(1.0, MAX_EDGE.toDouble() / max(result.width, result.height))
            if (factor < 1.0) {
                val scaled = result.scale(max(1, (result.width * factor).roundToInt()), max(1, (result.height * factor).roundToInt()))
                if (scaled !== result) result.recycle()
                result = scaled
            }
            return result
        } catch (error: Throwable) {
            result.recycle()
            throw error
        }
    }

    private fun validateSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0 || width > 40_000 || height > 40_000 || width.toLong() * height > 300_000_000L) {
            throw IOException("The selected image dimensions are unsupported")
        }
    }

    private companion object {
        const val MAX_EDGE = 1200
        const val MAX_SOURCE_BYTES = 40L * 1024 * 1024
        val FILE_NAME = Regex("cover_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg")
    }
}
