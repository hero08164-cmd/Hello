package com.bharatcloud.myvideoeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Environment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import com.google.common.collect.ImmutableList
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class TextClip(val id: Int, val text: String, val startSeconds: Float, val endSeconds: Float, val style: Int)

@UnstableApi
internal object VideoExporter {
    fun export(context: Context, input: android.net.Uri, clips: List<TextClip>, onProgress: (Int) -> Unit, onComplete: (File) -> Unit, onError: (String) -> Unit) {
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.cacheDir
        if (!folder.exists()) folder.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val output = File(folder, "Typography_$stamp.mp4")
        val overlays = clips.map { TimedTextOverlay(it) }
        val effects = Effects(emptyList(), listOf(OverlayEffect(ImmutableList.copyOf(overlays))))
        val edited = EditedMediaItem.Builder(MediaItem.fromUri(input)).setEffects(effects).build()
        val transformer = Transformer.Builder(context).addListener(object : Transformer.Listener {
            override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                onProgress(100)
                onComplete(output)
            }
            override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                onError(exportException.message ?: "Video export failed")
            }
        }).build()
        transformer.start(edited, output.absolutePath)
        onProgress(1)
    }
}

@UnstableApi
private class TimedTextOverlay(private val clip: TextClip) : CanvasOverlay() {
    private var width = 1080
    private var height = 1920
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (clip.style == 2) android.graphics.Color.rgb(34, 211, 197) else android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = if (clip.style == 3) 66f else 78f
        typeface = Typeface.create(if (clip.style == 1 || clip.style == 3) "sans-serif-medium" else "sans-serif-black", Typeface.NORMAL)
        setShadowLayer(if (clip.style == 2) 0f else 8f, 2f, 3f, android.graphics.Color.BLACK)
        style = if (clip.style == 2) Paint.Style.STROKE else Paint.Style.FILL
        strokeWidth = if (clip.style == 2) 3.5f else 0f
    }
    override fun setCanvasSize(width: Int, height: Int) { this.width = width; this.height = height }
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val seconds = presentationTimeUs / 1_000_000f
        if (seconds < clip.startSeconds || seconds > clip.endSeconds) return
        val lines = clip.text.split("\n")
        val lineHeight = paint.textSize * 1.18f
        val firstBaseline = height / 2f - (lines.size - 1) * lineHeight / 2f
        lines.forEachIndexed { index, line -> canvas.drawText(line, width / 2f, firstBaseline + index * lineHeight, paint) }
    }
}
