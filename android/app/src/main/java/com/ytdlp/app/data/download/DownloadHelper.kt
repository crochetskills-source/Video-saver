package com.ytdlp.app.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object DownloadHelper {

    fun downloadFileToDevice(
        context: Context,
        fileUrl: String,
        suggestedFileName: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(fileUrl))
                .setTitle(suggestedFileName)
                .setDescription("Downloading via yt-dlp Railway Server")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    suggestedFileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Saving to Downloads folder...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
