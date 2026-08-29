package com.dusko.tiktokssolo

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import java.io.File

/** Creates a first local montage from media selected from the Android/Samsung Gallery. */
class MediaMontage(private val context: Context) {
    fun export(media: List<Uri>, durationSeconds: Int, onDone: (Result<File>) -> Unit) {
        if (media.isEmpty()) {
            onDone(Result.failure(IllegalArgumentException("Aucun média sélectionné")))
            return
        }
        val outDir = File(context.getExternalFilesDir(null), "TikTokSolo")
        outDir.mkdirs()
        val output = File(outDir, "tiktok_solo_${System.currentTimeMillis()}.mp4")
        val items = media.map { uri -> EditedMediaItem.Builder(MediaItem.fromUri(uri)).build() }
        val sequence = EditedMediaItemSequence.Builder(items).build()
        val composition = Composition.Builder(sequence).build()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType("video/avc")
            .setAudioMimeType("audio/mp4a-latm")
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onDone(Result.success(output))
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    output.delete()
                    onDone(Result.failure(exportException))
                }
            }).build()
        transformer.start(composition, output.absolutePath)
    }
}
