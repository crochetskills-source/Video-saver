package com.ytdlp.app.data.models

import com.google.gson.annotations.SerializedName

data class ExtractRequest(
    @SerializedName("url") val url: String
)

data class DownloadRequest(
    @SerializedName("url") val url: String,
    @SerializedName("format_selector") val formatSelector: String = "bestvideo*+bestaudio/best",
    @SerializedName("audio_only") val audioOnly: Boolean = false,
    @SerializedName("audio_format") val audioFormat: String? = "mp3",
    @SerializedName("audio_quality") val audioQuality: String? = "320",
    @SerializedName("merge_output_format") val mergeOutputFormat: String? = "mp4",
    @SerializedName("embed_thumbnail") val embedThumbnail: Boolean = true,
    @SerializedName("embed_subtitles") val embedSubtitles: Boolean = false,
    @SerializedName("subtitles_langs") val subtitlesLangs: List<String>? = null,
    @SerializedName("embed_metadata") val embedMetadata: Boolean = true,
    @SerializedName("sponsorblock_remove") val sponsorblockRemove: List<String>? = null,
    @SerializedName("clip_start") val clipStart: String? = null,
    @SerializedName("clip_end") val clipEnd: String? = null
)

data class DownloadResponse(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("status") val status: String
)
